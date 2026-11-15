package com.fraccetta.scopabriscola.ui.lobby

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.fraccetta.scopabriscola.R
import com.fraccetta.scopabriscola.bluetooth.BtState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LobbyScreen(
    gameType: String,
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
        arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
    } else {
        arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN)
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        // Permissions granted or denied — scanning will handle the error via SecurityException
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val titleRes = if (gameType == "briscola") R.string.lobby_title_briscola else R.string.lobby_title_scopa
                    Text(stringResource(titleRes))
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.nav_back)
                        )
                    }
                }
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
                    IdleContent(
                        onHostClick = {
                            permLauncher.launch(btPermissions)
                            viewModel.startHosting()
                        },
                        onScanClick = {
                            permLauncher.launch(btPermissions)
                            viewModel.startScanning()
                        }
                    )
                }
                is BtState.Hosting -> {
                    HostingContent()
                }
                is BtState.Scanning -> {
                    ScanningContent(
                        pairedDevices = uiState.pairedDevices,
                        onDeviceClick = { device -> viewModel.connectTo(device) }
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
                            permLauncher.launch(btPermissions)
                            viewModel.startScanning()
                        }
                    )
                }
            }

            uiState.error?.let { error ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
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
        CircularProgressIndicator()
        Text(
            text = stringResource(R.string.lobby_hosting),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun ScanningContent(
    pairedDevices: List<BluetoothDevice>,
    onDeviceClick: (BluetoothDevice) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.lobby_scanning),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (pairedDevices.isEmpty()) {
            Text(
                text = stringResource(R.string.lobby_no_devices),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                text = stringResource(R.string.lobby_select_device),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn {
                items(pairedDevices) { device ->
                    DeviceRow(device = device, onClick = { onDeviceClick(device) })
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun DeviceRow(device: BluetoothDevice, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            val name = try { device.name } catch (e: SecurityException) { null }
            Text(
                text = name ?: stringResource(R.string.lobby_unknown_device),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = device.address,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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
                style = MaterialTheme.typography.titleMedium
            )
            CircularProgressIndicator()
            Text(
                text = stringResource(R.string.lobby_starting),
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            CircularProgressIndicator()
            Text(
                text = stringResource(R.string.lobby_waiting_handshake),
                style = MaterialTheme.typography.bodyLarge
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
