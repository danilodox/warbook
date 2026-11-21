package io.github.danilodox.warbook.history

import io.github.danilodox.warbook.api.MemberWarScoreDto
import io.github.danilodox.warbook.api.WarDetailDto
import io.github.danilodox.warbook.api.WarKind
import io.github.danilodox.warbook.api.WarOutcome
import io.github.danilodox.warbook.raid.dayMonthUtc
import io.github.danilodox.warbook.score.AttackMark
import io.github.danilodox.warbook.ui.history.formatPoints
import io.github.danilodox.warbook.war.WhatsAppText

/**
 * Resumo de uma guerra guardada e encerrada, pronto para colar no grupo do WhatsApp, no mesmo formato dos
 * outros resumos: blocos separados por uma linha vazia e nomes passados por [WhatsAppText.safeName].
 * Resultado e placar sempre; com os ataques guardados, também as [TOP] melhores notas, quem deixou ataque
 * sem usar e quem pegou alvo fácil.
 */
object WarRecordSummary {

    const val TOP = 3

    /** `null` enquanto a guerra não acabou: só vale resumir o resultado final. */
    fun of(detail: WarDetailDto, texts: WarRecordSummaryTexts): String? {
        val war = detail.war
        val result = war.result ?: return null
        val opponent = war.opponentName?.let(WhatsAppText::safeName) ?: "?"
        val title = if (war.kind == WarKind.CWL) texts.cwlTitle(opponent) else texts.warTitle(opponent)
        val header = listOf(
            "🏁 *$title* (${texts.date(dayMonthUtc(war.endTime))})",
            "${resultIcon(result)} *${texts.result(result)}*",
            "⭐ ${texts.stars}: *${war.stars}* × ${war.opponentStars}",
            "💥 ${texts.destruction}: ${texts.decimal(war.destruction)}% × ${texts.decimal(war.opponentDestruction)}%",
        )
        return listOf(header, best(detail.members, texts), missed(detail.members, texts), easy(detail.members, texts))
            .filter { it.isNotEmpty() }
            .joinToString("\n\n") { it.joinToString("\n") }
    }

    private fun resultIcon(result: WarOutcome) = when (result) {
        WarOutcome.WIN -> "🏆"
        WarOutcome.LOSE -> "😞"
        WarOutcome.TIE -> "🤝"
    }

    /** Só quem atacou entra: nota de quem não atacou é só a penalidade. */
    private fun best(members: List<MemberWarScoreDto>, texts: WarRecordSummaryTexts): List<String> {
        val top = members.filter { it.attacks.isNotEmpty() }.sortedByDescending { it.total }.take(TOP)
        if (top.isEmpty()) return emptyList()
        return listOf("📊 *${texts.bestTitle}*") +
            top.map { "• ${WhatsAppText.safeName(it.name)} ${formatPoints(it.total, texts.decimalSeparator)}" }
    }

    private fun missed(members: List<MemberWarScoreDto>, texts: WarRecordSummaryTexts): List<String> {
        val missing = members.filter { it.missed > 0 }.sortedBy { it.mapPosition }
        return when {
            members.isEmpty() -> emptyList()
            missing.isEmpty() -> listOf("✅ ${texts.allAttacked}")
            else -> listOf("❌ *${texts.missedTitle(missing.sumOf { it.missed })}*") +
                missing.map {
                    val used = it.attacks.size
                    "• ${WhatsAppText.safeName(it.name)} (${texts.attacksOf(used, used + it.missed)})"
                }
        }
    }

    private fun easy(members: List<MemberWarScoreDto>, texts: WarRecordSummaryTexts): List<String> {
        val easy = members
            .map { member -> member to member.attacks.count { it.mark == AttackMark.EASY_TARGET } }
            .filter { (_, count) -> count > 0 }
        if (easy.isEmpty()) return emptyList()
        return listOf("🎯 *${texts.easyTitle}*") +
            easy.map { (member, count) -> "• ${WhatsAppText.safeName(member.name)} (${texts.easyCount(count)})" }
    }
}
