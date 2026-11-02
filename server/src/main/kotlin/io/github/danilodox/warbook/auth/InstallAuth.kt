package io.github.danilodox.warbook.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import io.github.danilodox.warbook.api.ApiError
import io.github.danilodox.warbook.api.ApiErrorCode
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.response.header
import io.ktor.server.response.respond
import java.time.Instant
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.toJavaDuration

/** Instalação do app identificada pelo token (`sub` do JWT). */
data class InstallPrincipal(val installId: String)

/**
 * Tokens por instalação: JWT HS256 assinado com `JWT_SECRET`, sem banco.
 * Não prova que o pedido vem do app (isso é Play Integrity, v0.9); serve para limitar por instalação.
 */
class InstallTokens(secret: String, private val clock: () -> Instant = Instant::now) {
    private val algorithm = Algorithm.HMAC256(secret)

    val verifier: JWTVerifier = JWT.require(algorithm).withIssuer(ISSUER).withAudience(AUDIENCE).build()

    fun issue(): String {
        val now = clock()
        return JWT.create()
            .withIssuer(ISSUER)
            .withAudience(AUDIENCE)
            .withSubject(UUID.randomUUID().toString())
            .withIssuedAt(now)
            .withExpiresAt(now + VALIDITY.toJavaDuration())
            .sign(algorithm)
    }

    companion object {
        const val AUTH_NAME = "install"
        const val ISSUER = "warbook-server"
        const val AUDIENCE = "warbook-app"
        val VALIDITY: Duration = 365.days
    }
}

fun Application.installAuthentication(tokens: InstallTokens) {
    install(Authentication) {
        jwt(InstallTokens.AUTH_NAME) {
            verifier(tokens.verifier)
            validate { credential -> credential.payload.subject?.let(::InstallPrincipal) }
            challenge { _, _ ->
                // Com este cabeçalho o plugin Auth do app pede um token novo e repete a chamada.
                call.response.header(HttpHeaders.WWWAuthenticate, "Bearer realm=\"warbook\"")
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ApiError(ApiErrorCode.UNAUTHORIZED, "Token da instalação ausente ou inválido"),
                )
            }
        }
    }
}
