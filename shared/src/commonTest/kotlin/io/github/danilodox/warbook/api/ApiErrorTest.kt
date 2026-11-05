package io.github.danilodox.warbook.api

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ApiErrorTest {

    @Test
    fun serializaNoFormatoCodeMessage() {
        val error = ApiError(ApiErrorCode.WAR_LOG_PRIVATE, "Registro de guerra privado")

        val json = Json.encodeToString(ApiError.serializer(), error)

        assertEquals("""{"code":"WAR_LOG_PRIVATE","message":"Registro de guerra privado"}""", json)
        assertEquals(error, Json.decodeFromString(ApiError.serializer(), json))
    }

    @Test
    fun pedidoDeVerificacaoNaoMostraOTokenNoToString() {
        val token = "tk".repeat(4)

        assertFalse(token in VerifyRequest(token).toString())
        assertEquals("""{"token":"$token"}""", Json.encodeToString(VerifyRequest.serializer(), VerifyRequest(token)))
    }

    @Test
    fun jogadorDoServidorAntigoSemOsCamposNovosAindaDecodifica() {
        val json = """
            {"tag":"2PQ8LV9YC","name":"Dantas","townHallLevel":15,"expLevel":212,"trophies":4812,
             "warStars":1384,"attackWins":47,"donations":1240,"donationsReceived":860}
        """.trimIndent()

        val player = Json.decodeFromString(PlayerDto.serializer(), json)

        assertEquals(emptyList(), player.pets)
        assertEquals(null, player.league)
    }

    @Test
    fun guerraForaDeGuerraSoPrecisaDoEstado() {
        val war = Json.decodeFromString(CurrentWarDto.serializer(), """{"state":"NOT_IN_WAR"}""")

        assertEquals(CurrentWarDto(state = WarState.NOT_IN_WAR), war)
    }
}
