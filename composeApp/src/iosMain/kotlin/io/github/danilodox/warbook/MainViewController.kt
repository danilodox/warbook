package io.github.danilodox.warbook

import androidx.compose.ui.window.ComposeUIViewController
import io.github.danilodox.warbook.di.AppInfo
import io.github.danilodox.warbook.di.initKoin
import org.koin.mp.KoinPlatformTools
import platform.Foundation.NSBundle
import platform.UIKit.UIViewController

@Suppress("FunctionName", "ktlint:standard:function-naming")
fun MainViewController(): UIViewController {
    if (KoinPlatformTools.defaultContext().getOrNull() == null) {
        val version = NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String
        // Simulador acessa o servidor do Mac por localhost; produção entra no deploy (v0.9).
        initKoin(AppInfo(versionName = version ?: "dev", serverUrl = "http://localhost:8080"))
    }
    return ComposeUIViewController { App() }
}
