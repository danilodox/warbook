package io.github.danilodox.warbook.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.danilodox.warbook.data.Account
import io.github.danilodox.warbook.data.AccountsRepository
import io.github.danilodox.warbook.di.AppInfo
import io.github.danilodox.warbook.resources.Res
import io.github.danilodox.warbook.resources.me_add_account
import io.github.danilodox.warbook.resources.settings_about
import io.github.danilodox.warbook.resources.settings_accounts
import io.github.danilodox.warbook.resources.settings_active_verified
import io.github.danilodox.warbook.resources.settings_fan_notice
import io.github.danilodox.warbook.resources.settings_manage_accounts
import io.github.danilodox.warbook.resources.settings_verify_active
import io.github.danilodox.warbook.resources.settings_version
import io.github.danilodox.warbook.ui.theme.Spacing
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/** E3 mínima: contas, versão e o aviso de conteúdo de fã. Idioma, tema e widget ficam para depois. */
@Composable
fun SettingsRoute(
    onManageAccounts: () -> Unit,
    onVerify: (String) -> Unit,
    onAddAccount: () -> Unit,
    modifier: Modifier = Modifier,
    appInfo: AppInfo = koinInject(),
) {
    val repository: AccountsRepository = koinInject()
    val accounts by repository.accounts.collectAsStateWithLifecycle(initialValue = emptyList())
    SettingsScreen(
        active = accounts.firstOrNull { it.isActive } ?: accounts.firstOrNull(),
        versionName = appInfo.versionName,
        onManageAccounts = onManageAccounts,
        onVerify = onVerify,
        onAddAccount = onAddAccount,
        modifier = modifier,
    )
}

@Composable
fun SettingsScreen(
    active: Account?,
    versionName: String,
    onManageAccounts: () -> Unit,
    onVerify: (String) -> Unit,
    onAddAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Section(Res.string.settings_accounts)
        when {
            active == null -> SettingsRow(Res.string.me_add_account, onClick = onAddAccount)
            else -> {
                SettingsRow(Res.string.settings_manage_accounts, onClick = onManageAccounts)
                if (active.isVerified) {
                    SettingsRow(Res.string.settings_active_verified, onClick = null)
                } else {
                    SettingsRow(Res.string.settings_verify_active, onClick = { onVerify(active.tag) })
                }
            }
        }
        Section(Res.string.settings_about)
        ListItem(headlineContent = { Text(stringResource(Res.string.settings_version, versionName)) })
        Spacer(Modifier.weight(1f))
        Text(
            stringResource(Res.string.settings_fan_notice),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(Spacing.lg),
        )
    }
}

@Composable
private fun Section(title: StringResource) {
    Text(
        stringResource(title),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = Spacing.lg, end = Spacing.lg, top = Spacing.lg),
    )
}

@Composable
private fun SettingsRow(text: StringResource, onClick: (() -> Unit)?) {
    ListItem(
        headlineContent = { Text(stringResource(text)) },
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
    )
}
