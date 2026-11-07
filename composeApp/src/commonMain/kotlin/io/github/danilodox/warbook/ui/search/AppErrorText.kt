package io.github.danilodox.warbook.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.danilodox.warbook.api.ClanRole
import io.github.danilodox.warbook.data.AppError
import io.github.danilodox.warbook.resources.Res
import io.github.danilodox.warbook.resources.error_api_maintenance
import io.github.danilodox.warbook.resources.error_invalid_query
import io.github.danilodox.warbook.resources.error_invalid_tag
import io.github.danilodox.warbook.resources.error_invalid_verify_token
import io.github.danilodox.warbook.resources.error_network
import io.github.danilodox.warbook.resources.error_not_found
import io.github.danilodox.warbook.resources.error_rate_limited
import io.github.danilodox.warbook.resources.error_server
import io.github.danilodox.warbook.resources.error_war_log_private
import io.github.danilodox.warbook.resources.retry
import io.github.danilodox.warbook.resources.role_co_leader
import io.github.danilodox.warbook.resources.role_elder
import io.github.danilodox.warbook.resources.role_leader
import io.github.danilodox.warbook.resources.role_member
import io.github.danilodox.warbook.ui.theme.Spacing
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Cartão vermelho com o erro e, se adiantar, "Tentar de novo". */
@Composable
internal fun ErrorCard(error: AppError, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Column(Modifier.padding(Spacing.medium), verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
            Text(
                stringResource(error.message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            if (error.canRetry) {
                OutlinedButton(onClick = onRetry) { Text(stringResource(Res.string.retry)) }
            }
        }
    }
}

internal val ClanRole.label: StringResource
    get() = when (this) {
        ClanRole.MEMBER -> Res.string.role_member
        ClanRole.ELDER -> Res.string.role_elder
        ClanRole.CO_LEADER -> Res.string.role_co_leader
        ClanRole.LEADER -> Res.string.role_leader
    }

internal val AppError.message: StringResource
    get() = when (this) {
        AppError.INVALID_TAG -> Res.string.error_invalid_tag
        AppError.NOT_FOUND -> Res.string.error_not_found
        AppError.WAR_LOG_PRIVATE -> Res.string.error_war_log_private
        AppError.API_MAINTENANCE -> Res.string.error_api_maintenance
        AppError.RATE_LIMITED -> Res.string.error_rate_limited
        AppError.INVALID_VERIFY_TOKEN -> Res.string.error_invalid_verify_token
        AppError.INVALID_QUERY -> Res.string.error_invalid_query
        AppError.NETWORK -> Res.string.error_network
        AppError.SERVER -> Res.string.error_server
    }

/** Tag errada ou inexistente não muda tentando de novo; o resto pode passar. */
internal val AppError.canRetry: Boolean
    get() = this != AppError.INVALID_TAG && this != AppError.NOT_FOUND && this != AppError.WAR_LOG_PRIVATE
