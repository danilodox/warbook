package io.github.danilodox.warbook.score

import kotlinx.serialization.Serializable
import kotlin.math.round

// Pontuação de cada ataque numa guerra (G5). Regra pura: entra a guerra, sai a nota de cada membro.
// A defesa fica de fora de propósito: a nota mede só o que o membro fez atacando.

/** Membro de um dos lados. [mapPosition] de 1 a N, sem buracos. */
data class ScoreMember(val tag: String, val townHall: Int, val mapPosition: Int)

data class ScoreAttack(
    val order: Int,
    val attackerTag: String,
    val defenderTag: String,
    val stars: Int,
    val destruction: Int,
)

/**
 * Guerra para pontuar, do ponto de vista do nosso clã. [attacks] pode ter os dos dois lados: só contam os
 * feitos por [members] contra [opponents]. [ended] liga a penalidade de ataque não usado.
 */
data class ScoreWar(
    val isCwl: Boolean,
    val attacksPerMember: Int,
    val members: List<ScoreMember>,
    val opponents: List<ScoreMember>,
    val attacks: List<ScoreAttack>,
    val ended: Boolean,
)

@Serializable
enum class AttackMark {
    /** Mesma posição no mapa: neutro (fator 1). */
    MIRROR,

    /** Atacou CV menor havendo ainda base de CV igual ou maior sem 3★: leva o fator. */
    EASY_TARGET,

    /** Atacou CV menor quando já não havia base de CV igual ou maior sem 3★: sem penalidade. */
    CLEANUP,
}

data class AttackScore(
    val order: Int,
    val defenderTag: String,
    val stars: Int,
    val destruction: Int,
    /** Estrelas + destruição/100, de 0 a 4. */
    val base: Double,
    val factor: Double,
    val points: Double,
    val mark: AttackMark? = null,
)

data class MemberScore(
    val tag: String,
    val attacks: List<AttackScore>,
    /** Ataques que sobraram no fim da guerra; 0 enquanto ela não acabou. */
    val missed: Int,
    val penalty: Double,
    val total: Double,
)

object WarScoring {
    const val TH_STEP = 0.15
    const val MIN_FACTOR = 0.55
    const val MAX_FACTOR = 1.30
    const val MISSED_WAR = -2.0
    const val MISSED_CWL = -3.0
    private const val FULL_STARS = 3
    private const val PERCENT = 100.0

    /** Nota de cada membro do nosso lado, na ordem do mapa. */
    fun score(war: ScoreWar): List<MemberScore> {
        val ours = war.members.associateBy { it.tag }
        val theirs = war.opponents.associateBy { it.tag }
        val valid = war.attacks
            .filter { it.attackerTag in ours && it.defenderTag in theirs }
            .sortedBy { it.order }
        val scored = scoreInOrder(valid, ours, theirs)
        return war.members.sortedBy { it.mapPosition }.map { member ->
            val attacks = scored.filter { it.first == member.tag }.map { it.second }
            val missed = if (war.ended) (war.attacksPerMember - attacks.size).coerceAtLeast(0) else 0
            val penalty = missed * if (war.isCwl) MISSED_CWL else MISSED_WAR
            MemberScore(member.tag, attacks, missed, penalty, round2(attacks.sumOf { it.points } + penalty))
        }
    }

    /** Estrelas + destruição/100 (0 a 4). */
    fun base(stars: Int, destruction: Int): Double = stars + destruction / PERCENT

    /** 1 + 0,15 por CV de diferença (alvo − atacante), entre 0,55 e 1,30. */
    fun townHallFactor(attackerTownHall: Int, defenderTownHall: Int): Double =
        (1 + TH_STEP * (defenderTownHall - attackerTownHall)).coerceIn(MIN_FACTOR, MAX_FACTOR)

    /**
     * Refaz o mapa na ordem dos ataques: para cada um, sabe quais bases do adversário já tinham 3★. Devolve
     * (tag do atacante, nota) na ordem dos ataques.
     */
    private fun scoreInOrder(
        attacks: List<ScoreAttack>,
        ours: Map<String, ScoreMember>,
        theirs: Map<String, ScoreMember>,
    ): List<Pair<String, AttackScore>> {
        val tripled = mutableSetOf<String>()
        return attacks.map { attack ->
            val attacker = ours.getValue(attack.attackerTag)
            val defender = theirs.getValue(attack.defenderTag)
            val mark = mark(attacker, defender) { th -> theirs.values.any { it.townHall >= th && it.tag !in tripled } }
            if (attack.stars >= FULL_STARS) tripled += defender.tag
            val factor = when (mark) {
                AttackMark.MIRROR, AttackMark.CLEANUP -> 1.0
                else -> townHallFactor(attacker.townHall, defender.townHall)
            }
            val base = base(attack.stars, attack.destruction)
            attacker.tag to AttackScore(
                order = attack.order,
                defenderTag = defender.tag,
                stars = attack.stars,
                destruction = attack.destruction,
                base = base,
                factor = round2(factor),
                points = round2(base * factor),
                mark = mark,
            )
        }
    }

    /** [openAtOrAbove]: ainda há base de CV igual ou maior que o dado sem 3★ (antes deste ataque). */
    private fun mark(attacker: ScoreMember, defender: ScoreMember, openAtOrAbove: (Int) -> Boolean): AttackMark? =
        when {
            attacker.mapPosition == defender.mapPosition -> AttackMark.MIRROR
            defender.townHall >= attacker.townHall -> null
            openAtOrAbove(attacker.townHall) -> AttackMark.EASY_TARGET
            else -> AttackMark.CLEANUP
        }

    private fun round2(value: Double): Double = round(value * PERCENT) / PERCENT
}
