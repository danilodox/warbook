package io.github.danilodox.warbook.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextOverflow
import io.github.danilodox.warbook.api.ClanActivityDto
import io.github.danilodox.warbook.api.CwlSeasonDto
import io.github.danilodox.warbook.history.CwlSeasonSummary
import io.github.danilodox.warbook.history.cwlSeasonSummaryTextsFor
import io.github.danilodox.warbook.resources.Res
import io.github.danilodox.warbook.resources.decimal_separator
import io.github.danilodox.warbook.resources.history_cwl_season_rounds
import io.github.danilodox.warbook.resources.history_cwl_season_title
import io.github.danilodox.warbook.resources.history_record
import io.github.danilodox.warbook.ui.components.Panel
import io.github.danilodox.warbook.ui.theme.Spacing
import io.github.danilodox.warbook.ui.war.CopyAndShare
import org.jetbrains.compose.resources.stringResource

private const val CARD_TOP = 3

/**
 * Cartão da temporada da CWL no topo do histórico: placar somado, as [CARD_TOP] melhores notas e Copiar e
 * Compartilhar com o resumo da temporada. Some sem temporada guardada.
 */
@Composable
internal fun CwlSeasonCard(season: CwlSeasonDto, modifier: Modifier = Modifier) {
    val label = season.season?.let(CwlSeasonSummary::seasonLabel) ?: return
    val summary = CwlSeasonSummary.of(season, cwlSeasonSummaryTextsFor(Locale.current.language)) ?: return
    val separator = stringResource(Res.string.decimal_separator)
    Panel(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(Res.string.history_cwl_season_title, label),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                stringResource(Res.string.history_cwl_season_rounds, season.rounds, ClanActivityDto.CWL_ROUNDS),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            stringResource(Res.string.history_record, season.wins, season.losses, season.ties) +
                " · ⭐ ${season.stars} × ${season.opponentStars}",
            style = MaterialTheme.typography.bodyMedium,
        )
        season.members.filter { it.attacksUsed > 0 }.take(CARD_TOP).forEachIndexed { index, member ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${index + 1}.",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    member.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    formatPoints(member.total, separator),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        CopyAndShare(summary)
    }
}
