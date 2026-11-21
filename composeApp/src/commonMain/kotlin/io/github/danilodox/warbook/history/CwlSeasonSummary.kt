package io.github.danilodox.warbook.history

import io.github.danilodox.warbook.api.ClanActivityDto
import io.github.danilodox.warbook.api.CwlSeasonDto
import io.github.danilodox.warbook.ui.history.formatPoints
import io.github.danilodox.warbook.war.WhatsAppText

/**
 * Resumo da temporada da CWL para o WhatsApp: placar somado das rodadas guardadas, as [TOP] melhores notas
 * e quem deixou ataque sem usar (na CWL é um por rodada).
 */
object CwlSeasonSummary {

    const val TOP = 5

    /** `null` sem temporada guardada. */
    fun of(season: CwlSeasonDto, texts: CwlSeasonSummaryTexts): String? {
        val label = season.season?.let(::seasonLabel) ?: return null
        val header = listOf(
            "🏅 *${texts.title(label)}* · ${texts.rounds(season.rounds, ClanActivityDto.CWL_ROUNDS)}",
            "📈 ${texts.record(season.wins, season.losses, season.ties)}",
            "⭐ ${texts.stars}: *${season.stars}* × ${season.opponentStars}",
        )
        return listOf(header, best(season, texts), missed(season, texts))
            .filter { it.isNotEmpty() }
            .joinToString("\n\n") { it.joinToString("\n") }
    }

    /** "2026-11" → "11/2026"; fora desse formato, como veio. */
    fun seasonLabel(season: String): String {
        val parts = season.split("-")
        return if (parts.size == 2) "${parts[1]}/${parts[0]}" else season
    }

    private fun best(season: CwlSeasonDto, texts: CwlSeasonSummaryTexts): List<String> {
        val top = season.members.filter { it.attacksUsed > 0 }.take(TOP)
        if (top.isEmpty()) return emptyList()
        return listOf("📊 *${texts.bestTitle}*") + top.map {
            "• ${WhatsAppText.safeName(it.name)} ${formatPoints(it.total, texts.decimalSeparator)} (${it.stars}⭐)"
        }
    }

    private fun missed(season: CwlSeasonDto, texts: CwlSeasonSummaryTexts): List<String> {
        val missing = season.members.filter { it.attacksUsed < it.rounds }
        return when {
            season.members.isEmpty() -> emptyList()
            missing.isEmpty() -> listOf("✅ ${texts.allAttacked}")
            else -> listOf("❌ *${texts.missedTitle(missing.sumOf { it.rounds - it.attacksUsed })}*") +
                missing.map { "• ${WhatsAppText.safeName(it.name)} (${texts.attacksOf(it.attacksUsed, it.rounds)})" }
        }
    }
}

/** Textos do resumo da temporada da CWL, no código como os outros resumos. */
interface CwlSeasonSummaryTexts {
    val decimalSeparator: String
    val stars: String
    val bestTitle: String
    val allAttacked: String

    fun title(season: String): String

    fun rounds(stored: Int, total: Int): String

    fun record(wins: Int, losses: Int, ties: Int): String

    fun missedTitle(attacks: Int): String

    fun attacksOf(used: Int, rounds: Int): String
}

object PortugueseCwlSeasonSummaryTexts : CwlSeasonSummaryTexts {
    override val decimalSeparator = ","
    override val stars = "Estrelas"
    override val bestTitle = "Melhores notas da temporada"
    override val allAttacked = "Todo mundo atacou em todas as rodadas"

    override fun title(season: String) = "CWL de $season"

    override fun rounds(stored: Int, total: Int) = "$stored de $total rodadas"

    override fun record(wins: Int, losses: Int, ties: Int) =
        "${plural(wins, "vitória", "vitórias")} · ${plural(losses, "derrota", "derrotas")} · " +
            plural(ties, "empate", "empates")

    override fun missedTitle(attacks: Int) =
        if (attacks == 1) "1 ataque não foi feito" else "$attacks ataques não foram feitos"

    override fun attacksOf(used: Int, rounds: Int) = "$used de $rounds"
}

object EnglishCwlSeasonSummaryTexts : CwlSeasonSummaryTexts {
    override val decimalSeparator = "."
    override val stars = "Stars"
    override val bestTitle = "Season top scores"
    override val allAttacked = "Everyone attacked in every round"

    override fun title(season: String) = "CWL $season"

    override fun rounds(stored: Int, total: Int) = "$stored of $total rounds"

    override fun record(wins: Int, losses: Int, ties: Int) =
        "${plural(wins, "win", "wins")} · ${plural(losses, "loss", "losses")} · ${plural(ties, "draw", "draws")}"

    override fun missedTitle(attacks: Int) = if (attacks == 1) "1 attack missed" else "$attacks attacks missed"

    override fun attacksOf(used: Int, rounds: Int) = "$used of $rounds"
}

private fun plural(count: Int, one: String, many: String) = if (count == 1) "1 $one" else "$count $many"

/** Português para qualquer variante `pt`; inglês para o resto. */
fun cwlSeasonSummaryTextsFor(languageCode: String): CwlSeasonSummaryTexts =
    if (languageCode.lowercase().startsWith("pt")) PortugueseCwlSeasonSummaryTexts else EnglishCwlSeasonSummaryTexts
