package io.github.danilodox.warbook.coc

import io.github.danilodox.warbook.api.CurrentWarDto
import io.github.danilodox.warbook.api.WarAttackDto
import io.github.danilodox.warbook.api.WarClanDto
import io.github.danilodox.warbook.api.WarMemberDto
import io.github.danilodox.warbook.api.WarState

// Guerra da Supercell → DTOs próprios. Tags saem sem `#`.

private const val CWL_ATTACKS_PER_MEMBER = 1

internal fun cocWarState(value: String): WarState = when (value) {
    "preparation" -> WarState.PREPARATION
    "inWar" -> WarState.IN_WAR
    "warEnded" -> WarState.WAR_ENDED
    "inMatchmaking", "enterWar", "matched" -> WarState.MATCHMAKING
    else -> WarState.NOT_IN_WAR
}

/** Na CWL ([isCwl]) a API não manda `attacksPerMember`: é sempre 1. */
internal fun CocWar.toDto(isCwl: Boolean = false): CurrentWarDto {
    val warState = cocWarState(state)
    if (warState == WarState.NOT_IN_WAR || warState == WarState.MATCHMAKING) return CurrentWarDto(state = warState)
    return CurrentWarDto(
        state = warState,
        teamSize = teamSize,
        attacksPerMember = if (isCwl) CWL_ATTACKS_PER_MEMBER else attacksPerMember,
        isCwl = isCwl,
        preparationStartTime = parseCocTime(preparationStartTime),
        startTime = parseCocTime(startTime),
        endTime = parseCocTime(endTime),
        clan = clan?.toDto(against = opponent),
        opponent = opponent?.toDto(against = clan),
    )
}

private fun CocWarClan.toDto(against: CocWarClan?) =
    toDto(ownPositions = positions(), targetPositions = against?.positions().orEmpty())

/**
 * Tag (com `#`) → posição de 1 a N. A `mapPosition` da API pode ter buracos (membro que saiu do clã);
 * aqui vale a ordem.
 */
private fun CocWarClan.positions(): Map<String, Int> =
    members.sortedBy { it.mapPosition }.mapIndexed { index, member -> member.tag to index + 1 }.toMap()

private fun CocWarClan.toDto(ownPositions: Map<String, Int>, targetPositions: Map<String, Int>) = WarClanDto(
    tag = tag.orEmpty().withoutHash(),
    name = name.orEmpty(),
    badgeUrl = badgeUrls?.medium,
    clanLevel = clanLevel,
    stars = stars,
    destructionPercentage = destructionPercentage,
    attacks = attacks,
    members = members.sortedBy { it.mapPosition }.map { member ->
        WarMemberDto(
            tag = member.tag.withoutHash(),
            name = member.name,
            townHallLevel = member.townhallLevel,
            mapPosition = ownPositions.getValue(member.tag),
            attacks = member.attacks.sortedBy { it.order }.map { it.toDto(targetPositions) },
            opponentAttacks = member.opponentAttacks,
            bestOpponentAttack = member.bestOpponentAttack?.toDto(ownPositions),
        )
    },
)

/** [positions] é o mapa do clã que defende. */
private fun CocWarAttack.toDto(positions: Map<String, Int>) = WarAttackDto(
    defenderTag = defenderTag.withoutHash(),
    stars = stars,
    destructionPercentage = destructionPercentage,
    order = order,
    attackerTag = attackerTag.withoutHash(),
    defenderPosition = positions[defenderTag],
)
