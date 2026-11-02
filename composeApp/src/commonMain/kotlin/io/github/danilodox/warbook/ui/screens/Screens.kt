package io.github.danilodox.warbook.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.danilodox.warbook.di.AppInfo
import io.github.danilodox.warbook.resources.Res
import io.github.danilodox.warbook.resources.coming_soon
import io.github.danilodox.warbook.resources.settings_version
import io.github.danilodox.warbook.ui.theme.Spacing
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/** Tela vazia das abas; o conteúdo real começa na v0.2.0. */
@Composable
fun PlaceholderScreen(title: StringResource, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(Spacing.medium),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(title), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(Res.string.coming_soon), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun SettingsScreen(modifier: Modifier = Modifier, appInfo: AppInfo = koinInject()) {
    Column(
        modifier = modifier.fillMaxSize().padding(Spacing.medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.small),
    ) {
        Text(stringResource(Res.string.coming_soon), style = MaterialTheme.typography.bodyLarge)
        Text(
            stringResource(Res.string.settings_version, appInfo.versionName),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
