package com.fraccetta.scopabriscola.ui.screens.online

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.fraccetta.scopabriscola.online.OnlineLobbyState
import com.fraccetta.scopabriscola.online.OnlineLobbyViewModel
import com.fraccetta.scopabriscola.online.RoomInfo
import com.fraccetta.scopabriscola.ui.game.AssetBackground
import com.fraccetta.scopabriscola.ui.theme.AccentGold
import com.fraccetta.scopabriscola.ui.theme.CardCream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlineLobbyScreen(
    gameType: String,
    role: String,
    navController: NavHostController,
    viewModel: OnlineLobbyViewModel = hiltViewModel()
) {
    val lobbyState by viewModel.state.collectAsState()
    val isHost = role == "HOST"

    LaunchedEffect(lobbyState) {
        if (lobbyState is OnlineLobbyState.Connected) {
            val gameRoute = when (gameType.uppercase()) {
                "SCOPA" -> "game/scopa"
                "BRISCOLA" -> "game/briscola"
                "SCOPONE_TEAM", "SCOPONE_MIX" -> "game/scopone"
                else -> "game/${gameType.lowercase()}"
            }
            navController.navigate(gameRoute) {
                popUpTo("online-lobby/$gameType/$role") { inclusive = true }
            }
        }
    }

    LaunchedEffect(isHost) {
        if (isHost) viewModel.hostGame(gameType)
        else viewModel.startBrowsing(gameType)
    }

    val gameLabel = when (gameType.uppercase()) {
        "SCOPA" -> "Scopa"
        "BRISCOLA" -> "Briscola"
        "SCOPONE_TEAM", "SCOPONE_MIX" -> "Scopone Scientifico"
        else -> gameType.replaceFirstChar { it.uppercase() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF0D1F33), Color(0xFF1A3A5C), Color(0xFF0D2D4A))))
        )
        AssetBackground(assetPath = "backgrounds/menu_bg.webp", overlayAlpha = 0.45f, modifier = Modifier.fillMaxSize())

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("$gameLabel — Online", color = CardCream) },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.reset(); navController.popBackStack() }) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = CardCream)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (isHost) {
                    HostContent(lobbyState)
                } else {
                    JoinContent(lobbyState, gameType, viewModel)
                }
            }
        }
    }
}

@Composable
private fun HostContent(state: OnlineLobbyState) {
    when (state) {
        is OnlineLobbyState.Hosting -> {
            Text("Your room", color = CardCream, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = state.playerName,
                color = AccentGold,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Tell your friend to open «Find game» and tap your name",
                color = CardCream.copy(alpha = 0.65f),
                fontSize = 13.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            CircularProgressIndicator(color = AccentGold, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text("Waiting for opponent…", color = CardCream.copy(alpha = 0.6f), fontSize = 14.sp)
        }
        OnlineLobbyState.Error -> {
            Text("Connection error", color = Color.Red)
        }
        else -> {
            CircularProgressIndicator(color = AccentGold)
        }
    }
}

@Composable
private fun JoinContent(
    state: OnlineLobbyState,
    gameType: String,
    viewModel: OnlineLobbyViewModel
) {
    when (state) {
        is OnlineLobbyState.BrowsingRooms -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Open rooms",
                    color = CardCream,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${state.rooms.size} found",
                    color = CardCream.copy(alpha = 0.5f),
                    fontSize = 13.sp
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (state.rooms.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x22FFFFFF), RoundedCornerShape(12.dp))
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No open rooms yet", color = CardCream, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Ask your friend to tap «Host online» first",
                            color = CardCream.copy(alpha = 0.5f),
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.rooms) { room ->
                        RoomRow(room = room, onClick = { viewModel.joinRoom(room, gameType) })
                    }
                }
            }
        }
        OnlineLobbyState.Connecting -> {
            CircularProgressIndicator(color = AccentGold)
            Spacer(modifier = Modifier.height(12.dp))
            Text("Connecting…", color = CardCream)
        }
        OnlineLobbyState.Error -> {
            Text("Connection error", color = Color.Red)
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { viewModel.startBrowsing(gameType) },
                colors = ButtonDefaults.buttonColors(containerColor = AccentGold)
            ) {
                Text("Try again", color = Color(0xFF0D1F33))
            }
        }
        else -> {
            CircularProgressIndicator(color = AccentGold)
        }
    }
}

@Composable
private fun RoomRow(room: RoomInfo, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0x22FFFFFF), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0x44FFFFFF), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = room.hostName,
                color = AccentGold,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = when (room.gameType.uppercase()) {
                    "SCOPA" -> "Scopa"
                    "BRISCOLA" -> "Briscola"
                    else -> "Scopone Scientifico"
                },
                color = CardCream.copy(alpha = 0.6f),
                fontSize = 12.sp
            )
        }
        Text("Join →", color = AccentGold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}
