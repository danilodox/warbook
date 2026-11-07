package io.github.danilodox.warbook.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import io.github.danilodox.warbook.resources.Res
import io.github.danilodox.warbook.resources.ic_shield
import org.jetbrains.compose.resources.painterResource

// Imagens que a própria API do jogo entrega por URL (brasões e ligas). O Coil guarda em memória e em disco.
// Ícones de tropas e heróis continuam fora até a v0.6.

/** Brasão do clã. Sem URL, carregando ou com erro: um escudo neutro no mesmo tamanho. */
@Composable
fun ClanBadge(url: String?, contentDescription: String?, size: Dp, modifier: Modifier = Modifier) {
    RemoteImage(url, contentDescription, modifier.size(size), showFallbackIcon = true)
}

/** Ícone da liga de troféus. Sem URL não ocupa espaço; com erro fica só o fundo neutro. */
@Composable
fun LeagueIcon(url: String?, contentDescription: String?, size: Dp = 24.dp, modifier: Modifier = Modifier) {
    if (url == null) return
    RemoteImage(url, contentDescription, modifier.size(size), showFallbackIcon = false)
}

@Composable
private fun RemoteImage(url: String?, contentDescription: String?, modifier: Modifier, showFallbackIcon: Boolean) {
    val shape = MaterialTheme.shapes.small
    if (url == null) {
        Fallback(modifier.clip(shape), showFallbackIcon)
        return
    }
    SubcomposeAsyncImage(
        model = url,
        contentDescription = contentDescription,
        modifier = modifier.clip(shape),
        loading = { Placeholder() },
        error = { Fallback(Modifier.fillMaxSize(), showFallbackIcon) },
    )
}

@Composable
private fun Placeholder() {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant))
}

@Composable
private fun Fallback(modifier: Modifier, showIcon: Boolean) {
    Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
        if (showIcon) {
            Icon(
                painterResource(Res.drawable.ic_shield),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxSize().padding(4.dp),
            )
        }
    }
}
