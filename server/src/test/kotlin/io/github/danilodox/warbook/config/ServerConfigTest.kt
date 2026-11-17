package io.github.danilodox.warbook.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ServerConfigTest {

    private val apiKey = "chave-de-teste"

    // Montados com repeat para não parecerem segredos reais ao Gitleaks.
    private val jwtSecret = "teste".repeat(8)
    private val dbPassword = "senha".repeat(3)

    private val complete = mapOf(
        "COC_API_KEY" to apiKey,
        "JWT_SECRET" to jwtSecret,
        "DATABASE_URL" to "jdbc:postgresql://localhost:5432/warbook",
        "DATABASE_USER" to "warbook",
        "DATABASE_PASSWORD" to dbPassword,
    )

    @Test
    fun leTodasAsVariaveis() {
        val config = ServerConfig.fromEnv(complete::get)

        assertEquals(apiKey, config.cocApiKey)
        assertEquals(jwtSecret, config.jwtSecret)
        assertEquals("jdbc:postgresql://localhost:5432/warbook", config.database.url)
        assertEquals("warbook", config.database.user)
        assertEquals(dbPassword, config.database.password)
    }

    @Test
    fun falhaComMensagemClaraQuandoFaltaTudo() {
        val error = assertFailsWith<InvalidConfigException> { ServerConfig.fromEnv { null } }

        assertTrue("COC_API_KEY não definida" in error.message!!)
        assertTrue("JWT_SECRET não definida" in error.message!!)
        assertTrue("DATABASE_URL não definida" in error.message!!)
        assertTrue("DATABASE_USER não definida" in error.message!!)
        assertTrue("DATABASE_PASSWORD não definida" in error.message!!)
        assertTrue("server/.env.example" in error.message!!)
    }

    @Test
    fun faltaSoOBancoDizSoOBanco() {
        val env = complete - "DATABASE_PASSWORD"
        val error = assertFailsWith<InvalidConfigException> { ServerConfig.fromEnv(env::get) }

        assertTrue("DATABASE_PASSWORD não definida" in error.message!!)
        assertFalse("COC_API_KEY" in error.message!!)
    }

    @Test
    fun recusaUrlQueNaoEDoPostgres() {
        val env = complete + ("DATABASE_URL" to "postgres://localhost/warbook")
        val error = assertFailsWith<InvalidConfigException> { ServerConfig.fromEnv(env::get) }

        assertTrue("jdbc:postgresql://" in error.message!!)
    }

    @Test
    fun recusaSegredoCurto() {
        val env = complete + ("JWT_SECRET" to "curto")
        val error = assertFailsWith<InvalidConfigException> { ServerConfig.fromEnv(env::get) }

        assertTrue("pelo menos 32" in error.message!!)
        assertFalse("curto" in error.message!!)
    }

    @Test
    fun toStringNaoMostraSegredos() {
        val text = ServerConfig.fromEnv(complete::get).toString()

        assertFalse(apiKey in text)
        assertFalse(jwtSecret in text)
        assertFalse(dbPassword in text)
        assertTrue("warbook" in text)
    }
}
