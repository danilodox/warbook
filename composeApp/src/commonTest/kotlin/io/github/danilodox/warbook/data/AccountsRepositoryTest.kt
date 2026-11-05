package io.github.danilodox.warbook.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AccountsRepositoryTest {

    private val accountDao = FakeAccountDao()
    private val cacheDao = FakePlayerCacheDao()
    private val api = FakeWarbookRepository()
    private var now = 1_000L
    private val profiles = RoomProfileRepository(cacheDao, accountDao, api) { now }
    private val repository = RoomAccountsRepository(accountDao, profiles, api) { now++ }

    private suspend fun accounts() = repository.accounts.first()

    @Test
    fun primeiraContaEntraAtivaENaoVerificadaESalvaOPerfil() = runTest {
        repository.add(testPlayer())

        val account = accounts().single()
        assertEquals("Dantas", account.name)
        assertEquals("Picuí Raiz", account.clanName)
        assertTrue(account.isActive)
        assertFalse(account.isVerified)
        assertEquals("Dantas", profiles.observe("2PQ8LV9YC").first()?.player?.name)
    }

    @Test
    fun contaNovaViraAAtivaEAsOutrasNao() = runTest {
        repository.add(testPlayer())
        repository.add(testPlayer(tag = "9CJ2YQ0L", name = "Dantas Mini"))

        assertEquals(listOf("2PQ8LV9YC" to false, "9CJ2YQ0L" to true), accounts().map { it.tag to it.isActive })
    }

    @Test
    fun ativarTrocaAContaAtiva() = runTest {
        repository.add(testPlayer())
        repository.add(testPlayer(tag = "9CJ2YQ0L"))

        repository.activate("2PQ8LV9YC")

        assertEquals("2PQ8LV9YC", accounts().single { it.isActive }.tag)
    }

    @Test
    fun adicionarDeNovoMantemVerificacaoEOrdem() = runTest {
        repository.add(testPlayer())
        repository.add(testPlayer(tag = "9CJ2YQ0L"))
        repository.verify("2PQ8LV9YC", "token-do-jogo")

        repository.add(testPlayer(name = "Dantas Renomeado"))

        val first = accounts().first()
        assertEquals("Dantas Renomeado", first.name)
        assertTrue(first.isVerified)
        assertTrue(first.isActive)
    }

    @Test
    fun removerAAtivaPassaParaAMaisAntigaEApagaOPerfil() = runTest {
        repository.add(testPlayer())
        repository.add(testPlayer(tag = "9CJ2YQ0L"))
        repository.add(testPlayer(tag = "QQ8LV9YC"))

        repository.remove("QQ8LV9YC")

        assertEquals(listOf("2PQ8LV9YC" to true, "9CJ2YQ0L" to false), accounts().map { it.tag to it.isActive })
        assertNull(profiles.observe("QQ8LV9YC").first())
    }

    @Test
    fun removerAUltimaDeixaSemConta() = runTest {
        repository.add(testPlayer())

        repository.remove("2PQ8LV9YC")

        assertTrue(accounts().isEmpty())
    }

    @Test
    fun verificacaoComSucessoGuardaSoAProva() = runTest {
        repository.add(testPlayer())

        val result = repository.verify("2PQ8LV9YC", "token-do-jogo")

        assertEquals(ApiResult.Success(Unit), result)
        assertTrue(accounts().single().isVerified)
        assertEquals("prova-assinada", accountDao.rows.value.single().verificationToken)
        assertFalse("token-do-jogo" in accountDao.rows.value.toString(), "o token do jogo não é guardado")
    }

    @Test
    fun verificacaoRecusadaNaoMarcaAConta() = runTest {
        repository.add(testPlayer())
        api.verifyResult = ApiResult.Failure(AppError.INVALID_VERIFY_TOKEN)

        val result = repository.verify("2PQ8LV9YC", "token-errado")

        assertEquals(ApiResult.Failure(AppError.INVALID_VERIFY_TOKEN), result)
        assertFalse(accounts().single().isVerified)
    }
}
