package io.github.danilodox.warbook.ui.me

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.danilodox.warbook.data.Account
import io.github.danilodox.warbook.data.AccountsRepository
import io.github.danilodox.warbook.resources.Res
import io.github.danilodox.warbook.resources.me_add_account
import io.github.danilodox.warbook.resources.me_no_account_body
import io.github.danilodox.warbook.resources.me_no_account_title
import io.github.danilodox.warbook.resources.tab_me
import io.github.danilodox.warbook.ui.screens.PlaceholderScreen
import io.github.danilodox.warbook.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/** Aba Eu. Sem conta (modo "Só explorar"), mostra o convite para adicionar uma. */
@Composable
fun MeRoute(onAddAccount: () -> Unit, modifier: Modifier = Modifier) {
    val repository: AccountsRepository = koinInject()
    val accounts: List<Account>? by repository.accounts.collectAsStateWithLifecycle(initialValue = null)
    when {
        accounts == null -> Unit
        accounts.isNullOrEmpty() -> NoAccountScreen(onAddAccount, modifier)
        // O perfil (E1) entra no Lote 4.
        else -> PlaceholderScreen(Res.string.tab_me, modifier)
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
