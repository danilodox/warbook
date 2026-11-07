package io.github.danilodox.warbook.war

import io.github.danilodox.warbook.api.CurrentWarDto
import io.github.danilodox.warbook.api.CwlDto
import io.github.danilodox.warbook.api.WarClanDto
import io.github.danilodox.warbook.api.WarState

/**
 * Resumo da guerra pronto para colar no grupo do WhatsApp (`*negrito*`). Modelo fixo da v0.4:
 * placar, tempo restante e quem falta atacar; na preparação, a força por CV; na CWL, a rodada.
 */
object WarSummary {

    /** `null` quando não há o que resumir (fora de guerra, procurando adversário, sem os dois clãs). */
    fun war(war: CurrentWarDto, now: Long, texts: SummaryTexts): String? = warLines(war, now, texts)?.joinToString("\n")

    /** Resumo da rodada [round] (1 = primeira); `null` se ela ainda não foi sorteada. */
    fun cwl(cwl: CwlDto, round: Int, now: Long, texts: SummaryTexts): String? {
        val war = cwl.rounds.firstOrNull { it.round == round }?.war
        val lines = war?.let { warLines(it, now, texts) }
        return lines?.let { (listOf("*CWL · ${texts.round(round, cwl.rounds.size)}*") + it).joinToString("\n") }
    }

    private fun warLines(war: CurrentWarDto, now: Long, texts: SummaryTexts): List<String>? {
        val ours = war.clan
        val theirs = war.opponent
        if (ours == null || theirs == null) return null
        val header = "*${ours.name} × ${theirs.name}* (${war.teamSize} × ${war.teamSize})"
        val body = when (war.state) {
            WarState.PREPARATION -> preparation(war, now, texts)
            WarState.IN_WAR -> {
                val endsIn = "⏱ ${texts.endsIn(texts.duration((war.endTime ?: now) - now))}"
                score(ours, theirs, texts) + listOf(endsIn, "") + pending(war, texts)
            }
            WarState.WAR_ENDED -> listOfNotNull(war.result()?.let { resultLine(it, texts) }) +
                score(ours, theirs, texts) + listOf("") + missed(war, texts)
            WarState.NOT_IN_WAR, WarState.MATCHMAKING -> null
        }
        return body?.let { listOf(header) + it }
    }

    private fun preparation(war: CurrentWarDto, now: Long, texts: SummaryTexts): List<String> {
        val startsIn = texts.duration((war.startTime ?: now) - now)
        val lines = listOf("🛡 ${texts.preparation(startsIn)}")
        val strength = war.strength() ?: return lines
        val average = texts.decimal(strength.clan.averageTownHall) + " × " +
            texts.decimal(strength.opponent.averageTownHall)
        val levels = strength.townHallLevels.map { level ->
            val ourCount = strength.clan.townHallCounts[level] ?: 0
            val theirCount = strength.opponent.townHallCounts[level] ?: 0
            "${texts.townHall(level)}: $ourCount × $theirCount"
        }
        return lines + listOf(
            "",
            "*${texts.strengthTitle}*",
            "${texts.averageTownHall}: $average",
            "${texts.maxTownHall}: ${strength.clan.maxTownHall} × ${strength.opponent.maxTownHall}",
        ) + levels
    }

    private fun score(ours: WarClanDto, theirs: WarClanDto, texts: SummaryTexts) = listOf(
        "⭐ ${texts.stars}: *${ours.stars}* × ${theirs.stars}",
        "💥 ${texts.destruction}: ${texts.decimal(ours.destructionPercentage)}% × " +
            "${texts.decimal(theirs.destructionPercentage)}%",
    )

    private fun resultLine(result: WarResult, texts: SummaryTexts): String = when (result) {
        WarResult.WIN -> "🏆 *${texts.result(result)}*"
        WarResult.LOSS -> "😞 *${texts.result(result)}*"
        WarResult.DRAW -> "🤝 *${texts.result(result)}*"
    }

    private fun pending(war: CurrentWarDto, texts: SummaryTexts): List<String> {
        val pending = war.pendingAttackers()
        if (pending.isEmpty()) return listOf("✅ ${texts.allAttacked}")
        return listOf("⚔️ *${texts.pendingTitle(pending.sumOf { it.remaining }, pending.size)}*") +
            bullets(war, pending)
    }

    private fun missed(war: CurrentWarDto, texts: SummaryTexts): List<String> {
        val missed = war.pendingAttackers()
        if (missed.isEmpty()) return listOf("✅ ${texts.noneMissed}")
        return listOf("❌ *${texts.missedTitle(missed.sumOf { it.remaining })}*") + bullets(war, missed)
    }

    /** Com 1 ataque por membro (CWL) o "(1)" de cada nome não diz nada. */
    private fun bullets(war: CurrentWarDto, attackers: List<PendingAttacker>) = attackers.map {
        if (war.attacksPerMember > 1) "• ${it.name} (${it.remaining})" else "• ${it.name}"
    }
}
