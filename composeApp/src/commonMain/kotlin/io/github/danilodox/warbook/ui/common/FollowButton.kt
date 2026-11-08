package io.github.danilodox.warbook.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.danilodox.warbook.resources.Res
import io.github.danilodox.warbook.resources.follow
import io.github.danilodox.warbook.resources.follow_limit
import io.github.danilodox.warbook.resources.following
import io.github.danilodox.warbook.resources.ic_star
import io.github.danilodox.warbook.resources.ic_star_filled
import io.github.danilodox.warbook.ui.theme.Spacing
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** "☆ Seguir" (cheio) ou "★ Seguindo" (contorno, estrela dourada), com o aviso do limite embaixo quando ele estoura. */
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
            OutlinedButton(onClick = onToggle, enabled = enabled) {
                Icon(
                    painterResource(Res.drawable.ic_star_filled),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Text(stringResource(Res.string.following), Modifier.padding(start = Spacing.sm))
            }
        } else {
            Button(onClick = onToggle, enabled = enabled) {
                Icon(painterResource(Res.drawable.ic_star), contentDescription = null, Modifier.size(18.dp))
                Text(stringResource(Res.string.follow), Modifier.padding(start = Spacing.sm))
            }
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
