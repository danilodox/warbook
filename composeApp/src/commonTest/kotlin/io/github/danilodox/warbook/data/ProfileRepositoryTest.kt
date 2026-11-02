package io.github.danilodox.warbook.data

import io.github.danilodox.warbook.data.local.PlayerCacheEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProfileRepositoryTest {

    private val accountDao = FakeAccountDao()
    private val cacheDao = FakePlayerCacheDao()
    private val api = FakeWarbookRepository()
    private var now = 1_000L
    private val repository = RoomProfileRepository(cacheDao, accountDao, api) { now }
    private val accounts = RoomAccountsRepository(accountDao, repository, api) { now }

    @Test
    fun atualizarSalvaOPerfilComAHoraEAtualizaOCabecalhoDaConta() = runTest {
        accounts.add(testPlayer())
        now = 5_000L
        api.players["2PQ8LV9YC"] = ApiResult.Success(testPlayer(townHall = 16, clan = "Picuí Base"))

        repository.refresh("2PQ8LV9YC")

        val cached = repository.observe("2PQ8LV9YC").first()
        assertEquals(16, cached?.player?.townHallLevel)
        assertEquals(5_000L, cached?.fetchedAt)
        val account = accounts.accounts.first().single()
        assertEquals(16, account.townHallLevel)
        assertEquals("Picuí Base", account.clanName)
    }

    @Test
    fun erroNaAtualizacaoMantemOPerfilSalvo() = runTest {
        accounts.add(testPlayer())
        api.players["2PQ8LV9YC"] = ApiResult.Failure(AppError.NETWORK)

        val result = repository.refresh("2PQ8LV9YC")

        assertEquals(ApiResult.Failure(AppError.NETWORK), result)
        val cached = repository.observe("2PQ8LV9YC").first()
        assertEquals("Dantas", cached?.player?.name)
        assertEquals(1_000L, cached?.fetchedAt)
    }

    @Test
    fun perfilSalvoNumFormatoQueNaoServeMaisViraNulo() = runTest {
        cacheDao.upsert(PlayerCacheEntity("2PQ8LV9YC", """{"formato":"antigo"}""", 1L))

        assertNull(repository.observe("2PQ8LV9YC").first())
    }

    @Test
    fun semPerfilSalvoViraNulo() = runTest {
        assertNull(repository.observe("2PQ8LV9YC").first())
    }
}
