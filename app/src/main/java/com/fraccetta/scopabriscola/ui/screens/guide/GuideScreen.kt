package com.fraccetta.scopabriscola.ui.screens.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.fraccetta.scopabriscola.ui.game.AssetBackground
import com.fraccetta.scopabriscola.ui.theme.AccentGold
import com.fraccetta.scopabriscola.ui.theme.CardCream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuideScreen(navController: NavHostController) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Scopa", "Briscola", "Bluetooth")

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF0D1F33), Color(0xFF1A3A5C), Color(0xFF0D2D4A))
                    )
                )
        )
        AssetBackground(
            assetPath = "backgrounds/menu_bg.webp",
            overlayAlpha = 0.45f,
            modifier = Modifier.fillMaxSize()
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Game Guide", color = CardCream) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = CardCream
                            )
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
            ) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = AccentGold,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = AccentGold
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    color = if (selectedTab == index) AccentGold else CardCream.copy(alpha = 0.6f),
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }

                when (selectedTab) {
                    0 -> ScopaGuide()
                    1 -> BriscolaGuide()
                    2 -> BluetoothGuide()
                }
            }
        }
    }
}

@Composable
private fun ScopaGuide() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        GuideSection(
            title = "Objective",
            content = "Be the first to reach 11 points (or the score set in Settings) by winning multiple rounds."
        )
        GuideSection(
            title = "The deck",
            content = "Played with a 40-card Neapolitan deck divided into 4 suits: Cups, Coins, Swords and Clubs. Card values run from 1 (Ace) to 7, then Jack (8), Horse (9) and King (10)."
        )
        GuideSection(
            title = "Initial deal",
            content = "At the start of each hand, each player receives 3 cards. 4 more cards are placed face-up on the table."
        )
        GuideSection(
            title = "Turn",
            content = "On your turn, play one card from your hand.\n\n" +
                    "• If the card matches the value of one or more table cards, capture them.\n\n" +
                    "• If the card equals the sum of several table cards, you may capture that group (you choose if multiple options exist).\n\n" +
                    "• If no capture is possible, the card stays on the table.\n\n" +
                    "When both players run out of cards, 3 more are dealt each, until the deck is empty.\n\n" +
                    "At the end of the deck, any cards remaining on the table go to the last player who captured."
        )
        GuideSection(
            title = "Scopa!",
            content = "If your capture leaves the table completely empty, you score a «scopa»: 1 immediate point. Scopa cannot be scored on the very last capture of a hand."
        )
        GuideSection(
            title = "Scoring at round end",
            content = "At the end of each hand, points are tallied:\n\n" +
                    "• Cards: 1 point to whoever captured more cards (21+). Tie = no point.\n\n" +
                    "• Coins: 1 point to whoever captured more Coins cards (6+). Tie = no point.\n\n" +
                    "• Settebello: 1 point to whoever holds the 7 of Coins.\n\n" +
                    "• Primiera: 1 point to whoever has the highest primiera score. Take the highest-value card per suit (7=21, 6=18, Ace=16, 5=15, 4=14, 3=13, 2=12, face cards=10). Highest total wins.\n\n" +
                    "• Scope: each scopa scored during the hand is worth 1 point."
        )
        GuideSection(
            title = "End of game",
            content = "The first player to reach or exceed the target score wins. If both reach it in the same hand, the higher score wins. Exact tie means another hand is played."
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun BriscolaGuide() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        GuideSection(
            title = "Objective",
            content = "Accumulate more points than your opponent by capturing cards. Reaching 61 points (out of 120 total) wins the hand. The match is best of 3 hands."
        )
        GuideSection(
            title = "The deck",
            content = "Played with a 40-card Neapolitan deck divided into 4 suits: Cups, Coins, Swords and Clubs."
        )
        GuideSection(
            title = "The trump (Briscola)",
            content = "At the start, one card is turned face-up from the deck: its suit becomes the «briscola», the trump suit. That card stays visible and will be the last card drawn from the deck."
        )
        GuideSection(
            title = "Card values",
            content = "Ace: 11 pts\nThree: 10 pts\nKing: 4 pts\nHorse: 3 pts\nJack: 2 pts\n7, 6, 5, 4, 2: 0 pts\n\nTotal points in the deck: 120."
        )
        GuideSection(
            title = "Deal and draw",
            content = "Each player starts with 3 cards. After each trick, the winner draws first, then the loser, until the deck is empty."
        )
        GuideSection(
            title = "Turn",
            content = "The leading player plays a card face-up. The opponent responds with one card.\n\n" +
                    "Trick rules:\n\n" +
                    "• The highest card of the same suit wins.\n\n" +
                    "• A trump card beats any non-trump card.\n\n" +
                    "• If both play trump, the higher trump wins.\n\n" +
                    "• If neither player matches the suit or plays trump, the first card played wins.\n\n" +
                    "The trick winner takes both cards and leads the next trick."
        )
        GuideSection(
            title = "End of hand",
            content = "When all cards are played, points are counted. 61+ wins the hand. Exactly 60 each is a draw (another hand is played). First to win 2 hands wins the match."
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun BluetoothGuide() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        GuideSection(
            title = "How multiplayer works",
            content = "One player hosts the game, the other joins. The host's device broadcasts a Bluetooth Low Energy signal so the other player can find the game."
        )
        GuideSection(
            title = "Hosting a game",
            content = "Tap «Host game» on the game mode screen. Your device will start advertising via BLE. Wait for your opponent to find and connect to your game.\n\nMake sure Bluetooth is enabled and the app has Bluetooth permissions."
        )
        GuideSection(
            title = "Finding a game",
            content = "Tap «Find game». The app scans for nearby BLE signals from devices actively hosting.\n\n" +
                    "• Devices with an active game appear highlighted — tap one to connect.\n\n" +
                    "• Previously paired devices that are NOT hosting appear greyed out with «not in game».\n\n" +
                    "Only connect to highlighted devices. Grey entries are not hosting a game right now."
        )
        GuideSection(
            title = "Pairing for the first time",
            content = "BLE discovery works between devices that have already been paired via Android's Bluetooth settings. If a friend's device does not appear in the list, go to Android Settings → Bluetooth and pair their device first, then come back and scan."
        )
        GuideSection(
            title = "Connection problems",
            content = "If the connection fails or drops:\n\n" +
                    "• Make sure both devices have Bluetooth on and are within ~10 m of each other.\n\n" +
                    "• The host should still be on the «Waiting for opponent» screen.\n\n" +
                    "• The app will retry the connection automatically up to 3 times.\n\n" +
                    "• If retries fail, go back and start over."
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun GuideSection(title: String, content: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.45f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                color = AccentGold,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = content,
                color = CardCream,
                fontSize = 14.sp,
                lineHeight = 21.sp
            )
        }
    }
}
