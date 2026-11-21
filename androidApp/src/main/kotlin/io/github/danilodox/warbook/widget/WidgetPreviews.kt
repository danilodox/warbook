package io.github.danilodox.warbook.widget

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import java.util.Locale

/**
 * Prévia gerada do widget na lista de widgets (Android 15+). O Android limita quantas vezes por hora o app pode
 * mandar a prévia, então ela só é mandada quando muda a versão do app ou o idioma (o texto da prévia é traduzido).
 * Abaixo do Android 15 a lista usa o `previewImage` do `war_widget_info.xml`.
 */
internal object WidgetPreviews {
    private const val PREFS = "widget_previews"
    private const val KEY_SENT = "sent_for"
    private const val TAG = "WidgetPreviews"

    suspend fun publishIfNeeded(context: Context, versionCode: Int) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return
        val key = "$versionCode-${Locale.getDefault().toLanguageTag()}"
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getString(KEY_SENT, null) == key) return
        val result = GlanceAppWidgetManager(context).setWidgetPreviews(WarWidgetReceiver::class)
        if (result == GlanceAppWidgetManager.SET_WIDGET_PREVIEWS_RESULT_SUCCESS) {
            prefs.edit().putString(KEY_SENT, key).apply()
        } else {
            // Limite atingido: tenta de novo na próxima abertura do app.
            Log.i(TAG, "Prévia do widget não enviada (resultado $result)")
        }
    }
}
