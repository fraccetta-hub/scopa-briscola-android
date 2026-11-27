package com.fraccetta.scopabriscola.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.unit.sp
import com.fraccetta.scopabriscola.BuildConfig
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.fraccetta.scopabriscola.R
import com.fraccetta.scopabriscola.ui.game.AssetBackground
import com.fraccetta.scopabriscola.ui.screens.home.HomeViewModel
import com.fraccetta.scopabriscola.ui.theme.AccentGold
import com.fraccetta.scopabriscola.ui.theme.CardCream

private val OceanDark = Color(0xFF0D1F33)
private val OceanMid = Color(0xFF1A3A5C)
private val OceanDeep = Color(0xFF0D2D4A)

@Composable
fun HomeScreen(
    navController: NavHostController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val playerName by viewModel.playerName.collectAsState(initial = "")
    val updateInfo by viewModel.updateInfo.collectAsState()
    val context = LocalContext.current

    updateInfo?.let { info ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissUpdate() },
            title = { Text(text = "Update available") },
            text = { Text(text = "Version ${info.version} is available. Download it?") },
            confirmButton = {
                TextButton(onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(info.downloadUrl)))
                    viewModel.dismissUpdate()
                }) { Text(text = "Download") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissUpdate() }) { Text(text = "Not now") }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Ocean gradient fallback, then photo on top if available
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(OceanDark, OceanMid, OceanDeep))))
        AssetBackground(assetPath = "backgrounds/menu_bg.webp", overlayAlpha = 0.45f, modifier = Modifier.fillMaxSize())
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge,
                color = CardCream,
                fontWeight = FontWeight.Bold
            )

            if (playerName.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.home_welcome, playerName),
                    style = MaterialTheme.typography.bodyLarge,
                    color = CardCream.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = { navController.navigate("gamemode/scopa") },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1A5276),
                    contentColor = Color.White
                )
            ) {
                Text(text = "Scopa", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { navController.navigate("gamemode/briscola") },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1A5276),
                    contentColor = Color.White
                )
            ) {
                Text(text = "Briscola", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { navController.navigate("scoponemode") },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1A5276),
                    contentColor = Color.White
                )
            ) {
                Text(text = "Scopone Scientifico", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { navController.navigate("stats") },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentGold,
                    contentColor = OceanDark
                )
            ) {
                Text(text = stringResource(R.string.home_stats), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { navController.navigate("settings") },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentGold,
                    contentColor = OceanDark
                )
            ) {
                Text(text = stringResource(R.string.home_settings), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "v${BuildConfig.VERSION_NAME}",
                fontSize = 11.sp,
                color = CardCream.copy(alpha = 0.4f),
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    }
}
