package com.fraccetta.scopabriscola.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.fraccetta.scopabriscola.R
import com.fraccetta.scopabriscola.ui.game.AssetBackground
import com.fraccetta.scopabriscola.ui.theme.AccentGold
import com.fraccetta.scopabriscola.ui.theme.CardCream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavHostController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val playerName by viewModel.playerName.collectAsState(initial = "")
    val soundEnabled by viewModel.soundEnabled.collectAsState(initial = true)
    val scopaTargetScore by viewModel.scopaTargetScore.collectAsState(initial = 11)
    val updateCheckState by viewModel.updateCheckState.collectAsState()

    var nameInput by remember(playerName) { mutableStateOf(playerName) }
    var targetScoreInput by remember(scopaTargetScore) { mutableStateOf(scopaTargetScore.toString()) }
    var nameSaved by remember { mutableStateOf(false) }
    var scoreSaved by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Update check dialogs
    when (val state = updateCheckState) {
        is UpdateCheckState.UpdateAvailable -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismissUpdateCheck() },
                title = { Text("Aggiornamento disponibile") },
                text = { Text("Versione ${state.info.version} disponibile. Vuoi scaricarla?") },
                confirmButton = {
                    Button(onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(state.info.downloadUrl)))
                        viewModel.dismissUpdateCheck()
                    }) { Text("Scarica") }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissUpdateCheck() }) { Text("Non ora") }
                }
            )
        }
        is UpdateCheckState.UpToDate -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismissUpdateCheck() },
                title = { Text("App aggiornata") },
                text = { Text("Hai già l'ultima versione.") },
                confirmButton = {
                    Button(onClick = { viewModel.dismissUpdateCheck() }) { Text("OK") }
                }
            )
        }
        is UpdateCheckState.Error -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismissUpdateCheck() },
                title = { Text("Errore") },
                text = { Text("Impossibile controllare aggiornamenti. Verifica la connessione.") },
                confirmButton = {
                    Button(onClick = { viewModel.dismissUpdateCheck() }) { Text("OK") }
                }
            )
        }
        else -> {}
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0D1F33), Color(0xFF1A3A5C), Color(0xFF0D2D4A)))))
        AssetBackground(assetPath = "backgrounds/menu_bg.webp", overlayAlpha = 0.45f, modifier = Modifier.fillMaxSize())
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), color = CardCream) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.nav_back),
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
                .padding(16.dp)
        ) {
            Text(text = stringResource(R.string.settings_player_name_label), color = CardCream)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                label = { Text(stringResource(R.string.settings_player_name_hint), color = CardCream.copy(alpha = 0.7f)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = CardCream,
                    unfocusedTextColor = CardCream,
                    focusedBorderColor = CardCream,
                    unfocusedBorderColor = CardCream.copy(alpha = 0.5f)
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    viewModel.setPlayerName(nameInput)
                    scope.launch { nameSaved = true; delay(2000); nameSaved = false }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentGold)
            ) {
                Text(text = stringResource(R.string.settings_save_name), color = CardCream)
            }
            if (nameSaved) {
                Text(text = "✓ Nome salvato", color = AccentGold)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.settings_sound_label),
                    modifier = Modifier.weight(1f),
                    color = CardCream
                )
                Switch(
                    checked = soundEnabled,
                    onCheckedChange = { viewModel.setSoundEnabled(it) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(text = stringResource(R.string.settings_target_score_label), color = CardCream)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = targetScoreInput,
                onValueChange = { targetScoreInput = it },
                label = { Text(stringResource(R.string.settings_target_score_hint), color = CardCream.copy(alpha = 0.7f)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = CardCream,
                    unfocusedTextColor = CardCream,
                    focusedBorderColor = CardCream,
                    unfocusedBorderColor = CardCream.copy(alpha = 0.5f)
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    val score = targetScoreInput.toIntOrNull()?.coerceIn(1, 100) ?: 11
                    targetScoreInput = score.toString()
                    viewModel.setScopaTargetScore(score)
                    scope.launch { scoreSaved = true; delay(2000); scoreSaved = false }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentGold)
            ) {
                Text(text = stringResource(R.string.settings_save_target_score), color = CardCream)
            }
            if (scoreSaved) {
                Text(text = "✓ Punteggio salvato", color = AccentGold)
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { viewModel.checkForUpdate() },
                modifier = Modifier.fillMaxWidth(),
                enabled = updateCheckState !is UpdateCheckState.Checking,
                colors = ButtonDefaults.buttonColors(containerColor = AccentGold)
            ) {
                if (updateCheckState is UpdateCheckState.Checking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = CardCream
                    )
                } else {
                    Text("Controlla aggiornamenti", color = CardCream)
                }
            }
        }
    }
    } // end outer Box
}
