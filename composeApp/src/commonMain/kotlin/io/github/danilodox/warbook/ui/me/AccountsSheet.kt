package io.github.danilodox.warbook.ui.me

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.danilodox.warbook.data.Account
import io.github.danilodox.warbook.resources.Res
import io.github.danilodox.warbook.resources.accounts_active
import io.github.danilodox.warbook.resources.accounts_hold_hint
import io.github.danilodox.warbook.resources.accounts_remove
import io.github.danilodox.warbook.resources.accounts_remove_body
import io.github.danilodox.warbook.resources.accounts_remove_title
import io.github.danilodox.warbook.resources.accounts_summary
import io.github.danilodox.warbook.resources.accounts_title
import io.github.danilodox.warbook.resources.cancel
import io.github.danilodox.warbook.resources.me_add_account
import io.github.danilodox.warbook.resources.me_verified
import io.github.danilodox.warbook.resources.player_no_clan
import io.github.danilodox.warbook.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource

/** E2: contas do aparelho. Toque troca; segurar pede para remover. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsSheet(
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onSwitch: (String) -> Unit,
    onRemove: (String) -> Unit,
    onAdd: () -> Unit,
) {
    var removing by remember { mutableStateOf<Account?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = Spacing.medium).padding(bottom = Spacing.large),
            verticalArrangement = Arrangement.spacedBy(Spacing.small),
        ) {
            Text(stringResource(Res.string.accounts_title), style = MaterialTheme.typography.titleMedium)
            accounts.forEach { account ->
                AccountRow(account, onClick = { onSwitch(account.tag) }, onLongClick = { removing = account })
            }
            OutlinedButton(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.me_add_account))
            }
            Text(stringResource(Res.string.accounts_hold_hint), style = MaterialTheme.typography.bodySmall)
        }
    }

    removing?.let { account ->
        AlertDialog(
            onDismissRequest = { removing = null },
            title = { Text(stringResource(Res.string.accounts_remove_title, account.name)) },
            text = { Text(stringResource(Res.string.accounts_remove_body)) },
            confirmButton = {
                TextButton(onClick = {
                    onRemove(account.tag)
                    removing = null
                }) { Text(stringResource(Res.string.accounts_remove)) }
            },
            dismissButton = {
                TextButton(onClick = { removing = null }) { Text(stringResource(Res.string.cancel)) }
            },
        )
    }
}

@Composable
private fun AccountRow(account: Account, onClick: () -> Unit, onLongClick: () -> Unit) {
    val clan = account.clanName ?: stringResource(Res.string.player_no_clan)
    ListItem(
        headlineContent = { Text(account.name) },
        supportingContent = { Text(stringResource(Res.string.accounts_summary, account.townHallLevel, clan)) },
        trailingContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small / 2)) {
                if (account.isVerified) Badge("✓ ${stringResource(Res.string.me_verified)}")
                if (account.isActive) Badge(stringResource(Res.string.accounts_active))
            }
        },
        modifier = Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick,
            onLongClickLabel = stringResource(Res.string.accounts_remove),
        ),
    )
}

@Composable
private fun Badge(text: String) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.small) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = Spacing.small / 2),
        )
    }
}
