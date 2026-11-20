package io.github.danilodox.warbook.history

import io.github.danilodox.warbook.api.ApiErrorCode
import io.github.danilodox.warbook.api.ClanActivityDto
import io.github.danilodox.warbook.api.CwlPlannerDto
import io.github.danilodox.warbook.api.CwlSeasonDto
import io.github.danilodox.warbook.api.MemberWarScoreDto
import io.github.danilodox.warbook.api.PlayerHistoryDto
import io.github.danilodox.warbook.api.PlayerWarEntryDto
import io.github.danilodox.warbook.api.ScoredAttackDto
import io.github.danilodox.warbook.api.WarDetailDto
import io.github.danilodox.warbook.api.WarHistoryDto
import io.github.danilodox.warbook.api.WarKind
import io.github.danilodox.warbook.api.WarOutcome
import io.github.danilodox.warbook.api.WarState
import io.github.danilodox.warbook.api.WarSummaryDto
import io.github.danilodox.warbook.errors.ApiException
import io.github.danilodox.warbook.score.AttackMark
import io.github.danilodox.warbook.score.MemberScore
import io.github.danilodox.warbook.score.ScoreAttack
import io.github.danilodox.warbook.score.ScoreMember
import io.github.danilodox.warbook.score.ScoreWar
import io.github.danilodox.warbook.score.WarScoring
import kotlin.math.round

/** Histórico guardado → DTOs, com a nota de cada membro calculada na hora (a regra fica no `shared`). */
class WarHistoryService(private val history: WarHistoryRepository) {

    suspend fun wars(clanTag: String, limit: Int): WarHistoryDto =
        WarHistoryDto(history.recentWars(clanTag, limit).map { it.toSummary() })

    suspend fun war(clanTag: String, warId: Long): WarDetailDto {
        val war = history.war(clanTag, warId) ?: throw ApiException(ApiErrorCode.NOT_FOUND, "Guerra não encontrada")
        return WarDetailDto(war.toSummary(), if (war.isDetailed()) war.scoredMembers() else emptyList())
    }

    /** As [PlayerHistoryDto.MAX_WARS] guerras encerradas mais recentes do clã em que o jogador estava. */
    suspend fun player(clanTag: String, playerTag: String): PlayerHistoryDto {
        val entries = history.recentEndedSnapshots(clanTag, PlayerHistoryDto.MAX_WARS).mapNotNull { war ->
            val member = war.members.firstOrNull { !it.isOpponent && it.tag == playerTag } ?: return@mapNotNull null
            val score = war.scores().single { it.tag == playerTag }
            member to war.toEntry(member, score)
        }
        val wars = entries.map { it.second }
        val totals = wars.map { it.total }
        return PlayerHistoryDto(
            tag = playerTag,
            name = entries.firstOrNull()?.first?.name,
            wars = wars,
            attacksUsed = wars.sumOf { it.attacksUsed },
            attacksAvailable = wars.sumOf { it.attacksAvailable },
            easyTargets = wars.sumOf { it.easyTargets },
            cleanups = wars.sumOf { it.cleanups },
            average = totals.takeIf { it.isNotEmpty() }?.average()?.let(::round2),
            trend = trend(totals),
        )
    }

    /** Atividade dos membros: as últimas guerras comuns e a última temporada da CWL guardadas. */
    suspend fun activity(clanTag: String): ClanActivityDto = ClanActivity.of(
        wars = history.recentEndedSnapshots(clanTag, ClanActivityDto.WARS, WarType.WAR),
        cwl = history.recentEndedSnapshots(clanTag, ClanActivityDto.CWL_ROUNDS, WarType.CWL),
    )

    /** A temporada mais recente da CWL guardada, somada. */
    suspend fun cwlSeason(clanTag: String): CwlSeasonDto =
        CwlSeasonSummary.of(history.recentEndedSnapshots(clanTag, ClanActivityDto.CWL_ROUNDS, WarType.CWL))

    /** Estatísticas das últimas guerras guardadas para o planejador da CWL. */
    suspend fun planner(clanTag: String): CwlPlannerDto =
        PlannerStats.of(history.recentEndedSnapshots(clanTag, CwlPlannerDto.WARS))

    private fun WarRecord.scoredMembers(): List<MemberWarScoreDto> {
        val ours = members.filterNot { it.isOpponent }.associateBy { it.tag }
        val theirs = members.filter { it.isOpponent }.associateBy { it.tag }
        return scores().map { score ->
            val member = ours.getValue(score.tag)
            MemberWarScoreDto(
                tag = member.tag,
                name = member.name,
                townHall = member.townHall,
                mapPosition = member.mapPosition,
                attacks = score.attacks.map { attack ->
                    val defender = theirs[attack.defenderTag]
                    ScoredAttackDto(
                        order = attack.order,
                        defenderTag = attack.defenderTag,
                        stars = attack.stars,
                        destruction = attack.destruction,
                        factor = attack.factor,
                        points = attack.points,
                        mark = attack.mark,
                        defenderName = defender?.name,
                        defenderPosition = defender?.mapPosition,
                        defenderTownHall = defender?.townHall,
                    )
                },
                missed = score.missed,
                penalty = score.penalty,
                total = score.total,
            )
        }
    }

    private fun WarRecord.toEntry(member: WarRecordMember, score: MemberScore) = PlayerWarEntryDto(
        warId = requireNotNull(id) { "guerra lida do banco sem id" },
        kind = type.toKind(),
        endTime = endTime.toEpochMilli(),
        townHall = member.townHall,
        mapPosition = member.mapPosition,
        attacksUsed = score.attacks.size,
        attacksAvailable = attacksPerMember,
        total = score.total,
        easyTargets = score.attacks.count { it.mark == AttackMark.EASY_TARGET },
        cleanups = score.attacks.count { it.mark == AttackMark.CLEANUP },
        opponentName = opponent.name,
    )

    companion object {
        private const val CENTS = 100.0

        /**
         * Média das [PlayerHistoryDto.TREND_WINDOW] notas mais recentes menos a das anteriores (até o mesmo
         * tamanho). [totals] vem da mais recente para a mais antiga. Nula se não há notas anteriores.
         */
        fun trend(totals: List<Double>): Double? {
            val recent = totals.take(PlayerHistoryDto.TREND_WINDOW)
            val before = totals.drop(PlayerHistoryDto.TREND_WINDOW).take(PlayerHistoryDto.TREND_WINDOW)
            if (recent.isEmpty() || before.isEmpty()) return null
            return round2(recent.average() - before.average())
        }

        private fun round2(value: Double): Double = round(value * CENTS) / CENTS
    }
}

/** Nota de cada membro do nosso lado, pela regra do `shared`. */
internal fun WarRecord.scores(): List<MemberScore> = WarScoring.score(
    ScoreWar(
        isCwl = type == WarType.CWL,
        attacksPerMember = attacksPerMember,
        members = members.filterNot { it.isOpponent }.map { it.toScore() },
        opponents = members.filter { it.isOpponent }.map { it.toScore() },
        attacks = attacks.map { ScoreAttack(it.order, it.attackerTag, it.defenderTag, it.stars, it.destruction) },
        ended = state == WarState.WAR_ENDED,
    ),
)

private fun WarRecordMember.toScore() = ScoreMember(tag, townHall, mapPosition)

/** Retrato do agendador: tem escalação e ataques, então tem nota. O warlog só tem o placar. */
private fun WarRecord.isDetailed() = source == WarSource.SNAPSHOT

private fun WarType.toKind() = WarKind.valueOf(name)

private fun WarRecord.toSummary() = WarSummaryDto(
    id = requireNotNull(id) { "guerra lida do banco sem id" },
    kind = type.toKind(),
    state = state,
    endTime = endTime.toEpochMilli(),
    teamSize = teamSize,
    attacksPerMember = attacksPerMember,
    stars = clan.stars,
    destruction = clan.destruction,
    opponentStars = opponent.stars,
    opponentDestruction = opponent.destruction,
    detailed = isDetailed(),
    opponentTag = opponent.tag,
    opponentName = opponent.name,
    opponentBadgeUrl = opponent.badgeUrl,
    result = result?.let { WarOutcome.valueOf(it.name) },
    cwlSeason = cwlSeason,
)
