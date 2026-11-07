package io.github.danilodox.warbook.ui.share

import android.content.ClipData
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalContext

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
