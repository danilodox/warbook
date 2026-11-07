package io.github.danilodox.warbook.war

import io.github.danilodox.warbook.api.CurrentWarDto
import io.github.danilodox.warbook.api.WarMemberDto

// Contas sobre a guerra que a tela e o resumo do WhatsApp mostram. Sem Compose: testável direto.

/** Membro do clã com [remaining] ataques ainda não feitos. */
data class PendingAttacker(val tag: String, val name: String, val mapPosition: Int, val remaining: Int)

/**
 * Quem do clã ainda tem ataque sobrando, pela posição no mapa. Na guerra encerrada são os ataques perdidos.
 * Na CWL só os escalados estão na guerra, então só eles aparecem.
 */
fun CurrentWarDto.pendingAttackers(): List<PendingAttacker> = clan?.members.orEmpty()
    .map { PendingAttacker(it.tag, it.name, it.mapPosition, (attacksPerMember - it.attacks.size).coerceAtLeast(0)) }
    .filter { it.remaining > 0 }
    .sortedBy { it.mapPosition }

/** Um ataque do clã com o alvo já identificado ([defenderPosition] e [defenderTownHall] `null` se o alvo não veio). */
data class AttackLine(
    val attackerName: String,
    val defenderPosition: Int?,
    val defenderTownHall: Int?,
    val stars: Int,
    val destructionPercentage: Int,
    val order: Int,
)

/** Todos os ataques do clã, na ordem em que aconteceram. */
fun CurrentWarDto.ourAttacks(): List<AttackLine> {
    val defenders = opponent?.members.orEmpty().associateBy { it.tag }
    return clan?.members.orEmpty()
        .flatMap { member ->
            member.attacks.map {
                AttackLine(
                    attackerName = member.name,
                    defenderPosition = it.defenderPosition,
                    defenderTownHall = defenders[it.defenderTag]?.townHallLevel,
                    stars = it.stars,
                    destructionPercentage = it.destructionPercentage,
                    order = it.order,
                )
            }
        }
        .sortedBy { it.order }
}

private const val THREE_STARS = 3
private const val WEAK_MAX_STARS = 1
private const val BEST_FALLBACK = 3

/** Os 3 estrelas, na ordem; sem nenhum, os [BEST_FALLBACK] melhores por estrelas e destruição. */
fun CurrentWarDto.bestAttacks(): List<AttackLine> {
    val attacks = ourAttacks()
    return attacks.filter { it.stars == THREE_STARS }.ifEmpty {
        attacks.sortedWith(compareByDescending<AttackLine> { it.stars }.thenByDescending { it.destructionPercentage })
            .take(BEST_FALLBACK)
    }
}

/** Ataques de 0 ou 1 estrela, na ordem. */
fun CurrentWarDto.weakAttacks(): List<AttackLine> = ourAttacks().filter { it.stars <= WEAK_MAX_STARS }

enum class WarResult { WIN, LOSS, DRAW }

/** Pelas regras do jogo: mais estrelas vence; empate em estrelas decide pela destruição. */
fun CurrentWarDto.result(): WarResult? {
    val ours = clan
    val theirs = opponent
    if (ours == null || theirs == null) return null
    val comparison = compareValuesBy(ours, theirs, { it.stars }, { it.destructionPercentage })
    return when {
        comparison > 0 -> WarResult.WIN
        comparison < 0 -> WarResult.LOSS
        else -> WarResult.DRAW
    }
}

/** Força de um lado só pelo CV: os heróis precisariam do perfil de cada membro. */
data class ClanStrength(val averageTownHall: Double, val maxTownHall: Int, val townHallCounts: Map<Int, Int>)

data class StrengthComparison(val clan: ClanStrength, val opponent: ClanStrength) {
    /** Níveis de CV que aparecem em pelo menos um dos lados, do maior para o menor. */
    val townHallLevels: List<Int>
        get() = (clan.townHallCounts.keys + opponent.townHallCounts.keys).sortedDescending()
}

/** `null` se algum lado veio sem membros. */
fun CurrentWarDto.strength(): StrengthComparison? {
    val ours = clan?.members.orEmpty()
    val theirs = opponent?.members.orEmpty()
    if (ours.isEmpty() || theirs.isEmpty()) return null
    return StrengthComparison(ours.strength(), theirs.strength())
}

private fun List<WarMemberDto>.strength() = ClanStrength(
    averageTownHall = map { it.townHallLevel }.average(),
    maxTownHall = maxOf { it.townHallLevel },
    townHallCounts = groupingBy { it.townHallLevel }.eachCount(),
)
