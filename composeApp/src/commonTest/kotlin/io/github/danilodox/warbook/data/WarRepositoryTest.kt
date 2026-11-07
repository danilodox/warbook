package io.github.danilodox.warbook.data

import io.github.danilodox.warbook.api.CurrentWarDto
import io.github.danilodox.warbook.api.CwlDto
import io.github.danilodox.warbook.api.CwlState
import io.github.danilodox.warbook.api.WarState
import io.github.danilodox.warbook.data.local.WarCacheEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WarRepositoryTest {

    private val dao = FakeWarCacheDao()
    private val api = FakeWarbookRepository()
    private var now = 1_000L
    private val repository = RoomWarRepository(dao, api) { now }

    private val clan = "2YQUC8J0P"

    @Test
    fun guerraAtualizadaFicaSalvaComAHora() = runTest {
        api.wars[clan] = ApiResult.Success(CurrentWarDto(WarState.PREPARATION, teamSize = 15))

        repository.refreshWar(clan)

        val saved = repository.observeWar(clan).first()
        assertEquals(Cached(CurrentWarDto(WarState.PREPARATION, teamSize = 15), 1_000L), saved)
    }

    @Test
    fun erroMantemOResultadoSalvo() = runTest {
        api.wars[clan] = ApiResult.Success(CurrentWarDto(WarState.IN_WAR))
        repository.refreshWar(clan)
        now = 9_000L
        api.wars[clan] = ApiResult.Failure(AppError.NETWORK)

        assertEquals(ApiResult.Failure(AppError.NETWORK), repository.refreshWar(clan))
        assertEquals(1_000L, repository.observeWar(clan).first()?.fetchedAt)
    }

    @Test
    fun guerraECwlFicamSeparadas() = runTest {
        api.wars[clan] = ApiResult.Success(CurrentWarDto(WarState.NOT_IN_WAR))
        api.cwls[clan] = ApiResult.Success(CwlDto(CwlState.NOT_IN_CWL))

        repository.refreshWar(clan)
        repository.refreshCwl(clan)

        assertEquals(WarState.NOT_IN_WAR, repository.observeWar(clan).first()?.value?.state)
        assertEquals(CwlState.NOT_IN_CWL, repository.observeCwl(clan).first()?.value?.state)
        assertNull(repository.observeWar("OUTRO").first())
    }

    @Test
    fun formatoAntigoViraNulo() = runTest {
        dao.upsert(WarCacheEntity("cwl:$clan", """{"formato":"antigo"}""", 1L))

        assertNull(repository.observeCwl(clan).first())
    }
}
