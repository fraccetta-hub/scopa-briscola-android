package com.fraccetta.scopabriscola.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import com.fraccetta.scopabriscola.data.GameSession
import com.fraccetta.scopabriscola.ui.game.AssetBackground
import com.fraccetta.scopabriscola.ui.screens.home.HomeViewModel
import com.fraccetta.scopabriscola.ui.theme.AccentGold
import com.fraccetta.scopabriscola.ui.theme.CardCream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoponeGameModeScreen(
    navController: NavHostController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val playerName by viewModel.playerName.collectAsState(initial = "")

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0D1F33), Color(0xFF1A3A5C), Color(0xFF0D2D4A)))))
        AssetBackground(assetPath = "backgrounds/menu_bg.webp", overlayAlpha = 0.45f, modifier = Modifier.fillMaxSize())
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            TopAppBar(
                title = { Text("Scopone Scientifico", color = CardCream, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Indietro", tint = CardCream)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Scopone Scientifico",
                    style = MaterialTheme.typography.headlineLarge,
                    color = CardCream,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "4 players • 2 teams",
                    color = CardCream.copy(alpha = 0.6f),
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(48.dp))

                // VS CPU (offline)
                Button(
                    onClick = {
                        GameSession.isSolo = true
                        GameSession.isServer = true
                        GameSession.scoponeLocalPlayer = 0
                        GameSession.scoponeBtMode = 0
                        GameSession.localPlayerName = playerName.ifBlank { null }
                        GameSession.opponentName = "AI"
                        navController.navigate("game/scopone")
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = Color(0xFF0D1F33))
                ) {
                    Text("Vs CPU", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Text("You + Captain vs 2 AI", color = CardCream.copy(alpha = 0.5f), fontSize = 11.sp)

                Spacer(modifier = Modifier.height(24.dp))

                // BT Mode 1: 2 umani vs 2 AI
                ModeSection(title = "2 Humans vs 2 AI (BT)", subtitle = "You + BT partner vs 2 AI") {
                    Button(
                        onClick = {
                            GameSession.isSolo = false
                            GameSession.scoponeBtMode = 1
                            GameSession.localPlayerName = playerName.ifBlank { null }
                            navController.navigate("lobby/SCOPONE_TEAM/HOST")
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A5276), contentColor = Color.White)
                    ) { Text("Host game") }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            GameSession.isSolo = false
                            GameSession.scoponeBtMode = 1
                            GameSession.localPlayerName = playerName.ifBlank { null }
                            navController.navigate("lobby/SCOPONE_TEAM/SCAN")
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A5276), contentColor = Color.White)
                    ) { Text("Find game") }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Online Mode
                ModeSection(title = "Online (Internet)", subtitle = "2 umani vs 2 AI su internet") {
                    Button(
                        onClick = {
                            GameSession.isSolo = false
                            GameSession.scoponeBtMode = 1
                            GameSession.localPlayerName = playerName.ifBlank { null }
                            navController.navigate("online-lobby/SCOPONE_TEAM/HOST")
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B6B3A), contentColor = Color.White)
                    ) { Text("Host online") }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            GameSession.isSolo = false
                            GameSession.scoponeBtMode = 1
                            GameSession.localPlayerName = playerName.ifBlank { null }
                            navController.navigate("online-lobby/SCOPONE_TEAM/JOIN")
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B6B3A), contentColor = Color.White)
                    ) { Text("Join online") }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // BT Mode 2: umano+AI vs umano+AI
                ModeSection(title = "Human+AI vs Human+AI (BT)", subtitle = "You + AI vs BT partner + AI") {
                    Button(
                        onClick = {
                            GameSession.isSolo = false
                            GameSession.scoponeBtMode = 2
                            GameSession.localPlayerName = playerName.ifBlank { null }
                            navController.navigate("lobby/SCOPONE_MIX/HOST")
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A5276), contentColor = Color.White)
                    ) { Text("Host game") }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            GameSession.isSolo = false
                            GameSession.scoponeBtMode = 2
                            GameSession.localPlayerName = playerName.ifBlank { null }
                            navController.navigate("lobby/SCOPONE_MIX/SCAN")
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A5276), contentColor = Color.White)
                    ) { Text("Find game") }
                }
            }
        }
    }
}

@Composable
private fun ModeSection(title: String, subtitle: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0x22FFFFFF), androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text(title, color = CardCream, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text(subtitle, color = CardCream.copy(alpha = 0.5f), fontSize = 11.sp)
        Spacer(modifier = Modifier.height(10.dp))
        content()
    }
}
