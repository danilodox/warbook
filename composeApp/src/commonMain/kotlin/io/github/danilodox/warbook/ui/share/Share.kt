package io.github.danilodox.warbook.ui.share

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.ClipEntry

/** Texto puro para `LocalClipboard.setClipEntry`. Cada plataforma monta o [ClipEntry] do seu jeito. */
expect fun plainTextClipEntry(text: String): ClipEntry

/**
 * Abre o compartilhamento do sistema (share sheet) com um texto. `null` onde ainda não existe:
 * no iOS o botão Compartilhar fica escondido até o app ser testado num Mac.
 */
@Composable
expect fun rememberTextSharer(): ((String) -> Unit)?

/**
 * Compartilha uma imagem (o cartão de perfil) pelo share sheet. `null` onde ainda não existe:
 * no iOS o ícone fica escondido até o app ser testado num Mac.
 */
@Composable
expect fun rememberImageSharer(): (suspend (ImageBitmap) -> Unit)?
