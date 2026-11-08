package io.github.danilodox.warbook.ui.war

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import io.github.danilodox.warbook.resources.Res
import io.github.danilodox.warbook.resources.summary_part_best
import io.github.danilodox.warbook.resources.summary_part_missed
import io.github.danilodox.warbook.resources.summary_part_pending
import io.github.danilodox.warbook.resources.summary_part_score
import io.github.danilodox.warbook.resources.summary_part_strength
import io.github.danilodox.warbook.resources.summary_part_time
import io.github.danilodox.warbook.resources.summary_part_weak
import io.github.danilodox.warbook.resources.war_copied
import io.github.danilodox.warbook.resources.war_copy
import io.github.danilodox.warbook.resources.war_customize
import io.github.danilodox.warbook.resources.war_customize_done
import io.github.danilodox.warbook.resources.war_preview
import io.github.danilodox.warbook.resources.war_share
import io.github.danilodox.warbook.resources.war_summary_title
import io.github.danilodox.warbook.ui.share.plainTextClipEntry
import io.github.danilodox.warbook.ui.share.rememberTextSharer
import io.github.danilodox.warbook.ui.theme.Spacing
import io.github.danilodox.warbook.war.SummaryOptions
import io.github.danilodox.warbook.war.SummaryPart
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private const val COPIED_FEEDBACK_MS = 2_000L

/**
 * Copiar e Compartilhar o resumo do WhatsApp ("Copiado" fica no botão por 2 s). Em "Personalizar", uma ficha
 * por parte que muda o texto neste estado ([parts]) e a prévia exata do que vai ser copiado.
 */
@Composable
internal fun SummaryActions(
    summary: String,
    parts: List<SummaryPart>,
    options: SummaryOptions,
    ended: Boolean,
    onToggle: (SummaryPart) -> Unit,
) {
    var customizing by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(stringResource(Res.string.war_summary_title), style = MaterialTheme.typography.titleSmall)
        CopyAndShare(summary)
        if (parts.isNotEmpty()) {
            TextButton(onClick = { customizing = !customizing }) {
                Text(stringResource(if (customizing) Res.string.war_customize_done else Res.string.war_customize))
            }
        }
        if (customizing && parts.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                parts.forEach { part ->
                    FilterChip(
                        selected = part in options,
                        onClick = { onToggle(part) },
                        label = { Text(stringResource(part.label(ended))) },
                    )
                }
            }
            Text(stringResource(Res.string.war_preview), style = MaterialTheme.typography.labelMedium)
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(summary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(Spacing.sm)) }
        }
    }
}

@Composable
private fun CopyAndShare(summary: String) {
    val clipboard = LocalClipboard.current
    val share = rememberTextSharer()
    val scope = rememberCoroutineScope()
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(COPIED_FEEDBACK_MS)
            copied = false
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        OutlinedButton(
            onClick = {
                scope.launch {
                    clipboard.setClipEntry(plainTextClipEntry(summary))
                    copied = true
                }
            },
            modifier = Modifier.weight(1f),
        ) { Text(stringResource(if (copied) Res.string.war_copied else Res.string.war_copy)) }
        if (share != null) {
            Button(onClick = { share(summary) }, modifier = Modifier.weight(1f)) {
                Text(stringResource(Res.string.war_share))
            }
        }
    }
}

/** Na guerra encerrada, "quem falta atacar" vira "quem não atacou". */
private fun SummaryPart.label(ended: Boolean): StringResource = when (this) {
    SummaryPart.SCORE -> Res.string.summary_part_score
    SummaryPart.TIME -> Res.string.summary_part_time
    SummaryPart.ATTACKERS -> if (ended) Res.string.summary_part_missed else Res.string.summary_part_pending
    SummaryPart.STRENGTH -> Res.string.summary_part_strength
    SummaryPart.BEST_ATTACKS -> Res.string.summary_part_best
    SummaryPart.WEAK_ATTACKS -> Res.string.summary_part_weak
}
