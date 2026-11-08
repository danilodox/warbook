package io.github.danilodox.warbook.ui.verify

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.danilodox.warbook.data.AccountsRepository
import io.github.danilodox.warbook.resources.Res
import io.github.danilodox.warbook.resources.verify_button
import io.github.danilodox.warbook.resources.verify_intro
import io.github.danilodox.warbook.resources.verify_not_now
import io.github.danilodox.warbook.resources.verify_skip
import io.github.danilodox.warbook.resources.verify_step_1
import io.github.danilodox.warbook.resources.verify_step_2
import io.github.danilodox.warbook.resources.verify_step_3
import io.github.danilodox.warbook.resources.verify_step_4
import io.github.danilodox.warbook.resources.verify_steps_title
import io.github.danilodox.warbook.resources.verify_title
import io.github.danilodox.warbook.resources.verify_token_label
import io.github.danilodox.warbook.resources.verify_token_note
import io.github.danilodox.warbook.ui.components.NumberedStep
import io.github.danilodox.warbook.ui.onboarding.FlowColumn
import io.github.danilodox.warbook.ui.onboarding.FlowTopBar
import io.github.danilodox.warbook.ui.onboarding.ProgressButton
import io.github.danilodox.warbook.ui.search.message
import io.github.danilodox.warbook.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

private val steps = listOf(
    Res.string.verify_step_1,
    Res.string.verify_step_2,
    Res.string.verify_step_3,
    Res.string.verify_step_4,
)

/**
 * O3. [onDone] roda ao verificar ou ao pular. "Pular" e "Agora não" só aparecem no onboarding;
 * pelas Configurações, a saída é o voltar.
 */
@Composable
fun VerifyRoute(tag: String, fromOnboarding: Boolean, onBack: () -> Unit, onDone: () -> Unit) {
    val accounts: AccountsRepository = koinInject()
    val viewModel = viewModel(key = "verify-$tag") { VerifyViewModel(tag, accounts) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state) { if (state == VerifyState.Verified) onDone() }
    VerifyScreen(
        state = state,
        canSkip = fromOnboarding,
        onVerify = viewModel::verify,
        onTokenChanged = viewModel::clearError,
        onBack = onBack,
        onSkip = onDone,
    )
}

@Composable
fun VerifyScreen(
    state: VerifyState,
    canSkip: Boolean,
    onVerify: (String) -> Unit,
    onTokenChanged: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Só em memória: o token não vai para o estado salvo da tela.
    var token by remember { mutableStateOf("") }
    val busy = state == VerifyState.Verifying || state == VerifyState.Verified
    val submit = { if (token.isNotBlank()) onVerify(token) }

    Column(modifier.fillMaxSize()) {
        FlowTopBar(title = Res.string.verify_title, onBack = onBack) {
            if (canSkip) TextButton(onClick = onSkip, enabled = !busy) { Text(stringResource(Res.string.verify_skip)) }
        }
        FlowColumn {
            Text(stringResource(Res.string.verify_intro), style = MaterialTheme.typography.bodyMedium)
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text(stringResource(Res.string.verify_steps_title), style = MaterialTheme.typography.titleSmall)
                    steps.forEachIndexed { index, step -> NumberedStep(index + 1, stringResource(step)) }
                }
            }
            val error = (state as? VerifyState.Failed)?.error
            OutlinedTextField(
                value = token,
                onValueChange = {
                    token = it
                    onTokenChanged()
                },
                label = { Text(stringResource(Res.string.verify_token_label)) },
                singleLine = true,
                enabled = !busy,
                isError = error != null,
                supportingText = { Text(stringResource(error?.message ?: Res.string.verify_token_note)) },
                // Teclado de senha: sem sugestões e sem o teclado aprender o token.
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.weight(1f))
            ProgressButton(
                text = Res.string.verify_button,
                loading = busy,
                enabled = token.isNotBlank(),
                onClick = submit,
            )
            if (canSkip) {
                TextButton(onClick = onSkip, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(Res.string.verify_not_now))
                }
            }
        }
    }
}
