package io.github.danilodox.warbook.ui.me

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.danilodox.warbook.api.PlayerDto
import io.github.danilodox.warbook.resources.Res
import io.github.danilodox.warbook.resources.card_fan_app
import io.github.danilodox.warbook.resources.card_kind
import io.github.danilodox.warbook.resources.fan_notice_short
import io.github.danilodox.warbook.resources.player_clan_role
import io.github.danilodox.warbook.resources.player_level
import io.github.danilodox.warbook.resources.player_no_clan
import io.github.danilodox.warbook.resources.player_tag
import io.github.danilodox.warbook.resources.player_trophies
import io.github.danilodox.warbook.resources.player_war_stars
import io.github.danilodox.warbook.resources.warbook_logo
import io.github.danilodox.warbook.ui.common.ClanBadge
import io.github.danilodox.warbook.ui.components.ShieldSize
import io.github.danilodox.warbook.ui.components.TownHallShield
import io.github.danilodox.warbook.ui.components.grouped
import io.github.danilodox.warbook.ui.search.label
import io.github.danilodox.warbook.ui.theme.WarbookTheme
import io.github.danilodox.warbook.ui.theme.tabular
import io.github.danilodox.warbook.ui.units.UnitIcon
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Tamanho da imagem do cartão: 4:5, que o WhatsApp mostra inteira. */
const val CARD_WIDTH_PX = 1080
const val CARD_HEIGHT_PX = 1350

private const val CARD_HEROES = 4
private const val FRAME_ALPHA = 0.55f
private const val GRADIENT_MID = 0.55f
private const val LINE_HEIGHT = 1.3f

/** Cartão de perfil (design: cantoneiras douradas, filete, nome, CV, números, 4 heróis e aviso de fã). */
@Composable
internal fun ProfileCard(player: PlayerDto, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val background = Brush.radialGradient(
        0f to colors.surfaceContainerHigh,
        GRADIENT_MID to colors.surfaceContainerLow,
        1f to colors.surfaceContainerLowest,
        center = Offset(CARD_WIDTH_PX / 2f, 0f),
        radius = CARD_HEIGHT_PX.toFloat(),
    )
    Box(modifier.background(background)) {
        Box(
            Modifier.fillMaxSize().padding(9.dp)
                .border(1.dp, colors.primary.copy(alpha = FRAME_ALPHA), RoundedCornerShape(3.dp)),
        )
        Corner(Alignment.TopStart)
        Corner(Alignment.TopEnd)
        Corner(Alignment.BottomStart)
        Corner(Alignment.BottomEnd)
        Column(
            Modifier.fillMaxSize().padding(start = 24.dp, end = 24.dp, top = 22.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CardTop()
            CardIdentity(player)
            CardStats(player)
            CardHeroes(player)
            Spacer(Modifier.weight(1f))
            CardFooter()
        }
    }
}

/** Texto do cartão na fonte do corpo do app, sem a altura de linha do tema. */
@Composable
private fun cardText(size: TextUnit, weight: FontWeight = FontWeight.Normal, spacing: TextUnit = 0.sp): TextStyle =
    MaterialTheme.typography.bodyMedium.copy(
        fontSize = size,
        fontWeight = weight,
        letterSpacing = spacing,
        lineHeight = size * LINE_HEIGHT,
    )

/** Cantoneira: triângulo dourado com o ângulo reto no canto do cartão. */
@Composable
private fun BoxScope.Corner(alignment: Alignment) {
    val shape = GenericShape { size, _ ->
        val left = alignment == Alignment.TopStart || alignment == Alignment.BottomStart
        val top = alignment == Alignment.TopStart || alignment == Alignment.TopEnd
        val x = if (left) 0f else size.width
        val y = if (top) 0f else size.height
        moveTo(x, y)
        lineTo(size.width - x, y)
        lineTo(x, size.height - y)
        close()
    }
    Box(Modifier.align(alignment).size(26.dp).background(MaterialTheme.colorScheme.primary, shape))
}

@Composable
private fun CardTop() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Image(
            painterResource(Res.drawable.warbook_logo),
            contentDescription = null,
            modifier = Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)),
        )
        Text(
            "WARBOOK",
            style = WarbookTheme.text.wordmark.copy(fontSize = 15.sp),
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.weight(1f))
        Text(
            stringResource(Res.string.card_kind).uppercase(),
            style = cardText(10.sp, FontWeight.ExtraBold, 0.12.em),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CardIdentity(player: PlayerDto) {
    val display = WarbookTheme.text.wordmark.fontFamily
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        TownHallShield(player.townHallLevel, size = ShieldSize.ExtraLarge)
        Column {
            Text(
                player.name,
                style = TextStyle(
                    fontFamily = display,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 32.sp,
                    lineHeight = 34.sp,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                stringResource(Res.string.player_tag, player.tag),
                style = WarbookTheme.text.tag.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            CardClan(player)
        }
    }
}

@Composable
private fun CardClan(player: PlayerDto) {
    val clan = player.clan
    val style = cardText(13.sp, FontWeight.Bold)
    if (clan == null) {
        Text(stringResource(Res.string.player_no_clan), style = style, modifier = Modifier.padding(top = 4.dp))
        return
    }
    val text = player.role
        ?.let { stringResource(Res.string.player_clan_role, clan.name, stringResource(it.label)) }
        ?: clan.name
    Row(
        Modifier.padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ClanBadge(clan.badgeUrl, contentDescription = null, size = 20.dp)
        Text(text, style = style, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun CardStats(player: PlayerDto) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        CardStat(player.trophies.grouped(), stringResource(Res.string.player_trophies), Modifier.weight(1f))
        CardStat(player.warStars.grouped(), stringResource(Res.string.player_war_stars), Modifier.weight(1f))
        CardStat(player.expLevel.toString(), stringResource(Res.string.player_level), Modifier.weight(1f))
    }
}

@Composable
private fun CardStat(value: String, label: String, modifier: Modifier) {
    Column(
        modifier
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(10.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            value,
            style = TextStyle(
                fontFamily = WarbookTheme.text.wordmark.fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
            )
                .tabular(),
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            label.uppercase(),
            style = cardText(9.5.sp, FontWeight.Bold, 0.08.em),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun CardHeroes(player: PlayerDto) {
    val language = Locale.current.language
    val heroes = player.heroes.take(CARD_HEROES).map { it.toItem(language) }
    if (heroes.isEmpty()) return
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        heroes.forEach { hero ->
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Box(Modifier.size(50.dp), contentAlignment = Alignment.Center) { UnitIcon(hero) }
                Text(
                    hero.level.toString(),
                    style = cardText(13.sp, FontWeight.ExtraBold).tabular(),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp),
                )
            }
        }
        repeat(CARD_HEROES - heroes.size) { Spacer(Modifier.weight(1f)) }
    }
}

@Composable
private fun CardFooter() {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            stringResource(Res.string.card_fan_app).uppercase(),
            style = cardText(9.5.sp, FontWeight.ExtraBold, 0.1.em),
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            stringResource(Res.string.fan_notice_short),
            style = cardText(9.5.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
