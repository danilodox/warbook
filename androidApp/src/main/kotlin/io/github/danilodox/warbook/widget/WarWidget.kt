package io.github.danilodox.warbook.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import io.github.danilodox.warbook.MainActivity
import io.github.danilodox.warbook.data.currentTimeMillis
import io.github.danilodox.warbook.war.WarWidgetContent
import io.github.danilodox.warbook.war.WarWidgetData
import io.github.danilodox.warbook.war.WarWidgetLoader
import io.github.danilodox.warbook.war.summaryTextsFor
import io.github.danilodox.warbook.war.warWidgetTextsFor
import org.koin.core.context.GlobalContext
import java.util.Locale

/**
 * Widget da guerra atual do clã da conta ativa: placar, tempo e quantos ataques faltam. Atualiza a cada 30 min
 * (o mínimo do Android, em `war_widget_info.xml`); tocar abre o app.
 */
class WarWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val koin = GlobalContext.get()
        val loader = WarWidgetLoader(koin.get(), koin.get(), koin.get())
        val language = Locale.getDefault().language
        val data = WarWidgetContent.of(
            widget = loader.load(),
            now = currentTimeMillis(),
            texts = warWidgetTextsFor(language),
            summary = summaryTextsFor(language),
        )
        provideContent { WarWidgetLayout(data) }
    }
}

class WarWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WarWidget()
}

// Cores do "livro de guerra" (Tokens.kt do composeApp, que é internal): claro e escuro.
private val Background = ColorProvider(day = Color(0xFFF0E8DC), night = Color(0xFF251D17))
private val Ink = ColorProvider(day = Color(0xFF1F1B16), night = Color(0xFFEEE3D4))
private val Muted = ColorProvider(day = Color(0xFF4F4539), night = Color(0xFFD0C1AC))
private val Gold = ColorProvider(day = Color(0xFF7A5900), night = Color(0xFFE8BC5E))
private val War = ColorProvider(day = Color(0xFF9B2423), night = Color(0xFFFFB3AA))

@Composable
private fun WarWidgetLayout(data: WarWidgetData) {
    Column(
        modifier = GlanceModifier.fillMaxSize()
            .background(Background)
            .cornerRadius(16.dp)
            .padding(12.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            data.title,
            style = TextStyle(color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold),
            maxLines = 1,
        )
        data.score?.let {
            Spacer(GlanceModifier.height(4.dp))
            Text(it, style = TextStyle(color = Gold, fontSize = 16.sp, fontWeight = FontWeight.Bold), maxLines = 1)
        }
        Spacer(GlanceModifier.height(4.dp))
        Text(data.status, style = TextStyle(color = Muted, fontSize = 12.sp), maxLines = 2)
        data.pending?.let {
            Text(it, style = TextStyle(color = War, fontSize = 12.sp, fontWeight = FontWeight.Medium), maxLines = 1)
        }
    }
}
