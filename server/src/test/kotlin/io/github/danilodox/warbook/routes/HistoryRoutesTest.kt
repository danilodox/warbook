package io.github.danilodox.warbook.routes

import io.github.danilodox.warbook.api.ApiError
import io.github.danilodox.warbook.api.ApiErrorCode
import io.github.danilodox.warbook.api.ApiRoutes
import io.github.danilodox.warbook.api.ClanActivityDto
import io.github.danilodox.warbook.api.CwlSeasonDto
import io.github.danilodox.warbook.api.PlayerHistoryDto
import io.github.danilodox.warbook.api.WarDetailDto
import io.github.danilodox.warbook.api.WarHistoryDto
import io.github.danilodox.warbook.api.WarKind
import io.github.danilodox.warbook.api.WarOutcome
import io.github.danilodox.warbook.api.WarState
import io.github.danilodox.warbook.history.WarHistoryService
import io.github.danilodox.warbook.history.WarRecord
import io.github.danilodox.warbook.history.WarRecordAttack
import io.github.danilodox.warbook.history.WarRecordMember
import io.github.danilodox.warbook.history.WarResult
import io.github.danilodox.warbook.history.WarSide
import io.github.danilodox.warbook.history.WarSource
import io.github.danilodox.warbook.history.WarType
import io.github.danilodox.warbook.module
import io.github.danilodox.warbook.score.AttackMark
import io.github.danilodox.warbook.testing.FakeCoc
import io.github.danilodox.warbook.testing.FakePlayerIndex
import io.github.danilodox.warbook.testing.TestDatabase
import io.github.danilodox.warbook.testing.authorizedClient
import io.github.danilodox.warbook.testing.testConfig
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HistoryRoutesTest {

    private val history = TestDatabase.history
    private val now = Instant.parse("2026-11-20T12:00:00Z")

    @BeforeTest
    fun setUp() = runBlocking {
        TestDatabase.clean()
        history.recordVerifiedVisit(CLAN, "Picuí Raiz", "PQ2", now)
        Unit
    }

    private fun ApplicationTestBuilder.start() = application {
        module(testConfig, FakeCoc().engine, playerIndex = FakePlayerIndex(), warHistory = TestDatabase.history)
    }

    private val members = listOf(
        WarRecordMember("PQ2", isOpponent = false, "Dantas", townHall = 15, mapPosition = 1),
        WarRecordMember("PQ8", isOpponent = false, "Rapadura", townHall = 14, mapPosition = 2),
        WarRecordMember("E1", isOpponent = true, "Brabo", townHall = 15, mapPosition = 1),
        WarRecordMember("E2", isOpponent = true, "Mito", townHall = 13, mapPosition = 2),
    )

    /**
     * PQ2 (CV 15) bate no E2 (CV 13) com o E1 (CV 15) aberto: alvo fácil, 4 × 0,7 = 2,8; um ataque sobrando
     * (−2) → 0,8. PQ8 (CV 14) dá 2★ 60% no E1 (CV 15): 2,6 × 1,15 = 2,99; −2 → 0,99.
     */
    private fun snapshot(daysAgo: Long, state: WarState = WarState.WAR_ENDED, type: WarType = WarType.WAR) = WarRecord(
        type = type,
        source = WarSource.SNAPSHOT,
        state = state,
        teamSize = 2,
        attacksPerMember = if (type == WarType.CWL) 1 else 2,
        endTime = now.minus(daysAgo, ChronoUnit.DAYS),
        clan = WarSide(CLAN, "Picuí Raiz", stars = 5, destruction = 80.0, attacks = 2),
        opponent = WarSide("OPP", "Os Mito", "https://img/o.png", stars = 3, destruction = 60.0, attacks = 2),
        capturedAt = now,
        result = if (state == WarState.WAR_ENDED) WarResult.WIN else null,
        members = members,
        attacks = listOf(
            WarRecordAttack(1, "PQ2", "E2", stars = 3, destruction = 100),
            WarRecordAttack(2, "PQ8", "E1", stars = 2, destruction = 60),
        ),
    )

    private fun warlog(daysAgo: Long) = WarRecord(
        type = WarType.WAR,
        source = WarSource.WARLOG,
        state = WarState.WAR_ENDED,
        teamSize = 15,
        attacksPerMember = 2,
        endTime = now.minus(daysAgo, ChronoUnit.DAYS),
        clan = WarSide(CLAN, "Picuí Raiz", stars = 30, destruction = 70.0),
        opponent = WarSide("OPP9", "Antigo", stars = 31, destruction = 72.0),
        capturedAt = now,
        result = WarResult.LOSE,
    )

    private suspend fun <T> HttpResponse.decode(serializer: KSerializer<T>): T =
        Json.decodeFromString(serializer, bodyAsText())

    private suspend fun HttpResponse.errorCode() = decode(ApiError.serializer()).code

    private suspend fun HttpClient.wars(query: String = ""): HttpResponse = get(ApiRoutes.wars(CLAN) + query)

    @Test
    fun listaAsGuerrasDaMaisRecenteParaAMaisAntiga() = testApplication {
        start()
        history.saveWarlog(CLAN, listOf(warlog(daysAgo = 10)))
        val id = history.saveSnapshot(CLAN, snapshot(daysAgo = 2))

        val wars = authorizedClient().wars().decode(WarHistoryDto.serializer()).wars

        assertEquals(listOf(id), wars.take(1).map { it.id })
        assertEquals(listOf(true, false), wars.map { it.detailed })
        assertEquals(listOf(WarOutcome.WIN, WarOutcome.LOSE), wars.map { it.result })
        assertEquals("Os Mito", wars.first().opponentName)
        assertEquals(now.minus(2, ChronoUnit.DAYS).toEpochMilli(), wars.first().endTime)
    }

    @Test
    fun limitCortaALista() = testApplication {
        start()
        history.saveWarlog(CLAN, (1L..5L).map { warlog(daysAgo = it) })

        val wars = authorizedClient().wars("?limit=3").decode(WarHistoryDto.serializer()).wars

        assertEquals(3, wars.size)
    }

    @Test
    fun limitForaDaFaixaEErro() = testApplication {
        start()
        val client = authorizedClient()

        listOf("?limit=0", "?limit=101", "?limit=abc").forEach {
            val response = client.wars(it)
            assertEquals(HttpStatusCode.BadRequest, response.status, it)
            assertEquals(ApiErrorCode.INVALID_QUERY, response.errorCode(), it)
        }
    }

    @Test
    fun claNuncaMonitoradoTemListaVazia() = testApplication {
        start()

        val response = authorizedClient().get(ApiRoutes.wars("2PQ8LV9YC"))

        assertEquals(WarHistoryDto(emptyList()), response.decode(WarHistoryDto.serializer()))
    }

    @Test
    fun guerraVemComANotaDeCadaMembro() = testApplication {
        start()
        val id = history.saveSnapshot(CLAN, snapshot(daysAgo = 1))

        val detail = authorizedClient().get(ApiRoutes.war(CLAN, id)).decode(WarDetailDto.serializer())

        assertEquals(listOf("PQ2", "PQ8"), detail.members.map { it.tag })
        val p1 = detail.members.first()
        assertEquals("Dantas", p1.name)
        assertEquals(1, p1.missed)
        assertEquals(0.8, p1.total, 0.001)
        val hit = p1.attacks.single()
        assertEquals(AttackMark.EASY_TARGET, hit.mark)
        assertEquals(0.7, hit.factor, 0.001)
        assertEquals("Mito", hit.defenderName)
        assertEquals(2, hit.defenderPosition)
        assertEquals(13, hit.defenderTownHall)
        assertEquals(0.99, detail.members.last().total, 0.001)
    }

    @Test
    fun guerraEmAndamentoNaoPenalizaAtaqueQueFalta() = testApplication {
        start()
        val id = history.saveSnapshot(CLAN, snapshot(daysAgo = 0, state = WarState.IN_WAR))

        val detail = authorizedClient().get(ApiRoutes.war(CLAN, id)).decode(WarDetailDto.serializer())

        assertEquals(listOf(0, 0), detail.members.map { it.missed })
        assertEquals(2.8, detail.members.first().total, 0.001)
    }

    @Test
    fun guerraDoWarlogVemSemMembros() = testApplication {
        start()
        history.saveWarlog(CLAN, listOf(warlog(daysAgo = 3)))
        val id = history.recentWars(CLAN, 1).single().id!!

        val detail = authorizedClient().get(ApiRoutes.war(CLAN, id)).decode(WarDetailDto.serializer())

        assertEquals(false, detail.war.detailed)
        assertTrue(detail.members.isEmpty())
    }

    @Test
    fun guerraDeOutroClaOuIdInvalidoNaoExiste() = testApplication {
        start()
        val id = history.saveSnapshot(CLAN, snapshot(daysAgo = 1))
        val client = authorizedClient()

        listOf(ApiRoutes.war("2PQ8LV9YC", id), ApiRoutes.war(CLAN, id + 1), "${ApiRoutes.wars(CLAN)}/abc").forEach {
            val response = client.get(it)
            assertEquals(HttpStatusCode.NotFound, response.status, it)
            assertEquals(ApiErrorCode.NOT_FOUND, response.errorCode(), it)
        }
    }

    @Test
    fun historicoDoJogadorSoComGuerrasEncerradas() = testApplication {
        start()
        history.saveSnapshot(CLAN, snapshot(daysAgo = 0, state = WarState.IN_WAR))
        history.saveSnapshot(CLAN, snapshot(daysAgo = 2))
        history.saveSnapshot(CLAN, snapshot(daysAgo = 4, type = WarType.CWL))
        history.saveWarlog(CLAN, listOf(warlog(daysAgo = 6)))

        val player = authorizedClient().get(ApiRoutes.playerHistory(CLAN, "PQ2"))
            .decode(PlayerHistoryDto.serializer())

        assertEquals("Dantas", player.name)
        assertEquals(listOf(WarKind.WAR, WarKind.CWL), player.wars.map { it.kind })
        // Guerra: 2,8 − 2 = 0,8. CWL: 1 ataque por membro, usou → 2,8.
        assertEquals(listOf(0.8, 2.8), player.wars.map { it.total })
        assertEquals(1.8, player.average!!, 0.001)
        assertEquals(2, player.attacksUsed)
        assertEquals(3, player.attacksAvailable)
        assertEquals(2, player.easyTargets)
        assertNull(player.trend, "poucas guerras para comparar")
    }

    @Test
    fun jogadorSemGuerrasNoCla() = testApplication {
        start()
        history.saveSnapshot(CLAN, snapshot(daysAgo = 2))

        val player = authorizedClient().get(ApiRoutes.playerHistory(CLAN, "9CJ2YQ0L"))
            .decode(PlayerHistoryDto.serializer())

        assertTrue(player.wars.isEmpty())
        assertNull(player.average)
        assertNull(player.name)
    }

    @Test
    fun atividadeSoComGuerrasComunsEncerradasEACwl() = testApplication {
        start()
        history.saveSnapshot(CLAN, snapshot(daysAgo = 0, state = WarState.IN_WAR))
        history.saveSnapshot(CLAN, snapshot(daysAgo = 2))
        history.saveSnapshot(CLAN, snapshot(daysAgo = 4, type = WarType.CWL).copy(cwlSeason = "2026-11"))

        val activity = authorizedClient().get(ApiRoutes.activity(CLAN)).decode(ClanActivityDto.serializer())

        assertEquals(1, activity.warsCounted, "a guerra em andamento fica de fora")
        assertEquals("2026-11", activity.cwlSeason)
        val p1 = activity.members.single { it.tag == "PQ2" }
        assertEquals(1, p1.attacksUsed)
        assertEquals(2, p1.attacksAvailable)
        assertEquals(1, p1.cwlRounds)
        assertEquals(1, p1.cwlAttacksUsed)
    }

    @Test
    fun temporadaDaCwlSomaAsRodadasGuardadas() = testApplication {
        start()
        val client = authorizedClient()
        val empty = client.get(ApiRoutes.cwlSeason(CLAN)).decode(CwlSeasonDto.serializer())
        assertNull(empty.season, "sem CWL guardada")

        history.saveSnapshot(CLAN, snapshot(daysAgo = 2))
        history.saveSnapshot(CLAN, snapshot(daysAgo = 4, type = WarType.CWL).copy(cwlSeason = "2026-11"))

        val season = client.get(ApiRoutes.cwlSeason(CLAN)).decode(CwlSeasonDto.serializer())

        assertEquals("2026-11", season.season)
        assertEquals(1, season.rounds, "a guerra comum fica de fora")
        assertEquals(1, season.members.single { it.tag == "PQ2" }.attacksUsed)
    }

    @Test
    fun tagDoJogadorInvalida() = testApplication {
        start()

        val response = authorizedClient().get(ApiRoutes.playerHistory(CLAN, "xx!"))

        assertEquals(ApiErrorCode.INVALID_TAG, response.errorCode())
    }

    @Test
    fun tendenciaComparaAsCincoMaisRecentesComAsCincoAnteriores() {
        val recentFirst = listOf(4.0, 4.0, 4.0, 4.0, 4.0, 2.0, 2.0, 2.0, 2.0, 2.0, 0.0)
        assertEquals(2.0, WarHistoryService.trend(recentFirst))
        assertEquals(-1.0, WarHistoryService.trend(listOf(1.0, 1.0, 1.0, 1.0, 1.0, 2.0)))
        assertNull(WarHistoryService.trend(listOf(3.0, 3.0, 3.0, 3.0, 3.0)))
        assertNull(WarHistoryService.trend(emptyList()))
    }

    private companion object {
        /** Tag válida (só caracteres que o jogo usa). */
        const val CLAN = "2YQUC8J0P"
    }
}
