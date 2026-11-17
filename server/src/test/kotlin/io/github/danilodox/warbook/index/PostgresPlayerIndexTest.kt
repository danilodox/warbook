package io.github.danilodox.warbook.index

import io.github.danilodox.warbook.testing.TestDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PostgresPlayerIndexTest {

    private val job = Job()
    private val scope = CoroutineScope(job)
    private val seenAt = Instant.parse("2026-11-08T12:00:00Z")
    private val index = PostgresPlayerIndex(TestDatabase.database, scope, Clock.fixed(seenAt, ZoneOffset.UTC))

    private fun player(tag: String, name: String, th: Int = 15, clanTag: String? = "2Y0", clanName: String? = "PURY") =
        IndexedPlayer(tag, name, th, clanTag, clanName)

    @BeforeTest
    fun clean() = TestDatabase.clean()

    @Test
    fun achaPeloNickSemAcentoNemMaiuscula() = runBlocking {
        index.upsert(listOf(player("P1", "JoÃo Guerreiro"), player("P2", "Maria")))

        val found = index.search("joao")

        assertEquals(listOf("P1"), found.map { it.tag })
        val joao = found.single()
        assertEquals("JoÃo Guerreiro", joao.name) // nick original
        assertEquals(15, joao.townHallLevel)
        assertEquals("2Y0", joao.clanTag)
        assertEquals("PURY", joao.clanName)
        assertEquals(seenAt.toEpochMilli(), joao.seenAt)
    }

    @Test
    fun achaNickEstilizadoDigitandoNormal() = runBlocking {
        index.upsert(listOf(player("P1", "𝓓𝓪𝓷𝓲𝓵𝓸"), player("P2", "ᴅᴀɴɪʟᴏ"), player("P3", "Outro")))

        assertEquals(setOf("P1", "P2"), index.search("Danilo").map { it.tag }.toSet())
    }

    @Test
    fun igualVemAntesDeComecaComQueVemAntesDeParecido() = runBlocking {
        index.upsert(
            listOf(
                player("P1", "o danilo da guerra"),
                player("P2", "Danilo"),
                player("P3", "danilo2"),
            ),
        )

        assertEquals(listOf("P2", "P3", "P1"), index.search("danilo").map { it.tag })
    }

    @Test
    fun toleraErroDeDigitacao() = runBlocking {
        index.upsert(listOf(player("P1", "Guerreiro")))

        assertEquals(listOf("P1"), index.search("gerreiro").map { it.tag })
    }

    @Test
    fun trechoCurtoNoMeioDoNickAcha() = runBlocking {
        index.upsert(listOf(player("P1", "SuperBoss"), player("P2", "Nada")))

        assertEquals(listOf("P1"), index.search("boss").map { it.tag })
    }

    @Test
    fun curingasDoLikeSaoLetrasComuns() = runBlocking {
        index.upsert(listOf(player("P1", "%%%"), player("P2", "abc")))

        assertEquals(listOf("P1"), index.search("%%%").map { it.tag })
    }

    @Test
    fun noMaximoVinteResultados() = runBlocking {
        index.upsert((1..30).map { player("P$it", "Clash $it") })

        assertEquals(20, index.search("clash").size)
    }

    @Test
    fun atualizaOJogadorQueJaEstava() = runBlocking {
        index.upsert(listOf(player("P1", "Danilo", th = 14, clanTag = "2Y0", clanName = "PURY")))
        index.upsert(listOf(player("P1", "Danilo BR", th = 15, clanTag = null, clanName = null)))

        val danilo = index.search("danilo").single()
        assertEquals("Danilo BR", danilo.name)
        assertEquals(15, danilo.townHallLevel)
        assertNull(danilo.clanTag)
    }

    @Test
    fun tagRepetidaNoMesmoLoteNaoQuebra() = runBlocking {
        index.upsert(listOf(player("P1", "Danilo"), player("P1", "Danilo")))

        assertEquals(1, index.search("danilo").size)
    }

    @Test
    fun recordGravaEmSegundoPlano() = runBlocking {
        index.record(listOf(player("P1", "Danilo")))
        job.children.forEach { it.join() }

        assertEquals(listOf("P1"), index.search("danilo").map { it.tag })
    }

    @Test
    fun recordComBancoForaDoArSoViraLog() = runBlocking {
        val broken = org.jetbrains.exposed.v1.jdbc.Database.connect(
            url = "jdbc:postgresql://localhost:1/warbook",
            user = "warbook",
            password = "x",
        )
        val brokenIndex = PostgresPlayerIndex(broken, scope)

        brokenIndex.record(listOf(player("P1", "Danilo")))
        job.children.forEach { it.join() }

        assertTrue(job.isActive) // o erro não derrubou o escopo das gravações
    }
}
