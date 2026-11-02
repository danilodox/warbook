package io.github.danilodox.warbook.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ServerConfigTest {

    private val apiKey = "chave-de-teste"

    // Montado com repeat para não parecer chave real ao Gitleaks.
    private val jwtSecret = "teste".repeat(8)

    @Test
    fun leAsDuasVariaveis() {
        val config = ServerConfig.fromEnv(mapOf("COC_API_KEY" to apiKey, "JWT_SECRET" to jwtSecret)::get)

        assertEquals(apiKey, config.cocApiKey)
        assertEquals(jwtSecret, config.jwtSecret)
    }

    @Test
    fun falhaComMensagemClaraQuandoFaltaTudo() {
        val error = assertFailsWith<InvalidConfigException> { ServerConfig.fromEnv { null } }

        assertTrue("COC_API_KEY não definida" in error.message!!)
        assertTrue("JWT_SECRET não definida" in error.message!!)
        assertTrue("server/.env.example" in error.message!!)
    }

    @Test
    fun recusaSegredoCurto() {
        val error = assertFailsWith<InvalidConfigException> {
            ServerConfig.fromEnv(mapOf("COC_API_KEY" to apiKey, "JWT_SECRET" to "curto")::get)
        }

        assertTrue("pelo menos 32" in error.message!!)
        assertFalse("curto" in error.message!!)
    }

    @Test
    fun toStringNaoMostraSegredos() {
        val text = ServerConfig(cocApiKey = apiKey, jwtSecret = jwtSecret).toString()

        assertFalse(apiKey in text)
        assertFalse(jwtSecret in text)
    }
}
