package io.github.danilodox.warbook

import androidx.compose.ui.window.ComposeUIViewController
import io.github.danilodox.warbook.di.AppInfo
import io.github.danilodox.warbook.di.initKoin
import org.koin.mp.KoinPlatformTools
import platform.Foundation.NSBundle
import platform.UIKit.UIViewController

@Suppress("FunctionName")
fun MainViewController(): UIViewController {
    if (KoinPlatformTools.defaultContext().getOrNull() == null) {
        val version = NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String
        initKoin(AppInfo(versionName = version ?: "dev"))
    }
    return ComposeUIViewController { App() }
}
