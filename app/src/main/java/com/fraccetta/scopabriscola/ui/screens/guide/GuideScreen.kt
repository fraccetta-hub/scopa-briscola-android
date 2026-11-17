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
    val tabs = listOf("Scopa", "Briscola")

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
                    title = { Text("Guida ai giochi", color = CardCream) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Indietro",
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
            title = "Obiettivo",
            content = "Raggiungere per primi 11 punti (o il punteggio scelto nelle impostazioni) vincendo più partite."
        )
        GuideSection(
            title = "Il mazzo",
            content = "Si gioca con un mazzo napoletano da 40 carte, suddiviso in 4 semi: Coppe, Denari, Spade e Bastoni. I valori delle carte sono da 1 (Asso) a 7, poi Fante (8), Cavallo (9) e Re (10)."
        )
        GuideSection(
            title = "Distribuzione iniziale",
            content = "All'inizio di ogni mano, ogni giocatore riceve 3 carte in mano. Altre 4 carte vengono poste scoperte sul tavolo."
        )
        GuideSection(
            title = "Turno di gioco",
            content = "A turno, ogni giocatore deve giocare una carta dalla propria mano.\n\n" +
                    "• Se la carta giocata ha lo stesso valore di una o più carte sul tavolo, si prendono quelle carte.\n\n" +
                    "• Se la carta giocata è uguale alla somma di più carte sul tavolo, si possono prendere quelle carte (si sceglie la combinazione se ce ne sono più d'una).\n\n" +
                    "• Se non è possibile prendere nulla, la carta rimane sul tavolo.\n\n" +
                    "Quando entrambi i giocatori hanno esaurito le carte in mano, vengono distribuite altre 3 carte a testa, finché il mazzo è esaurito.\n\n" +
                    "Al termine del mazzo, le eventuali carte rimaste sul tavolo vanno all'ultimo giocatore che ha preso."
        )
        GuideSection(
            title = "Scopa!",
            content = "Se con la tua presa lasci il tavolo completamente vuoto, fai «scopa»: vale 1 punto immediato. Non si fa scopa sull'ultima presa della mano (il tavolo si svuota normalmente)."
        )
        GuideSection(
            title = "Conteggio punti a fine mano",
            content = "Alla fine di ogni mano si contano i punti:\n\n" +
                    "• Carte: 1 punto a chi ha preso più carte (25+). In caso di parità nessuno prende il punto.\n\n" +
                    "• Denari: 1 punto a chi ha preso più carte di Denari (6+). In caso di parità nessuno prende il punto.\n\n" +
                    "• Settebello: 1 punto a chi ha il 7 di Denari.\n\n" +
                    "• Primiera: 1 punto a chi ha il punteggio di primiera più alto. Si prende la carta di valore più alto per ogni seme (il 7 vale 21, il 6 vale 18, l'1 vale 16, il 5 vale 15, il 4 vale 14, il 3 vale 13, il 2 vale 12, Fante/Cavallo/Re valgono 10). Chi ha la somma maggiore vince il punto.\n\n" +
                    "• Scope: ogni scopa fatta durante la mano vale 1 punto."
        )
        GuideSection(
            title = "Fine partita",
            content = "Vince chi raggiunge o supera per primo il punteggio obiettivo (default 11). Se entrambi lo raggiungono nella stessa mano, vince chi ha il punteggio più alto. In caso di parità si gioca un'altra mano."
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
            title = "Obiettivo",
            content = "Accumulare più punti dell'avversario prendendo carte durante la partita. Chi arriva a 61 punti (su 120 totali) vince la mano. La partita si gioca al meglio di 3 mani."
        )
        GuideSection(
            title = "Il mazzo",
            content = "Si gioca con un mazzo napoletano da 40 carte, suddiviso in 4 semi: Coppe, Denari, Spade e Bastoni."
        )
        GuideSection(
            title = "La briscola",
            content = "All'inizio della partita viene girata una carta dal mazzo: il suo seme diventa la «briscola», il seme vincente. La carta girata rimiene visibile e sarà l'ultima carta pescata dal mazzo."
        )
        GuideSection(
            title = "Valore delle carte",
            content = "Asso: 11 punti\nTre: 10 punti\nRe: 4 punti\nCavallo: 3 punti\nFante: 2 punti\n7, 6, 5, 4, 2: 0 punti\n\nTotale punti nel mazzo: 120."
        )
        GuideSection(
            title = "Distribuzione e pescata",
            content = "All'inizio ogni giocatore riceve 3 carte. Dopo ogni turno, chi ha giocato pesca una carta dal mazzo (prima il vincente, poi il perdente), finché il mazzo è esaurito."
        )
        GuideSection(
            title = "Turno di gioco",
            content = "Il giocatore di mano gioca una carta scoperta. L'avversario risponde con un'altra carta.\n\n" +
                    "Regole di presa:\n\n" +
                    "• Vince la carta dello stesso seme con valore più alto.\n\n" +
                    "• Una carta di briscola batte sempre una carta di qualsiasi altro seme.\n\n" +
                    "• Se si giocano due briscole, vince quella di valore più alto.\n\n" +
                    "• Se nessuno gioca lo stesso seme né la briscola, vince chi ha giocato per primo.\n\n" +
                    "Chi vince il turno prende entrambe le carte e guida il turno successivo."
        )
        GuideSection(
            title = "Fine mano",
            content = "Quando le carte sono esaurite si contano i punti. Chi ha 61+ vince la mano. Con esattamente 60 punti a testa è pareggio (si gioca un'altra mano). La partita si vince al meglio di 3 mani."
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
