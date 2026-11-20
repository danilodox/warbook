package io.github.danilodox.warbook.coc

import io.github.danilodox.warbook.api.WarState
import io.github.danilodox.warbook.history.WarRecord
import io.github.danilodox.warbook.history.WarRecordAttack
import io.github.danilodox.warbook.history.WarRecordMember
import io.github.danilodox.warbook.history.WarSide
import io.github.danilodox.warbook.history.WarSource
import io.github.danilodox.warbook.history.WarType
import java.time.Instant

private const val CWL_ATTACKS_PER_MEMBER = 1
private val SNAPSHOT_STATES = setOf(WarState.PREPARATION, WarState.IN_WAR, WarState.WAR_ENDED)

/** Dados de onde a guerra veio, para o retrato. [warTag] e [cwlSeason] só na CWL. */
internal data class SnapshotOrigin(
    val clanTag: String,
    val capturedAt: Instant,
    val type: WarType = WarType.WAR,
    val warTag: String? = null,
    val cwlSeason: String? = null,
)

/**
 * Guerra da API → retrato completo do ponto de vista de [SnapshotOrigin.clanTag] (sem `#`). Na CWL a guerra
 * vem pelo warTag e o clã pode estar do lado `opponent`: aqui os lados são trocados. `null` fora de guerra,
 * sem fim marcado ou se o clã não está na guerra.
 */
internal fun CocWar.toSnapshot(origin: SnapshotOrigin): WarRecord? {
    val warState = cocWarState(state)
    val end = parseCocTime(endTime)
    val sides = sides(origin.clanTag)
    if (warState !in SNAPSHOT_STATES || end == null || sides == null) return null
    val (ours, theirs) = sides
    val isCwl = origin.type == WarType.CWL
    return WarRecord(
        type = origin.type,
        source = WarSource.SNAPSHOT,
        state = warState,
        teamSize = teamSize,
        attacksPerMember = if (isCwl) CWL_ATTACKS_PER_MEMBER else attacksPerMember,
        endTime = Instant.ofEpochMilli(end),
        clan = ours.toSide(),
        opponent = theirs.toSide(),
        capturedAt = origin.capturedAt,
        preparationStartTime = parseCocTime(preparationStartTime)?.let(Instant::ofEpochMilli),
        startTime = parseCocTime(startTime)?.let(Instant::ofEpochMilli),
        warTag = origin.warTag,
        cwlSeason = origin.cwlSeason,
        result = if (warState == WarState.WAR_ENDED) warResult(ours, theirs) else null,
        members = ours.recordMembers(isOpponent = false) + theirs.recordMembers(isOpponent = true),
        attacks = (ours.members + theirs.members)
            .flatMap { member -> member.attacks.map { it.toRecord(member.tag) } }
            .distinctBy { it.order }
            .sortedBy { it.order },
    )
}

/** `true` se [clanTag] (sem `#`) é um dos lados da guerra. */
internal fun CocWar.involves(clanTag: String): Boolean = "#$clanTag" == clan?.tag || "#$clanTag" == opponent?.tag

private fun CocWar.sides(clanTag: String): Pair<CocWarClan, CocWarClan>? {
    val ours = clan
    val theirs = opponent
    return when {
        ours == null || theirs == null -> null
        ours.tag == "#$clanTag" -> ours to theirs
        theirs.tag == "#$clanTag" -> theirs to ours
        else -> null
    }
}

private fun CocWarClan.toSide() = WarSide(
    tag = tag?.withoutHash(),
    name = name,
    badgeUrl = badgeUrls?.medium,
    stars = stars,
    destruction = destructionPercentage,
    attacks = attacks,
)

/** Posição de 1 a N pela ordem do mapa: a `mapPosition` da API pode ter buracos. */
private fun CocWarClan.recordMembers(isOpponent: Boolean) = members.sortedBy { it.mapPosition }
    .mapIndexed { index, member ->
        WarRecordMember(
            tag = member.tag.withoutHash(),
            isOpponent = isOpponent,
            name = member.name,
            townHall = member.townhallLevel,
            mapPosition = index + 1,
        )
    }

/** Ataque sem `attackerTag` (versões antigas da API): vale o membro que o traz. */
private fun CocWarAttack.toRecord(memberTag: String) = WarRecordAttack(
    order = order,
    attackerTag = attackerTag.ifEmpty { memberTag }.withoutHash(),
    defenderTag = defenderTag.withoutHash(),
    stars = stars,
    destruction = destructionPercentage,
    duration = duration,
)
