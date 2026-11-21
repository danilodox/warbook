package io.github.danilodox.warbook

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import io.github.danilodox.warbook.navigation.Route
import io.github.danilodox.warbook.ui.history.ClanActivityRoute
import io.github.danilodox.warbook.ui.history.CwlPlannerRoute
import io.github.danilodox.warbook.ui.history.PlayerPerformanceRoute
import io.github.danilodox.warbook.ui.history.WarHistoryRoute
import io.github.danilodox.warbook.ui.history.WarRecordRoute
import io.github.danilodox.warbook.ui.war.WarRoute

/**
 * Aba Guerra e o histórico de guerras (G5): a lista do clã, uma guerra com a nota de cada membro, o
 * desempenho de um jogador e a atividade dos membros.
 */
internal fun NavGraphBuilder.warDestinations(navController: NavHostController) {
    composable<Route.War> {
        WarRoute(
            onAddAccount = { navController.navigate(Route.Onboarding) },
            onOpenHistory = { clanTag -> navController.navigate(Route.WarHistory(clanTag)) },
        )
    }
    composable<Route.WarHistory> { entry ->
        val clanTag = entry.toRoute<Route.WarHistory>().clanTag
        WarHistoryRoute(
            clanTag,
            onOpenWar = { warId -> navController.navigate(Route.WarRecord(clanTag, warId)) },
            onOpenActivity = { navController.navigate(Route.ClanActivity(clanTag)) },
            onOpenPlanner = { navController.navigate(Route.CwlPlanner(clanTag)) },
        )
    }
    composable<Route.CwlPlanner> { entry -> CwlPlannerRoute(entry.toRoute<Route.CwlPlanner>().clanTag) }
    composable<Route.WarRecord> { entry ->
        val route = entry.toRoute<Route.WarRecord>()
        WarRecordRoute(
            clanTag = route.clanTag,
            warId = route.warId,
            onOpenPlayer = { playerTag -> navController.navigate(Route.PlayerPerformance(route.clanTag, playerTag)) },
        )
    }
    composable<Route.PlayerPerformance> { entry ->
        val route = entry.toRoute<Route.PlayerPerformance>()
        PlayerPerformanceRoute(
            clanTag = route.clanTag,
            playerTag = route.playerTag,
            onOpenWar = { warId -> navController.navigate(Route.WarRecord(route.clanTag, warId)) },
            onOpenProfile = { navController.navigate(Route.Player(route.playerTag)) },
        )
    }
    composable<Route.ClanActivity> { entry ->
        val clanTag = entry.toRoute<Route.ClanActivity>().clanTag
        ClanActivityRoute(
            clanTag,
            onOpenMember = { playerTag -> navController.navigate(Route.PlayerPerformance(clanTag, playerTag)) },
        )
    }
}
