package io.github.danilodox.warbook.api

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class ApiErrorTest {

    @Test
    fun serializaNoFormatoCodeMessage() {
        val error = ApiError(ApiErrorCode.WAR_LOG_PRIVATE, "Registro de guerra privado")

        val json = Json.encodeToString(ApiError.serializer(), error)

        assertEquals("""{"code":"WAR_LOG_PRIVATE","message":"Registro de guerra privado"}""", json)
        assertEquals(error, Json.decodeFromString(ApiError.serializer(), json))
    }

    @Test
    fun guerraForaDeGuerraSoPrecisaDoEstado() {
        val war = Json.decodeFromString(CurrentWarDto.serializer(), """{"state":"NOT_IN_WAR"}""")

        assertEquals(CurrentWarDto(state = WarState.NOT_IN_WAR), war)
    }
}
