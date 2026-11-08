package io.github.danilodox.warbook.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.danilodox.warbook.data.FollowKind
import io.github.danilodox.warbook.data.RecentSearch
import io.github.danilodox.warbook.resources.Res
import io.github.danilodox.warbook.resources.follow
import io.github.danilodox.warbook.resources.follow_limit
import io.github.danilodox.warbook.resources.following
import io.github.danilodox.warbook.resources.ic_tab_me
import io.github.danilodox.warbook.resources.search_clan_line
import io.github.danilodox.warbook.resources.search_clear
import io.github.danilodox.warbook.resources.search_done
import io.github.danilodox.warbook.resources.search_edit
import io.github.danilodox.warbook.resources.search_following_empty
import io.github.danilodox.warbook.resources.search_nick_notice
import io.github.danilodox.warbook.resources.search_no_results
import io.github.danilodox.warbook.resources.search_player_line
import io.github.danilodox.warbook.resources.search_recent_title
import io.github.danilodox.warbook.resources.search_remove
import io.github.danilodox.warbook.resources.search_result_line
import io.github.danilodox.warbook.resources.search_results
import io.github.danilodox.warbook.resources.search_too_short
import io.github.danilodox.warbook.ui.common.ClanBadge
import io.github.danilodox.warbook.ui.theme.Spacing
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private val ImageSize = 36.dp

/** O resultado da última busca: aviso, carregando, erro ou a lista de clãs com a estrela de seguir. */
internal fun LazyListScope.outcome(state: SearchUiState, viewModel: SearchViewModel, onRetry: () -> Unit) {
    when (val outcome = state.outcome) {
        SearchOutcome.None -> Unit
        SearchOutcome.NickNotice -> item { NoticeCard(Res.string.search_nick_notice) }
        SearchOutcome.TooShort -> item { NoticeCard(Res.string.search_too_short) }
        SearchOutcome.Searching -> item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        is SearchOutcome.Failed -> item { ErrorCard(outcome.error, onRetry) }
        is SearchOutcome.Results -> {
            if (state.followLimitReached) item { NoticeCard(Res.string.follow_limit) }
            if (outcome.clans.isEmpty()) {
                item { NoticeCard(Res.string.search_no_results) }
            } else {
                item { SectionHeader(Res.string.search_results) }
                items(outcome.clans, key = { it.clan.tag }) { result ->
                    val clan = result.clan
                    EntryRow(
                        kind = FollowKind.CLAN,
                        name = clan.name,
                        line = stringResource(
                            Res.string.search_result_line,
                            clan.tag,
                            clan.clanLevel,
                            clan.memberCount,
                        ),
                        imageUrl = clan.badgeUrl,
                        onClick = { viewModel.open(SearchTarget(FollowKind.CLAN, clan.tag)) },
                    ) { FollowStar(result.following) { viewModel.toggleFollow(result) } }
                }
            }
        }
    }
}

/** "Seguindo" (B1): jogadores e clãs, o mais recente primeiro; "Editar" mostra o remover de cada um. */
internal fun LazyListScope.followedSection(state: SearchUiState, viewModel: SearchViewModel) {
    item(key = "followed-header") {
        SectionHeader(Res.string.following) {
            if (state.followed.isNotEmpty()) {
                TextButton(onClick = viewModel::toggleEditing) {
                    Text(stringResource(if (state.editing) Res.string.search_done else Res.string.search_edit))
                }
            }
        }
    }
    if (state.followed.isEmpty()) {
        item(key = "followed-empty") {
            Text(stringResource(Res.string.search_following_empty), style = MaterialTheme.typography.bodyMedium)
        }
    }
    items(state.followed, key = { "followed-${it.kind}-${it.tag}" }) { item ->
        EntryRow(
            kind = item.kind,
            name = item.name,
            line = levelLine(item.kind, item.level, item.tag),
            imageUrl = item.imageUrl,
            onClick = { viewModel.open(SearchTarget(item.kind, item.tag)) },
        ) {
            if (state.editing) {
                TextButton(onClick = { viewModel.unfollow(item) }) { Text(stringResource(Res.string.search_remove)) }
            }
        }
    }
}

/** "Recentes": o que a busca abriu, com "Limpar". */
internal fun LazyListScope.recentSection(recent: List<RecentSearch>, viewModel: SearchViewModel) {
    if (recent.isEmpty()) return
    item(key = "recent-header") {
        SectionHeader(Res.string.search_recent_title) {
            TextButton(onClick = viewModel::clearRecent) { Text(stringResource(Res.string.search_clear)) }
        }
    }
    items(recent, key = { "recent-${it.kind}-${it.tag}" }) { item ->
        EntryRow(
            kind = item.kind,
            name = item.name,
            line = levelLine(item.kind, item.level, item.tag),
            imageUrl = item.imageUrl,
            onClick = { viewModel.open(SearchTarget(item.kind, item.tag)) },
        )
    }
}

/** "#TAG · CV 16" ou "#TAG · nível 20"; sem o número, só a tag. */
@Composable
private fun levelLine(kind: FollowKind, level: Int?, tag: String): String = when {
    level == null -> "#$tag"
    kind == FollowKind.PLAYER -> stringResource(Res.string.search_player_line, tag, level)
    else -> stringResource(Res.string.search_clan_line, tag, level)
}

@Composable
private fun SectionHeader(title: StringResource, action: @Composable () -> Unit = {}) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
    ) {
        Text(stringResource(title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        action()
    }
}

@Suppress("LongParameterList") // Os campos da linha + o botão da direita.
@Composable
private fun EntryRow(
    kind: FollowKind,
    name: String,
    line: String,
    imageUrl: String?,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = Spacing.xs),
    ) {
        Box(Modifier.size(ImageSize), contentAlignment = Alignment.Center) {
            if (kind == FollowKind.CLAN) {
                ClanBadge(imageUrl, contentDescription = null, size = ImageSize)
            } else {
                Icon(painterResource(Res.drawable.ic_tab_me), contentDescription = null)
            }
        }
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyLarge)
            Text(line, style = MaterialTheme.typography.bodySmall)
        }
        trailing()
    }
}

/** ☆ / ★: seguir ou deixar de seguir sem abrir o clã. */
@Composable
private fun FollowStar(following: Boolean, onToggle: () -> Unit) {
    val description = stringResource(if (following) Res.string.following else Res.string.follow)
    IconButton(onClick = onToggle, modifier = Modifier.semantics { contentDescription = description }) {
        Text(
            if (following) "★" else "☆",
            style = MaterialTheme.typography.titleLarge,
            color = if (following) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NoticeCard(text: StringResource) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            stringResource(text),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(Spacing.lg),
        )
    }
}
