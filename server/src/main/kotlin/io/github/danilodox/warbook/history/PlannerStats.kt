package io.github.danilodox.warbook.history

import io.github.danilodox.warbook.api.CwlPlannerDto
import io.github.danilodox.warbook.api.PlannerStatsDto
import kotlin.math.round

/**
 * Estatísticas para o planejador da CWL: por membro do nosso lado, a média da nota, as guerras e os ataques
 * usados e disponíveis. [wars] são guerras encerradas com retrato (comuns e CWL), da mais recente para a
 * mais antiga.
 */
object PlannerStats {

    private const val CENTS = 100.0

    fun of(wars: List<WarRecord>): CwlPlannerDto {
        val lines = wars.flatMap { war ->
            val ours = war.members.filterNot { it.isOpponent }.associateBy { it.tag }
            war.scores().map { score -> Triple(ours.getValue(score.tag), score, war.attacksPerMember) }
        }
        // O nome da guerra mais recente em que o membro aparece.
        val members = lines.groupBy { it.first.tag }.map { (tag, entries) ->
            PlannerStatsDto(
                tag = tag,
                name = entries.first().first.name,
                wars = entries.size,
                attacksUsed = entries.sumOf { it.second.attacks.size },
                attacksAvailable = entries.sumOf { it.third },
                average = round(entries.map { it.second.total }.average() * CENTS) / CENTS,
            )
        }
        return CwlPlannerDto(warsCounted = wars.size, members = members)
    }
}
