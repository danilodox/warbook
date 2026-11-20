package io.github.danilodox.warbook.history

import io.github.danilodox.warbook.api.WarState
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.toJavaDuration

// Histórico de guerras guardado no Postgres. Tags sem `#`, menos o warTag da CWL, que é usado como vem.

enum class WarType { WAR, CWL }

/** [WARLOG]: só o resultado (importado ao ativar). [SNAPSHOT]: retrato completo, com membros e ataques. */
enum class WarSource { WARLOG, SNAPSHOT }

enum class WarResult { WIN, LOSE, TIE }

/** Clã monitorado. [isActive] é calculado: pausa sozinho depois de [PAUSE_AFTER] sem membro verificado. */
data class MonitoredClan(
    val clanTag: String,
    val clanName: String,
    val activatedBy: String,
    val activatedAt: Instant,
    val lastSeenAt: Instant,
    val warlogImportedAt: Instant?,
) {
    fun isActive(now: Instant): Boolean = lastSeenAt.plus(PAUSE_AFTER.toJavaDuration()) > now

    companion object {
        val PAUSE_AFTER: Duration = 30.days
    }
}

/** Um lado da guerra. [attacks] é nulo quando a fonte não diz (warlog). */
data class WarSide(
    val tag: String?,
    val name: String?,
    val badgeUrl: String? = null,
    val stars: Int,
    val destruction: Double,
    val attacks: Int? = null,
)

data class WarRecordMember(
    val tag: String,
    val isOpponent: Boolean,
    val name: String,
    val townHall: Int,
    val mapPosition: Int,
)

data class WarRecordAttack(
    val order: Int,
    val attackerTag: String,
    val defenderTag: String,
    val stars: Int,
    val destruction: Int,
    val duration: Int? = null,
)

/**
 * Uma guerra do ponto de vista do clã monitorado: [clan] = nós, [opponent] = eles.
 * Identidade no banco: clã + [endTime]. [id] só vem preenchido na leitura.
 */
data class WarRecord(
    val type: WarType,
    val source: WarSource,
    val state: WarState,
    val teamSize: Int,
    val attacksPerMember: Int,
    val endTime: Instant,
    val clan: WarSide,
    val opponent: WarSide,
    val capturedAt: Instant,
    val preparationStartTime: Instant? = null,
    val startTime: Instant? = null,
    val warTag: String? = null,
    val cwlSeason: String? = null,
    val result: WarResult? = null,
    val members: List<WarRecordMember> = emptyList(),
    val attacks: List<WarRecordAttack> = emptyList(),
    val id: Long? = null,
)
