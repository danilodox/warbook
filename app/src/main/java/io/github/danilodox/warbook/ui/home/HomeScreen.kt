package io.github.danilodox.warbook.ui.home

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import io.github.danilodox.warbook.R

//#2PLVVQYRP

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                is NavigationEvent.ToPlayerDetails -> {
                    navController.navigate("player_details/${Uri.encode(event.playerTag)}")
                }

                else -> {}
            }
        }
    }

    when {
        uiState.isLoading -> {
            Box(modifier = Modifier.fillMaxSize().background(colorResource(id = R.color.DarkGrayBackground)), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        uiState.error != null -> {
            Box(modifier = Modifier.fillMaxSize().background(colorResource(id = R.color.DarkGrayBackground)), contentAlignment = Alignment.Center) {
                Text("Error: ${uiState.error}", color = Color.Red)
            }
        }
        uiState.player != null -> {
            HomeScreenContent(
                state = uiState,
                navController = navController,
                onEvent = {event ->
                    when (event) {
                        is HomeUiEvent.OnBackClicked -> {
                            navController.popBackStack()
                        }
                        else -> viewModel.onEvent(event)
                    }
                }
            )
        }
    }
}


@Composable
fun HomeScreenContent(
    state: HomeUiState,
    navController: NavController,
    onEvent: (HomeUiEvent) -> Unit
) {
    val player = state.player!!

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.DarkGrayBackground))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            HomeTopBar(onBackClicked = { onEvent(HomeUiEvent.OnBackClicked) })

            PlayerProfileCard(
                playerName = player.name,
                playerTag = player.tag,
                playerLevel = player.expLevel,
                playerAvatarId = R.drawable.clash_logo,
                clanName = player.clan?.name ?: "No Clan",
                onClick = {
                    onEvent(HomeUiEvent.OnPlayerProfileClicked(player.tag))
                }
            )
            NavigationButton(
                iconId = R.drawable.clash_logo,
                title = "MY CLAN",
                subtitle = "Current war stats",
                onClick = {
                    player.clan?.let {
                        navController.navigate("clan_info/${Uri.encode(it.tag)}")
                    }
                }
            )

            NavigationButton(
                iconId = R.drawable.clash_logo,
                title = "WAR INFO",
                subtitle = "View war log & stats",
                onClick = { onEvent(HomeUiEvent.OnWarInfoClicked) }
            )
            NavigationButton(
                iconId = R.drawable.clash_logo,
                title = "SEARCH PLAYER",
                subtitle = "Find player profile",
                onClick = { onEvent(HomeUiEvent.OnSearchPlayerClicked) }
            )
        }
    }
    }


@Composable
private fun HomeTopBar(onBackClicked: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = onBackClicked,
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back to Login",
                tint = Color.White
            )
        }

        Text(
            text = "Home Screen",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}


@Composable
fun PlayerProfileCard(
    playerName: String,
    playerTag: String,
    playerLevel: Int,
    playerAvatarId: Int,
    clanName: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.DarkSectionBackground))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Image(
                    painter = painterResource(id = playerAvatarId),
                    contentDescription = "Player Avatar",
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .border(3.dp, colorResource(id = R.color.LightGold), CircleShape)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Player info
            Column {
                Text(text = "LEVEL $playerLevel", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(text = playerName, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Text(text = "TAG: $playerTag", fontSize = 14.sp, color = colorResource(id = R.color.GrayText))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "CLAN: $clanName", fontSize = 14.sp, color = colorResource(id = R.color.GrayText))
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
        }
    }
}

@Composable
fun NavigationButton(
    iconId: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .border(1.dp, Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.DarkSectionBackground))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = iconId),
                contentDescription = title,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(text = subtitle, fontSize = 14.sp, color = colorResource(id = R.color.GrayText))
            }
        }
    }
}


