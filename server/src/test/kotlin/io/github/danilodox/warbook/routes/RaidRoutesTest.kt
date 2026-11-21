package io.github.danilodox.warbook.routes

import io.github.danilodox.warbook.api.ApiError
import io.github.danilodox.warbook.api.ApiErrorCode
import io.github.danilodox.warbook.api.ApiRoutes
import io.github.danilodox.warbook.api.CapitalRaidsDto
import io.github.danilodox.warbook.api.RaidAbsentMemberDto
import io.github.danilodox.warbook.api.RaidClanDto
import io.github.danilodox.warbook.api.RaidState
import io.github.danilodox.warbook.coc.CocService
import io.github.danilodox.warbook.module
import io.github.danilodox.warbook.testing.CocFixtures
import io.github.danilodox.warbook.testing.FakeCoc
import io.github.danilodox.warbook.testing.FakePlayerIndex
import io.github.danilodox.warbook.testing.FakeTicker
import io.github.danilodox.warbook.testing.RaidFixtures
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
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class RaidRoutesTest {

    private val fake = FakeCoc()
    private val ticker = FakeTicker()

    private suspend fun ApplicationTestBuilder.start(): HttpClient {
        application {
            module(testConfig, fake.engine, ticker, playerIndex = FakePlayerIndex(), warHistory = TestDatabase.history)
        }
        return authorizedClient()
    }

    private suspend fun HttpResponse.raids(): CapitalRaidsDto =
        Json.decodeFromString(CapitalRaidsDto.serializer(), bodyAsText())

    private suspend fun HttpClient.getRaids() = get(ApiRoutes.capitalRaids(CocFixtures.CLAN_TAG))

    @Test
    fun raidMapeiaTotaisMembrosEClas() = testApplication {
        val api = start()
        fake.on(RaidFixtures.RAID_PATH, body = RaidFixtures.seasons)
        fake.on(CocFixtures.CLAN_PATH, body = CocFixtures.clan)

        val response = api.getRaids()

        assertEquals(HttpStatusCode.OK, response.status)
        val (current, previous) = response.raids().seasons
        assertEquals(RaidState.ONGOING, current.state)
        assertEquals(RaidState.ENDED, previous.state)
        assertEquals(1_794_553_200_000, current.startTime) // 2026-11-13 07:00 UTC
        assertEquals(52000, current.capitalTotalLoot)
        assertEquals(7, current.enemyDistrictsDestroyed)
        assertEquals(listOf(RaidClanDto("DEF1", "Alvo", 9, "https://img/m.png", 9, 8, 7)), current.attackLog)
        assertEquals(listOf(RaidClanDto("ATK1", "Invasor", 12, null, 20, 9, 9)), current.defenseLog)
    }

    @Test
    fun membrosPorOuroComOBonusSomadoAoLimite() = testApplication {
        val api = start()
        fake.on(RaidFixtures.RAID_PATH, body = RaidFixtures.seasons)
        fake.on(CocFixtures.CLAN_PATH, body = CocFixtures.clan)

        val members = api.getRaids().raids().seasons.first().members

        assertEquals(listOf("ABC", "LOW"), members.map { it.tag }, "quem mais saqueou primeiro, tag sem #")
        assertEquals(listOf(6 to 6, 3 to 5), members.map { it.attacks to it.attackLimit })
    }

    @Test
    fun medalhasSaoPorAtaqueMaisDefesaSoNoRaidEncerrado() = testApplication {
        val api = start()
        fake.on(RaidFixtures.RAID_PATH, body = RaidFixtures.seasons)
        fake.on(CocFixtures.CLAN_PATH, body = CocFixtures.clan)

        val (current, previous) = api.getRaids().raids().seasons

        assertTrue(current.members.all { it.medals == 0 }, "em andamento a API manda as recompensas zeradas")
        assertEquals(148, previous.offensiveReward)
        assertEquals(176, previous.defensiveReward)
        assertEquals(148 * 6 + 176, previous.members.single().medals)
    }

    @Test
    fun quemNaoAtacouSaiDosMembrosAtuaisSoNoRaidMaisRecente() = testApplication {
        val api = start()
        fake.on(RaidFixtures.RAID_PATH, body = RaidFixtures.seasons)
        fake.on(CocFixtures.CLAN_PATH, body = CocFixtures.clan)

        val (current, previous) = api.getRaids().raids().seasons

        assertEquals(listOf(RaidAbsentMemberDto(CocFixtures.PLAYER_TAG, "Dantas", 15)), current.notAttacked)
        assertTrue(previous.notAttacked.isEmpty())
    }

    @Test
    fun claForaDoArAindaMostraORaidSemQuemNaoAtacou() = testApplication {
        val api = start()
        fake.on(RaidFixtures.RAID_PATH, body = RaidFixtures.seasons)
        // Sem resposta para o clã: o FakeCoc devolve 404.

        val response = api.getRaids()

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.raids().seasons.first().notAttacked.isEmpty())
    }

    @Test
    fun pedeSoAsUltimasTemporadas() = testApplication {
        val api = start()
        fake.on(RaidFixtures.RAID_PATH, body = RaidFixtures.EMPTY)

        val response = api.getRaids()

        assertTrue(response.raids().seasons.isEmpty())
        val request = fake.requests.first { it.url.encodedPath == RaidFixtures.RAID_PATH }
        assertEquals(CocService.RAID_SEASONS.toString(), request.url.parameters["limit"])
    }

    @Test
    fun cacheEvitaNovaChamadaAteVencer() = testApplication {
        val api = start()
        fake.on(RaidFixtures.RAID_PATH, body = RaidFixtures.seasons)
        fake.on(CocFixtures.CLAN_PATH, body = CocFixtures.clan)
        fun raidCalls() = fake.requests.count { it.url.encodedPath == RaidFixtures.RAID_PATH }

        api.getRaids()
        api.getRaids()
        assertEquals(1, raidCalls())

        ticker.advance(CocService.RAID_TTL + 1.seconds)
        api.getRaids()
        assertEquals(2, raidCalls())
    }

    @Test
    fun registroPrivadoSeAApiRecusarViraWarLogPrivate() = testApplication {
        val api = start()
        fake.on(RaidFixtures.RAID_PATH, HttpStatusCode.Forbidden, CocFixtures.ERROR_ACCESS_DENIED)

        val response = api.getRaids()

        assertEquals(HttpStatusCode.Forbidden, response.status)
        val error = Json.decodeFromString(ApiError.serializer(), response.bodyAsText())
        assertEquals(ApiErrorCode.WAR_LOG_PRIVATE, error.code)
    }

    @Test
    fun tagInvalidaNemChegaNaApi() = testApplication {
        val api = start()

        val response = api.get(ApiRoutes.capitalRaids("XYZ"))

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(fake.requests.isEmpty())
    }
}
