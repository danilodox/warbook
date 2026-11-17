package io.github.danilodox.warbook.raid

import io.github.danilodox.warbook.ui.components.formatThousands

/**
 * Textos do resumo do raid para o WhatsApp. Como os da guerra, ficam no código para o texto inteiro ser
 * testado sem Compose. "Ouro da Capital" sempre por extenso: o raid não dá ouro comum.
 */
interface RaidSummaryTexts {
    val thousandsSeparator: String
    val title: String
    val ongoing: String
    val capitalGold: String
    val attacks: String
    val districts: String
    val raids: String
    val allAttacked: String
    val attackedLessTitle: String

    /** "13/11" em português, "11/13" em inglês. */
    fun date(date: DayMonth): String

    fun medals(perAttack: Int, full: Int): String

    fun endsIn(remaining: String): String

    fun notAttackedTitle(members: Int, ongoing: Boolean): String

    fun attacksOf(attacks: Int, limit: Int): String

    fun unusedAttacks(attacks: Int): String

    /** "5h 12min"; abaixo de 1 h só os minutos. */
    fun duration(millis: Long): String

    fun number(value: Int): String = formatThousands(value, thousandsSeparator)
}

private const val MINUTE_MS = 60_000L
private const val MINUTES_PER_HOUR = 60L
private const val HOURS_PER_DAY = 24L

private fun twoDigits(value: Int) = value.toString().padStart(2, '0')

/** Raid dura 3 dias: acima de 24 h mostra dias e horas. */
private fun formatDuration(millis: Long, minuteUnit: String, dayUnit: String): String {
    val minutes = millis.coerceAtLeast(0) / MINUTE_MS
    val hours = minutes / MINUTES_PER_HOUR
    return when {
        hours >= HOURS_PER_DAY -> "${hours / HOURS_PER_DAY}$dayUnit ${hours % HOURS_PER_DAY}h"
        hours > 0 -> "${hours}h ${twoDigits((minutes % MINUTES_PER_HOUR).toInt())}$minuteUnit"
        else -> "$minutes$minuteUnit"
    }
}

object PortugueseRaidSummaryTexts : RaidSummaryTexts {
    override val thousandsSeparator = "."
    override val title = "Raid da Capital"
    override val ongoing = "Em andamento"
    override val capitalGold = "Ouro da Capital"
    override val attacks = "Ataques"
    override val districts = "Distritos"
    override val raids = "Raids"
    override val allAttacked = "Todo mundo usou todos os ataques"
    override val attackedLessTitle = "Atacaram menos"

    override fun date(date: DayMonth) = "${twoDigits(date.day)}/${twoDigits(date.month)}"

    override fun medals(perAttack: Int, full: Int) =
        "Medalhas de raid: ${number(perAttack)} por ataque · ${number(full)} com 6"

    override fun endsIn(remaining: String) = "Termina em $remaining"

    override fun notAttackedTitle(members: Int, ongoing: Boolean) = when {
        ongoing && members == 1 -> "Falta 1 membro atacar"
        ongoing -> "Faltam $members membros atacar"
        members == 1 -> "1 membro não atacou"
        else -> "$members membros não atacaram"
    }

    override fun attacksOf(attacks: Int, limit: Int) = "$attacks de $limit"

    override fun unusedAttacks(attacks: Int) =
        if (attacks == 1) "1 ataque sobrando no clã" else "$attacks ataques sobrando no clã"

    override fun duration(millis: Long) = formatDuration(millis, "min", "d")
}

object EnglishRaidSummaryTexts : RaidSummaryTexts {
    override val thousandsSeparator = ","
    override val title = "Capital Raid"
    override val ongoing = "Ongoing"
    override val capitalGold = "Capital Gold"
    override val attacks = "Attacks"
    override val districts = "Districts"
    override val raids = "Raids"
    override val allAttacked = "Everyone used all attacks"
    override val attackedLessTitle = "Attacked less"

    override fun date(date: DayMonth) = "${twoDigits(date.month)}/${twoDigits(date.day)}"

    override fun medals(perAttack: Int, full: Int) =
        "Raid medals: ${number(perAttack)} per attack · ${number(full)} with 6"

    override fun endsIn(remaining: String) = "Ends in $remaining"

    override fun notAttackedTitle(members: Int, ongoing: Boolean) = when {
        ongoing && members == 1 -> "1 member still to attack"
        ongoing -> "$members members still to attack"
        members == 1 -> "1 member didn't attack"
        else -> "$members members didn't attack"
    }

    override fun attacksOf(attacks: Int, limit: Int) = "$attacks of $limit"

    override fun unusedAttacks(attacks: Int) =
        if (attacks == 1) "1 attack left in the clan" else "$attacks attacks left in the clan"

    override fun duration(millis: Long) = formatDuration(millis, "m", "d")
}

/** Português para qualquer variante `pt`; inglês para o resto. */
fun raidSummaryTextsFor(languageCode: String): RaidSummaryTexts =
    if (languageCode.lowercase().startsWith("pt")) PortugueseRaidSummaryTexts else EnglishRaidSummaryTexts
