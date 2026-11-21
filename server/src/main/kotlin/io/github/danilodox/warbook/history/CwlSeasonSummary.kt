package io.github.danilodox.warbook.history

import io.github.danilodox.warbook.api.CwlMemberSeasonDto
import io.github.danilodox.warbook.api.CwlSeasonDto
import kotlin.math.round

/**
 * Temporada da CWL somada a partir das rodadas encerradas guardadas. [rounds] vem da mais recente para a
 * mais antiga; só entra a temporada da primeira. Rodadas importadas do warlog não têm ataques: contam no
 * placar, não nas notas.
 */
object CwlSeasonSummary {

    private const val CENTS = 100.0

    fun of(rounds: List<WarRecord>): CwlSeasonDto {
        val season = rounds.firstOrNull()?.cwlSeason ?: return CwlSeasonDto()
        val ofSeason = rounds.filter { it.cwlSeason == season }
        val lines = ofSeason.flatMap { round ->
            val ours = round.members.filterNot { it.isOpponent }.associateBy { it.tag }
            round.scores().map { score -> ours.getValue(score.tag) to score }
        }
        // O nome e o CV da rodada mais recente em que o membro aparece.
        val members = lines.groupBy { it.first.tag }.map { (_, entries) ->
            val member = entries.first().first
            val scores = entries.map { it.second }
            CwlMemberSeasonDto(
                tag = member.tag,
                name = member.name,
                townHall = member.townHall,
                rounds = scores.size,
                attacksUsed = scores.sumOf { it.attacks.size },
                stars = scores.sumOf { score -> score.attacks.sumOf { it.stars } },
                total = round(scores.sumOf { it.total } * CENTS) / CENTS,
            )
        }
        return CwlSeasonDto(
            season = season,
            rounds = ofSeason.size,
            wins = ofSeason.count { it.result == WarResult.WIN },
            losses = ofSeason.count { it.result == WarResult.LOSE },
            ties = ofSeason.count { it.result == WarResult.TIE },
            stars = ofSeason.sumOf { it.clan.stars },
            opponentStars = ofSeason.sumOf { it.opponent.stars },
            members = members.sortedWith(
                compareByDescending<CwlMemberSeasonDto> { it.total }.thenBy { it.name.lowercase() },
            ),
        )
    }
}
