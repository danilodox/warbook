package io.github.danilodox.warbook.data

import io.github.danilodox.warbook.api.ClanDto
import io.github.danilodox.warbook.api.ClanSummaryDto
import io.github.danilodox.warbook.api.CurrentWarDto
import io.github.danilodox.warbook.api.CwlDto
import io.github.danilodox.warbook.api.PlayerDto
import io.github.danilodox.warbook.data.local.AccountDao
import io.github.danilodox.warbook.data.local.AccountEntity
import io.github.danilodox.warbook.data.local.PlayerCacheDao
import io.github.danilodox.warbook.data.local.PlayerCacheEntity
import io.github.danilodox.warbook.data.local.WarCacheDao
import io.github.danilodox.warbook.data.local.WarCacheEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** [AccountDao] em memória. As funções com corpo (`addAndActivate`, `removeAndPickNext`) são as reais. */
class FakeAccountDao : AccountDao {
    val rows = MutableStateFlow<List<AccountEntity>>(emptyList())

    override fun observeAll(): Flow<List<AccountEntity>> = rows.map { list -> list.sortedBy { it.addedAt } }

    override suspend fun get(tag: String) = rows.value.firstOrNull { it.tag == tag }

    override suspend fun firstTag() = rows.value.minByOrNull { it.addedAt }?.tag

    override suspend fun upsert(account: AccountEntity) {
        rows.value = rows.value.filterNot { it.tag == account.tag } + account
    }

    override suspend fun delete(tag: String) {
        rows.value = rows.value.filterNot { it.tag == tag }
    }

    override suspend fun setActive(tag: String) = update { it.copy(isActive = it.tag == tag) }

    override suspend fun setVerificationToken(tag: String, token: String) =
        update { if (it.tag == tag) it.copy(verificationToken = token) else it }

    override suspend fun updateSummary(tag: String, name: String, townHallLevel: Int, clanName: String?) =
        update { if (it.tag == tag) it.copy(name = name, townHallLevel = townHallLevel, clanName = clanName) else it }

    private fun update(change: (AccountEntity) -> AccountEntity) {
        rows.value = rows.value.map(change)
    }
}

class FakePlayerCacheDao : PlayerCacheDao {
    val rows = MutableStateFlow<Map<String, PlayerCacheEntity>>(emptyMap())

    override fun observe(tag: String): Flow<PlayerCacheEntity?> = rows.map { it[tag] }

    override suspend fun upsert(entry: PlayerCacheEntity) {
        rows.value += entry.tag to entry
    }

    override suspend fun delete(tag: String) {
        rows.value -= tag
    }
}

class FakeWarCacheDao : WarCacheDao {
    val rows = MutableStateFlow<Map<String, WarCacheEntity>>(emptyMap())

    override fun observe(cacheKey: String): Flow<WarCacheEntity?> = rows.map { it[cacheKey] }

    override suspend fun upsert(entry: WarCacheEntity) {
        rows.value += entry.cacheKey to entry
    }
}

/** Servidor falso no nível do repositório: respostas fixas por tag e registro das chamadas. */
class FakeWarbookRepository : WarbookRepository {
    val players = mutableMapOf<String, ApiResult<PlayerDto>>()
    val wars = mutableMapOf<String, ApiResult<CurrentWarDto>>()
    val cwls = mutableMapOf<String, ApiResult<CwlDto>>()
    var verifyResult: ApiResult<String> = ApiResult.Success("prova-assinada")
    val verifyCalls = mutableListOf<Pair<String, String>>()
    val playerCalls = mutableListOf<String>()
    val warCalls = mutableListOf<String>()
    val cwlCalls = mutableListOf<String>()

    override suspend fun player(rawTag: String): ApiResult<PlayerDto> {
        playerCalls += rawTag
        return players[rawTag] ?: ApiResult.Failure(AppError.NOT_FOUND)
    }

    override suspend fun clan(rawTag: String): ApiResult<ClanDto> = error("não usado")

    override suspend fun currentWar(rawClanTag: String): ApiResult<CurrentWarDto> {
        warCalls += rawClanTag
        return wars[rawClanTag] ?: ApiResult.Failure(AppError.NOT_FOUND)
    }

    override suspend fun currentCwl(rawClanTag: String): ApiResult<CwlDto> {
        cwlCalls += rawClanTag
        return cwls[rawClanTag] ?: ApiResult.Failure(AppError.NOT_FOUND)
    }

    override suspend fun verify(rawTag: String, gameToken: String): ApiResult<String> {
        verifyCalls += rawTag to gameToken
        return verifyResult
    }
}

class FakeAppPreferences(explore: Boolean = false) : AppPreferences {
    override val exploreMode = MutableStateFlow(explore)

    override suspend fun setExploreMode(enabled: Boolean) {
        exploreMode.value = enabled
    }
}

fun testPlayer(tag: String = "2PQ8LV9YC", name: String = "Dantas", townHall: Int = 15, clan: String? = "Picuí Raiz") =
    PlayerDto(
        tag = tag,
        name = name,
        townHallLevel = townHall,
        expLevel = 212,
        trophies = 4812,
        warStars = 1384,
        attackWins = 47,
        donations = 1240,
        donationsReceived = 860,
        clan = clan?.let { ClanSummaryDto("2YQUC8J0P", it) },
    )
