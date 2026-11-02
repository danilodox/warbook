package io.github.danilodox.warbook

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.navigation.compose.rememberNavController
import io.github.danilodox.warbook.ui.favorite_player.FavoritePlayersViewModel
import org.koin.androidx.compose.koinViewModel
import io.github.danilodox.warbook.ui.core.navigation.AppNavigation
import io.github.danilodox.warbook.ui.player_login.PlayerLoginViewModel
import io.github.danilodox.warbook.ui.home.HomeViewModel

class MainActivity : ComponentActivity() {

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
    }

}
