package io.github.danilodox.warbook.coc

import io.github.danilodox.warbook.api.CwlClanDto
import io.github.danilodox.warbook.api.CwlDto
import io.github.danilodox.warbook.api.CwlRoundDto
import io.github.danilodox.warbook.api.CwlState
import io.github.danilodox.warbook.api.WarState

// Grupo da CWL + guerras das rodadas → CwlDto. Tags saem sem `#`.

/** warTag de rodada ainda não sorteada. */
internal const val UNDRAWN_WAR_TAG = "#0"

/** Estrelas extras por vitória na classificação do grupo. */
private const val WIN_BONUS_STARS = 10

internal fun cocCwlState(value: String): CwlState = when (value) {
    "preparation" -> CwlState.PREPARATION
    "inWar" -> CwlState.IN_WAR
    "ended" -> CwlState.ENDED
    else -> CwlState.NOT_IN_CWL
}

/** warTags já sorteadas, sem repetir. */
internal fun CocLeagueGroup.drawnWarTags(): List<String> =
    rounds.flatMap { it.warTags }.filter { it != UNDRAWN_WAR_TAG }.distinct()

/** [wars] tem as guerras de [drawnWarTags] pelo warTag. [clanTag] é o clã consultado, sem `#`. */
internal fun CocLeagueGroup.toDto(clanTag: String, wars: Map<String, CocWar>): CwlDto {
    val cwlState = cocCwlState(state)
    if (cwlState == CwlState.NOT_IN_CWL) return CwlDto(cwlState)
    val roundWars = rounds.map { round -> round.warTags.mapNotNull { wars[it] } }
    val ours = roundWars.mapIndexed { index, list ->
        CwlRoundDto(round = index + 1, war = list.firstNotNullOfOrNull { it.facing("#$clanTag") }?.toDto(isCwl = true))
    }
    return CwlDto(
        state = cwlState,
        season = season,
        currentRound = currentRound(ours),
        clans = standings(roundWars.flatten()),
        rounds = ours,
    )
}

/** A guerra com [ownTag] no lado `clan`; `null` se o clã não está nela. */
private fun CocWar.facing(ownTag: String): CocWar? = when (ownTag) {
    clan?.tag -> this
    opponent?.tag -> copy(clan = opponent, opponent = clan)
    else -> null
}

private fun currentRound(rounds: List<CwlRoundDto>): Int? {
    fun first(state: WarState) = rounds.firstOrNull { it.war?.state == state }
    return (first(WarState.IN_WAR) ?: first(WarState.PREPARATION) ?: rounds.lastOrNull { it.war != null })?.round
}

/** Soma as guerras em andamento e encerradas; o bônus de vitória só conta nas encerradas. */
private fun CocLeagueGroup.standings(wars: List<CocWar>): List<CwlClanDto> {
    val stars = mutableMapOf<String, Int>()
    val destruction = mutableMapOf<String, Double>()
    val wins = mutableMapOf<String, Int>()
    wars.filter { cocWarState(it.state) in COUNTED_STATES }.forEach { war ->
        listOfNotNull(war.clan, war.opponent).forEach { side ->
            val tag = side.tag ?: return@forEach
            stars.merge(tag, side.stars, Int::plus)
            destruction.merge(tag, side.destructionPercentage, Double::plus)
        }
        war.winnerTag()?.let { tag ->
            wins.merge(tag, 1, Int::plus)
            stars.merge(tag, WIN_BONUS_STARS, Int::plus)
        }
    }
    return clans
        .map {
            CwlClanDto(
                tag = it.tag.withoutHash(),
                name = it.name,
                badgeUrl = it.badgeUrls?.medium,
                clanLevel = it.clanLevel,
                stars = stars[it.tag] ?: 0,
                destructionPercentage = destruction[it.tag] ?: 0.0,
                wins = wins[it.tag] ?: 0,
            )
        }
        .sortedWith(compareByDescending<CwlClanDto> { it.stars }.thenByDescending { it.destructionPercentage })
}

private val COUNTED_STATES = setOf(WarState.IN_WAR, WarState.WAR_ENDED)

/** Vence quem tem mais estrelas; empate em estrelas decide pela destruição. Só em guerra encerrada. */
private fun CocWar.winnerTag(): String? {
    val home = clan
    val away = opponent
    if (home == null || away == null || cocWarState(state) != WarState.WAR_ENDED) return null
    val result = compareValuesBy(home, away, { it.stars }, { it.destructionPercentage })
    return when {
        result > 0 -> home.tag
        result < 0 -> away.tag
        else -> null
    }
}
