package io.github.danilodox.warbook

import io.github.danilodox.warbook.di.AppInfo
import io.github.danilodox.warbook.di.initKoin
import io.github.danilodox.warbook.navigation.Route
import io.github.danilodox.warbook.navigation.TopLevelTab
import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class FoundationTest {

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun abasNaOrdemEuClaGuerraBuscar() {
        assertEquals(
            listOf(Route.Me, Route.Clan, Route.War, Route.Search),
            TopLevelTab.entries.map { it.route },
        )
    }

    @Test
    fun koinEntregaAppInfo() {
        val koin = initKoin(AppInfo(versionName = "0.1.0")).koin

        assertEquals("0.1.0", koin.get<AppInfo>().versionName)
    }
}
