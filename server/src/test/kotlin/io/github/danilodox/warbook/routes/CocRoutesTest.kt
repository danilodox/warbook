package io.github.danilodox.warbook.routes

import io.github.danilodox.warbook.api.ApiError
import io.github.danilodox.warbook.api.ApiErrorCode
import io.github.danilodox.warbook.api.ApiRoutes
import io.github.danilodox.warbook.api.ClanDto
import io.github.danilodox.warbook.api.ClanRole
import io.github.danilodox.warbook.api.CurrentWarDto
import io.github.danilodox.warbook.api.PlayerDto
import io.github.danilodox.warbook.api.UnitLevelDto
import io.github.danilodox.warbook.api.WarFrequency
import io.github.danilodox.warbook.api.WarState
import io.github.danilodox.warbook.coc.CocService
import io.github.danilodox.warbook.module
import io.github.danilodox.warbook.testing.CocFixtures
import io.github.danilodox.warbook.testing.FakeCoc
import io.github.danilodox.warbook.testing.FakeTicker
import io.github.danilodox.warbook.testing.authorizedClient
import io.github.danilodox.warbook.testing.testConfig
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class CocRoutesTest {

    private val fake = FakeCoc()
    private val ticker = FakeTicker()

    private suspend fun ApplicationTestBuilder.start(): HttpClient {
        application { module(testConfig, fake.engine, ticker) }
        return authorizedClient()
    }

    private suspend fun <T> HttpResponse.decode(serializer: KSerializer<T>): T =
        Json.decodeFromString(serializer, bodyAsText())

    private suspend fun HttpResponse.error(): ApiError = decode(ApiError.serializer())

    @Test
    fun jogadorMapeiaParaODtoProprio() = testApplication {
        val api = start()
        fake.on(CocFixtures.PLAYER_PATH, body = CocFixtures.player)

        val response = api.get(ApiRoutes.player(CocFixtures.PLAYER_TAG))

        assertEquals(HttpStatusCode.OK, response.status)
        val player = response.decode(PlayerDto.serializer())
        assertEquals(CocFixtures.PLAYER_TAG, player.tag)
        assertEquals(15, player.townHallLevel)
        assertEquals(ClanRole.CO_LEADER, player.role)
        assertEquals(CocFixtures.CLAN_TAG, player.clan?.tag)
        assertEquals("https://img/m.png", player.clan?.badgeUrl)
        assertEquals(listOf("Barbarian King"), player.heroes.map { it.name }, "heróis da base do construtor ficam fora")
        assertEquals("Legend League", player.league)
        assertEquals(1240, player.donations)
        assertEquals(47, player.attackWins)
    }

    @Test
    fun tropasSeparamPetsMaquinasESemSupertropas() = testApplication {
        val api = start()
        fake.on(CocFixtures.PLAYER_PATH, body = CocFixtures.player)

        val player = api.get(ApiRoutes.player(CocFixtures.PLAYER_TAG)).decode(PlayerDto.serializer())

        assertEquals(listOf("Barbarian"), player.troops.map { it.name }, "sem supertropas e sem base do construtor")
        assertEquals(listOf("Wall Wrecker"), player.siegeMachines.map { it.name })
        assertEquals(listOf("L.A.S.S.I"), player.pets.map { it.name })
        assertEquals(listOf("Lightning Spell"), player.spells.map { it.name })
        assertEquals(UnitLevelDto("Earthquake Boots", 15, 18), player.heroEquipment.single())
    }

    @Test
    fun mandaATagCodificadaEAChaveNoCabecalho() = testApplication {
        val api = start()
        fake.on(CocFixtures.PLAYER_PATH, body = CocFixtures.player)

        api.get(ApiRoutes.player("2pq8lv9yc"))

        val request = fake.requests.single()
        assertEquals("api.clashofclans.com", request.url.host)
        assertEquals(CocFixtures.PLAYER_PATH, request.url.encodedPath)
        assertEquals("Bearer ${testConfig.cocApiKey}", request.headers[HttpHeaders.Authorization])
    }

    @Test
    fun claMapeiaMembrosELigas() = testApplication {
        val api = start()
        fake.on(CocFixtures.CLAN_PATH, body = CocFixtures.clan)

        val clan = api.get(ApiRoutes.clan(CocFixtures.CLAN_TAG)).decode(ClanDto.serializer())

        assertEquals(2, clan.memberCount)
        assertEquals("Crystal League I", clan.warLeague)
        assertEquals(WarFrequency.ALWAYS, clan.warFrequency)
        assertEquals(12, clan.requiredTownHallLevel)
        assertEquals(ClanRole.LEADER, clan.members.first().role)
        assertTrue(clan.isWarLogPublic)
    }

    @Test
    fun guerraEmAndamentoTrazPlacarHorariosEMapa() = testApplication {
        val api = start()
        fake.on(CocFixtures.WAR_PATH, body = CocFixtures.warInWar)

        val war = api.get(ApiRoutes.currentWar(CocFixtures.CLAN_TAG)).decode(CurrentWarDto.serializer())

        assertEquals(WarState.IN_WAR, war.state)
        assertEquals(1_793_707_200_000L, war.endTime, "2026-11-03T12:00:00Z")
        val clan = assertNotNull(war.clan)
        assertEquals(31, clan.stars)
        assertEquals(listOf(1, 2), clan.members.map { it.mapPosition }, "mapa ordenado pela posição")
        assertEquals("DEF", clan.members[1].attacks.single().defenderTag)
        assertEquals("Os Mito", war.opponent?.name)
    }

    @Test
    fun foraDeGuerraVemSoOEstado() = testApplication {
        val api = start()
        fake.on(CocFixtures.WAR_PATH, body = CocFixtures.WAR_NOT_IN_WAR)

        val war = api.get(ApiRoutes.currentWar(CocFixtures.CLAN_TAG)).decode(CurrentWarDto.serializer())

        assertEquals(CurrentWarDto(state = WarState.NOT_IN_WAR), war)
    }

    @Test
    fun tagInvalidaNemChegaNaApi() = testApplication {
        val api = start()

        val response = api.get(ApiRoutes.player("ABC!"))

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals(ApiErrorCode.INVALID_TAG, response.error().code)
        assertTrue(fake.requests.isEmpty())
    }

    @Test
    fun naoEncontradoViraNotFound() = testApplication {
        val api = start()
        fake.on(CocFixtures.PLAYER_PATH, HttpStatusCode.NotFound, """{"reason":"notFound"}""")

        val response = api.get(ApiRoutes.player(CocFixtures.PLAYER_TAG))

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertEquals(ApiErrorCode.NOT_FOUND, response.error().code)
    }

    @Test
    fun registroDeGuerraPrivadoViraWarLogPrivate() = testApplication {
        val api = start()
        fake.on(CocFixtures.WAR_PATH, HttpStatusCode.Forbidden, CocFixtures.ERROR_ACCESS_DENIED)

        val response = api.get(ApiRoutes.currentWar(CocFixtures.CLAN_TAG))

        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertEquals(ApiErrorCode.WAR_LOG_PRIVATE, response.error().code)
    }

    @Test
    fun chaveRecusadaPorIpNaoPareceRegistroPrivado() = testApplication {
        val api = start()
        fake.on(CocFixtures.WAR_PATH, HttpStatusCode.Forbidden, CocFixtures.ERROR_INVALID_IP)

        val response = api.get(ApiRoutes.currentWar(CocFixtures.CLAN_TAG))

        assertEquals(HttpStatusCode.BadGateway, response.status)
        assertEquals(ApiErrorCode.UPSTREAM_ERROR, response.error().code)
    }

    @Test
    fun manutencaoViraApiMaintenance() = testApplication {
        val api = start()
        fake.on(CocFixtures.CLAN_PATH, HttpStatusCode.ServiceUnavailable, CocFixtures.ERROR_MAINTENANCE)

        val response = api.get(ApiRoutes.clan(CocFixtures.CLAN_TAG))

        assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
        assertEquals(ApiErrorCode.API_MAINTENANCE, response.error().code)
    }

    @Test
    fun limiteDaApiViraRateLimited() = testApplication {
        val api = start()
        fake.on(CocFixtures.PLAYER_PATH, HttpStatusCode.TooManyRequests, CocFixtures.ERROR_THROTTLED)

        val response = api.get(ApiRoutes.player(CocFixtures.PLAYER_TAG))

        assertEquals(HttpStatusCode.TooManyRequests, response.status)
        assertEquals(ApiErrorCode.RATE_LIMITED, response.error().code)
    }

    @Test
    fun cacheEvitaNovaChamadaAteVencer() = testApplication {
        val api = start()
        fake.on(CocFixtures.PLAYER_PATH, body = CocFixtures.player)

        api.get(ApiRoutes.player(CocFixtures.PLAYER_TAG))
        api.get(ApiRoutes.player(CocFixtures.PLAYER_TAG))
        assertEquals(1, fake.requests.size)

        ticker.advance(CocService.PLAYER_TTL + 1.seconds)
        api.get(ApiRoutes.player(CocFixtures.PLAYER_TAG))
        assertEquals(2, fake.requests.size)
    }

    @Test
    fun cacheDaGuerraVenceAntesDoDoJogador() = testApplication {
        val api = start()
        fake.on(CocFixtures.WAR_PATH, body = CocFixtures.warInWar)

        api.get(ApiRoutes.currentWar(CocFixtures.CLAN_TAG))
        ticker.advance(CocService.WAR_TTL + 1.seconds)
        api.get(ApiRoutes.currentWar(CocFixtures.CLAN_TAG))

        assertEquals(2, fake.requests.size)
    }

    @Test
    fun erroNaoEntraNoCache() = testApplication {
        val api = start()
        fake.on(CocFixtures.PLAYER_PATH, HttpStatusCode.ServiceUnavailable, CocFixtures.ERROR_MAINTENANCE)

        api.get(ApiRoutes.player(CocFixtures.PLAYER_TAG))
        api.get(ApiRoutes.player(CocFixtures.PLAYER_TAG))

        assertEquals(2, fake.requests.size)
    }
}
