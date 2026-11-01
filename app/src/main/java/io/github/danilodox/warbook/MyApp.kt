package io.github.danilodox.warbook

import android.app.Application
import io.github.danilodox.warbook.data.datastore.SecureTokenProvider
import io.github.danilodox.warbook.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MyApp: Application() {
    private lateinit var secureTokenProvider: SecureTokenProvider

    override fun onCreate() {
        super.onCreate()

        secureTokenProvider = SecureTokenProvider(applicationContext)

        val apiTokenFromBuild = BuildConfig.API_KEY

        if (apiTokenFromBuild.isNotEmpty()) {
            secureTokenProvider.saveToken(apiTokenFromBuild)
        }

        startKoin {
            androidContext(this@MyApp)
            modules(appModules)
        }

    }

}