package io.github.danilodox.warbook

import android.app.Application
import io.github.danilodox.warbook.di.AppInfo
import io.github.danilodox.warbook.di.initKoin
import org.koin.android.ext.koin.androidContext

class WarbookApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin(AppInfo(versionName = BuildConfig.VERSION_NAME, serverUrl = BuildConfig.SERVER_URL)) {
            androidContext(this@WarbookApplication)
        }
    }
}
