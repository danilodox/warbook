package io.github.danilodox.warbook.score

import io.github.danilodox.warbook.api.PlannerStatsDto
import kotlin.math.round

/** Membro atual do clã com o que se sabe dele nas guerras guardadas ([stats] nulo sem histórico). */
data class PlannerCandidate(val tag: String, val name: String, val townHall: Int, val stats: PlannerStatsDto? = null) {
    /** Posição na sugestão; nula sem nota guardada. */
    val rating: Double? = stats?.let { CwlPlanner.rating(it.average, it.attacksUsed, it.attacksAvailable) }
}

/**
 * Escalação sugerida da CWL: a média da nota × a presença (ataques usados / disponíveis). Quem tem nota boa
 * mas falta muito cai; quem não tem histórico vai para o fim, do maior CV para o menor.
 */
object CwlPlanner {

    /** Tamanhos de escalação da CWL. */
    val SIZES = listOf(15, 30)

    private const val CENTS = 100.0

    /** Média negativa não melhora com a falta: só a positiva é multiplicada pela presença. */
    fun rating(average: Double?, attacksUsed: Int, attacksAvailable: Int): Double? {
        if (average == null || attacksAvailable <= 0) return null
        val presence = attacksUsed.coerceIn(0, attacksAvailable).toDouble() / attacksAvailable
        val value = if (average > 0) average * presence else average
        return round(value * CENTS) / CENTS
    }

    /** Da melhor posição para a pior; empate pelo CV maior e depois pelo nome. */
    fun rank(candidates: List<PlannerCandidate>): List<PlannerCandidate> = candidates.sortedWith(
        compareBy<PlannerCandidate> { it.rating == null }
            .thenByDescending { it.rating ?: 0.0 }
            .thenByDescending { it.townHall }
            .thenBy { it.name.lowercase() },
    )

    /** Os [size] primeiros da [rank]. */
    fun suggest(candidates: List<PlannerCandidate>, size: Int): Set<String> =
        rank(candidates).take(size).map { it.tag }.toSet()

    /** Os escolhidos na ordem do mapa: do maior CV para o menor e, no mesmo CV, pela posição na sugestão. */
    fun lineup(candidates: List<PlannerCandidate>, selected: Set<String>): List<PlannerCandidate> {
        val ranked = rank(candidates)
        val order = ranked.withIndex().associate { (index, candidate) -> candidate.tag to index }
        return ranked.filter { it.tag in selected }
            .sortedWith(compareByDescending<PlannerCandidate> { it.townHall }.thenBy { order.getValue(it.tag) })
    }
}
