package io.github.danilodox.warbook.data

import io.github.danilodox.warbook.api.ClanSearchResultDto
import io.github.danilodox.warbook.data.local.ClanCacheEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ClanRepositoryTest {

    private val accountDao = FakeAccountDao()
    private val cache = FakeClanCacheDao()
    private val api = FakeWarbookRepository()
    private var now = 1_000L
    private val repository = RoomClanRepository(cache, api, AccountsOnlyCacheScope(accountDao)) { now }

    @Test
    fun keepSalvaComAHoraEErroMantemOSalvo() = runTest {
        api.clans["2YQUC8J0P"] = ApiResult.Success(testClan())
        repository.refresh("2YQUC8J0P", keep = true)
        now = 9_000L
        api.clans["2YQUC8J0P"] = ApiResult.Failure(AppError.NETWORK)

        val result = repository.refresh("2YQUC8J0P", keep = true)

        assertEquals(ApiResult.Failure(AppError.NETWORK), result)
        val cached = repository.observe("2YQUC8J0P").first()
        assertEquals("Picuí Raiz", cached?.value?.name)
        assertEquals(1_000L, cached?.fetchedAt)
    }

    @Test
    fun memoriaMaisNovaGanhaDoSalvo() = runTest {
        api.clans["2YQUC8J0P"] = ApiResult.Success(testClan())
        repository.refresh("2YQUC8J0P", keep = true)
        now = 5_000L
        api.clans["2YQUC8J0P"] = ApiResult.Success(testClan(level = 19))

        repository.refresh("2YQUC8J0P")

        assertEquals(19, repository.observe("2YQUC8J0P").first()?.value?.clanLevel)
        assertEquals(1_000L, cache.rows.value.getValue("2YQUC8J0P").fetchedAt, "o banco não mudou")
    }

    @Test
    fun claSalvoNumFormatoQueNaoServeMaisViraNulo() = runTest {
        cache.upsert(ClanCacheEntity("2YQUC8J0P", """{"formato":"antigo"}""", 1L))

        assertNull(repository.observe("2YQUC8J0P").first())
    }

    @Test
    fun buscaPassaONomeDireto() = runTest {
        val found = listOf(ClanSearchResultDto("OPP", "Os Mito", null, 20, 50))
        api.searches["mito"] = ApiResult.Success(found)

        assertEquals(ApiResult.Success(found), repository.search("mito"))
        assertTrue(cache.rows.value.isEmpty(), "a busca não guarda nada")
    }
}
