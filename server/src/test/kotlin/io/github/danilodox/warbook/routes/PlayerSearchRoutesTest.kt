package io.github.danilodox.warbook.routes

import io.github.danilodox.warbook.api.ApiError
import io.github.danilodox.warbook.api.ApiErrorCode
import io.github.danilodox.warbook.api.ApiRoutes
import io.github.danilodox.warbook.api.PlayerSearchResponse
import io.github.danilodox.warbook.api.PlayerSearchResultDto
import io.github.danilodox.warbook.index.IndexedPlayer
import io.github.danilodox.warbook.module
import io.github.danilodox.warbook.testing.CocFixtures
import io.github.danilodox.warbook.testing.FakeCoc
import io.github.danilodox.warbook.testing.FakePlayerIndex
import io.github.danilodox.warbook.testing.FakeTicker
import io.github.danilodox.warbook.testing.TestDatabase
import io.github.danilodox.warbook.testing.authorizedClient
import io.github.danilodox.warbook.testing.testConfig
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Busca por nick e o que alimenta o índice. O índice de verdade é testado em `PostgresPlayerIndexTest`. */
class PlayerSearchRoutesTest {

    private val fake = FakeCoc()
    private val ticker = FakeTicker()
    private val index = FakePlayerIndex()

    private suspend fun ApplicationTestBuilder.start(): HttpClient {
        application { module(testConfig, fake.engine, ticker, playerIndex = index, warHistory = TestDatabase.history) }
        return authorizedClient()
    }

    @Test
    fun buscaDevolveOQueOIndiceAchou() = testApplication {
        val api = start()
        val found = PlayerSearchResultDto("P1", "Dantas", 15, CocFixtures.CLAN_TAG, "Picuí Raiz", seenAt = 1_000)
        index.results = listOf(found)

        val response = api.get(ApiRoutes.PLAYER_SEARCH) { parameter(ApiRoutes.PARAM_NAME, "  dantas ") }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.decodeFromString(PlayerSearchResponse.serializer(), response.bodyAsText())
        assertEquals(listOf(found), body.items)
        assertEquals(listOf("dantas"), index.searches, "nick aparado")
        assertTrue(fake.requests.isEmpty(), "a busca por nick não chama a API do jogo")
    }

    @Test
    fun nickCurtoOuSemNickViraInvalidQuery() = testApplication {
        val api = start()

        for (response in listOf(
            api.get(ApiRoutes.PLAYER_SEARCH) { parameter(ApiRoutes.PARAM_NAME, " ab ") },
            api.get(ApiRoutes.PLAYER_SEARCH) { parameter(ApiRoutes.PARAM_NAME, "a".repeat(51)) },
            api.get(ApiRoutes.PLAYER_SEARCH),
        )) {
            assertEquals(HttpStatusCode.BadRequest, response.status)
            val error = Json.decodeFromString(ApiError.serializer(), response.bodyAsText())
            assertEquals(ApiErrorCode.INVALID_QUERY, error.code)
        }
        assertTrue(index.searches.isEmpty())
    }

    @Test
    fun buscaPedeToken() = testApplication {
        application { module(testConfig, fake.engine, ticker, playerIndex = index, warHistory = TestDatabase.history) }

        val response = client.get(ApiRoutes.PLAYER_SEARCH) { parameter(ApiRoutes.PARAM_NAME, "dantas") }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun jogadorConsultadoEntraNoIndice() = testApplication {
        val api = start()
        fake.on(CocFixtures.PLAYER_PATH, body = CocFixtures.player)

        api.get(ApiRoutes.player(CocFixtures.PLAYER_TAG))

        val expected = IndexedPlayer(CocFixtures.PLAYER_TAG, "Dantas", 15, CocFixtures.CLAN_TAG, "Picuí Raiz")
        assertEquals(listOf(listOf(expected)), index.recorded)
    }

    @Test
    fun membrosDoClaAbertoEntramNoIndiceNumaGravacaoSo() = testApplication {
        val api = start()
        fake.on(CocFixtures.CLAN_PATH, body = CocFixtures.clan)

        api.get(ApiRoutes.clan(CocFixtures.CLAN_TAG))

        val batch = index.recorded.single()
        assertEquals(listOf("ABC", CocFixtures.PLAYER_TAG), batch.map { it.tag })
        assertEquals(listOf("Maré Alta", "Dantas"), batch.map { it.name })
        assertEquals(listOf(16, 15), batch.map { it.townHallLevel })
        assertTrue(batch.all { it.clanTag == CocFixtures.CLAN_TAG && it.clanName == "Picuí Raiz" })
    }

    @Test
    fun respostaDoCacheNaoGravaDeNovo() = testApplication {
        val api = start()
        fake.on(CocFixtures.PLAYER_PATH, body = CocFixtures.player)

        api.get(ApiRoutes.player(CocFixtures.PLAYER_TAG))
        api.get(ApiRoutes.player(CocFixtures.PLAYER_TAG))

        assertEquals(1, index.recorded.size)
    }

    @Test
    fun erroDaApiNaoGravaNada() = testApplication {
        val api = start()

        api.get(ApiRoutes.player(CocFixtures.PLAYER_TAG)) // FakeCoc responde 404

        assertTrue(index.recorded.isEmpty())
    }
}
