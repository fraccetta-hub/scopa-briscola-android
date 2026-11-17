package com.fraccetta.scopabriscola.ui.lobby

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.fraccetta.scopabriscola.R
import com.fraccetta.scopabriscola.bluetooth.BtState
import com.fraccetta.scopabriscola.ui.game.AssetBackground
import com.fraccetta.scopabriscola.ui.theme.AccentGold
import com.fraccetta.scopabriscola.ui.theme.CardCream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LobbyScreen(
    gameType: String,
    role: String = "HOST",
    navController: NavHostController,
    viewModel: LobbyViewModel = hiltViewModel()
) {
    LaunchedEffect(gameType) {
        viewModel.setGameType(gameType)
    }

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { destination ->
            navController.navigate(destination) {
                popUpTo("lobby/{gameType}") { inclusive = true }
            }
        }
    }

    val uiState by viewModel.uiState.collectAsState()

    val btPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_ADVERTISE)
    } else {
        arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN)
    }

    var pendingBtAction by rememberSaveable { mutableStateOf<String?>(null) }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) {
            when (pendingBtAction) {
                "HOST" -> viewModel.startHosting()
                "SCAN" -> viewModel.startScanning()
            }
        } else {
            viewModel.onPermissionDenied()
        }
        pendingBtAction = null
    }

    // Auto-trigger BT action on first composition
    LaunchedEffect(role) {
        pendingBtAction = role
        permLauncher.launch(btPermissions)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0D1F33), Color(0xFF1A3A5C), Color(0xFF0D2D4A)))))
        AssetBackground(assetPath = "backgrounds/menu_bg.webp", overlayAlpha = 0.45f, modifier = Modifier.fillMaxSize())
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    val titleRes = if (gameType.equals("briscola", ignoreCase = true)) R.string.lobby_title_briscola else R.string.lobby_title_scopa
                    Text(stringResource(titleRes), color = CardCream)
                },
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
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (val state = uiState.btState) {
                is BtState.Idle -> {
                    // Auto-triggered via LaunchedEffect — spinner while permissions requested
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Spacer(modifier = Modifier.height(32.dp))
                        CircularProgressIndicator()
                    }
                }
                is BtState.Hosting -> {
                    HostingContent()
                }
                is BtState.Scanning -> {
                    ScanningContent(
                        discoveredHosts = uiState.discoveredHosts,
                        onHostClick = { host -> viewModel.connectTo(host.device) }
                    )
                }
                is BtState.Connected -> {
                    ConnectedContent(
                        remoteName = uiState.remoteName,
                        remoteDevice = state.remoteDevice
                    )
                }
                is BtState.Disconnected -> {
                    DisconnectedContent(
                        onRetryClick = {
                            pendingBtAction = "SCAN"
                            permLauncher.launch(btPermissions)
                        }
                    )
                }
            }

            uiState.errorResId?.let { resId ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(resId),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
    } // end outer Box
}

@Composable
private fun IdleContent(onHostClick: () -> Unit, onScanClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = stringResource(R.string.lobby_choose_role),
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onHostClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.lobby_host))
        }
        Button(
            onClick = onScanClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.lobby_scan))
        }
    }
}

@Composable
private fun HostingContent() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        CircularProgressIndicator(color = CardCream)
        Text(
            text = stringResource(R.string.lobby_hosting),
            style = MaterialTheme.typography.bodyLarge,
            color = CardCream
        )
    }
}

@Composable
private fun ScanningContent(
    discoveredHosts: List<DiscoveredHost>,
    onHostClick: (DiscoveredHost) -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        CircularProgressIndicator(color = CardCream)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.lobby_scanning),
            style = MaterialTheme.typography.titleMedium,
            color = CardCream
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (discoveredHosts.isEmpty()) {
            Text(
                text = stringResource(R.string.lobby_no_devices),
                style = MaterialTheme.typography.bodyMedium,
                color = CardCream.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) },
                colors = ButtonDefaults.buttonColors(containerColor = AccentGold)
            ) {
                Text(text = "Associa nuovo dispositivo", color = Color(0xFF0D1F33))
            }
        } else {
            Text(
                text = stringResource(R.string.lobby_select_device),
                style = MaterialTheme.typography.bodyMedium,
                color = CardCream
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn {
                items(discoveredHosts) { host ->
                    HostRow(host = host, onClick = { onHostClick(host) })
                    HorizontalDivider(color = CardCream.copy(alpha = 0.2f))
                }
            }
        }
    }
}

@Composable
private fun HostRow(host: DiscoveredHost, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${host.playerName} sta ospitando",
            style = MaterialTheme.typography.bodyLarge,
            color = CardCream
        )
    }
}

@Composable
private fun ConnectedContent(remoteName: String?, remoteDevice: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        if (remoteName != null) {
            Text(
                text = stringResource(R.string.lobby_connected_to, remoteName),
                style = MaterialTheme.typography.titleMedium,
                color = CardCream
            )
            CircularProgressIndicator(color = CardCream)
            Text(
                text = stringResource(R.string.lobby_starting),
                style = MaterialTheme.typography.bodyMedium,
                color = CardCream
            )
        } else {
            CircularProgressIndicator(color = CardCream)
            Text(
                text = stringResource(R.string.lobby_waiting_handshake),
                style = MaterialTheme.typography.bodyLarge,
                color = CardCream
            )
        }
    }
}

@Composable
private fun DisconnectedContent(onRetryClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = stringResource(R.string.lobby_disconnected),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
        )
        Button(onClick = onRetryClick) {
            Text(stringResource(R.string.lobby_retry))
        }
    }
}
