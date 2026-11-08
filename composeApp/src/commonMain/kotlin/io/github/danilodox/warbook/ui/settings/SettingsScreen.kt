package io.github.danilodox.warbook.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.danilodox.warbook.data.Account
import io.github.danilodox.warbook.data.AccountsRepository
import io.github.danilodox.warbook.data.AppPreferences
import io.github.danilodox.warbook.data.ThemeMode
import io.github.danilodox.warbook.di.AppInfo
import io.github.danilodox.warbook.resources.Res
import io.github.danilodox.warbook.resources.fan_policy_link
import io.github.danilodox.warbook.resources.ic_check
import io.github.danilodox.warbook.resources.ic_chevron_right
import io.github.danilodox.warbook.resources.me_add_account
import io.github.danilodox.warbook.resources.settings_about
import io.github.danilodox.warbook.resources.settings_accounts
import io.github.danilodox.warbook.resources.settings_active_verified
import io.github.danilodox.warbook.resources.settings_appearance
import io.github.danilodox.warbook.resources.settings_fan_notice
import io.github.danilodox.warbook.resources.settings_fan_title
import io.github.danilodox.warbook.resources.settings_manage_accounts
import io.github.danilodox.warbook.resources.settings_verify_active
import io.github.danilodox.warbook.resources.settings_version
import io.github.danilodox.warbook.resources.theme_dark
import io.github.danilodox.warbook.resources.theme_light
import io.github.danilodox.warbook.resources.theme_system
import io.github.danilodox.warbook.ui.theme.Spacing
import io.github.danilodox.warbook.ui.theme.WarbookTheme
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/** E3: contas, aparência, sobre e o aviso de conteúdo de fã. Idioma e widget ficam para depois. */
@Composable
fun SettingsRoute(
    onManageAccounts: () -> Unit,
    onVerify: (String) -> Unit,
    onAddAccount: () -> Unit,
    modifier: Modifier = Modifier,
    appInfo: AppInfo = koinInject(),
) {
    val repository: AccountsRepository = koinInject()
    val preferences: AppPreferences = koinInject()
    val accounts by repository.accounts.collectAsStateWithLifecycle(initialValue = emptyList())
    val themeMode by preferences.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.DARK)
    val scope = rememberCoroutineScope()
    SettingsScreen(
        active = accounts.firstOrNull { it.isActive } ?: accounts.firstOrNull(),
        versionName = appInfo.versionName,
        themeMode = themeMode,
        actions = SettingsActions(
            onManageAccounts = onManageAccounts,
            onVerify = onVerify,
            onAddAccount = onAddAccount,
            onThemeChange = { mode -> scope.launch { preferences.setThemeMode(mode) } },
        ),
        modifier = modifier,
    )
}

data class SettingsActions(
    val onManageAccounts: () -> Unit,
    val onVerify: (String) -> Unit,
    val onAddAccount: () -> Unit,
    val onThemeChange: (ThemeMode) -> Unit,
)

private const val FAN_POLICY_URL = "https://www.supercell.com/fan-content-policy"

@Composable
fun SettingsScreen(
    active: Account?,
    versionName: String,
    themeMode: ThemeMode,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Section(Res.string.settings_accounts) {
            when {
                active == null -> SettingsRow(Res.string.me_add_account, onClick = actions.onAddAccount)
                else -> {
                    SettingsRow(Res.string.settings_manage_accounts, onClick = actions.onManageAccounts)
                    if (active.isVerified) {
                        SettingsRow(Res.string.settings_active_verified, onClick = null, verified = true)
                    } else {
                        SettingsRow(Res.string.settings_verify_active, onClick = { actions.onVerify(active.tag) })
                    }
                }
            }
        }
        Section(Res.string.settings_appearance) {
            Column(Modifier.selectableGroup()) {
                listOf(
                    ThemeMode.DARK to Res.string.theme_dark,
                    ThemeMode.LIGHT to Res.string.theme_light,
                    ThemeMode.SYSTEM to Res.string.theme_system,
                ).forEach { (mode, label) ->
                    ThemeRow(label, selected = mode == themeMode) { actions.onThemeChange(mode) }
                }
            }
        }
        Section(Res.string.settings_about) {
            SettingsRow(text = stringResource(Res.string.settings_version, versionName), onClick = null)
        }
        Section(Res.string.settings_fan_title) { FanNoticeBlock() }
    }
}

/** O aviso obrigatório da Política de Conteúdo de Fãs, com o link para ela. */
@Composable
private fun FanNoticeBlock() {
    val uriHandler = LocalUriHandler.current
    Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(stringResource(Res.string.settings_fan_notice), style = MaterialTheme.typography.bodyMedium)
        Text(
            stringResource(Res.string.fan_policy_link),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier.clickable(role = Role.Button) { uriHandler.openUri(FAN_POLICY_URL) },
        )
    }
}

@Composable
private fun ThemeRow(label: StringResource, selected: Boolean, onSelect: () -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(label), style = MaterialTheme.typography.bodyLarge) },
        leadingContent = { RadioButton(selected = selected, onClick = null) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onSelect),
    )
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
