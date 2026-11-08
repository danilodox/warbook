package io.github.danilodox.warbook.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.danilodox.warbook.resources.Res
import io.github.danilodox.warbook.resources.error_code
import io.github.danilodox.warbook.resources.ic_cloud_off
import io.github.danilodox.warbook.resources.ic_offline
import io.github.danilodox.warbook.resources.ic_refresh
import io.github.danilodox.warbook.resources.retry
import io.github.danilodox.warbook.resources.retry_short
import io.github.danilodox.warbook.ui.theme.Spacing
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Faixa em couro: o conteúdo salvo continua na tela e esta faixa diz de quando ele é. */
@Composable
fun OfflineBanner(text: String, modifier: Modifier = Modifier, onRetry: (() -> Unit)? = null) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.small,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(start = Spacing.md, end = Spacing.xs, top = Spacing.xs, bottom = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Icon(painterResource(Res.drawable.ic_offline), contentDescription = null, modifier = Modifier.size(17.dp))
            Text(
                text,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f).padding(vertical = Spacing.xs),
            )
            if (onRetry != null) {
                TextButton(onClick = onRetry) {
                    Text(
                        stringResource(Res.string.retry_short),
                        textDecoration = TextDecoration.Underline,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        }
    }
}

/**
 * Erro de tela cheia: ícone no tom de erro, título, o que fazer, "Tentar de novo" e o código pequeno, para suporte.
 */
@Composable
fun ErrorState(
    title: String,
    body: String?,
    modifier: Modifier = Modifier,
    code: String? = null,
    icon: DrawableResource = Res.drawable.ic_cloud_off,
    onRetry: (() -> Unit)? = null,
) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = Spacing.xl, vertical = Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Box(
            Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.errorContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(30.dp),
            )
        }
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        if (body != null) {
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        if (onRetry != null) {
            Button(onClick = onRetry) {
                Icon(
                    painterResource(Res.drawable.ic_refresh),
                    contentDescription = null,
                    modifier = Modifier.size(17.dp),
                )
                Text(stringResource(Res.string.retry), modifier = Modifier.padding(start = Spacing.sm))
            }
        }
        if (code != null) {
            Text(
                stringResource(Res.string.error_code, code),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private const val SHIMMER_MS = 1600

/** Bloco do skeleton. O brilho passa devagar e fica parado quando o sistema pede menos movimento. */
@Composable
fun SkeletonBlock(width: Dp?, height: Dp, modifier: Modifier = Modifier, shape: Shape = MaterialTheme.shapes.small) {
    val base = MaterialTheme.colorScheme.surfaceContainerHigh
    val light = MaterialTheme.colorScheme.surfaceContainerHighest
    val sized = if (width == null) modifier.fillMaxWidth().height(height) else modifier.size(width, height)
    val moving = !rememberReduceMotion()
    val phase = if (moving) {
        val transition = rememberInfiniteTransition(label = "skeleton")
        val value by transition.animateFloat(
            initialValue = -1f,
            targetValue = 2f,
            animationSpec = infiniteRepeatable(tween(SHIMMER_MS, easing = LinearEasing), RepeatMode.Restart),
            label = "shimmer",
        )
        value
    } else {
        null
    }
    Box(
        sized.clip(shape).drawBehind {
            if (phase == null) {
                drawRect(base)
            } else {
                val x = size.width * phase
                drawRect(
                    Brush.linearGradient(
                        listOf(base, light, base),
                        start = Offset(x - size.width, 0f),
                        end = Offset(x, 0f),
                    ),
                )
            }
        },
    )
}

/** Fração da largura, para blocos de texto (60 % do nome, 40 % da tag…). */
@Composable
fun SkeletonLine(fraction: Float, height: Dp, modifier: Modifier = Modifier) {
    SkeletonBlock(width = null, height = height, modifier = modifier.fillMaxWidth(fraction))
}
