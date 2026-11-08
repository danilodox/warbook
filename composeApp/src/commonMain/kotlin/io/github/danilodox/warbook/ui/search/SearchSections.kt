package io.github.danilodox.warbook.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.danilodox.warbook.data.FollowKind
import io.github.danilodox.warbook.data.RecentSearch
import io.github.danilodox.warbook.resources.Res
import io.github.danilodox.warbook.resources.follow
import io.github.danilodox.warbook.resources.follow_limit
import io.github.danilodox.warbook.resources.following
import io.github.danilodox.warbook.resources.ic_star
import io.github.danilodox.warbook.resources.ic_star_filled
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
import io.github.danilodox.warbook.ui.components.ErrorState
import io.github.danilodox.warbook.ui.components.TownHallShield
import io.github.danilodox.warbook.ui.theme.Spacing
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import io.github.danilodox.warbook.ui.components.SectionHeader as DsSectionHeader

private val ImageSize = 36.dp

/** O resultado da última busca: aviso, carregando, erro ou a lista de clãs com a estrela de seguir. */
internal fun LazyListScope.outcome(state: SearchUiState, viewModel: SearchViewModel, onRetry: () -> Unit) {
    when (val outcome = state.outcome) {
        SearchOutcome.None -> Unit
        SearchOutcome.NickNotice -> item { NoticeCard(Res.string.search_nick_notice) }
        SearchOutcome.TooShort -> item { NoticeCard(Res.string.search_too_short) }
        SearchOutcome.Searching -> item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        is SearchOutcome.Failed -> item {
            val error = outcome.error
            ErrorState(
                title = stringResource(error.message),
                body = null,
                code = error.name.takeIf { error.canRetry },
                onRetry = onRetry.takeIf { error.canRetry },
            )
        }
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
                        image = EntryImage(clan.badgeUrl, level = null),
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
            Text(
                stringResource(Res.string.search_following_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    items(state.followed, key = { "followed-${it.kind}-${it.tag}" }) { item ->
        EntryRow(
            kind = item.kind,
            name = item.name,
            line = levelLine(item.kind, item.level, item.tag),
            image = EntryImage(item.imageUrl, item.level),
            onClick = { viewModel.open(SearchTarget(item.kind, item.tag)) },
        ) {
            if (state.editing) {
                TextButton(onClick = { viewModel.unfollow(item) }) { Text(stringResource(Res.string.search_remove)) }
            } else {
                Icon(
                    painterResource(Res.drawable.ic_star_filled),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = Spacing.sm).size(18.dp),
                )
            }
        }
    }
}

/** "Recentes": fichas com o que a busca abriu, e "Limpar". */
internal fun LazyListScope.recentSection(recent: List<RecentSearch>, viewModel: SearchViewModel) {
    if (recent.isEmpty()) return
    item(key = "recent-header") {
        SectionHeader(Res.string.search_recent_title) {
            TextButton(onClick = viewModel::clearRecent) { Text(stringResource(Res.string.search_clear)) }
        }
    }
    item(key = "recent-chips") {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            recent.forEach { item ->
                AssistChip(
                    onClick = { viewModel.open(SearchTarget(item.kind, item.tag)) },
                    label = { Text(item.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    leadingIcon = {
                        if (item.kind == FollowKind.CLAN) {
                            ClanBadge(item.imageUrl, contentDescription = null, size = 18.dp)
                        } else {
                            Icon(
                                painterResource(Res.drawable.ic_tab_me),
                                contentDescription = null,
                                Modifier.size(16.dp),
                            )
                        }
                    },
                )
            }
        }
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
    DsSectionHeader(stringResource(title), trailing = action)
}

/** Brasão do clã ou, do jogador, o escudo do CV (sem o CV salvo, o ícone de pessoa). */
private data class EntryImage(val url: String?, val level: Int?)

@Composable
private fun EntryRow(
    kind: FollowKind,
    name: String,
    line: String,
    image: EntryImage,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.xs + Spacing.xxs),
    ) {
        Box(Modifier.size(ImageSize), contentAlignment = Alignment.Center) {
            when {
                kind == FollowKind.CLAN -> ClanBadge(image.url, contentDescription = null, size = ImageSize)
                image.level != null -> TownHallShield(image.level)
                else -> Icon(painterResource(Res.drawable.ic_tab_me), contentDescription = null)
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(line, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing()
    }
}

/** ☆ / ★: seguir ou deixar de seguir sem abrir o clã. */
@Composable
private fun FollowStar(following: Boolean, onToggle: () -> Unit) {
    IconButton(onClick = onToggle) {
        Icon(
            painterResource(if (following) Res.drawable.ic_star_filled else Res.drawable.ic_star),
            contentDescription = stringResource(if (following) Res.string.following else Res.string.follow),
            tint = if (following) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NoticeCard(text: StringResource) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            stringResource(text),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(Spacing.lg),
        )
    }
}
