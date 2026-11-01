package io.github.danilodox.warbook.ui.core.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController

@Composable
fun BottomBarNavigation(
    navController: NavController,
    selectedRoute: String = "favorites"
) {
    NavigationBar(
        containerColor = Color(0xFF1E1E1E)
    ) {
        NavigationBarItem(
            selected = selectedRoute == "favorites",
            onClick = { navController.navigate("favorites") },
            icon = {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Favoritos",
                    tint = if (selectedRoute == "favorites") Color.Red else Color.White
                )
            },
            label = {
                Text("Favoritos", color = Color.White)
            }
        )

        // Depois você pode adicionar mais itens aqui, como:
        // - Home
        // - Buscar
    }
}