package io.github.danilodox.warbook.ui.share

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

actual fun plainTextClipEntry(text: String): ClipEntry = ClipEntry(ClipData.newPlainText("Warbook", text))

@Composable
actual fun rememberTextSharer(): ((String) -> Unit)? {
    val context = LocalContext.current
    return remember(context) {
        { text ->
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            context.startActivity(Intent.createChooser(send, null))
        }
    }
}

private const val PNG_QUALITY = 100

@Composable
actual fun rememberImageSharer(): (suspend (ImageBitmap) -> Unit)? {
    val context = LocalContext.current
    return remember(context) {
        { image ->
            val file = withContext(Dispatchers.IO) { writePng(context, image) }
            // A pasta é a do share_paths.xml; a autoridade, a do provider no manifest do androidApp.
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newRawUri(null, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(send, null))
        }
    }
}

/** Grava sempre no mesmo arquivo do cache: só existe o último cartão compartilhado. */
private fun writePng(context: Context, image: ImageBitmap): File {
    val dir = File(context.cacheDir, "share").apply { mkdirs() }
    val file = File(dir, "warbook-perfil.png")
    file.outputStream().use { image.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, it) }
    return file
}
