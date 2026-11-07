package io.github.danilodox.warbook.ui.war

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import io.github.danilodox.warbook.resources.Res
import io.github.danilodox.warbook.resources.war_copied
import io.github.danilodox.warbook.resources.war_copy
import io.github.danilodox.warbook.resources.war_share
import io.github.danilodox.warbook.resources.war_summary_title
import io.github.danilodox.warbook.ui.share.plainTextClipEntry
import io.github.danilodox.warbook.ui.share.rememberTextSharer
import io.github.danilodox.warbook.ui.theme.Spacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

private const val COPIED_FEEDBACK_MS = 2_000L

/** Copiar e Compartilhar o resumo do WhatsApp. "Copiado" fica no botão por 2 s. */
@Composable
internal fun SummaryActions(summary: String) {
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
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
        Text(stringResource(Res.string.war_summary_title), style = MaterialTheme.typography.titleSmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
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
}
