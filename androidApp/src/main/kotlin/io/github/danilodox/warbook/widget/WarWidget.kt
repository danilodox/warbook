package io.github.danilodox.warbook.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.PreviewSizeMode
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import io.github.danilodox.warbook.MainActivity
import io.github.danilodox.warbook.data.AppPreferences
import io.github.danilodox.warbook.data.ThemeMode
import io.github.danilodox.warbook.data.currentTimeMillis
import io.github.danilodox.warbook.war.WarWidgetContent
import io.github.danilodox.warbook.war.WarWidgetData
import io.github.danilodox.warbook.war.WarWidgetLoader
import io.github.danilodox.warbook.war.summaryTextsFor
import io.github.danilodox.warbook.war.warWidgetTextsFor
import kotlinx.coroutines.flow.first
import org.koin.core.context.GlobalContext
import java.util.Locale

/** 3×1 no launcher (uma linha de células): título, placar e "tempo · pendentes" em linhas curtas. */
private val Compact = DpSize(160.dp, 40.dp)

/** Duas linhas de células ou mais: o layout completo, com o status em até duas linhas. */
private val Tall = DpSize(160.dp, 110.dp)

/**
 * Widget da guerra atual do clã da conta ativa: placar, tempo e quantos ataques faltam. Atualiza a cada 30 min
 * (o mínimo do Android, em `war_widget_info.xml`); tocar abre o app. As cores seguem o tema do app.
 */
class WarWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(Compact, Tall))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val koin = GlobalContext.get()
        val preferences = koin.get<AppPreferences>()
        val initialTheme = preferences.themeMode.first()
        val loader = WarWidgetLoader(koin.get(), koin.get(), koin.get())
        val language = Locale.getDefault().language
        val data = WarWidgetContent.of(
            widget = loader.load(),
            now = currentTimeMillis(),
            texts = warWidgetTextsFor(language),
            summary = summaryTextsFor(language),
        )
        provideContent {
            // Trocar o tema nas Configurações redesenha o widget sem esperar os 30 min.
            val theme by preferences.themeMode.collectAsState(initialTheme)
            val colors = remember(theme) { WidgetColors(theme) }
            WarWidgetLayout(data, colors)
        }
    }

    // Prévia gerada na lista de widgets (Android 15+, ver WidgetPreviews): guerra de exemplo, no tema do aparelho,
    // porque a lista segue o sistema e a prévia só é refeita quando muda a versão ou o idioma.
    override val previewSizeMode: PreviewSizeMode = SizeMode.Responsive(setOf(Compact, Tall))

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        val language = Locale.getDefault().language
        val data = WarWidgetContent.preview(warWidgetTextsFor(language), summaryTextsFor(language))
        provideContent { WarWidgetLayout(data, WidgetColors(ThemeMode.SYSTEM)) }
    }
}

class WarWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WarWidget()
}

@Composable
internal fun WarWidgetLayout(data: WarWidgetData, colors: WidgetColors) {
    val compact = LocalSize.current.height < Tall.height
    Column(
        modifier = GlanceModifier.fillMaxSize()
            .background(colors.background)
            .cornerRadius(16.dp)
            .padding(horizontal = 12.dp, vertical = if (compact) 6.dp else 12.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (compact) CompactContent(data, colors) else TallContent(data, colors)
    }
}

@Composable
private fun CompactContent(data: WarWidgetData, colors: WidgetColors) {
    Text(
        data.title,
        style = TextStyle(color = colors.ink, fontSize = 13.sp, fontWeight = FontWeight.Bold),
        maxLines = 1,
    )
    val score = data.score
    if (score == null) {
        Text(data.status, style = TextStyle(color = colors.muted, fontSize = 12.sp), maxLines = 2)
        return
    }
    Text(score, style = TextStyle(color = colors.gold, fontSize = 13.sp, fontWeight = FontWeight.Bold), maxLines = 1)
    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        // O tempo fica inteiro; os pendentes vêm logo depois e são cortados se faltar espaço.
        Text(data.status, style = TextStyle(color = colors.muted, fontSize = 11.sp), maxLines = 1)
        data.pending?.let {
            Text(
                " · $it",
                modifier = GlanceModifier.defaultWeight(),
                style = TextStyle(color = colors.war, fontSize = 11.sp, fontWeight = FontWeight.Medium),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun TallContent(data: WarWidgetData, colors: WidgetColors) {
    Text(
        data.title,
        style = TextStyle(color = colors.ink, fontSize = 14.sp, fontWeight = FontWeight.Bold),
        maxLines = 1,
    )
    data.score?.let {
        Spacer(GlanceModifier.height(4.dp))
        Text(it, style = TextStyle(color = colors.gold, fontSize = 16.sp, fontWeight = FontWeight.Bold), maxLines = 1)
    }
    Spacer(GlanceModifier.height(4.dp))
    Text(data.status, style = TextStyle(color = colors.muted, fontSize = 12.sp), maxLines = 2)
    data.pending?.let {
        Text(
            it,
            style = TextStyle(color = colors.war, fontSize = 12.sp, fontWeight = FontWeight.Medium),
            maxLines = 1,
        )
    }
}
