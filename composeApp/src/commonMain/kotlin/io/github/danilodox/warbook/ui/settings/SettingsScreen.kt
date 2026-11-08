package io.github.danilodox.warbook.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.danilodox.warbook.data.Account
import io.github.danilodox.warbook.data.AccountsRepository
import io.github.danilodox.warbook.di.AppInfo
import io.github.danilodox.warbook.resources.Res
import io.github.danilodox.warbook.resources.ic_check
import io.github.danilodox.warbook.resources.ic_chevron_right
import io.github.danilodox.warbook.resources.me_add_account
import io.github.danilodox.warbook.resources.settings_about
import io.github.danilodox.warbook.resources.settings_accounts
import io.github.danilodox.warbook.resources.settings_active_verified
import io.github.danilodox.warbook.resources.settings_fan_notice
import io.github.danilodox.warbook.resources.settings_manage_accounts
import io.github.danilodox.warbook.resources.settings_verify_active
import io.github.danilodox.warbook.resources.settings_version
import io.github.danilodox.warbook.ui.theme.Spacing
import io.github.danilodox.warbook.ui.theme.WarbookTheme
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/** E3: contas, sobre e o aviso de conteúdo de fã. Idioma e widget ficam para depois. */
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
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Section(Res.string.settings_accounts) {
            when {
                active == null -> SettingsRow(Res.string.me_add_account, onClick = onAddAccount)
                else -> {
                    SettingsRow(Res.string.settings_manage_accounts, onClick = onManageAccounts)
                    if (active.isVerified) {
                        SettingsRow(Res.string.settings_active_verified, onClick = null, verified = true)
                    } else {
                        SettingsRow(Res.string.settings_verify_active, onClick = { onVerify(active.tag) })
                    }
                }
            }
        }
        Section(Res.string.settings_about) {
            SettingsRow(text = stringResource(Res.string.settings_version, versionName), onClick = null)
        }
        Text(
            stringResource(Res.string.settings_fan_notice),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.lg),
        )
    }
}

/** Título em dourado e as linhas num bloco de cantos arredondados. */
@Composable
private fun Section(title: StringResource, content: @Composable () -> Unit) {
    Column(Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm)) {
        Text(
            stringResource(title).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = Spacing.xs, bottom = Spacing.sm),
        )
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column { content() }
        }
    }
}

@Composable
private fun SettingsRow(text: StringResource, onClick: (() -> Unit)?, verified: Boolean = false) {
    SettingsRow(stringResource(text), onClick, verified)
}

@Composable
private fun SettingsRow(text: String, onClick: (() -> Unit)?, verified: Boolean = false) {
    ListItem(
        headlineContent = { Text(text, style = MaterialTheme.typography.bodyLarge) },
        leadingContent = if (verified) {
            {
                Icon(
                    painterResource(Res.drawable.ic_check),
                    contentDescription = null,
                    tint = WarbookTheme.colors.success,
                    modifier = Modifier.size(20.dp),
                )
            }
        } else {
            null
        },
        trailingContent = if (onClick != null) {
            {
                Icon(
                    painterResource(Res.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        } else {
            null
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
    )
}
