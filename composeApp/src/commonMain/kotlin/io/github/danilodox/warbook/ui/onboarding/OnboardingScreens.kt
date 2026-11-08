package io.github.danilodox.warbook.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.danilodox.warbook.api.PlayerDto
import io.github.danilodox.warbook.resources.Res
import io.github.danilodox.warbook.resources.app_name
import io.github.danilodox.warbook.resources.back
import io.github.danilodox.warbook.resources.fan_notice_short
import io.github.danilodox.warbook.resources.ic_arrow_back
import io.github.danilodox.warbook.resources.ic_help
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
import io.github.danilodox.warbook.resources.player_trophies
import io.github.danilodox.warbook.resources.search_tag_label
import io.github.danilodox.warbook.resources.warbook_logo
import io.github.danilodox.warbook.ui.components.StatRow
import io.github.danilodox.warbook.ui.components.StatTile
import io.github.danilodox.warbook.ui.components.grouped
import io.github.danilodox.warbook.ui.me.ProfileHeader
import io.github.danilodox.warbook.ui.search.message
import io.github.danilodox.warbook.ui.theme.Spacing
import io.github.danilodox.warbook.ui.theme.WarbookTheme
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** O1: logo, campo da tag, ajuda e a saída "Só explorar" (só na primeira abertura). Aviso de fã no rodapé. */
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
    var showHelp by rememberSaveable { mutableStateOf(false) }
    val submit = { if (tag.isNotBlank()) onContinue(tag) }

    FlowColumn(modifier) {
        Brand(Modifier.padding(top = Spacing.xl, bottom = Spacing.sm))
        Text(stringResource(Res.string.onboarding_tag_question), style = MaterialTheme.typography.titleMedium)
        TagField(
            tag = tag,
            error = state.error?.message,
            onChange = {
                tag = it
                onTagChanged()
            },
            onDone = submit,
        )
        TextButton(onClick = { showHelp = !showHelp }) {
            Icon(painterResource(Res.drawable.ic_help), contentDescription = null, Modifier.size(18.dp))
            Text(stringResource(Res.string.onboarding_help_title), Modifier.padding(start = Spacing.sm))
        }
        if (showHelp) TagHelpCard()
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
        FanNotice()
    }
}

/** Logo, "WARBOOK" e a frase do app. */
@Composable
private fun Brand(modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Image(
            painterResource(Res.drawable.warbook_logo),
            contentDescription = null,
            modifier = Modifier.size(84.dp).clip(MaterialTheme.shapes.extraLarge),
        )
        Text(
            stringResource(Res.string.app_name).uppercase(),
            style = WarbookTheme.text.wordmark,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            stringResource(Res.string.onboarding_tagline),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** Campo da tag em monoespaçada, com o "#" fixo. */
@Composable
private fun TagField(tag: String, error: StringResource?, onChange: (String) -> Unit, onDone: () -> Unit) {
    val mono = WarbookTheme.text.tag.copy(fontSize = 16.sp, lineHeight = 24.sp)
    OutlinedTextField(
        value = tag,
        // O "#" já aparece fixo no campo; se vier colado junto, sai.
        onValueChange = { onChange(it.trimStart('#', ' ')) },
        label = { Text(stringResource(Res.string.search_tag_label)) },
        prefix = { Text("#", style = mono, color = MaterialTheme.colorScheme.primary) },
        textStyle = mono,
        singleLine = true,
        isError = error != null,
        supportingText = { Text(stringResource(error ?: Res.string.onboarding_tag_hint)) },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters,
            autoCorrectEnabled = false,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun TagHelpCard() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            stringResource(Res.string.onboarding_help_body),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(Spacing.lg),
        )
    }
}

/** "Material não oficial…": rodapé do onboarding (o texto completo fica nas Configurações). */
@Composable
internal fun FanNotice(modifier: Modifier = Modifier) {
    Text(
        stringResource(Res.string.fan_notice_short),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth(),
    )
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
            Text(stringResource(Res.string.onboarding_confirm_title), style = MaterialTheme.typography.headlineSmall)
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    ProfileHeader(player)
                    StatRow {
                        StatTile(stringResource(Res.string.player_level), player.expLevel.toString())
                        StatTile(stringResource(Res.string.player_trophies), player.trophies.grouped())
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

/**
 * Coluna rolável das telas de fluxo, que sobe com o teclado. Tem pelo menos a altura da tela,
 * para um Spacer(Modifier.weight(1f)) empurrar os botões e o rodapé para baixo.
 */
@Composable
internal fun FlowColumn(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    BoxWithConstraints(modifier.fillMaxSize().imePadding()) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).heightIn(min = maxHeight).padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
            content = content,
        )
    }
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
