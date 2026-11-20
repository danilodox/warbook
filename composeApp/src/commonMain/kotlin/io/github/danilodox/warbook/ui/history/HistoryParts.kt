package io.github.danilodox.warbook.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.danilodox.warbook.api.WarOutcome
import io.github.danilodox.warbook.api.WarSummaryDto
import io.github.danilodox.warbook.raid.dayMonthUtc
import io.github.danilodox.warbook.resources.Res
import io.github.danilodox.warbook.resources.history_date
import io.github.danilodox.warbook.resources.history_in_progress
import io.github.danilodox.warbook.resources.history_result_lose
import io.github.danilodox.warbook.resources.history_result_tie
import io.github.danilodox.warbook.resources.history_result_win
import io.github.danilodox.warbook.ui.components.SkeletonBlock
import io.github.danilodox.warbook.ui.me.OfflineCard
import io.github.danilodox.warbook.ui.me.StaleBanner
import io.github.danilodox.warbook.ui.theme.Spacing
import io.github.danilodox.warbook.ui.theme.WarbookTheme
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

// Peças das telas do histórico usadas na lista e na guerra.

/** Dia/mês do fim da guerra (UTC), na ordem do idioma. */
@Composable
internal fun warDate(epochMillis: Long): String {
    val date = dayMonthUtc(epochMillis)
    return stringResource(Res.string.history_date, twoDigits(date.day), twoDigits(date.month))
}

private fun twoDigits(value: Int) = value.toString().padStart(2, '0')

/** Selo do resultado: vitória no verde de sucesso, derrota no vermelho do tema, empate neutro. */
@Composable
internal fun ResultBadge(war: WarSummaryDto) {
    val (label, container, content) = resultStyle(war.result)
    Surface(color = container, shape = MaterialTheme.shapes.small) {
        Text(
            stringResource(label),
            style = MaterialTheme.typography.labelMedium,
            color = content,
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
        )
    }
}

@Composable
private fun resultStyle(result: WarOutcome?): Triple<StringResource, Color, Color> {
    val colors = MaterialTheme.colorScheme
    val extras = WarbookTheme.colors
    return when (result) {
        WarOutcome.WIN -> Triple(Res.string.history_result_win, extras.successContainer, extras.success)
        WarOutcome.LOSE -> Triple(Res.string.history_result_lose, colors.tertiaryContainer, colors.onTertiaryContainer)
        WarOutcome.TIE -> Triple(Res.string.history_result_tie, colors.surfaceContainerHighest, colors.onSurface)
        null -> Triple(Res.string.history_in_progress, colors.surfaceContainerHighest, colors.onSurfaceVariant)
    }
}

/**
 * Molde das telas do histórico: o que chegou (com a faixa de desatualizado se a atualização falhou), o
 * cartão de erro sem nada carregado, ou o skeleton. Puxar atualiza.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun <T> LoadScreen(
    state: LoadState<T>,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit,
) {
    val value = state.value
    val error = state.error
    PullToRefreshBox(
        isRefreshing = state.loading && value != null,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
    ) {
        when {
            value != null -> Column(Modifier.fillMaxSize()) {
                if (error != null) StaleBanner(error, state.fetchedAt, onRefresh)
                content(value)
            }
            error != null -> OfflineCard(error, onRetry = onRefresh)
            else -> HistorySkeleton()
        }
    }
}

@Composable
internal fun HistorySkeleton() {
    Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        repeat(SKELETON_ROWS) { SkeletonBlock(width = null, height = 64.dp, modifier = Modifier.fillMaxWidth()) }
    }
}

private const val SKELETON_ROWS = 6
