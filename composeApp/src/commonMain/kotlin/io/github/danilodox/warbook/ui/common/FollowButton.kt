package io.github.danilodox.warbook.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.danilodox.warbook.resources.Res
import io.github.danilodox.warbook.resources.follow
import io.github.danilodox.warbook.resources.follow_limit
import io.github.danilodox.warbook.resources.following
import org.jetbrains.compose.resources.stringResource

/** "Seguir" (cheio) ou "✓ Seguindo" (contorno), com o aviso do limite embaixo quando ele estoura. */
@Composable
fun FollowButton(
    following: Boolean,
    limitReached: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        if (following) {
            OutlinedButton(onClick = onToggle, enabled = enabled) { Text("✓ ${stringResource(Res.string.following)}") }
        } else {
            Button(onClick = onToggle, enabled = enabled) { Text(stringResource(Res.string.follow)) }
        }
        if (limitReached) {
            Text(
                stringResource(Res.string.follow_limit),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
