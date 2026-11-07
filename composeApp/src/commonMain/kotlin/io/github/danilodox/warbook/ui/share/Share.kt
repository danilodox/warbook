package io.github.danilodox.warbook.ui.share

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ClipEntry

/** Texto puro para `LocalClipboard.setClipEntry`. Cada plataforma monta o [ClipEntry] do seu jeito. */
expect fun plainTextClipEntry(text: String): ClipEntry

/**
 * Abre o compartilhamento do sistema (share sheet) com um texto. `null` onde ainda não existe:
 * no iOS o botão Compartilhar fica escondido até o app ser testado num Mac.
 */
@Composable
expect fun rememberTextSharer(): ((String) -> Unit)?
