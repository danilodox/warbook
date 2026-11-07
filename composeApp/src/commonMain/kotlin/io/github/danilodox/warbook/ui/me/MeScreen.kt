package io.github.danilodox.warbook.ui.me

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import io.github.danilodox.warbook.resources.me_add_account
import io.github.danilodox.warbook.resources.me_no_account_body
import io.github.danilodox.warbook.resources.me_no_account_title
import io.github.danilodox.warbook.resources.offline_body
import io.github.danilodox.warbook.resources.offline_no_connection
import io.github.danilodox.warbook.resources.offline_title
import io.github.danilodox.warbook.resources.retry
import io.github.danilodox.warbook.resources.updated_days
import io.github.danilodox.warbook.resources.updated_hours
import io.github.danilodox.warbook.resources.updated_just_now
import io.github.danilodox.warbook.resources.updated_minutes
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
                    if (error != null) StaleBanner(error, profile.fetchedAt)
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
        modifier = modifier.fillMaxSize().padding(Spacing.medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.small, Alignment.CenterVertically),
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

/** Faixa de cima quando a atualização falhou e a tela mostra o perfil salvo. */
@Composable
internal fun StaleBanner(error: AppError, fetchedAt: Long) {
    val reason = if (error == AppError.NETWORK) Res.string.offline_no_connection else error.message
    val now = remember(fetchedAt) { currentTimeMillis() }
    val age = when (val value = ageOf(fetchedAt, now)) {
        Age.JustNow -> stringResource(Res.string.updated_just_now)
        is Age.Minutes -> stringResource(Res.string.updated_minutes, value.value)
        is Age.Hours -> stringResource(Res.string.updated_hours, value.value)
        is Age.Days -> stringResource(Res.string.updated_days, value.value)
    }
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.fillMaxWidth()) {
        Text(
            "${stringResource(reason)} $age",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = Spacing.medium, vertical = Spacing.small),
        )
    }
}

/** Sem perfil salvo e sem conseguir buscar: só o cartão do erro. Rolável para o puxar-para-atualizar. */
@Composable
internal fun OfflineCard(error: AppError, onRetry: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.medium)) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                Modifier.fillMaxWidth().padding(Spacing.medium),
                verticalArrangement = Arrangement.spacedBy(Spacing.small),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (error == AppError.NETWORK) {
                    Text(stringResource(Res.string.offline_title), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(Res.string.offline_body), style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text(stringResource(error.message), style = MaterialTheme.typography.bodyMedium)
                }
                if (error.canRetry) OutlinedButton(onClick = onRetry) { Text(stringResource(Res.string.retry)) }
            }
        }
    }
}

/** Formato do perfil enquanto a primeira busca não volta. Os números são só o tamanho dos blocos. */
@Suppress("MagicNumber")
@Composable
internal fun ProfileSkeleton() {
    val color = MaterialTheme.colorScheme.surfaceVariant
    val shape = RoundedCornerShape(8.dp)
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        listOf(0.6f to 48.dp, 1f to 64.dp, 0.3f to 16.dp, 1f to 120.dp, 0.3f to 16.dp, 1f to 120.dp)
            .forEach { (width, height) ->
                Box(Modifier.fillMaxWidth(width).height(height).background(color, shape))
            }
    }
}
