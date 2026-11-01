package io.github.danilodox.warbook.ui.clan_info

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavController
import io.github.danilodox.warbook.R
import io.github.danilodox.warbook.domain.model.ClanDetails
import io.github.danilodox.warbook.domain.model.ClanMember
import io.github.danilodox.warbook.ui.clan_info.NavigationEvent
import androidx.compose.material3.Scaffold as Scaffold1

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClanInfoScreen(
    navController: NavController,
    clanTag: String,
    viewModel: ClanInfoViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(clanTag) {
        viewModel.loadClanDetails(clanTag)
    }

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                is NavigationEvent.ToMemberDetails -> {
                    navController.navigate("member_details/${Uri.encode(event.playerTag)}")
                }
            }
        }
    }

    Scaffold1(
        topBar = {
            TopAppBar(
                title = { Text("Clan Info", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colorResource(id = R.color.DarkSectionBackground))
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(colorResource(id = R.color.DarkGrayBackground))
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.error != null -> {
                    Text("Error: ${uiState.error}", color = Color.Red, modifier = Modifier.align(
                        Alignment.Center))
                }
                uiState.clanDetails != null -> {
                    ClanInfoContent(
                        clanDetails = uiState.clanDetails!!,
                        onEvent = viewModel::onEvent
                    )
                }
            }
        }
    }
}

@Composable
fun ClanInfoContent(
    clanDetails: ClanDetails,
    onEvent: (ClanInfoUiEvent) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Card de Detalhes do Clã
        item {
            ClanDetailsCard(clan = clanDetails)
        }

        // Header da Lista de Membros
        item {
            Text(
                text = "MEMBERS",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = colorResource(id = R.color.GrayText),
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Lista de Membros
        items(clanDetails.members) { member ->
            ClanMemberItem(
                member = member,
                onClick = { onEvent(ClanInfoUiEvent.OnMemberClicked(member.tag)) }
            )
        }
    }
}

@Composable
fun ClanDetailsCard(clan: ClanDetails) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.DarkSectionBackground))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
//                AsyncImage(
//                    model = clan.badgeUrl,
//                    contentDescription = "Clan Badge",
//                    placeholder = painterResource(id = R.drawable.ic_clan_badge),
//                    modifier = Modifier.size(60.dp)
//                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = clan.name, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(text = "CLAN LEVEL ${clan.level}", fontSize = 14.sp, color = colorResource(id = R.color.GrayText))
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatItem(label = "Members", value = "${clan.membersCount}/50")
                StatItem(label = "War Wins", value = clan.warWins.toString())
                StatItem(label = "War Losses", value = clan.warLosses.toString())
                StatItem(label = "War Ties", value = clan.warTies.toString())
            }
        }
    }
}

@Composable
fun ClanMemberItem(member: ClanMember, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.DarkSectionBackground))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
//            AsyncImage(
//                model = member.leagueIconUrl,
//                contentDescription = "Member League",
//                placeholder = painterResource(id = R.drawable.ic_league_badge_placeholder), // TODO: Crie um placeholder
//                modifier = Modifier
//                    .size(50.dp)
//                    .clip(CircleShape)
//            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = member.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Row {
                    Text(text = "TH${member.townHallLevel}", fontSize = 14.sp, color = colorResource(id = R.color.GrayText))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = "#${member.tag}", fontSize = 14.sp, color = colorResource(id = R.color.GrayText))
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = member.trophies.toString(), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    //Image(painter = painterResource(id = R.drawable.ic_trophy), contentDescription = "Trophies", modifier = Modifier.size(16.dp)) // TODO: Adicione este ícone
                    Image(painter = painterResource(id = R.drawable.clash_logo), contentDescription = "Trophies", modifier = Modifier.size(16.dp)) // TODO: Adicione este ícone
                }
                Text(text = "EXP ${member.expLevel}", fontSize = 14.sp, color = colorResource(id = R.color.GrayText))
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(text = label, fontSize = 12.sp, color = colorResource(id = R.color.GrayText))
    }
}