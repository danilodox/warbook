package io.github.danilodox.warbook.navigation

import io.github.danilodox.warbook.resources.Res
import io.github.danilodox.warbook.resources.ic_tab_clan
import io.github.danilodox.warbook.resources.ic_tab_me
import io.github.danilodox.warbook.resources.ic_tab_search
import io.github.danilodox.warbook.resources.ic_tab_war
import io.github.danilodox.warbook.resources.tab_clan
import io.github.danilodox.warbook.resources.tab_me
import io.github.danilodox.warbook.resources.tab_search
import io.github.danilodox.warbook.resources.tab_war
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

/** Destinos da navegação type-safe. */
sealed interface Route {
    @Serializable
    data object Me : Route

    @Serializable
    data object Clan : Route

    @Serializable
    data object War : Route

    @Serializable
    data object Search : Route

    @Serializable
    data object Settings : Route
}

/** Abas da barra inferior, na ordem em que aparecem. */
enum class TopLevelTab(val route: Route, val label: StringResource, val icon: DrawableResource) {
    ME(Route.Me, Res.string.tab_me, Res.drawable.ic_tab_me),
    CLAN(Route.Clan, Res.string.tab_clan, Res.drawable.ic_tab_clan),
    WAR(Route.War, Res.string.tab_war, Res.drawable.ic_tab_war),
    SEARCH(Route.Search, Res.string.tab_search, Res.drawable.ic_tab_search),
}
