package io.github.danilodox.warbook.coc

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Ticker
import io.github.danilodox.warbook.api.ClanDto
import io.github.danilodox.warbook.api.CurrentWarDto
import io.github.danilodox.warbook.api.PlayerDto
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

/**
 * Busca na API do jogo e guarda o resultado em memória, para poupar o limite da chave.
 * Só sucessos entram no cache; erros sempre consultam de novo.
 */
class CocService(private val client: CocClient, ticker: Ticker = Ticker.systemTicker()) {
    private val players = cache<PlayerDto>(PLAYER_TTL, ticker)
    private val clans = cache<ClanDto>(CLAN_TTL, ticker)
    private val wars = cache<CurrentWarDto>(WAR_TTL, ticker)

    suspend fun player(tag: String): PlayerDto = players.getOrFetch(tag) { client.player(tag).toDto() }

    suspend fun clan(tag: String): ClanDto = clans.getOrFetch(tag) { client.clan(tag).toDto() }

    suspend fun currentWar(clanTag: String): CurrentWarDto =
        wars.getOrFetch(clanTag) { client.currentWar(clanTag).toDto() }

    private suspend fun <T : Any> Cache<String, T>.getOrFetch(key: String, fetch: suspend () -> T): T =
        getIfPresent(key) ?: fetch().also { put(key, it) }

    companion object {
        val WAR_TTL: Duration = 1.minutes
        val PLAYER_TTL: Duration = 2.minutes
        val CLAN_TTL: Duration = 5.minutes
        private const val MAX_ENTRIES = 10_000L

        private fun <T : Any> cache(ttl: Duration, ticker: Ticker): Cache<String, T> = Caffeine.newBuilder()
            .expireAfterWrite(ttl.toJavaDuration())
            .maximumSize(MAX_ENTRIES)
            .ticker(ticker)
            .build()
    }
}
