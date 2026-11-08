package io.github.danilodox.warbook.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/** Ações que a tela aberta põe na barra de topo: a barra é do App, mas só a tela sabe o que compartilhar. */
@Stable
class TopBarActions {
    var share: (() -> Unit)? by mutableStateOf(null)
}

val LocalTopBarActions = staticCompositionLocalOf { TopBarActions() }

/** Mostra o ícone Compartilhar no topo enquanto esta tela estiver na tela; `null` esconde. */
@Composable
fun TopBarShareAction(onShare: (() -> Unit)?) {
    val holder = LocalTopBarActions.current
    val latest by rememberUpdatedState(onShare)
    val enabled = onShare != null
    DisposableEffect(holder, enabled) {
        val action: () -> Unit = { latest?.invoke() }
        if (enabled) holder.share = action
        // Na troca de tela a nova pode registrar antes de a antiga sair: só limpa se ainda for a sua.
        onDispose { if (holder.share === action) holder.share = null }
    }
}
