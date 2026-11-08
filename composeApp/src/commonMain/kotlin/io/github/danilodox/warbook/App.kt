package io.github.danilodox.warbook

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import io.github.danilodox.warbook.data.AccountsRepository
import io.github.danilodox.warbook.data.AppPreferences
import io.github.danilodox.warbook.data.ThemeMode
import io.github.danilodox.warbook.data.WarbookHttpClients
import io.github.danilodox.warbook.data.WarbookRepository
import io.github.danilodox.warbook.navigation.Route
import io.github.danilodox.warbook.navigation.StartViewModel
import io.github.danilodox.warbook.navigation.TopLevelTab
import io.github.danilodox.warbook.navigation.exploreFromOnboarding
import io.github.danilodox.warbook.navigation.hasInBackStack
import io.github.danilodox.warbook.navigation.isIn
import io.github.danilodox.warbook.navigation.leaveOnboarding
import io.github.danilodox.warbook.navigation.navigateToTab
import io.github.danilodox.warbook.resources.Res
import io.github.danilodox.warbook.resources.app_name
import io.github.danilodox.warbook.resources.back
import io.github.danilodox.warbook.resources.card_share
import io.github.danilodox.warbook.resources.ic_arrow_back
import io.github.danilodox.warbook.resources.ic_settings
import io.github.danilodox.warbook.resources.ic_share
import io.github.danilodox.warbook.resources.screen_clan
import io.github.danilodox.warbook.resources.screen_compare
import io.github.danilodox.warbook.resources.screen_player
import io.github.danilodox.warbook.resources.settings
import io.github.danilodox.warbook.ui.clan.ClanDetailRoute
import io.github.danilodox.warbook.ui.clan.ClanTabActions
import io.github.danilodox.warbook.ui.clan.ClanTabRoute
import io.github.danilodox.warbook.ui.common.LocalTopBarActions
import io.github.danilodox.warbook.ui.common.TopBarActions
import io.github.danilodox.warbook.ui.compare.CompareRoute
import io.github.danilodox.warbook.ui.me.MeRoute
import io.github.danilodox.warbook.ui.onboarding.ConfirmScreen
import io.github.danilodox.warbook.ui.onboarding.OnboardingViewModel
import io.github.danilodox.warbook.ui.onboarding.TagEvent
import io.github.danilodox.warbook.ui.onboarding.TagScreen
import io.github.danilodox.warbook.ui.player.PlayerRoute
import io.github.danilodox.warbook.ui.search.SearchRoute
import io.github.danilodox.warbook.ui.settings.SettingsRoute
import io.github.danilodox.warbook.ui.theme.SystemBarsAppearance
import io.github.danilodox.warbook.ui.theme.WarbookTheme
import io.github.danilodox.warbook.ui.verify.VerifyRoute
import io.github.danilodox.warbook.ui.war.WarRoute
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun App() {
    val httpClients: WarbookHttpClients = koinInject()
    // Primeira abertura: já pede o token da instalação, sem esperar a primeira busca.
    LaunchedEffect(Unit) { httpClients.tokens.warmUp() }

    val accounts: AccountsRepository = koinInject()
    val preferences: AppPreferences = koinInject()
    val startViewModel = viewModel { StartViewModel(accounts, preferences) }
    val start by startViewModel.start.collectAsStateWithLifecycle()

    val themeMode by preferences.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.DARK)
    val dark = themeMode.isDark(isSystemInDarkTheme())

    WarbookTheme(darkTheme = dark) {
        SystemBarsAppearance(dark)
        // Ler o banco é rápido: até lá fica só o fundo, sem tela de carregamento.
        val startRoute = start
        if (startRoute == null) Surface(Modifier.fillMaxSize()) {} else WarbookNavigation(startRoute)
    }
}

@Composable
private fun WarbookNavigation(startRoute: Route) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val tabHere = TopLevelTab.entries.firstOrNull { destination.isIn(it.route) }
    // Jogador e Clã abrem por cima de uma aba: a barra inferior continua marcando a aba de origem.
    var originTab by rememberSaveable { mutableStateOf<TopLevelTab?>(null) }
    if (tabHere != null && tabHere != originTab) originTab = tabHere
    val detailTitle = detailTitleOf(destination)
    val currentTab = tabHere ?: originTab.takeIf { detailTitle != null }
    val onSettings = destination?.hasRoute(Route.Settings::class) == true
    // Onboarding e verificação desenham o próprio topo e não têm barra inferior.
    val inFlow = destination == null || destination.isIn(Route.Onboarding) || destination.hasRoute(Route.Verify::class)

    val topBarActions = remember { TopBarActions() }
    CompositionLocalProvider(LocalTopBarActions provides topBarActions) {
        Scaffold(
            topBar = {
                if (!inFlow) {
                    WarbookTopBar(
                        onSettings = onSettings,
                        detailTitle = detailTitle,
                        currentTab = currentTab,
                        onShare = topBarActions.share.takeUnless { onSettings },
                        onBack = { navController.popBackStack() },
                        onOpenSettings = { navController.navigate(Route.Settings) { launchSingleTop = true } },
                    )
                }
            },
            bottomBar = {
                if (!inFlow && !onSettings) {
                    WarbookBottomBar(currentTab, onDetail = detailTitle != null, navController)
                }
            },
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = startRoute,
                modifier = Modifier.padding(padding),
            ) {
                mainGraph(navController)
                onboardingGraph(navController)
                verifyDestination(navController)
            }
        }
    }
}

/** O título das telas que abrem por cima de uma aba (Jogador, Clã), ou null. */
private fun detailTitleOf(destination: NavDestination?): StringResource? = when {
    destination?.hasRoute(Route.Player::class) == true -> Res.string.screen_player
    destination?.hasRoute(Route.ClanDetail::class) == true -> Res.string.screen_clan
    destination?.hasRoute(Route.Compare::class) == true -> Res.string.screen_compare
    else -> null
}

@Composable
private fun WarbookBottomBar(currentTab: TopLevelTab?, onDetail: Boolean, navController: NavHostController) {
    NavigationBar {
        TopLevelTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = tab == currentTab,
                onClick = {
                    // Na própria aba, por cima dela (Jogador/Clã): volta para a raiz da aba.
                    if (tab == currentTab && onDetail) {
                        navController.popBackStack(tab.route, inclusive = false)
                    } else {
                        navController.navigateToTab(tab)
                    }
                },
                icon = { Icon(painterResource(tab.icon), contentDescription = null) },
                label = { Text(stringResource(tab.label)) },
            )
        }
    }
}

/** As abas e as Configurações. */
private fun NavGraphBuilder.mainGraph(navController: NavHostController) {
    navigation<Route.Main>(startDestination = Route.Me) {
        composable<Route.Me> { entry ->
            val openAccounts by entry.savedStateHandle.getStateFlow(OPEN_ACCOUNTS, false)
                .collectAsStateWithLifecycle()
            MeRoute(
                openAccounts = openAccounts,
                onAccountsOpened = { entry.savedStateHandle[OPEN_ACCOUNTS] = false },
                onAddAccount = { navController.navigate(Route.Onboarding) },
                onVerify = { tag -> navController.navigate(Route.Verify(tag, fromOnboarding = false)) },
            )
        }
        composable<Route.Clan> {
            ClanTabRoute(
                ClanTabActions(
                    onAddAccount = { navController.navigate(Route.Onboarding) },
                    onSearchClans = {
                        navController.navigateToTab(TopLevelTab.SEARCH)
                        // A aba Buscar abre já no modo Clã.
                        navController.currentBackStackEntry?.savedStateHandle?.set(SEARCH_CLANS, true)
                    },
                    onOpenWar = { navController.navigateToTab(TopLevelTab.WAR) },
                    onOpenMember = { playerTag -> navController.navigate(Route.Player(playerTag)) },
                ),
            )
        }
        composable<Route.War> { WarRoute(onAddAccount = { navController.navigate(Route.Onboarding) }) }
        searchDestination(navController)
        composable<Route.Player> { entry ->
            val route = entry.toRoute<Route.Player>()
            PlayerRoute(
                tag = route.tag,
                fromSearch = route.fromSearch,
                onOpenClan = { clanTag -> navController.navigate(Route.ClanDetail(clanTag)) },
                onCompare = { navController.navigate(Route.Compare(route.tag)) },
            )
        }
        composable<Route.Compare> {
            CompareRoute(it.toRoute<Route.Compare>().tag, onAddAccount = { navController.navigate(Route.Onboarding) })
        }
        composable<Route.ClanDetail> { entry ->
            val route = entry.toRoute<Route.ClanDetail>()
            ClanDetailRoute(
                tag = route.tag,
                fromSearch = route.fromSearch,
                onOpenMember = { playerTag -> navController.navigate(Route.Player(playerTag)) },
            )
        }
    }
    composable<Route.Settings> {
        SettingsRoute(
            // A lista de contas mora na aba Eu: volta para ela já com a lista aberta.
            onManageAccounts = {
                navController.getBackStackEntry<Route.Me>().savedStateHandle[OPEN_ACCOUNTS] = true
                navController.navigateToTab(TopLevelTab.ME)
            },
            onVerify = { tag -> navController.navigate(Route.Verify(tag, fromOnboarding = false)) },
            onAddAccount = { navController.navigate(Route.Onboarding) },
        )
    }
}

private const val OPEN_ACCOUNTS = "open_accounts"

/** Pedido para a aba Buscar abrir no modo Clã (botão "Buscar clãs" da aba Clã sem clã). */
private const val SEARCH_CLANS = "search_clans"

/** Aba Buscar: abre Jogador e Clã marcados como vindos da busca (entram nas "Recentes"). */
private fun NavGraphBuilder.searchDestination(navController: NavHostController) {
    composable<Route.Search> { entry ->
        val openClanMode by entry.savedStateHandle.getStateFlow(SEARCH_CLANS, false)
            .collectAsStateWithLifecycle()
        SearchRoute(
            openClanMode = openClanMode,
            onClanModeOpened = { entry.savedStateHandle[SEARCH_CLANS] = false },
            onOpenPlayer = { tag -> navController.navigate(Route.Player(tag, fromSearch = true)) },
            onOpenClan = { tag -> navController.navigate(Route.ClanDetail(tag, fromSearch = true)) },
        )
    }
}

private fun NavGraphBuilder.verifyDestination(navController: NavHostController) {
    composable<Route.Verify> { entry ->
        val route = entry.toRoute<Route.Verify>()
        VerifyRoute(
            tag = route.tag,
            fromOnboarding = route.fromOnboarding,
            onBack = { navController.popBackStack() },
            onDone = { if (route.fromOnboarding) navController.leaveOnboarding() else navController.popBackStack() },
        )
    }
}

/** O1 → O2 → O3 (a O3 é o destino [Route.Verify]). */
private fun NavGraphBuilder.onboardingGraph(navController: NavHostController) {
    navigation<Route.Onboarding>(startDestination = Route.OnboardingTag) {
        composable<Route.OnboardingTag> { entry ->
            val viewModel = navController.onboardingViewModel(entry)
            val state by viewModel.state.collectAsStateWithLifecycle()
            // "Só explorar" só faz sentido na primeira abertura, não ao adicionar outra conta.
            val canExplore = remember { !navController.hasInBackStack<Route.Main>() }
            LaunchedEffect(viewModel) {
                viewModel.tagScreenEvents.collect { event ->
                    when (event) {
                        TagEvent.PlayerFound -> navController.navigate(Route.OnboardingConfirm)
                        TagEvent.Explore -> navController.exploreFromOnboarding()
                    }
                }
            }
            TagScreen(
                state = state,
                canExplore = canExplore,
                onContinue = viewModel::lookUp,
                onTagChanged = viewModel::clearError,
                onExplore = viewModel::explore,
            )
        }
        composable<Route.OnboardingConfirm> { entry ->
            val viewModel = navController.onboardingViewModel(entry)
            val state by viewModel.state.collectAsStateWithLifecycle()
            LaunchedEffect(viewModel) {
                viewModel.accountAdded.collect { tag ->
                    navController.navigate(Route.Verify(tag, fromOnboarding = true))
                }
            }
            state.player?.let { player ->
                ConfirmScreen(
                    player = player,
                    saving = state.saving,
                    onConfirm = viewModel::confirm,
                    onChangeTag = { navController.popBackStack() },
                )
            }
        }
    }
}

/** O ViewModel do onboarding vive no grafo, para a O1 e a O2 dividirem o jogador achado. */
@Composable
private fun NavHostController.onboardingViewModel(entry: NavBackStackEntry): OnboardingViewModel {
    val graphEntry = remember(entry) { getBackStackEntry<Route.Onboarding>() }
    val api: WarbookRepository = koinInject()
    val accounts: AccountsRepository = koinInject()
    val preferences: AppPreferences = koinInject()
    return viewModel(viewModelStoreOwner = graphEntry) { OnboardingViewModel(api, accounts, preferences) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WarbookTopBar(
    onSettings: Boolean,
    detailTitle: StringResource?,
    currentTab: TopLevelTab?,
    onShare: (() -> Unit)?,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val title = when {
        onSettings -> Res.string.settings
        detailTitle != null -> detailTitle
        currentTab != null -> currentTab.label
        else -> Res.string.app_name
    }
    TopAppBar(
        title = { Text(stringResource(title)) },
        navigationIcon = {
            if (onSettings || detailTitle != null) {
                IconButton(onClick = onBack) {
                    Icon(painterResource(Res.drawable.ic_arrow_back), stringResource(Res.string.back))
                }
            }
        },
        actions = {
            if (onShare != null) {
                IconButton(onClick = onShare) {
                    Icon(painterResource(Res.drawable.ic_share), stringResource(Res.string.card_share))
                }
            }
            if (!onSettings) {
                IconButton(onClick = onOpenSettings) {
                    Icon(painterResource(Res.drawable.ic_settings), stringResource(Res.string.settings))
                }
            }
        },
    )
}
