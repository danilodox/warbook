package io.github.danilodox.warbook.api

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class HealthResponseTest {

    @Test
    fun serializaEDesserializaSemPerderCampos() {
        val original = HealthResponse(status = HealthResponse.STATUS_OK, version = "0.1.0")

        val json = Json.encodeToString(HealthResponse.serializer(), original)

        assertEquals("""{"status":"ok","version":"0.1.0"}""", json)
        assertEquals(original, Json.decodeFromString(HealthResponse.serializer(), json))
    }
}
