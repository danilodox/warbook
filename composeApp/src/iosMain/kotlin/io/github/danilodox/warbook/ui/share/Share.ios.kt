package io.github.danilodox.warbook.ui.share

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.ClipEntry

// Nunca compilado: o iOS precisa de um Mac (ver handoff).

@OptIn(ExperimentalComposeUiApi::class)
actual fun plainTextClipEntry(text: String): ClipEntry = ClipEntry.withPlainText(text)

/** Stub: o share sheet do iOS (UIActivityViewController) entra quando houver como testar. */
@Composable
actual fun rememberTextSharer(): ((String) -> Unit)? = null

/** Stub: o compartilhar imagem do iOS entra junto com o de texto. */
@Composable
actual fun rememberImageSharer(): (suspend (ImageBitmap) -> Unit)? = null
