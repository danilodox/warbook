package io.github.danilodox.warbook.war

import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Textos do resumo do WhatsApp. Ficam no código, e não no Compose Resources, para o texto inteiro
 * ser testado sem Compose. A tela também usa [decimal] e [duration].
 */
interface SummaryTexts {
    val decimalSeparator: Char
    val stars: String
    val destruction: String
    val strengthTitle: String
    val averageTownHall: String
    val maxTownHall: String
    val allAttacked: String
    val noneMissed: String
    val preparationTitle: String
    val bestAttacksTitle: String
    val weakAttacksTitle: String

    fun townHall(level: Int): String

    fun round(round: Int, total: Int): String

    fun preparation(startsIn: String): String

    fun endsIn(remaining: String): String

    fun result(result: WarResult): String

    fun pendingTitle(attacks: Int, members: Int): String

    fun missedTitle(attacks: Int): String

    /** Linha final de uma lista cortada: "+2 ataques". */
    fun more(count: Int): String

    /** "5h 12min"; abaixo de 1 h só os minutos. */
    fun duration(millis: Long): String

    /** Uma casa decimal, com o separador do idioma. */
    fun decimal(value: Double): String {
        val tenths = (value * TENTHS).roundToLong()
        return "${tenths / TENTHS}$decimalSeparator${abs(tenths % TENTHS)}"
    }
}

private const val TENTHS = 10
private const val MINUTE_MS = 60_000L
private const val MINUTES_PER_HOUR = 60L

/** Horas e minutos inteiros de [millis] (negativo vira zero). */
private fun hoursAndMinutes(millis: Long): Pair<Long, Long> {
    val minutes = millis.coerceAtLeast(0) / MINUTE_MS
    return minutes / MINUTES_PER_HOUR to minutes % MINUTES_PER_HOUR
}

private fun formatDuration(millis: Long, minuteUnit: String): String {
    val (hours, minutes) = hoursAndMinutes(millis)
    return if (hours > 0) "${hours}h ${minutes.toString().padStart(2, '0')}$minuteUnit" else "$minutes$minuteUnit"
}

object PortugueseSummaryTexts : SummaryTexts {
    override val decimalSeparator = ','
    override val stars = "Estrelas"
    override val destruction = "Destruição"
    override val strengthTitle = "Força por CV"
    override val averageTownHall = "CV médio"
    override val maxTownHall = "CV máximo"
    override val allAttacked = "Todos já atacaram"
    override val noneMissed = "Ninguém deixou de atacar"
    override val preparationTitle = "Preparação"
    override val bestAttacksTitle = "Melhores ataques"
    override val weakAttacksTitle = "Ataques ruins"

    override fun townHall(level: Int) = "CV $level"

    override fun round(round: Int, total: Int) = "Rodada $round de $total"

    override fun preparation(startsIn: String) = "Preparação: a batalha começa em $startsIn"

    override fun endsIn(remaining: String) = "Termina em $remaining"

    override fun result(result: WarResult) = when (result) {
        WarResult.WIN -> "Vitória!"
        WarResult.LOSS -> "Derrota"
        WarResult.DRAW -> "Empate"
    }

    override fun pendingTitle(attacks: Int, members: Int): String {
        val left = if (attacks == 1) "Falta 1 ataque" else "Faltam $attacks ataques"
        val who = if (members == 1) "1 membro" else "$members membros"
        return "$left ($who)"
    }

    override fun missedTitle(attacks: Int) =
        if (attacks == 1) "1 ataque não foi feito" else "$attacks ataques não foram feitos"

    override fun more(count: Int) = if (count == 1) "+1 ataque" else "+$count ataques"

    override fun duration(millis: Long) = formatDuration(millis, "min")
}

object EnglishSummaryTexts : SummaryTexts {
    override val decimalSeparator = '.'
    override val stars = "Stars"
    override val destruction = "Destruction"
    override val strengthTitle = "Strength by TH"
    override val averageTownHall = "Average TH"
    override val maxTownHall = "Max TH"
    override val allAttacked = "Everyone has attacked"
    override val noneMissed = "No missed attacks"
    override val preparationTitle = "Preparation"
    override val bestAttacksTitle = "Best attacks"
    override val weakAttacksTitle = "Weak attacks"

    override fun townHall(level: Int) = "TH $level"

    override fun round(round: Int, total: Int) = "Round $round of $total"

    override fun preparation(startsIn: String) = "Preparation: battle starts in $startsIn"

    override fun endsIn(remaining: String) = "Ends in $remaining"

    override fun result(result: WarResult) = when (result) {
        WarResult.WIN -> "Victory!"
        WarResult.LOSS -> "Defeat"
        WarResult.DRAW -> "Draw"
    }

    override fun pendingTitle(attacks: Int, members: Int): String {
        val left = if (attacks == 1) "1 attack left" else "$attacks attacks left"
        val who = if (members == 1) "1 member" else "$members members"
        return "$left ($who)"
    }

    override fun missedTitle(attacks: Int) = if (attacks == 1) "1 attack missed" else "$attacks attacks missed"

    override fun more(count: Int) = if (count == 1) "+1 attack" else "+$count attacks"

    override fun duration(millis: Long) = formatDuration(millis, "m")
}

/** Português para qualquer variante `pt`; inglês para o resto. */
fun summaryTextsFor(languageCode: String): SummaryTexts =
    if (languageCode.lowercase().startsWith("pt")) PortugueseSummaryTexts else EnglishSummaryTexts
