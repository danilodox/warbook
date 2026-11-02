package io.github.danilodox.warbook

import io.github.danilodox.warbook.api.ApiRoutes
import io.github.danilodox.warbook.api.HealthResponse
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApplicationTest {

    @Test
    fun healthRespondeOkEmJson() = testApplication {
        application { module() }

        val response = client.get(ApiRoutes.HEALTH)

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.contentType()!!.match(ContentType.Application.Json))
        val body = Json.decodeFromString(HealthResponse.serializer(), response.bodyAsText())
        assertEquals(HealthResponse.STATUS_OK, body.status)
        assertTrue(body.version.isNotBlank())
    }
}
