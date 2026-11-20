package io.github.danilodox.warbook.history

import com.auth0.jwt.exceptions.JWTVerificationException
import io.github.danilodox.warbook.api.ApiErrorCode
import io.github.danilodox.warbook.api.MonitoringDto
import io.github.danilodox.warbook.api.MonitoringState
import io.github.danilodox.warbook.api.TagNormalizer
import io.github.danilodox.warbook.auth.VerificationTokens
import io.github.danilodox.warbook.coc.CocService
import io.github.danilodox.warbook.errors.ApiException
import java.time.Clock
import java.time.Instant
import kotlin.time.toJavaDuration

/**
 * Liga, reativa e renova o monitoramento do clã. Só um membro verificado (prova JWT da v0.3) consegue, e o
 * clã precisa ter o registro de guerra público. Ninguém desliga; pausa sozinho (ver [MonitoredClan.isActive]).
 *
 * [onActivated] roda na primeira ativação de cada clã (importação do warlog).
 */
class MonitoringService(
    private val history: WarHistoryRepository,
    private val coc: CocService,
    private val verificationTokens: VerificationTokens,
    private val clock: Clock = Clock.systemUTC(),
    private val onActivated: suspend (MonitoredClan) -> Unit = {},
) {
    suspend fun status(clanTag: String): MonitoringDto =
        history.monitoredClan(clanTag)?.toDto(clock.instant()) ?: MonitoringDto(MonitoringState.OFF)

    /**
     * Um membro verificado abriu o app ou tocou em Ativar/Reativar. A lista de membros e o registro vêm do
     * cache do clã (até 5 min): quem acabou de entrar no clã pode precisar esperar um pouco.
     */
    suspend fun recordVisit(clanTag: String, verificationToken: String): MonitoringDto {
        val playerTag = verifiedPlayer(verificationToken)
        val clan = coc.clan(clanTag)
        if (clan.members.none { it.tag == playerTag }) {
            throw ApiException(ApiErrorCode.NOT_CLAN_MEMBER, "A conta verificada não é membro deste clã")
        }
        if (!clan.isWarLogPublic) throw ApiException(ApiErrorCode.WAR_LOG_PRIVATE, "Registro de guerra privado")
        val now = clock.instant()
        val visit = history.recordVerifiedVisit(clanTag, clan.name, playerTag, now)
        if (visit.newlyActivated) onActivated(visit.clan)
        return visit.clan.toDto(now)
    }

    /** Tag do jogador da prova. Prova de outro servidor, adulterada ou um token de instalação não servem. */
    private fun verifiedPlayer(token: String): String {
        val subject = try {
            verificationTokens.verifier.verify(token).subject
        } catch (_: JWTVerificationException) {
            null
        }
        return subject?.let(TagNormalizer::normalize)
            ?: throw ApiException(ApiErrorCode.NOT_VERIFIED, "Prova de verificação inválida")
    }

    private fun MonitoredClan.toDto(now: Instant): MonitoringDto = MonitoringDto(
        state = if (isActive(now)) MonitoringState.ACTIVE else MonitoringState.PAUSED,
        activatedAt = activatedAt.toEpochMilli(),
        lastSeenAt = lastSeenAt.toEpochMilli(),
        pausesAt = lastSeenAt.plus(MonitoredClan.PAUSE_AFTER.toJavaDuration()).toEpochMilli(),
        warlogImported = warlogImportedAt != null,
    )
}
