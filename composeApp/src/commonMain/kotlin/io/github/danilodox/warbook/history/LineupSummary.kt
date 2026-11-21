package io.github.danilodox.warbook.history

import io.github.danilodox.warbook.score.CwlPlanner
import io.github.danilodox.warbook.score.PlannerCandidate
import io.github.danilodox.warbook.war.WhatsAppText

/**
 * Escalação do planejador pronta para o grupo: os escolhidos na ordem do mapa (nome e CV, sem nota, para
 * não expor ninguém) e até [RESERVES] reservas, os próximos da sugestão que ficaram de fora.
 */
object LineupSummary {

    const val RESERVES = 5

    /** `null` sem ninguém escolhido. [candidates] na ordem da sugestão. */
    fun of(candidates: List<PlannerCandidate>, selected: Set<String>, size: Int, texts: LineupSummaryTexts): String? {
        val lineup = CwlPlanner.lineup(candidates, selected)
        if (lineup.isEmpty()) return null
        val header = "📋 *${texts.title}* (${texts.count(lineup.size, size)})"
        val members = lineup.mapIndexed { index, member -> "${index + 1}. ${line(member, texts)}" }
        val reserves = CwlPlanner.rank(candidates).filterNot { it.tag in selected }.take(RESERVES)
        val blocks = listOf(listOf(header) + members) +
            listOfNotNull(
                reserves.takeIf { it.isNotEmpty() }
                    ?.let { listOf("🪑 *${texts.reservesTitle}*") + it.map { member -> "• ${line(member, texts)}" } },
            )
        return blocks.joinToString("\n\n") { it.joinToString("\n") }
    }

    private fun line(member: PlannerCandidate, texts: LineupSummaryTexts) =
        "${WhatsAppText.safeName(member.name)} · ${texts.townHall(member.townHall)}"
}

/** Textos da escalação, no código como os outros resumos. */
interface LineupSummaryTexts {
    val title: String
    val reservesTitle: String

    /** "15 × 15"; fora do tamanho, "14 de 15". */
    fun count(selected: Int, size: Int): String

    fun townHall(level: Int): String
}

object PortugueseLineupSummaryTexts : LineupSummaryTexts {
    override val title = "Escalação da CWL"
    override val reservesTitle = "Reservas"

    override fun count(selected: Int, size: Int) = if (selected == size) "$size × $size" else "$selected de $size"

    override fun townHall(level: Int) = "CV $level"
}

object EnglishLineupSummaryTexts : LineupSummaryTexts {
    override val title = "CWL lineup"
    override val reservesTitle = "Reserves"

    override fun count(selected: Int, size: Int) = if (selected == size) "$size × $size" else "$selected of $size"

    override fun townHall(level: Int) = "TH $level"
}

/** Português para qualquer variante `pt`; inglês para o resto. */
fun lineupSummaryTextsFor(languageCode: String): LineupSummaryTexts =
    if (languageCode.lowercase().startsWith("pt")) PortugueseLineupSummaryTexts else EnglishLineupSummaryTexts
