package io.github.danilodox.warbook.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.danilodox.warbook.api.PlayerDto
import io.github.danilodox.warbook.resources.Res
import io.github.danilodox.warbook.resources.app_name
import io.github.danilodox.warbook.resources.back
import io.github.danilodox.warbook.resources.ic_arrow_back
import io.github.danilodox.warbook.resources.onboarding_confirm_no
import io.github.danilodox.warbook.resources.onboarding_confirm_title
import io.github.danilodox.warbook.resources.onboarding_confirm_yes
import io.github.danilodox.warbook.resources.onboarding_continue
import io.github.danilodox.warbook.resources.onboarding_explore
import io.github.danilodox.warbook.resources.onboarding_help_body
import io.github.danilodox.warbook.resources.onboarding_help_title
import io.github.danilodox.warbook.resources.onboarding_tag_hint
import io.github.danilodox.warbook.resources.onboarding_tag_question
import io.github.danilodox.warbook.resources.onboarding_tagline
import io.github.danilodox.warbook.resources.player_level
import io.github.danilodox.warbook.resources.player_no_clan
import io.github.danilodox.warbook.resources.player_tag
import io.github.danilodox.warbook.resources.player_town_hall
import io.github.danilodox.warbook.resources.player_trophies
import io.github.danilodox.warbook.resources.search_tag_label
import io.github.danilodox.warbook.ui.search.label
import io.github.danilodox.warbook.ui.search.message
import io.github.danilodox.warbook.ui.theme.Spacing
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** O1: tag do jogador, com ajuda e a saída "Só explorar" (só na primeira abertura). */
@Composable
fun TagScreen(
    state: OnboardingState,
    canExplore: Boolean,
    onContinue: (String) -> Unit,
    onTagChanged: () -> Unit,
    onExplore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var tag by rememberSaveable { mutableStateOf("") }
    val submit = { if (tag.isNotBlank()) onContinue(tag) }

    FlowColumn(modifier) {
        Spacer(Modifier.size(Spacing.large))
        Text(
            stringResource(Res.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            stringResource(Res.string.onboarding_tagline),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.size(Spacing.medium))
        Text(stringResource(Res.string.onboarding_tag_question), style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = tag,
            // O "#" já aparece fixo no campo; se vier colado junto, sai.
            onValueChange = {
                tag = it.trimStart('#', ' ')
                onTagChanged()
            },
            label = { Text(stringResource(Res.string.search_tag_label)) },
            prefix = { Text("#") },
            singleLine = true,
            isError = state.error != null,
            supportingText = {
                Text(stringResource(state.error?.message ?: Res.string.onboarding_tag_hint))
            },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                autoCorrectEnabled = false,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            modifier = Modifier.fillMaxWidth(),
        )
        TagHelpCard()
        Spacer(Modifier.weight(1f))
        ProgressButton(
            text = Res.string.onboarding_continue,
            loading = state.lookingUp,
            enabled = tag.isNotBlank(),
            onClick = submit,
        )
        if (canExplore) {
            TextButton(onClick = onExplore, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.onboarding_explore))
            }
        }
    }
}

@Composable
private fun TagHelpCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(Spacing.medium), verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
            Text(stringResource(Res.string.onboarding_help_title), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(Res.string.onboarding_help_body), style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** O2: o mínimo para reconhecer a conta antes de adicionar. */
@Composable
fun ConfirmScreen(
    player: PlayerDto,
    saving: Boolean,
    onConfirm: () -> Unit,
    onChangeTag: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        FlowTopBar(title = null, onBack = onChangeTag)
        FlowColumn {
            Text(
                stringResource(Res.string.onboarding_confirm_title),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    Modifier.fillMaxWidth().padding(Spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(Spacing.small),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(player.name, style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(Res.string.player_tag, player.tag), style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.large)) {
                        Stat(Res.string.player_town_hall, player.townHallLevel)
                        Stat(Res.string.player_level, player.expLevel)
                        Stat(Res.string.player_trophies, player.trophies)
                    }
                    val clan = player.clan
                    Text(
                        clan?.name ?: stringResource(Res.string.player_no_clan),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    player.role?.takeIf { clan != null }?.let {
                        Text(stringResource(it.label), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            ProgressButton(
                text = Res.string.onboarding_confirm_yes,
                loading = saving,
                enabled = true,
                onClick = onConfirm,
            )
            OutlinedButton(onClick = onChangeTag, enabled = !saving, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.onboarding_confirm_no))
            }
        }
    }
}

/** Topo das telas de fluxo (sem a barra do app). Os insets já vêm do Scaffold de fora. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FlowTopBar(title: StringResource?, onBack: () -> Unit, actions: @Composable () -> Unit = {}) {
    TopAppBar(
        title = { title?.let { Text(stringResource(it)) } },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(painterResource(Res.drawable.ic_arrow_back), stringResource(Res.string.back))
            }
        },
        actions = { actions() },
        windowInsets = WindowInsets(0.dp),
    )
}

/** Coluna rolável das telas de fluxo, que sobe com o teclado. */
@Composable
internal fun FlowColumn(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(Spacing.medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        content = content,
    )
}

@Composable
internal fun ProgressButton(text: StringResource, loading: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled && !loading, modifier = Modifier.fillMaxWidth()) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        } else {
            Text(stringResource(text))
        }
    }
}

@Composable
private fun Stat(label: StringResource, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(label), style = MaterialTheme.typography.labelSmall)
        Text(value.toString(), style = MaterialTheme.typography.titleMedium)
    }
}
