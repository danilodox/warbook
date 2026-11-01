package io.github.danilodox.warbook.ui.core.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ClashProgressBar(progress: Float, modifier: Modifier = Modifier) {
    val clampedProgress = progress.coerceIn(0f, 1f) // Garante que o progresso fique entre 0 e 1

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        // Texto de porcentagem acima da barra
        Text(
            text = "${(clampedProgress * 100).toInt()}%",
            color = Color.Yellow,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        // Barra de Progresso dentro de um Card
        Card(
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(2.dp, Color(0xFFD4AF37)), // Dourado estilo Clash
            elevation = CardDefaults.cardElevation(6.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF3E2723)) // Marrom escuro
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .background(Color.DarkGray), // Fundo da barra
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(clampedProgress) // Tamanho proporcional ao progresso
                        .height(20.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                listOf(Color(0xFFFFD700), Color(0xFFFFA500)) // Gradiente dourado
                            )
                        )
                        .clip(RoundedCornerShape(6.dp)) // Borda arredondada
                )
            }
        }
    }
}