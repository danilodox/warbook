package io.github.danilodox.warbook.history

import io.github.danilodox.warbook.api.WarOutcome
import io.github.danilodox.warbook.raid.DayMonth
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Textos do resumo de fim de guerra. Como os outros resumos, ficam no código para o texto inteiro ser
 * testado sem Compose.
 */
interface WarRecordSummaryTexts {
    val decimalSeparator: String
    val stars: String
    val destruction: String
    val bestTitle: String
    val allAttacked: String
    val easyTitle: String

    fun warTitle(opponent: String): String

    fun cwlTitle(opponent: String): String

    /** "13/11" em português, "11/13" em inglês. */
    fun date(date: DayMonth): String

    fun result(result: WarOutcome): String

    fun missedTitle(attacks: Int): String

    fun attacksOf(used: Int, available: Int): String

    fun easyCount(attacks: Int): String

    /** Uma casa decimal, com o separador do idioma. */
    fun decimal(value: Double): String {
        val tenths = (value * TENTHS).roundToLong()
        return "${tenths / TENTHS}$decimalSeparator${abs(tenths % TENTHS)}"
    }
}

private const val TENTHS = 10

private fun twoDigits(value: Int) = value.toString().padStart(2, '0')

object PortugueseWarRecordSummaryTexts : WarRecordSummaryTexts {
    override val decimalSeparator = ","
    override val stars = "Estrelas"
    override val destruction = "Destruição"
    override val bestTitle = "Melhores notas"
    override val allAttacked = "Todo mundo usou todos os ataques"
    override val easyTitle = "Pegaram alvo fácil"

    override fun warTitle(opponent: String) = "Fim da guerra contra $opponent"

    override fun cwlTitle(opponent: String) = "Fim da rodada da CWL contra $opponent"

    override fun date(date: DayMonth) = "${twoDigits(date.day)}/${twoDigits(date.month)}"

    override fun result(result: WarOutcome) = when (result) {
        WarOutcome.WIN -> "Vitória!"
        WarOutcome.LOSE -> "Derrota"
        WarOutcome.TIE -> "Empate"
    }

    override fun missedTitle(attacks: Int) =
        if (attacks == 1) "1 ataque não foi feito" else "$attacks ataques não foram feitos"

    override fun attacksOf(used: Int, available: Int) = "$used de $available"

    override fun easyCount(attacks: Int) = if (attacks == 1) "1 ataque" else "$attacks ataques"
}

object EnglishWarRecordSummaryTexts : WarRecordSummaryTexts {
    override val decimalSeparator = "."
    override val stars = "Stars"
    override val destruction = "Destruction"
    override val bestTitle = "Top scores"
    override val allAttacked = "Everyone used all attacks"
    override val easyTitle = "Took easy targets"

    override fun warTitle(opponent: String) = "War against $opponent is over"

    override fun cwlTitle(opponent: String) = "CWL round against $opponent is over"

    override fun date(date: DayMonth) = "${twoDigits(date.month)}/${twoDigits(date.day)}"

    override fun result(result: WarOutcome) = when (result) {
        WarOutcome.WIN -> "Victory!"
        WarOutcome.LOSE -> "Defeat"
        WarOutcome.TIE -> "Draw"
    }

    override fun missedTitle(attacks: Int) = if (attacks == 1) "1 attack missed" else "$attacks attacks missed"

    override fun attacksOf(used: Int, available: Int) = "$used of $available"

    override fun easyCount(attacks: Int) = if (attacks == 1) "1 attack" else "$attacks attacks"
}

/** Português para qualquer variante `pt`; inglês para o resto. */
fun warRecordSummaryTextsFor(languageCode: String): WarRecordSummaryTexts =
    if (languageCode.lowercase().startsWith("pt")) PortugueseWarRecordSummaryTexts else EnglishWarRecordSummaryTexts
