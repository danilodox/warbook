package io.github.danilodox.warbook.ui.war

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.danilodox.warbook.api.MonitoringState
import io.github.danilodox.warbook.data.MonitoringRepository
import io.github.danilodox.warbook.raid.dayMonthUtc
import io.github.danilodox.warbook.resources.Res
import io.github.danilodox.warbook.resources.monitoring_activate
import io.github.danilodox.warbook.resources.monitoring_active
import io.github.danilodox.warbook.resources.monitoring_off_body
import io.github.danilodox.warbook.resources.monitoring_off_title
import io.github.danilodox.warbook.resources.monitoring_paused
import io.github.danilodox.warbook.resources.monitoring_reactivate
import io.github.danilodox.warbook.resources.monitoring_verify_hint
import io.github.danilodox.warbook.ui.components.Panel
import io.github.danilodox.warbook.ui.search.message
import io.github.danilodox.warbook.ui.theme.Spacing
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/** Cartão do monitoramento do clã [clanTag]: estado, Ativar e Reativar. Some se o estado não carregar. */
@Composable
fun MonitoringCard(clanTag: String, modifier: Modifier = Modifier) {
    val repository: MonitoringRepository = koinInject()
    val viewModel = viewModel(key = "monitoring-$clanTag") { MonitoringViewModel(clanTag, repository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    MonitoringCardContent(state, onActivate = viewModel::activate, modifier = modifier)
}

@Composable
fun MonitoringCardContent(state: MonitoringUiState, onActivate: () -> Unit, modifier: Modifier = Modifier) {
    val status = state.status ?: return
    Panel(modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm)) {
        when (status.state) {
            MonitoringState.ACTIVE -> Text(
                stringResource(
                    Res.string.monitoring_active,
                    sinceDay(status.activatedAt),
                    sinceMonth(status.activatedAt),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            MonitoringState.PAUSED -> ActionRow(
                text = stringResource(Res.string.monitoring_paused),
                button = Res.string.monitoring_reactivate,
                state = state,
                onActivate = onActivate,
            )
            MonitoringState.OFF -> {
                Text(stringResource(Res.string.monitoring_off_title), style = MaterialTheme.typography.titleSmall)
                ActionRow(
                    text = stringResource(Res.string.monitoring_off_body),
                    button = Res.string.monitoring_activate,
                    state = state,
                    onActivate = onActivate,
                )
            }
        }
        if (status.state != MonitoringState.ACTIVE && !state.canActivate) {
            Text(
                stringResource(Res.string.monitoring_verify_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        state.error?.let {
            Text(
                stringResource(it.message),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun ActionRow(text: String, button: StringResource, state: MonitoringUiState, onActivate: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        if (state.canActivate) {
            val label = @Composable { Text(stringResource(button)) }
            if (state.status?.state == MonitoringState.OFF) {
                Button(onClick = onActivate, enabled = !state.activating) { label() }
            } else {
                FilledTonalButton(onClick = onActivate, enabled = !state.activating) { label() }
            }
        }
    }
}

private fun sinceDay(epochMillis: Long?) = epochMillis?.let { twoDigits(dayMonthUtc(it).day) }.orEmpty()

private fun sinceMonth(epochMillis: Long?) = epochMillis?.let { twoDigits(dayMonthUtc(it).month) }.orEmpty()

private fun twoDigits(value: Int) = value.toString().padStart(2, '0')
