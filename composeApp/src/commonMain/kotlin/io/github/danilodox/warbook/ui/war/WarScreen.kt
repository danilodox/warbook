package io.github.danilodox.warbook.ui.war

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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.danilodox.warbook.data.AccountsRepository
import io.github.danilodox.warbook.data.AppError
import io.github.danilodox.warbook.data.AppPreferences
import io.github.danilodox.warbook.data.Cached
import io.github.danilodox.warbook.data.ProfileRepository
import io.github.danilodox.warbook.data.WarRepository
import io.github.danilodox.warbook.data.currentTimeMillis
import io.github.danilodox.warbook.resources.Res
import io.github.danilodox.warbook.resources.me_add_account
import io.github.danilodox.warbook.resources.war_invite_body
import io.github.danilodox.warbook.resources.war_invite_title
import io.github.danilodox.warbook.resources.war_no_clan_body
import io.github.danilodox.warbook.resources.war_no_clan_title
import io.github.danilodox.warbook.resources.war_private_body
import io.github.danilodox.warbook.resources.war_private_refresh
import io.github.danilodox.warbook.resources.war_private_step_1
import io.github.danilodox.warbook.resources.war_private_step_2
import io.github.danilodox.warbook.resources.war_private_step_3
import io.github.danilodox.warbook.resources.war_private_step_4
import io.github.danilodox.warbook.resources.war_private_steps_title
import io.github.danilodox.warbook.resources.war_private_title
import io.github.danilodox.warbook.resources.war_private_who
import io.github.danilodox.warbook.resources.war_tab_current
import io.github.danilodox.warbook.resources.war_tab_cwl
import io.github.danilodox.warbook.ui.me.OfflineCard
import io.github.danilodox.warbook.ui.me.StaleBanner
import io.github.danilodox.warbook.ui.theme.Spacing
import io.github.danilodox.warbook.war.SummaryOptions
import io.github.danilodox.warbook.war.SummaryPart
import io.github.danilodox.warbook.war.SummaryTexts
import io.github.danilodox.warbook.war.summaryTextsFor
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/** Aba Guerra. [onAddAccount] abre o onboarding (convite de quem não tem conta). */
@Composable
fun WarRoute(onAddAccount: () -> Unit, modifier: Modifier = Modifier) {
    val accounts: AccountsRepository = koinInject()
    val profiles: ProfileRepository = koinInject()
    val wars: WarRepository = koinInject()
    val preferences: AppPreferences = koinInject()
    val viewModel = viewModel { WarViewModel(accounts, profiles, wars, preferences) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Roda a cada vez que a aba entra na tela (voltar de outra aba também conta como abrir).
    LaunchedEffect(viewModel) { viewModel.onOpen() }

    WarScreen(
        state = state,
        actions = WarActions(
            onSelectTab = viewModel::selectTab,
            onSelectRound = viewModel::selectRound,
            onRefresh = viewModel::refresh,
            onAddAccount = onAddAccount,
            onToggleSummaryPart = viewModel::toggleSummaryPart,
        ),
        modifier = modifier,
    )
}

data class WarActions(
    val onSelectTab: (WarTab) -> Unit,
    val onSelectRound: (Int) -> Unit,
    val onRefresh: () -> Unit,
    val onAddAccount: () -> Unit,
    val onToggleSummaryPart: (SummaryPart) -> Unit = {},
)

/**
 * O que as partes da tela precisam além dos dados: quem é "você", a hora, os textos do idioma e as partes
 * do resumo escolhidas (com o que liga e desliga cada uma).
 */
data class WarContext(
    val accountTag: String?,
    val now: Long,
    val texts: SummaryTexts,
    val summaryOptions: SummaryOptions = SummaryOptions(),
    val onToggleSummaryPart: (SummaryPart) -> Unit = {},
)

private const val CLOCK_TICK_MS = 30_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarScreen(state: WarUiState, actions: WarActions, modifier: Modifier = Modifier) {
    when {
        !state.loaded -> Unit
        state.accountTag == null && !state.fixedClan -> Message(
            title = Res.string.war_invite_title,
            body = Res.string.war_invite_body,
            modifier = modifier,
        ) { Button(onClick = actions.onAddAccount) { Text(stringResource(Res.string.me_add_account)) } }
        else -> Column(modifier.fillMaxSize()) {
            TabSelector(state.tab, actions.onSelectTab)
            val section = if (state.tab == WarTab.CURRENT) state.war else state.cwl
            PullToRefreshBox(
                isRefreshing = section.refreshing && section.cached != null,
                onRefresh = actions.onRefresh,
                modifier = Modifier.fillMaxSize(),
            ) { WarBody(state, actions) }
        }
    }
}

@Composable
private fun WarBody(state: WarUiState, actions: WarActions) {
    // A contagem regressiva anda sem buscar nada no servidor.
    val now by produceState(currentTimeMillis()) {
        while (true) {
            delay(CLOCK_TICK_MS)
            value = currentTimeMillis()
        }
    }
    val context = WarContext(
        accountTag = state.accountTag,
        now = now,
        texts = summaryTextsFor(Locale.current.language),
        summaryOptions = state.summaryOptions,
        onToggleSummaryPart = actions.onToggleSummaryPart,
    )
    val profileError = state.profile.error
    when {
        !state.profileKnown && profileError != null -> OfflineCard(profileError, onRetry = actions.onRefresh)
        !state.profileKnown -> WarSkeleton()
        state.clan == null -> Message(Res.string.war_no_clan_title, Res.string.war_no_clan_body)
        state.tab == WarTab.CURRENT -> SectionBody(state.war, actions.onRefresh) { cached, modifier ->
            CurrentWarContent(cached.value, context, modifier)
        }
        else -> SectionBody(state.cwl, actions.onRefresh) { cached, modifier ->
            CwlContent(cached.value, state.round, actions.onSelectRound, context, modifier)
        }
    }
}

@Composable
private fun TabSelector(selected: WarTab, onSelect: (WarTab) -> Unit) {
    val tabs = listOf(WarTab.CURRENT to Res.string.war_tab_current, WarTab.CWL to Res.string.war_tab_cwl)
    SingleChoiceSegmentedButtonRow(
        Modifier.fillMaxWidth().padding(horizontal = Spacing.medium, vertical = Spacing.small),
    ) {
        tabs.forEachIndexed { index, (tab, label) ->
            SegmentedButton(
                selected = tab == selected,
                onClick = { onSelect(tab) },
                shape = SegmentedButtonDefaults.itemShape(index, tabs.size),
            ) { Text(stringResource(label)) }
        }
    }
}

/**
 * Uma sub-aba: o salvo (com a faixa se a atualização falhou), o cartão de erro sem nada salvo ou o skeleton.
 * Registro privado sempre mostra o passo a passo, mesmo com algo salvo.
 */
@Composable
private fun <T> SectionBody(
    section: Section<T>,
    onRetry: () -> Unit,
    content: @Composable (Cached<T>, Modifier) -> Unit,
) {
    val cached = section.cached
    val error = section.error
    when {
        error == AppError.WAR_LOG_PRIVATE -> PrivateWarLog(onRetry)
        cached != null -> Column(Modifier.fillMaxSize()) {
            if (error != null) StaleBanner(error, cached.fetchedAt)
            content(cached, Modifier.weight(1f))
        }
        error != null -> OfflineCard(error, onRetry)
        else -> WarSkeleton()
    }
}

/** Mensagem no meio da tela, rolável para o puxar-para-atualizar funcionar. */
@Composable
internal fun Message(
    title: StringResource,
    body: StringResource,
    modifier: Modifier = Modifier,
    action: @Composable () -> Unit = {},
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.large),
        verticalArrangement = Arrangement.spacedBy(Spacing.small, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(title), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(stringResource(body), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        action()
    }
}

/** S3: passo a passo para o líder deixar o registro de guerra público. */
@Composable
private fun PrivateWarLog(onRefresh: () -> Unit) {
    val steps = listOf(
        Res.string.war_private_step_1,
        Res.string.war_private_step_2,
        Res.string.war_private_step_3,
        Res.string.war_private_step_4,
    )
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        Text(stringResource(Res.string.war_private_title), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(Res.string.war_private_body), style = MaterialTheme.typography.bodyMedium)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(Spacing.medium), verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
                Text(stringResource(Res.string.war_private_steps_title), style = MaterialTheme.typography.titleSmall)
                Text(stringResource(Res.string.war_private_who), style = MaterialTheme.typography.bodySmall)
                steps.forEachIndexed { index, step ->
                    Text("${index + 1}. ${stringResource(step)}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        OutlinedButton(onClick = onRefresh) { Text(stringResource(Res.string.war_private_refresh)) }
    }
}

/** Formato da guerra enquanto a primeira busca não volta. Os números são só o tamanho dos blocos. */
@Suppress("MagicNumber")
@Composable
private fun WarSkeleton() {
    val color = MaterialTheme.colorScheme.surfaceVariant
    val shape = RoundedCornerShape(8.dp)
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        listOf(1f to 140.dp, 1f to 72.dp, 0.3f to 16.dp, 1f to 48.dp, 1f to 48.dp, 1f to 48.dp, 1f to 48.dp)
            .forEach { (width, height) ->
                Box(Modifier.fillMaxWidth(width).height(height).background(color, shape))
            }
    }
}
