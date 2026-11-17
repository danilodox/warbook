package io.github.danilodox.warbook.raid

import io.github.danilodox.warbook.api.RaidSeasonDto
import io.github.danilodox.warbook.api.RaidState
import io.github.danilodox.warbook.war.WhatsAppText

/**
 * Resumo de um fim de semana de raid pronto para colar no grupo do WhatsApp (`*negrito*`), no mesmo formato do
 * resumo da guerra: blocos separados por uma linha vazia e nomes passados por [WhatsAppText.safeName].
 * Total do clã, medalhas (quando o raid acabou), quem não atacou (só no raid mais recente) e quem atacou menos.
 */
object RaidSummary {

    fun season(season: RaidSeasonDto, clanName: String?, now: Long, texts: RaidSummaryTexts): String =
        listOf(header(season, clanName, now, texts), totals(season, texts), missing(season, texts))
            .filter { it.isNotEmpty() }
            .joinToString("\n\n") { it.joinToString("\n") }

    private fun header(season: RaidSeasonDto, clanName: String?, now: Long, texts: RaidSummaryTexts): List<String> {
        val title = listOfNotNull(texts.title, clanName?.let(WhatsAppText::safeName)).joinToString(" · ")
        val date = season.startTime?.let { " (${texts.date(dayMonthUtc(it))})" }.orEmpty()
        val endTime = season.endTime
        val status = when {
            season.state != RaidState.ONGOING -> null
            endTime != null -> "⏱ ${texts.endsIn(texts.duration(endTime - now))}"
            else -> "⏱ ${texts.ongoing}"
        }
        return listOfNotNull("🏛 *$title*$date", status)
    }

    private fun totals(season: RaidSeasonDto, texts: RaidSummaryTexts): List<String> {
        val medals = if (season.state != RaidState.ONGOING && season.offensiveReward > 0) {
            val full = season.fullMedals()
            "🏅 ${texts.medals(season.offensiveReward, full)}"
        } else {
            null
        }
        return listOfNotNull(
            "💰 ${texts.capitalGold}: *${texts.number(season.capitalTotalLoot)}*",
            "⚔️ ${texts.attacks}: ${season.totalAttacks} · ${texts.districts}: ${season.enemyDistrictsDestroyed} · " +
                "${texts.raids}: ${season.raidsCompleted}",
            medals,
        )
    }

    /** Quem não atacou e quem atacou menos, num bloco só; se ninguém faltou, uma linha de "todos atacaram". */
    private fun missing(season: RaidSeasonDto, texts: RaidSummaryTexts): List<String> {
        val absent = season.notAttacked
        val less = season.attackedLess()
        if (absent.isEmpty() && less.isEmpty()) {
            return if (season.members.isEmpty()) emptyList() else listOf("✅ ${texts.allAttacked}")
        }
        val ongoing = season.state == RaidState.ONGOING
        val absentLines = if (absent.isEmpty()) {
            emptyList()
        } else {
            listOf("❌ *${texts.notAttackedTitle(absent.size, ongoing)}*") +
                absent.map { "• ${WhatsAppText.safeName(it.name)}" }
        }
        val lessLines = if (less.isEmpty()) {
            emptyList()
        } else {
            listOf("⚠️ *${texts.attackedLessTitle}*") +
                less.map { "• ${WhatsAppText.safeName(it.name)} (${texts.attacksOf(it.attacks, it.attackLimit)})" }
        }
        return listOf("🎯 ${texts.unusedAttacks(season.unusedAttacks())}") + absentLines + lessLines
    }
}
