package io.github.danilodox.warbook.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.danilodox.warbook.resources.Res
import io.github.danilodox.warbook.resources.you_tag
import io.github.danilodox.warbook.ui.theme.Spacing
import io.github.danilodox.warbook.ui.theme.tabular
import org.jetbrains.compose.resources.stringResource

/** Um número com o rótulo embaixo (Nível, Troféus, Doou…). Use dentro de [StatRow]. */
@Composable
fun RowScope.StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.small,
        modifier = modifier.weight(1f),
    ) {
        Column(Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs + Spacing.xxs)) {
            Text(value, style = MaterialTheme.typography.titleMedium.tabular(), maxLines = 1)
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Linha de [StatTile] com o mesmo espaço entre eles. */
@Composable
fun StatRow(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs + Spacing.xxs),
        content = content,
    )
}

/** Título de seção com uma ação ou contagem opcional à direita. */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, trailing: @Composable () -> Unit = {}) {
    Row(
        modifier.fillMaxWidth().padding(top = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
            modifier = Modifier.weight(1f),
        )
        trailing()
    }
}

/** Etiqueta "você" em dourado, ao lado do nome da sua linha. */
@Composable
fun YouTag(modifier: Modifier = Modifier) {
    Surface(color = MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.extraSmall, modifier = modifier) {
        Text(
            stringResource(Res.string.you_tag).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(horizontal = 5.dp),
        )
    }
}

/** Fundo da sua linha em listas (membros, mapa da guerra). */
@Composable
fun Modifier.youHighlight(isYou: Boolean): Modifier = if (isYou) {
    background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.medium)
} else {
    this
}

/** Cor do texto que fica sobre [youHighlight]. */
@Composable
fun youContentColor(isYou: Boolean) =
    if (isYou) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
