package io.github.danilodox.warbook.raid

/** Dia e mês (1–12) de uma data, sem depender de biblioteca de datas. */
data class DayMonth(val day: Int, val month: Int)

private const val MILLIS_PER_DAY = 86_400_000L

/**
 * Data em UTC de [epochMillis]. O raid começa sexta 07:00 e acaba segunda 07:00 UTC: no Brasil (UTC−3) cai no
 * mesmo dia, então UTC basta para rotular o fim de semana. Algoritmo "civil from days" de Howard Hinnant
 * (ano começando em março; 146097 dias = 400 anos; 719468 = dias de 0000-03-01 até 1970-01-01).
 */
@Suppress("MagicNumber") // Constantes do algoritmo, explicadas acima.
fun dayMonthUtc(epochMillis: Long): DayMonth {
    val z = epochMillis.floorDiv(MILLIS_PER_DAY) + 719_468L
    val era = z.floorDiv(146_097L)
    val doe = z - era * 146_097L
    val yoe = (doe - doe / 1460 + doe / 36_524 - doe / 146_096) / 365
    val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
    val mp = (5 * doy + 2) / 153
    val day = doy - (153 * mp + 2) / 5 + 1
    val month = if (mp < 10) mp + 3 else mp - 9
    return DayMonth(day.toInt(), month.toInt())
}
