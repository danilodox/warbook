package io.github.danilodox.warbook.ui.history

import io.github.danilodox.warbook.api.WarOutcome
import io.github.danilodox.warbook.api.WarSummaryDto
import kotlin.math.abs
import kotlin.math.roundToLong

/** Vitórias, derrotas e empates de uma lista de guerras (as sem resultado ficam de fora). */
data class WarRecordCount(val wins: Int, val losses: Int, val ties: Int)

fun List<WarSummaryDto>.recordCount() = WarRecordCount(
    wins = count { it.result == WarOutcome.WIN },
    losses = count { it.result == WarOutcome.LOSE },
    ties = count { it.result == WarOutcome.TIE },
)

private const val CENTS = 100

/** Nota com duas casas e o separador do idioma: `2,80`, `-2,00`. [signed] põe `+` nos positivos. */
fun formatPoints(value: Double, decimalSeparator: String, signed: Boolean = false): String {
    val cents = (value * CENTS).roundToLong()
    val sign = when {
        cents < 0 -> "-"
        signed && cents > 0 -> "+"
        else -> ""
    }
    val whole = abs(cents) / CENTS
    val fraction = (abs(cents) % CENTS).toString().padStart(2, '0')
    return "$sign$whole$decimalSeparator$fraction"
}
