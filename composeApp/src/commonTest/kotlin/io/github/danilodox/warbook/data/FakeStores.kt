package io.github.danilodox.warbook.data

import io.github.danilodox.warbook.api.ClanDto
import io.github.danilodox.warbook.api.ClanMemberDto
import io.github.danilodox.warbook.api.ClanRole
import io.github.danilodox.warbook.api.ClanSearchResultDto
import io.github.danilodox.warbook.api.ClanSummaryDto
import io.github.danilodox.warbook.api.CurrentWarDto
import io.github.danilodox.warbook.api.CwlDto
import io.github.danilodox.warbook.api.PlayerDto
import io.github.danilodox.warbook.data.local.AccountDao
import io.github.danilodox.warbook.data.local.AccountEntity
import io.github.danilodox.warbook.data.local.ClanCacheDao
import io.github.danilodox.warbook.data.local.ClanCacheEntity
import io.github.danilodox.warbook.data.local.FollowedDao
import io.github.danilodox.warbook.data.local.FollowedEntity
import io.github.danilodox.warbook.data.local.PlayerCacheDao
import io.github.danilodox.warbook.data.local.PlayerCacheEntity
import io.github.danilodox.warbook.data.local.WarCacheDao
import io.github.danilodox.warbook.data.local.WarCacheEntity
import io.github.danilodox.warbook.war.SummaryOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

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

    override suspend fun updateSummary(
        tag: String,
        name: String,
        townHallLevel: Int,
        clanName: String?,
        clanTag: String?,
    ) = update {
        if (it.tag != tag) {
            it
        } else {
            it.copy(name = name, townHallLevel = townHallLevel, clanName = clanName, clanTag = clanTag)
        }
    }

    override suspend fun countWithClan(clanTag: String) = rows.value.count { it.clanTag == clanTag }

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

    override suspend fun delete(cacheKey: String) {
        rows.value -= cacheKey
    }
}

class FakeClanCacheDao : ClanCacheDao {
    val rows = MutableStateFlow<Map<String, ClanCacheEntity>>(emptyMap())

    override fun observe(tag: String): Flow<ClanCacheEntity?> = rows.map { it[tag] }

    override suspend fun upsert(entry: ClanCacheEntity) {
        rows.value += entry.tag to entry
    }

    override suspend fun delete(tag: String) {
        rows.value -= tag
    }
}

class FakeFollowedDao : FollowedDao {
    val rows = MutableStateFlow<List<FollowedEntity>>(emptyList())

    override fun observeAll(): Flow<List<FollowedEntity>> =
        rows.map { list -> list.sortedByDescending { it.followedAt } }

    override fun observeCount(kind: String, tag: String): Flow<Int> =
        rows.map { list -> list.count { it.kind == kind && it.tag == tag } }

    override suspend fun count(kind: String, tag: String) = rows.value.count { it.kind == kind && it.tag == tag }

    override suspend fun countAll() = rows.value.size

    override suspend fun upsert(entry: FollowedEntity) {
        rows.value = rows.value.filterNot { it.kind == entry.kind && it.tag == entry.tag } + entry
    }

    override suspend fun updateSummary(kind: String, tag: String, name: String, level: Int?, imageUrl: String?) {
        rows.value = rows.value.map {
            if (it.kind == kind && it.tag == tag) it.copy(name = name, level = level, imageUrl = imageUrl) else it
        }
    }

    override suspend fun delete(kind: String, tag: String) {
        rows.value = rows.value.filterNot { it.kind == kind && it.tag == tag }
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
    val clans = mutableMapOf<String, ApiResult<ClanDto>>()
    val searches = mutableMapOf<String, ApiResult<List<ClanSearchResultDto>>>()
    val clanCalls = mutableListOf<String>()
    val searchCalls = mutableListOf<String>()

    override suspend fun player(rawTag: String): ApiResult<PlayerDto> {
        playerCalls += rawTag
        return players[rawTag] ?: ApiResult.Failure(AppError.NOT_FOUND)
    }

    override suspend fun clan(rawTag: String): ApiResult<ClanDto> {
        clanCalls += rawTag
        return clans[rawTag] ?: ApiResult.Failure(AppError.NOT_FOUND)
    }

    override suspend fun searchClans(rawName: String): ApiResult<List<ClanSearchResultDto>> {
        searchCalls += rawName
        return searches[rawName] ?: ApiResult.Success(emptyList())
    }

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
    override val summaryOptions = MutableStateFlow(SummaryOptions())
    override val recentSearches = MutableStateFlow<List<RecentSearch>>(emptyList())
    override val themeMode = MutableStateFlow(ThemeMode.DARK)

    override suspend fun setThemeMode(mode: ThemeMode) {
        themeMode.value = mode
    }

    override suspend fun addRecentSearch(item: RecentSearch) {
        recentSearches.update { RecentSearches.add(it, item) }
    }

    override suspend fun clearRecentSearches() {
        recentSearches.value = emptyList()
    }

    override suspend fun setExploreMode(enabled: Boolean) {
        exploreMode.value = enabled
    }

    override suspend fun updateSummaryOptions(transform: (SummaryOptions) -> SummaryOptions) {
        summaryOptions.update(transform)
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

fun testClan(tag: String = "2YQUC8J0P", name: String = "Picuí Raiz", level: Int = 18) = ClanDto(
    tag = tag,
    name = name,
    badgeUrl = "https://img/m.png",
    clanLevel = level,
    memberCount = 3,
    isWarLogPublic = true,
    members = listOf(
        ClanMemberDto("ABC", "Maré Alta", ClanRole.LEADER, 16, 250, 5100, null, 3412, 980, 1),
        ClanMemberDto("2PQ8LV9YC", "Dantas", ClanRole.CO_LEADER, 15, 212, 4812, null, 1240, 860, 2),
        ClanMemberDto("XYZ", "Rapadura", ClanRole.MEMBER, 13, 150, 3900, null, 96, 1004, 3),
    ),
)
