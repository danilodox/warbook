package io.github.danilodox.warbook.ui.player_login

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import io.github.danilodox.warbook.R

@Composable
fun WelcomeLoginScreen(
    navController: NavController, viewModel: PlayerLoginViewModel
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val isLoading = uiState.isLoading
    var playerTag by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is PlayerLoginUiEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }
                is PlayerLoginUiEvent.NavigateToHome  -> {
                    navController.navigate("home_screen/${Uri.encode(event.tag)}")
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(20.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.clash_logo),
                contentDescription = "Clash of Clans Logo",
                modifier = Modifier.size(120.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Enter your Clash Tag",
                color = Color.Yellow,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = playerTag,
                onValueChange = { playerTag = it },
                label = { Text("Tag") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Yellow,
                    unfocusedBorderColor = Color.Gray,
                    focusedLabelColor = Color.Yellow
                ),
                textStyle = TextStyle(color = Color.White)
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (isLoading) {
                CircularProgressIndicator(color = Color.Yellow)
            } else {
                Button(
                    onClick = {
                        //val tag = "#2PLVVQYRP"
                        viewModel.loginWithTag(playerTag)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "Confirm", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }
        }
    }
}