package io.github.danilodox.warbook.ui.components

import androidx.compose.runtime.Composable
import io.github.danilodox.warbook.resources.Res
import io.github.danilodox.warbook.resources.thousands_separator
import org.jetbrains.compose.resources.stringResource

private const val GROUP = 3

/** 5312 → "5.312" com [separator] "." (PT) ou "5,312" com "," (EN). */
fun formatThousands(value: Int, separator: String): String {
    val digits = value.toLong().let { if (it < 0) -it else it }.toString()
    val grouped = digits.reversed().chunked(GROUP).joinToString(separator).reversed()
    return if (value < 0) "-$grouped" else grouped
}

/** O número com o separador de milhar do idioma do app. */
@Composable
fun Int.grouped(): String = formatThousands(this, stringResource(Res.string.thousands_separator))
