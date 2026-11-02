package io.github.danilodox.warbook.auth

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.principal
import io.ktor.server.plugins.origin
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.ratelimit.RateLimitName
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/** Limites contra uso do servidor como proxy grátis. Acima deles a resposta é 429 `RATE_LIMITED`. */
data class RateLimits(
    val installsPerHourPerIp: Int = 10,
    val perMinutePerInstall: Int = 60,
    val perMinutePerIp: Int = 120,
    /** A verificação é rara (uma vez por conta) e o alvo óbvio de quem tenta adivinhar tokens. */
    val verifyPerHourPerInstall: Int = 5,
    val verifyPerHourPerIp: Int = 10,
) {
    companion object {
        val INSTALLS = RateLimitName("installs-por-ip")
        val PER_IP = RateLimitName("api-por-ip")
        val PER_INSTALL = RateLimitName("api-por-instalacao")
        val VERIFY_PER_IP = RateLimitName("verify-por-ip")
        val VERIFY_PER_INSTALL = RateLimitName("verify-por-instalacao")
    }
}

fun Application.installRateLimits(limits: RateLimits) {
    install(RateLimit) {
        register(RateLimits.INSTALLS) {
            rateLimiter(limit = limits.installsPerHourPerIp, refillPeriod = 1.hours)
            requestKey { call -> call.request.origin.remoteHost }
        }
        register(RateLimits.PER_IP) {
            rateLimiter(limit = limits.perMinutePerIp, refillPeriod = 1.minutes)
            requestKey { call -> call.request.origin.remoteHost }
        }
        register(RateLimits.PER_INSTALL) {
            rateLimiter(limit = limits.perMinutePerInstall, refillPeriod = 1.minutes)
            requestKey { call -> call.principal<InstallPrincipal>()?.installId ?: call.request.origin.remoteHost }
        }
        register(RateLimits.VERIFY_PER_IP) {
            rateLimiter(limit = limits.verifyPerHourPerIp, refillPeriod = 1.hours)
            requestKey { call -> call.request.origin.remoteHost }
        }
        register(RateLimits.VERIFY_PER_INSTALL) {
            rateLimiter(limit = limits.verifyPerHourPerInstall, refillPeriod = 1.hours)
            requestKey { call -> call.principal<InstallPrincipal>()?.installId ?: call.request.origin.remoteHost }
        }
    }
}
