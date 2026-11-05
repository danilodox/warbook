package io.github.danilodox.warbook.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import java.time.Instant

/**
 * Prova de verificação de conta: JWT HS256 com o mesmo `JWT_SECRET`, sem banco e sem validade.
 * A audience é diferente da do token da instalação, então um não serve no lugar do outro.
 * Claims: `sub` = tag do jogador (sem `#`), `iat` = data da verificação, `installId` = instalação que verificou.
 */
class VerificationTokens(secret: String, private val clock: () -> Instant = Instant::now) {
    private val algorithm = Algorithm.HMAC256(secret)

    /** Usado na v0.8 para autorizar o monitoramento do clã. */
    val verifier: JWTVerifier = JWT.require(algorithm).withIssuer(InstallTokens.ISSUER).withAudience(AUDIENCE).build()

    fun issue(playerTag: String, installId: String): String = JWT.create()
        .withIssuer(InstallTokens.ISSUER)
        .withAudience(AUDIENCE)
        .withSubject(playerTag)
        .withClaim(CLAIM_INSTALL_ID, installId)
        .withIssuedAt(clock())
        .sign(algorithm)

    companion object {
        const val AUDIENCE = "warbook-verification"
        const val CLAIM_INSTALL_ID = "installId"
    }
}
