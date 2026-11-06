package io.github.danilodox.warbook.routes

import io.github.danilodox.warbook.api.ApiError
import io.github.danilodox.warbook.api.ApiErrorCode
import io.github.danilodox.warbook.api.ApiRoutes
import io.github.danilodox.warbook.api.CwlDto
import io.github.danilodox.warbook.api.CwlState
import io.github.danilodox.warbook.api.WarState
import io.github.danilodox.warbook.coc.CocService
import io.github.danilodox.warbook.module
import io.github.danilodox.warbook.testing.CocFixtures
import io.github.danilodox.warbook.testing.CwlFixtures
import io.github.danilodox.warbook.testing.FakeCoc
import io.github.danilodox.warbook.testing.FakeTicker
import io.github.danilodox.warbook.testing.authorizedClient
import io.github.danilodox.warbook.testing.testConfig
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class CwlRoutesTest {

    private val fake = FakeCoc()
    private val ticker = FakeTicker()

    private suspend fun ApplicationTestBuilder.start(): HttpClient {
        application { module(testConfig, fake.engine, ticker) }
        return authorizedClient()
    }

    private fun onWholeGroup() {
        fake.on(CwlFixtures.GROUP_PATH, body = CwlFixtures.group)
        CwlFixtures.wars.forEach { (tag, body) -> fake.on(CwlFixtures.warPath(tag), body = body) }
    }

    private suspend fun HttpClient.cwl(): HttpResponse = get(ApiRoutes.currentCwl(CocFixtures.CLAN_TAG))

    private suspend fun HttpResponse.cwl(): CwlDto = Json.decodeFromString(CwlDto.serializer(), bodyAsText())

    @Test
    fun juntaGrupoERodadasComONossoClaSempreEmClan() = testApplication {
        val api = start()
        onWholeGroup()

        val cwl = api.cwl().cwl()

        assertEquals(CwlState.IN_WAR, cwl.state)
        assertEquals("2026-11", cwl.season)
        assertEquals(listOf(1, 2, 3), cwl.rounds.map { it.round })
        val first = assertNotNull(cwl.rounds[0].war)
        assertEquals(WarState.WAR_ENDED, first.state)
        assertEquals(CocFixtures.CLAN_TAG, first.clan?.tag)
        val second = assertNotNull(cwl.rounds[1].war)
        assertEquals(CocFixtures.CLAN_TAG, second.clan?.tag, "lado trocado: o nosso clã vem em clan")
        assertEquals(8, second.clan?.stars)
        assertEquals("BBB", second.opponent?.tag)
        assertTrue(second.isCwl)
        assertEquals(1, second.attacksPerMember)
        assertNull(cwl.rounds[2].war, "rodada com #0 não foi sorteada")
        assertEquals(2, cwl.currentRound, "a rodada em guerra")
    }

    @Test
    fun rodadaNaoSorteadaNaoChamaAApi() = testApplication {
        val api = start()
        onWholeGroup()

        api.cwl()

        val paths = fake.requests.map { it.url.encodedPath }
        assertEquals(5, paths.size, "grupo + 4 guerras")
        assertTrue(paths.none { it.endsWith("%230") })
    }

    @Test
    fun classificacaoSomaEstrelasComBonusDeVitoria() = testApplication {
        val api = start()
        onWholeGroup()

        val clans = api.cwl().cwl().clans

        assertEquals(listOf(CocFixtures.CLAN_TAG, "CCC", "DDD", "BBB"), clans.map { it.tag })
        assertEquals(listOf(38, 31, 22, 20), clans.map { it.stars }, "vitória vale +10; guerra em andamento sem bônus")
        assertEquals(listOf(1, 1, 0, 0), clans.map { it.wins }, "empate em estrelas decide na destruição")
        assertEquals(120.0, clans.first().destructionPercentage)
        assertEquals("https://img/a.png", clans.first().badgeUrl)
    }

    @Test
    fun foraDaCwlVemSoOEstado() = testApplication {
        val api = start()
        fake.on(CwlFixtures.GROUP_PATH, HttpStatusCode.NotFound, """{ "reason": "notFound" }""")

        val response = api.cwl()

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(CwlDto(CwlState.NOT_IN_CWL), response.cwl())
    }

    @Test
    fun registroPrivadoViraWarLogPrivate() = testApplication {
        val api = start()
        fake.on(CwlFixtures.GROUP_PATH, HttpStatusCode.Forbidden, CocFixtures.ERROR_ACCESS_DENIED)

        val response = api.cwl()

        assertEquals(HttpStatusCode.Forbidden, response.status)
        val error = Json.decodeFromString(ApiError.serializer(), response.bodyAsText())
        assertEquals(ApiErrorCode.WAR_LOG_PRIVATE, error.code)
    }

    @Test
    fun falhaNumaGuerraDaRodadaVolta502() = testApplication {
        val api = start()
        onWholeGroup()
        fake.on(CwlFixtures.warPath("W4"), HttpStatusCode.InternalServerError, """{ "reason": "unknownException" }""")

        val response = api.cwl()

        assertEquals(HttpStatusCode.BadGateway, response.status)
    }

    @Test
    fun cacheGuardaACwlEDepoisSoBuscaAsGuerrasQueNaoAcabaram() = testApplication {
        val api = start()
        onWholeGroup()

        api.cwl()
        api.cwl()
        assertEquals(5, fake.requests.size, "segunda consulta sai do cache")

        ticker.advance(CocService.WAR_TTL + 1.seconds)
        api.cwl()
        val again = fake.requests.drop(5).map { it.url.encodedPath }
        assertEquals(
            setOf(CwlFixtures.GROUP_PATH, CwlFixtures.warPath("W3"), CwlFixtures.warPath("W4")),
            again.toSet(),
            "guerras encerradas ficam no cache",
        )
        assertEquals(3, again.size)
    }
}
