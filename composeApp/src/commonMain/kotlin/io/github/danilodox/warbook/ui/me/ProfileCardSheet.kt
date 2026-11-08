package io.github.danilodox.warbook.ui.me

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.danilodox.warbook.api.PlayerDto
import io.github.danilodox.warbook.resources.Res
import io.github.danilodox.warbook.resources.card_title
import io.github.danilodox.warbook.resources.ic_share
import io.github.danilodox.warbook.resources.war_share
import io.github.danilodox.warbook.ui.common.TopBarShareAction
import io.github.danilodox.warbook.ui.share.rememberImageSharer
import io.github.danilodox.warbook.ui.theme.Spacing
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private val PreviewMaxWidth = 360.dp

/** O cartão é desenhado em 360 × 450 "dp" com 3 px por dp: as medidas são as do mockup (prévia em 1/3). */
private const val CARD_DENSITY = 3f

/**
 * Mostra o cartão do tamanho que couber na largura e grava o desenho em [layer],
 * sempre em [CARD_WIDTH_PX] × [CARD_HEIGHT_PX], para virar imagem com `layer.toImageBitmap()`.
 */
@Composable
fun ProfileCardPreview(player: PlayerDto, layer: GraphicsLayer, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(CARD_WIDTH_PX.toFloat() / CARD_HEIGHT_PX)
            .clip(MaterialTheme.shapes.extraSmall)
            .layout { measurable, constraints ->
                val placeable = measurable.measure(Constraints.fixed(CARD_WIDTH_PX, CARD_HEIGHT_PX))
                val width = constraints.maxWidth
                val scale = width.toFloat() / CARD_WIDTH_PX
                layout(width, (CARD_HEIGHT_PX * scale).toInt()) {
                    placeable.placeWithLayer(0, 0) {
                        scaleX = scale
                        scaleY = scale
                        transformOrigin = TransformOrigin(0f, 0f)
                    }
                }
            },
    ) {
        CompositionLocalProvider(LocalDensity provides Density(CARD_DENSITY, fontScale = 1f)) {
            ProfileCard(
                player,
                Modifier.fillMaxSize().drawWithContent {
                    layer.record { this@drawWithContent.drawContent() }
                    drawLayer(layer)
                },
            )
        }
    }
}

/** Prévia do cartão de perfil numa folha, com o botão que gera a imagem e abre o compartilhar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileCardSheet(player: PlayerDto, share: suspend (ImageBitmap) -> Unit, onDismiss: () -> Unit) {
    val layer = rememberGraphicsLayer()
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = Spacing.lg, end = Spacing.lg, bottom = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text(stringResource(Res.string.card_title), style = MaterialTheme.typography.titleLarge)
            ProfileCardPreview(
                player,
                layer,
                Modifier.widthIn(max = PreviewMaxWidth).align(Alignment.CenterHorizontally),
            )
            Button(
                onClick = {
                    busy = true
                    scope.launch {
                        try {
                            share(layer.toImageBitmap())
                        } finally {
                            busy = false
                        }
                    }
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(painterResource(Res.drawable.ic_share), contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.width(Spacing.sm))
                Text(stringResource(Res.string.war_share))
            }
        }
    }
}

/** Ícone Compartilhar no topo para o perfil aberto, que abre a folha do cartão. */
@Composable
fun ProfileCardAction(player: PlayerDto?) {
    val share = rememberImageSharer()
    var open by rememberSaveable { mutableStateOf(false) }
    TopBarShareAction(if (share != null && player != null) ({ open = true }) else null)
    if (open && player != null && share != null) ProfileCardSheet(player, share) { open = false }
}
