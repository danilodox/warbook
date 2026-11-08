package io.github.danilodox.warbook.ui.war

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.unit.dp
import io.github.danilodox.warbook.resources.Res
import io.github.danilodox.warbook.resources.ic_check
import io.github.danilodox.warbook.resources.ic_copy
import io.github.danilodox.warbook.resources.ic_edit
import io.github.danilodox.warbook.resources.ic_share
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
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val COPIED_FEEDBACK_MS = 2_000L

/**
 * Barra presa embaixo da Guerra: "Resumo para o WhatsApp", Copiar ("Copiado" fica no botão por 2 s)
 * e Compartilhar. O lápis abre "Personalizar": uma ficha por parte que muda o texto neste estado ([parts])
 * e a prévia exata do que vai ser copiado.
 */
@Composable
internal fun SummaryDock(
    summary: String,
    parts: List<SummaryPart>,
    options: SummaryOptions,
    ended: Boolean,
    onToggle: (SummaryPart) -> Unit,
) {
    var customizing by rememberSaveable { mutableStateOf(false) }
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Column(
                Modifier.padding(start = Spacing.lg, end = Spacing.sm, top = Spacing.xs, bottom = Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(Res.string.war_summary_title),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f),
                    )
                    if (parts.isNotEmpty()) {
                        IconButton(onClick = { customizing = true }) {
                            Icon(painterResource(Res.drawable.ic_edit), stringResource(Res.string.war_customize))
                        }
                    }
                }
                CopyAndShare(summary, Modifier.padding(end = Spacing.sm))
            }
        }
    }
    if (customizing && parts.isNotEmpty()) {
        CustomizeSheet(summary, parts, options, ended, onToggle, onDismiss = { customizing = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomizeSheet(
    summary: String,
    parts: List<SummaryPart>,
    options: SummaryOptions,
    ended: Boolean,
    onToggle: (SummaryPart) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding()
                .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text(stringResource(Res.string.war_customize), style = MaterialTheme.typography.titleLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                parts.forEach { part ->
                    val selected = part in options
                    FilterChip(
                        selected = selected,
                        onClick = { onToggle(part) },
                        label = { Text(stringResource(part.label(ended))) },
                        leadingIcon = if (selected) {
                            { Icon(painterResource(Res.drawable.ic_check), null, Modifier.size(16.dp)) }
                        } else {
                            null
                        },
                    )
                }
            }
            Text(
                stringResource(Res.string.war_preview),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(summary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(Spacing.md)) }
            CopyAndShare(summary)
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.war_customize_done))
            }
        }
    }
}

@Composable
private fun CopyAndShare(summary: String, modifier: Modifier = Modifier) {
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
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        FilledTonalButton(
            onClick = {
                scope.launch {
                    clipboard.setClipEntry(plainTextClipEntry(summary))
                    copied = true
                }
            },
            modifier = Modifier.weight(1f),
        ) {
            ButtonContent(
                if (copied) Res.drawable.ic_check else Res.drawable.ic_copy,
                if (copied) Res.string.war_copied else Res.string.war_copy,
            )
        }
        if (share != null) {
            Button(onClick = { share(summary) }, modifier = Modifier.weight(1f)) {
                ButtonContent(Res.drawable.ic_share, Res.string.war_share)
            }
        }
    }
}

@Composable
private fun ButtonContent(icon: DrawableResource, text: StringResource) {
    Icon(painterResource(icon), contentDescription = null, Modifier.size(18.dp))
    Text(stringResource(text), Modifier.padding(start = Spacing.sm))
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
