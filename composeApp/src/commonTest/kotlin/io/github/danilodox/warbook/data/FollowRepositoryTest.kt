package io.github.danilodox.warbook.data

import io.github.danilodox.warbook.api.ClanSearchResultDto
import io.github.danilodox.warbook.api.CurrentWarDto
import io.github.danilodox.warbook.api.WarState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Seguindo + a regra do que fica salvo (contas, clãs das contas e seguidos). */
class FollowRepositoryTest {

    private val accountDao = FakeAccountDao()
    private val followedDao = FakeFollowedDao()
    private val playerCache = FakePlayerCacheDao()
    private val clanCache = FakeClanCacheDao()
    private val warCache = FakeWarCacheDao()
    private val api = FakeWarbookRepository()
    private var now = 1_000L
    private val scope = RoomCacheScope(accountDao, followedDao)
    private val profiles = RoomProfileRepository(playerCache, accountDao, api, scope, followedDao) { now }
    private val clans = RoomClanRepository(clanCache, api, scope, followedDao) { now }
    private val wars = RoomWarRepository(warCache, api, scope) { now }
    private val accounts = RoomAccountsRepository(accountDao, profiles, api) { now }
    private val follows = RoomFollowRepository(followedDao, profiles, clans, wars) { now++ }

    @Test
    fun jogadorDePassagemFicaSoNaMemoria() = runTest {
        api.players["BRABO"] = ApiResult.Success(testPlayer(tag = "BRABO", name = "Brabo"))

        profiles.refresh("BRABO")

        assertEquals("Brabo", profiles.observe("BRABO").first()?.player?.name, "aparece enquanto o app está aberto")
        assertTrue(playerCache.rows.value.isEmpty(), "não vai para o banco")
    }

    @Test
    fun seguirGuardaOJogadorEAtualizarMantemNoBanco() = runTest {
        follows.followPlayer(testPlayer(tag = "BRABO", name = "Brabo"))
        api.players["BRABO"] = ApiResult.Success(testPlayer(tag = "BRABO", name = "Brabo Jr", townHall = 16))

        profiles.refresh("BRABO")

        assertNotNull(playerCache.rows.value["BRABO"])
        assertEquals("Brabo Jr", profiles.observe("BRABO").first()?.player?.name)
        val item = follows.followed.first().single()
        assertEquals(Followed(FollowKind.PLAYER, "BRABO", "Brabo Jr", 16, null), item, "a lista acompanha o perfil")
    }

    @Test
    fun deixarDeSeguirApagaOSalvoMenosDeUmaConta() = runTest {
        accounts.add(testPlayer())
        follows.followPlayer(testPlayer())
        follows.followPlayer(testPlayer(tag = "BRABO", name = "Brabo"))

        follows.unfollow(FollowKind.PLAYER, "2PQ8LV9YC")
        follows.unfollow(FollowKind.PLAYER, "BRABO")

        assertNotNull(playerCache.rows.value["2PQ8LV9YC"], "perfil da conta fica")
        assertNull(playerCache.rows.value["BRABO"])
        assertTrue(follows.followed.first().isEmpty())
    }

    @Test
    fun removerContaSeguidaMantemOPerfil() = runTest {
        accounts.add(testPlayer())
        follows.followPlayer(testPlayer())

        accounts.remove("2PQ8LV9YC")

        assertNotNull(playerCache.rows.value["2PQ8LV9YC"])
    }

    @Test
    fun limiteDeCinquentaNoTotal() = runTest {
        repeat(FollowRepository.LIMIT - 1) { follows.followClan(searchResult("C$it")) }
        assertEquals(FollowResult.FOLLOWED, follows.followPlayer(testPlayer(tag = "BRABO")))

        assertEquals(FollowResult.LIMIT_REACHED, follows.followClan(searchResult("MAIS")))
        val again = follows.followPlayer(testPlayer(tag = "BRABO"))
        assertEquals(FollowResult.FOLLOWED, again, "o mesmo de novo não conta")
        assertEquals(FollowRepository.LIMIT, follows.followed.first().size)
    }

    @Test
    fun claSeguidoPelaBuscaGuardaNaPrimeiraAbertura() = runTest {
        follows.followClan(searchResult("OPP"))
        assertTrue(clanCache.rows.value.isEmpty())
        api.clans["OPP"] = ApiResult.Success(testClan(tag = "OPP", name = "Os Mito", level = 20))

        clans.refresh("OPP")

        assertNotNull(clanCache.rows.value["OPP"])
        assertEquals(20, follows.followed.first().single().level)
        assertTrue(follows.isFollowed(FollowKind.CLAN, "OPP").first())
    }

    @Test
    fun claDaContaFicaSalvoEDeixarDeSeguirNaoApaga() = runTest {
        accounts.add(testPlayer())
        api.clans["2YQUC8J0P"] = ApiResult.Success(testClan())
        api.wars["2YQUC8J0P"] = ApiResult.Success(CurrentWarDto(WarState.NOT_IN_WAR))
        follows.followClan(testClan())

        clans.refresh("2YQUC8J0P")
        wars.refreshWar("2YQUC8J0P")
        follows.unfollow(FollowKind.CLAN, "2YQUC8J0P")

        assertNotNull(clanCache.rows.value["2YQUC8J0P"])
        assertNotNull(warCache.rows.value["war:2YQUC8J0P"])
    }

    @Test
    fun deixarDeSeguirClaApagaClaEGuerra() = runTest {
        api.wars["OPP"] = ApiResult.Success(CurrentWarDto(WarState.NOT_IN_WAR))
        follows.followClan(testClan(tag = "OPP"))
        wars.refreshWar("OPP")
        assertNotNull(warCache.rows.value["war:OPP"])

        follows.unfollow(FollowKind.CLAN, "OPP")

        assertNull(clanCache.rows.value["OPP"])
        assertNull(warCache.rows.value["war:OPP"])
        assertFalse(follows.isFollowed(FollowKind.CLAN, "OPP").first())
    }

    @Test
    fun guerraDeClaDePassagemFicaSoNaMemoriaMasKeepForca() = runTest {
        api.wars["OPP"] = ApiResult.Success(CurrentWarDto(WarState.NOT_IN_WAR))

        wars.refreshWar("OPP")
        assertNotNull(wars.observeWar("OPP").first())
        assertTrue(warCache.rows.value.isEmpty())

        wars.refreshWar("OPP", keep = true)
        assertNotNull(warCache.rows.value["war:OPP"])
    }

    private fun searchResult(tag: String) = ClanSearchResultDto(tag, "Clã $tag", null, 10, 30)
}
