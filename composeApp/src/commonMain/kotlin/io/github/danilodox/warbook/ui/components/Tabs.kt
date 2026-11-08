package io.github.danilodox.warbook.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.danilodox.warbook.ui.theme.Spacing
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Sub-abas com o traço dourado embaixo (Info | Membros, Guerra atual | CWL). */
@Composable
fun <T> SubTabs(tabs: List<Pair<T, StringResource>>, selected: T, onSelect: (T) -> Unit) {
    PrimaryTabRow(
        selectedTabIndex = tabs.indexOfFirst { it.first == selected }.coerceAtLeast(0),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        tabs.forEach { (tab, label) ->
            Tab(
                selected = tab == selected,
                onClick = { onSelect(tab) },
                text = { Text(stringResource(label), style = MaterialTheme.typography.titleSmall) },
                selectedContentColor = MaterialTheme.colorScheme.primary,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Escolha entre modos em botões segmentados (Jogador | Clã). */
@Composable
fun <T> SegmentedTabs(tabs: List<Pair<T, StringResource>>, selected: T, onSelect: (T) -> Unit) {
    SingleChoiceSegmentedButtonRow(
        Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.sm),
    ) {
        tabs.forEachIndexed { index, (tab, label) ->
            SegmentedButton(
                selected = tab == selected,
                onClick = { onSelect(tab) },
                shape = SegmentedButtonDefaults.itemShape(index, tabs.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    activeContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
            ) { Text(stringResource(label)) }
        }
    }
}
