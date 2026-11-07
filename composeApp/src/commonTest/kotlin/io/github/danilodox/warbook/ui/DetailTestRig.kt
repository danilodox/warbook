package io.github.danilodox.warbook.ui

import io.github.danilodox.warbook.data.FakeAccountDao
import io.github.danilodox.warbook.data.FakeAppPreferences
import io.github.danilodox.warbook.data.FakeClanCacheDao
import io.github.danilodox.warbook.data.FakeFollowedDao
import io.github.danilodox.warbook.data.FakePlayerCacheDao
import io.github.danilodox.warbook.data.FakeWarCacheDao
import io.github.danilodox.warbook.data.FakeWarbookRepository
import io.github.danilodox.warbook.data.RoomAccountsRepository
import io.github.danilodox.warbook.data.RoomCacheScope
import io.github.danilodox.warbook.data.RoomClanRepository
import io.github.danilodox.warbook.data.RoomFollowRepository
import io.github.danilodox.warbook.data.RoomProfileRepository
import io.github.danilodox.warbook.data.RoomWarRepository

/** Os repositórios reais sobre DAOs falsos, montados como no Koin, para os testes das telas novas. */
class DetailTestRig {
    var now = 1_000L
    val api = FakeWarbookRepository()
    val accountDao = FakeAccountDao()
    val followedDao = FakeFollowedDao()
    val playerCache = FakePlayerCacheDao()
    val clanCache = FakeClanCacheDao()
    val preferences = FakeAppPreferences()
    private val scope = RoomCacheScope(accountDao, followedDao)
    val profiles = RoomProfileRepository(playerCache, accountDao, api, scope, followedDao) { now }
    val clans = RoomClanRepository(clanCache, api, scope, followedDao) { now }
    val wars = RoomWarRepository(FakeWarCacheDao(), api, scope) { now }
    val accounts = RoomAccountsRepository(accountDao, profiles, api) { now++ }
    val follows = RoomFollowRepository(followedDao, profiles, clans, wars) { now++ }
}
