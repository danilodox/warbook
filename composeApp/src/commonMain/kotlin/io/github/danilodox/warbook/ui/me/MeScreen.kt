package io.github.danilodox.warbook.ui.me

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.danilodox.warbook.data.AccountsRepository
import io.github.danilodox.warbook.data.AppError
import io.github.danilodox.warbook.data.ProfileRepository
import io.github.danilodox.warbook.data.currentTimeMillis
import io.github.danilodox.warbook.resources.Res
import io.github.danilodox.warbook.resources.ic_offline
import io.github.danilodox.warbook.resources.me_add_account
import io.github.danilodox.warbook.resources.me_no_account_body
import io.github.danilodox.warbook.resources.me_no_account_title
import io.github.danilodox.warbook.resources.offline_body
import io.github.danilodox.warbook.resources.offline_no_connection
import io.github.danilodox.warbook.resources.offline_title
import io.github.danilodox.warbook.resources.updated_days
import io.github.danilodox.warbook.resources.updated_hours
import io.github.danilodox.warbook.resources.updated_just_now
import io.github.danilodox.warbook.resources.updated_minutes
import io.github.danilodox.warbook.ui.components.ErrorState
import io.github.danilodox.warbook.ui.components.OfflineBanner
import io.github.danilodox.warbook.ui.components.SkeletonBlock
import io.github.danilodox.warbook.ui.components.SkeletonLine
import io.github.danilodox.warbook.ui.search.canRetry
import io.github.danilodox.warbook.ui.search.message
import io.github.danilodox.warbook.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/**
 * Aba Eu. [openAccounts] vem de "Gerenciar contas" nas Configurações: abre a lista de contas
 * e avisa com [onAccountsOpened].
 */
@Composable
fun MeRoute(
    openAccounts: Boolean,
    onAccountsOpened: () -> Unit,
    onAddAccount: () -> Unit,
    onVerify: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accounts: AccountsRepository = koinInject()
    val profiles: ProfileRepository = koinInject()
    val viewModel = viewModel { MeViewModel(accounts, profiles) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showAccounts by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(openAccounts) {
        if (openAccounts) {
            showAccounts = true
            onAccountsOpened()
        }
    }

    MeScreen(
        state = state,
        onRefresh = viewModel::refresh,
        onOpenAccounts = { showAccounts = true },
        onAddAccount = onAddAccount,
        onVerify = onVerify,
        modifier = modifier,
    )
    if (showAccounts && state.accounts.isNotEmpty()) {
        AccountsSheet(
            accounts = state.accounts,
            onDismiss = { showAccounts = false },
            onSwitch = { tag ->
                viewModel.switchTo(tag)
                showAccounts = false
            },
            onRemove = viewModel::remove,
            onAdd = {
                showAccounts = false
                onAddAccount()
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeScreen(
    state: MeState,
    onRefresh: () -> Unit,
    onOpenAccounts: () -> Unit,
    onAddAccount: () -> Unit,
    onVerify: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val active = state.active
    when {
        !state.loaded -> Unit
        active == null -> NoAccountScreen(onAddAccount, modifier)
        else -> PullToRefreshBox(
            isRefreshing = state.refreshing && state.profile != null,
            onRefresh = onRefresh,
            modifier = modifier.fillMaxSize(),
        ) {
            val profile = state.profile
            val error = state.refreshError
            when {
                profile != null -> Column(Modifier.fillMaxSize()) {
                    if (error != null) StaleBanner(error, profile.fetchedAt, onRefresh)
                    ProfileContent(
                        player = profile.player,
                        header = { AccountHeader(profile.player, active, onOpenAccounts) { onVerify(active.tag) } },
                        modifier = Modifier.weight(1f),
                    )
                }
                error != null -> OfflineCard(error, onRetry = onRefresh)
                else -> ProfileSkeleton()
            }
        }
    }
}

@Composable
fun NoAccountScreen(onAddAccount: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(Res.string.me_no_account_title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(Res.string.me_no_account_body),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onAddAccount) { Text(stringResource(Res.string.me_add_account)) }
    }
}

/** Faixa em couro quando a atualização falhou e a tela mostra o perfil salvo. */
@Composable
internal fun StaleBanner(error: AppError, fetchedAt: Long, onRetry: (() -> Unit)? = null) {
    val reason = if (error == AppError.NETWORK) Res.string.offline_no_connection else error.message
    val now = remember(fetchedAt) { currentTimeMillis() }
    val age = when (val value = ageOf(fetchedAt, now)) {
        Age.JustNow -> stringResource(Res.string.updated_just_now)
        is Age.Minutes -> stringResource(Res.string.updated_minutes, value.value)
        is Age.Hours -> stringResource(Res.string.updated_hours, value.value)
        is Age.Days -> stringResource(Res.string.updated_days, value.value)
    }
    OfflineBanner(
        "${stringResource(reason)} $age",
        modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        onRetry = onRetry?.takeIf { error.canRetry },
    )
}

/** Sem perfil salvo e sem conseguir buscar: o erro de tela cheia. Rolável para o puxar-para-atualizar. */
@Composable
internal fun OfflineCard(error: AppError, onRetry: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        if (error == AppError.NETWORK) {
            ErrorState(
                title = stringResource(Res.string.offline_title),
                body = stringResource(Res.string.offline_body),
                icon = Res.drawable.ic_offline,
                onRetry = onRetry,
            )
        } else {
            ErrorState(
                title = stringResource(error.message),
                body = null,
                code = error.name,
                onRetry = onRetry.takeIf { error.canRetry },
            )
        }
    }
}

/** Formato do perfil enquanto a primeira busca não volta. Os números são só o tamanho dos blocos. */
@Suppress("MagicNumber")
@Composable
internal fun ProfileSkeleton() {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            SkeletonBlock(width = 48.dp, height = 52.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                SkeletonLine(0.6f, 22.dp)
                SkeletonLine(0.35f, 14.dp)
                SkeletonLine(0.5f, 14.dp)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs + Spacing.xxs)) {
            repeat(3) { SkeletonBlock(width = null, height = 48.dp, modifier = Modifier.weight(1f)) }
        }
        SkeletonLine(0.3f, 16.dp)
        repeat(3) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SkeletonBlock(width = 38.dp, height = 38.dp)
                SkeletonLine(1f, 14.dp, Modifier.weight(1f))
            }
        }
        SkeletonLine(0.3f, 16.dp)
        repeat(2) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs + Spacing.xxs)) {
                repeat(5) { SkeletonBlock(width = null, height = 56.dp, modifier = Modifier.weight(1f)) }
            }
        }
    }
}
