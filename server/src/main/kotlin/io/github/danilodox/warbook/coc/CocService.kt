package io.github.danilodox.warbook.coc

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Ticker
import io.github.danilodox.warbook.api.ApiErrorCode
import io.github.danilodox.warbook.api.ClanDto
import io.github.danilodox.warbook.api.ClanSearch
import io.github.danilodox.warbook.api.ClanSearchResponse
import io.github.danilodox.warbook.api.CurrentWarDto
import io.github.danilodox.warbook.api.CwlDto
import io.github.danilodox.warbook.api.CwlState
import io.github.danilodox.warbook.api.PlayerDto
import io.github.danilodox.warbook.api.WarState
import io.github.danilodox.warbook.errors.ApiException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

/**
 * Busca na API do jogo e guarda o resultado em memória, para poupar o limite da chave.
 * Só sucessos entram no cache; erros sempre consultam de novo.
 */
class CocService(private val client: CocClient, ticker: Ticker = Ticker.systemTicker()) {
    private val players = cache<PlayerDto>(PLAYER_TTL, ticker)
    private val clans = cache<ClanDto>(CLAN_TTL, ticker)
    private val clanSearches = cache<ClanSearchResponse>(CLAN_TTL, ticker)
    private val wars = cache<CurrentWarDto>(WAR_TTL, ticker)
    private val cwls = cache<CwlDto>(WAR_TTL, ticker)

    /** Guerra da CWL encerrada não muda mais: fica bem mais tempo, pelo warTag. */
    private val endedLeagueWars = cache<CocWar>(ENDED_LEAGUE_WAR_TTL, ticker)
    private val leagueWarFetches = Semaphore(MAX_PARALLEL_LEAGUE_WARS)

    suspend fun player(tag: String): PlayerDto = players.getOrFetch(tag) { client.player(tag).toDto() }

    suspend fun clan(tag: String): ClanDto = clans.getOrFetch(tag) { client.clan(tag).toDto() }

    /** [name] já validado por [ClanSearch.normalize]. Maiúsculas e minúsculas dividem o mesmo cache. */
    suspend fun searchClans(name: String): ClanSearchResponse = clanSearches.getOrFetch(name.lowercase()) {
        ClanSearchResponse(client.searchClans(name, ClanSearch.MAX_RESULTS).items.map { it.toSearchResult() })
    }

    suspend fun currentWar(clanTag: String): CurrentWarDto =
        wars.getOrFetch(clanTag) { client.currentWar(clanTag).toDto() }

    /**
     * Grupo + guerra de cada rodada sorteada (até 7 × 4 na primeira vez; depois só as que não acabaram).
     * Fora da CWL a API responde 404, que aqui vira [CwlState.NOT_IN_CWL].
     */
    suspend fun currentCwl(clanTag: String): CwlDto = cwls.getOrFetch(clanTag) {
        val group = try {
            client.leagueGroup(clanTag)
        } catch (e: ApiException) {
            if (e.code == ApiErrorCode.NOT_FOUND) return@getOrFetch CwlDto(CwlState.NOT_IN_CWL)
            throw e
        }
        val warTags = if (cocCwlState(group.state) == CwlState.NOT_IN_CWL) emptyList() else group.drawnWarTags()
        val leagueWars = coroutineScope { warTags.map { tag -> async { tag to leagueWar(tag) } }.awaitAll() }
        group.toDto(clanTag, leagueWars.toMap())
    }

    /** Sempre consulta a API: o token do jogo vale uma vez só. */
    suspend fun verifyToken(tag: String, token: String): Boolean = client.verifyToken(tag, token)

    private suspend fun leagueWar(warTag: String): CocWar = endedLeagueWars.getIfPresent(warTag)
        ?: leagueWarFetches.withPermit { client.leagueWar(warTag) }.also {
            if (cocWarState(it.state) == WarState.WAR_ENDED) endedLeagueWars.put(warTag, it)
        }

    private suspend fun <T : Any> Cache<String, T>.getOrFetch(key: String, fetch: suspend () -> T): T =
        getIfPresent(key) ?: fetch().also { put(key, it) }

    companion object {
        val WAR_TTL: Duration = 1.minutes
        val PLAYER_TTL: Duration = 2.minutes
        val CLAN_TTL: Duration = 5.minutes
        val ENDED_LEAGUE_WAR_TTL: Duration = 24.hours
        private const val MAX_ENTRIES = 10_000L

        /** Para não estourar o limite por segundo da chave na primeira consulta de um grupo. */
        private const val MAX_PARALLEL_LEAGUE_WARS = 4

        private fun <T : Any> cache(ttl: Duration, ticker: Ticker): Cache<String, T> = Caffeine.newBuilder()
            .expireAfterWrite(ttl.toJavaDuration())
            .maximumSize(MAX_ENTRIES)
            .ticker(ticker)
            .build()
    }
}
