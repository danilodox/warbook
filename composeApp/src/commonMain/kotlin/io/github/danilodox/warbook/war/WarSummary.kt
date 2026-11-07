package io.github.danilodox.warbook.war

import io.github.danilodox.warbook.api.CurrentWarDto
import io.github.danilodox.warbook.api.CwlDto
import io.github.danilodox.warbook.api.WarClanDto
import io.github.danilodox.warbook.api.WarState

/**
 * Resumo da guerra pronto para colar no grupo do WhatsApp (`*negrito*`). O cabeçalho sempre vai; o resto
 * segue as [SummaryOptions] (o padrão é o modelo fixo da v0.4.0). Blocos são separados por uma linha vazia.
 */
object WarSummary {

    /** `null` quando não há o que resumir (fora de guerra, procurando adversário, sem os dois clãs). */
    fun war(war: CurrentWarDto, now: Long, texts: SummaryTexts, options: SummaryOptions = SummaryOptions()): String? =
        blocks(war, now, texts, options)?.join()

    /** Resumo da rodada [round] (1 = primeira); `null` se ela ainda não foi sorteada. */
    fun cwl(
        cwl: CwlDto,
        round: Int,
        now: Long,
        texts: SummaryTexts,
        options: SummaryOptions = SummaryOptions(),
    ): String? {
        val war = cwl.rounds.firstOrNull { it.round == round }?.war
        val blocks = war?.let { blocks(it, now, texts, options) } ?: return null
        val roundLine = "*CWL · ${texts.round(round, cwl.rounds.size)}*"
        return (listOf(listOf(roundLine) + blocks.first()) + blocks.drop(1)).join()
    }

    private fun List<List<String>>.join() = filter { it.isNotEmpty() }.joinToString("\n\n") { it.joinToString("\n") }

    /** O primeiro bloco começa pelo cabeçalho. `null` quando não há guerra para resumir. */
    private fun blocks(
        war: CurrentWarDto,
        now: Long,
        texts: SummaryTexts,
        options: SummaryOptions,
    ): List<List<String>>? {
        val ours = war.clan
        val theirs = war.opponent
        if (ours == null || theirs == null) return null
        val header = "*${WhatsAppText.safeName(ours.name)} × ${WhatsAppText.safeName(theirs.name)}* " +
            "(${war.teamSize} × ${war.teamSize})"
        val body = when (war.state) {
            WarState.PREPARATION -> preparation(war, now, texts, options)
            WarState.IN_WAR -> listOf(
                score(ours, theirs, texts, options) + endsIn(war, now, texts, options),
                attackers(war, texts, options, ended = false),
            ) + attackBlocks(war, texts, options)
            WarState.WAR_ENDED -> listOf(
                listOfNotNull(war.result()?.takeIf { SummaryPart.SCORE in options }?.let { resultLine(it, texts) }) +
                    score(ours, theirs, texts, options),
                attackers(war, texts, options, ended = true),
            ) + attackBlocks(war, texts, options)
            WarState.NOT_IN_WAR, WarState.MATCHMAKING -> null
        }
        return body?.let { listOf(listOf(header) + it.first()) + it.drop(1) }
    }

    private fun preparation(
        war: CurrentWarDto,
        now: Long,
        texts: SummaryTexts,
        options: SummaryOptions,
    ): List<List<String>> {
        val time = if (SummaryPart.TIME in options) {
            texts.preparation(texts.duration((war.startTime ?: now) - now))
        } else {
            texts.preparationTitle
        }
        val strength = war.strength()?.takeIf { SummaryPart.STRENGTH in options } ?: return listOf(listOf("🛡 $time"))
        val average = texts.decimal(strength.clan.averageTownHall) + " × " +
            texts.decimal(strength.opponent.averageTownHall)
        val levels = strength.townHallLevels.map { level ->
            val ourCount = strength.clan.townHallCounts[level] ?: 0
            val theirCount = strength.opponent.townHallCounts[level] ?: 0
            "${texts.townHall(level)}: $ourCount × $theirCount"
        }
        return listOf(
            listOf("🛡 $time"),
            listOf(
                "*${texts.strengthTitle}*",
                "${texts.averageTownHall}: $average",
                "${texts.maxTownHall}: ${strength.clan.maxTownHall} × ${strength.opponent.maxTownHall}",
            ) + levels,
        )
    }

    private fun score(ours: WarClanDto, theirs: WarClanDto, texts: SummaryTexts, options: SummaryOptions) =
        if (SummaryPart.SCORE !in options) {
            emptyList()
        } else {
            listOf(
                "⭐ ${texts.stars}: *${ours.stars}* × ${theirs.stars}",
                "💥 ${texts.destruction}: ${texts.decimal(ours.destructionPercentage)}% × " +
                    "${texts.decimal(theirs.destructionPercentage)}%",
            )
        }

    private fun endsIn(war: CurrentWarDto, now: Long, texts: SummaryTexts, options: SummaryOptions) =
        if (SummaryPart.TIME in options) {
            listOf("⏱ ${texts.endsIn(texts.duration((war.endTime ?: now) - now))}")
        } else {
            emptyList()
        }

    private fun resultLine(result: WarResult, texts: SummaryTexts): String = when (result) {
        WarResult.WIN -> "🏆 *${texts.result(result)}*"
        WarResult.LOSS -> "😞 *${texts.result(result)}*"
        WarResult.DRAW -> "🤝 *${texts.result(result)}*"
    }

    /** Em guerra, quem falta atacar; na encerrada ([ended]), quem não atacou. Vazio se a parte está desligada. */
    private fun attackers(
        war: CurrentWarDto,
        texts: SummaryTexts,
        options: SummaryOptions,
        ended: Boolean,
    ): List<String> {
        if (SummaryPart.ATTACKERS !in options) return emptyList()
        val pending = war.pendingAttackers()
        val title = when {
            pending.isEmpty() -> "✅ ${if (ended) texts.noneMissed else texts.allAttacked}"
            ended -> "❌ *${texts.missedTitle(pending.sumOf { it.remaining })}*"
            else -> "⚔️ *${texts.pendingTitle(pending.sumOf { it.remaining }, pending.size)}*"
        }
        // Com 1 ataque por membro (CWL) o "(1)" de cada nome não diz nada.
        return listOf(title) + pending.map {
            val name = WhatsAppText.safeName(it.name)
            if (war.attacksPerMember > 1) "• $name (${it.remaining})" else "• $name"
        }
    }
}
