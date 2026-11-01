package io.github.danilodox.warbook

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import io.github.danilodox.warbook.ui.favorite_player.FavoritePlayersViewModel
import org.koin.androidx.compose.koinViewModel
import io.github.danilodox.warbook.ui.core.navigation.AppNavigation
import io.github.danilodox.warbook.ui.player_login.PlayerLoginViewModel
import io.github.danilodox.warbook.ui.home.HomeViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val client = HttpClient(CIO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val navController = rememberNavController()
            val favoritePlayersViewModel = koinViewModel<FavoritePlayersViewModel>()


            MaterialTheme {
                AppNavigation(navController = navController,
                    favoritePlayersViewModel = favoritePlayersViewModel
                )
            }
        }
        lifecycleScope.launch {
            val responseText = getHelloFromServer()
            Log.d("WarbookBackend", responseText)
        }
    }

    private suspend fun getHelloFromServer(): String {
        return try {
            // ⚠️ IMPORTANTE: O IP MÁGICO
            // Para o emulador Android, 'localhost' é o próprio celular.
            // Para falar com o 'localhost' do seu PC, use o IP especial 10.0.2.2
            val response: HttpResponse = client.get("http://10.0.2.2:8080/hello")

            response.bodyAsText()
        } catch (e: Exception) {
            Log.e("WarbookBackend", "Erro ao conectar: ${e.message}")
            "Erro: ${e.message}"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        client.close() // Feche o cliente quando a activity for destruída
    }

}