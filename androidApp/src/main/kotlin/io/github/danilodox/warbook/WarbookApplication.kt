package io.github.danilodox.warbook

import android.app.Application
import androidx.glance.appwidget.updateAll
import io.github.danilodox.warbook.data.AppPreferences
import io.github.danilodox.warbook.di.AppInfo
import io.github.danilodox.warbook.di.initKoin
import io.github.danilodox.warbook.widget.WarWidget
import io.github.danilodox.warbook.widget.WidgetPreviews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext

class WarbookApplication : Application() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        initKoin(AppInfo(versionName = BuildConfig.VERSION_NAME, serverUrl = BuildConfig.SERVER_URL)) {
            androidContext(this@WarbookApplication)
        }
        redrawWidgetsOnThemeChange()
        scope.launch { WidgetPreviews.publishIfNeeded(this@WarbookApplication, BuildConfig.VERSION_CODE) }
    }

    /**
     * O widget só relê o tema quando é redesenhado; sem isto, trocar o tema nas Configurações levaria até
     * 30 min para chegar aos widgets que já estão na tela.
     */
    private fun redrawWidgetsOnThemeChange() {
        val preferences = get<AppPreferences>()
        scope.launch {
            preferences.themeMode.distinctUntilChanged().drop(1).collect {
                WarWidget().updateAll(this@WarbookApplication)
            }
        }
    }
}
