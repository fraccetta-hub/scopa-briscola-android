package com.fraccetta.scopabriscola.ui.screens.online

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.fraccetta.scopabriscola.online.OnlineLobbyState
import com.fraccetta.scopabriscola.online.OnlineLobbyViewModel
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
                    title = { Text("Online — ${gameType.replaceFirstChar { it.uppercase() }}", color = CardCream) },
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
            Text("Room code", color = CardCream, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = state.code,
                color = AccentGold,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 8.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text("Share this code with your friend", color = CardCream.copy(alpha = 0.7f), fontSize = 14.sp)
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
    var codeInput by remember { mutableStateOf("") }

    when (state) {
        OnlineLobbyState.Idle, OnlineLobbyState.RoomNotFound -> {
            Text("Enter room code", color = CardCream, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = codeInput,
                onValueChange = { codeInput = it.uppercase().take(6) },
                label = { Text("6-character code", color = CardCream.copy(alpha = 0.7f)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = CardCream,
                    unfocusedTextColor = CardCream,
                    focusedBorderColor = AccentGold,
                    unfocusedBorderColor = CardCream.copy(alpha = 0.5f)
                )
            )
            if (state == OnlineLobbyState.RoomNotFound) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Room not found. Check the code.", color = Color.Red, fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { viewModel.joinGame(codeInput, gameType) },
                enabled = codeInput.length == 6,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentGold)
            ) {
                Text("Join", color = CardCream, fontWeight = FontWeight.Bold)
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
            Button(onClick = { viewModel.reset() }, colors = ButtonDefaults.buttonColors(containerColor = AccentGold)) {
                Text("Try again", color = CardCream)
            }
        }
        else -> {
            CircularProgressIndicator(color = AccentGold)
        }
    }
}
