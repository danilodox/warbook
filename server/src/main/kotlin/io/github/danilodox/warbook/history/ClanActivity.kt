package io.github.danilodox.warbook.history

import io.github.danilodox.warbook.api.ClanActivityDto
import io.github.danilodox.warbook.api.MemberActivityDto

/**
 * Atividade dos membros a partir das guerras guardadas. [wars]: guerras comuns encerradas; [cwl]: rodadas
 * encerradas da CWL. As duas listas vêm da mais recente para a mais antiga; da CWL só entra a temporada
 * mais recente.
 */
object ClanActivity {

    fun of(wars: List<WarRecord>, cwl: List<WarRecord>): ClanActivityDto {
        val season = cwl.firstOrNull()?.cwlSeason
        val rounds = cwl.filter { it.cwlSeason == season }
        val warStats = wars.flatMap { it.participation() }.groupBy { it.member.tag }
        val cwlStats = rounds.flatMap { it.participation() }.groupBy { it.member.tag }
        // O nome e o CV mais recentes: a primeira vez que aparece nas listas (que vêm da mais recente).
        val latest = (wars + rounds).sortedByDescending { it.endTime }
            .flatMap { war -> war.members.filterNot { it.isOpponent } }
            .distinctBy { it.tag }
        val members = latest.map { member ->
            val inWars = warStats[member.tag].orEmpty()
            val inCwl = cwlStats[member.tag].orEmpty()
            MemberActivityDto(
                tag = member.tag,
                name = member.name,
                townHall = member.townHall,
                wars = inWars.size,
                attacksUsed = inWars.sumOf { it.used },
                attacksAvailable = inWars.sumOf { it.available },
                cwlRounds = inCwl.size,
                cwlAttacksUsed = inCwl.sumOf { it.used },
            )
        }
        return ClanActivityDto(
            warsCounted = wars.size,
            members = members.sortedWith(
                compareByDescending<MemberActivityDto> { it.attacksAvailable - it.attacksUsed }
                    .thenByDescending { it.cwlRounds - it.cwlAttacksUsed }
                    .thenBy { it.name.lowercase() },
            ),
            cwlRounds = rounds.size,
            cwlSeason = season,
        )
    }

    private class Participation(val member: WarRecordMember, val used: Int, val available: Int)

    /** Cada membro do nosso lado: ataques usados (contra o adversário) e disponíveis. */
    private fun WarRecord.participation(): List<Participation> {
        val opponents = members.filter { it.isOpponent }.map { it.tag }.toSet()
        val usedBy = attacks.filter { it.defenderTag in opponents }.groupingBy { it.attackerTag }.eachCount()
        return members.filterNot { it.isOpponent }.map { member ->
            Participation(member, used = minOf(usedBy[member.tag] ?: 0, attacksPerMember), available = attacksPerMember)
        }
    }
}
