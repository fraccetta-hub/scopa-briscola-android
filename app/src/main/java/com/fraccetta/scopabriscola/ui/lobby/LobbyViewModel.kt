package com.fraccetta.scopabriscola.ui.lobby

import android.bluetooth.BluetoothDevice
import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fraccetta.scopabriscola.R
import com.fraccetta.scopabriscola.bluetooth.BleDiscovery
import com.fraccetta.scopabriscola.bluetooth.BtManager
import dagger.hilt.android.qualifiers.ApplicationContext
import com.fraccetta.scopabriscola.bluetooth.BtMessage
import com.fraccetta.scopabriscola.bluetooth.BtState
import com.fraccetta.scopabriscola.bluetooth.ConnectionEvent
import com.fraccetta.scopabriscola.data.GameSession
import com.fraccetta.scopabriscola.data.prefs.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DiscoveredHost(val device: BluetoothDevice, val playerName: String, val isHosting: Boolean = false)

data class LobbyUiState(
    val btState: BtState = BtState.Idle,
    val discoveredHosts: List<DiscoveredHost> = emptyList(),
    val remoteName: String? = null,
    @StringRes val errorResId: Int? = null
)

@HiltViewModel
class LobbyViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val btManager: BtManager,
    private val userPrefs: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(LobbyUiState())
    val uiState: StateFlow<LobbyUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<String>()
    val navigationEvent: SharedFlow<String> = _navigationEvent.asSharedFlow()

    private var isServer = false
    private var gameType = ""
    private var eventsJob: kotlinx.coroutines.Job? = null
    private val bleDiscovery = BleDiscovery(context)

    init {
        viewModelScope.launch {
            btManager.state.collect { btState ->
                _uiState.update { it.copy(btState = btState) }
            }
        }
        subscribeToEvents()
    }

    private fun subscribeToEvents() {
        eventsJob?.cancel()
        eventsJob = viewModelScope.launch {
            btManager.events.collect { event ->
                when (event) {
                    is ConnectionEvent.Connected -> {
                        val playerName = userPrefs.playerName.first()
                            .ifEmpty { context.getString(R.string.lobby_default_player_name) }
                        val role = if (isServer) "SERVER" else "CLIENT"
                        GameSession.localPlayerName = playerName
                        btManager.send(BtMessage.Handshake(playerName, role))
                    }
                    is ConnectionEvent.MessageReceived -> {
                        val msg = event.msg
                        if (msg is BtMessage.Handshake) {
                            _uiState.update { it.copy(remoteName = msg.playerName) }
                            GameSession.isServer = isServer
                            GameSession.isSolo = false
                            GameSession.opponentName = msg.playerName
                            val destination = when {
                                gameType.equals("briscola", ignoreCase = true) -> {
                                    GameSession.localPlayer = if (isServer) 1 else 2
                                    "game/briscola"
                                }
                                gameType.equals("SCOPONE_TEAM", ignoreCase = true) -> {
                                    // 2 humans + 2 AI: server=p0, client=p2
                                    GameSession.scoponeBtMode = 1
                                    GameSession.scoponeLocalPlayer = if (isServer) 0 else 2
                                    GameSession.partnerName = msg.playerName
                                    "game/scopone"
                                }
                                gameType.equals("SCOPONE_MIX", ignoreCase = true) -> {
                                    // user+AI vs BT human+AI: server=p0, client=p1
                                    GameSession.scoponeBtMode = 2
                                    GameSession.scoponeLocalPlayer = if (isServer) 0 else 1
                                    "game/scopone"
                                }
                                else -> {
                                    GameSession.localPlayer = if (isServer) 1 else 2
                                    "game/scopa"
                                }
                            }
                            _navigationEvent.emit(destination)
                        }
                    }
                    is ConnectionEvent.Lost -> {
                        _uiState.update { it.copy(errorResId = R.string.lobby_connection_lost) }
                    }
                }
            }
        }
    }

    fun setGameType(type: String) {
        gameType = type
    }

    fun startHosting() {
        isServer = true
        _uiState.update { it.copy(errorResId = null) }
        viewModelScope.launch {
            val playerName = userPrefs.playerName.first()
                .ifEmpty { context.getString(R.string.lobby_default_player_name) }
            bleDiscovery.startAdvertising(playerName)
        }
        btManager.startHosting()
        subscribeToEvents()
    }

    fun startScanning() {
        isServer = false
        _uiState.update { it.copy(errorResId = null, discoveredHosts = emptyList()) }
        btManager.startScanning()

        // Pre-populate with paired devices (isHosting=false until BLE confirms)
        val paired = bleDiscovery.getPairedDevices().map { device ->
            DiscoveredHost(device, device.name ?: device.address, isHosting = false)
        }
        if (paired.isNotEmpty()) {
            _uiState.update { it.copy(discoveredHosts = paired) }
        }

        bleDiscovery.startScanning(
            onFound = { device, name ->
                _uiState.update { state ->
                    val existing = state.discoveredHosts.toMutableList()
                    val idx = existing.indexOfFirst { it.device.address == device.address }
                    if (idx >= 0) {
                        existing[idx] = existing[idx].copy(playerName = name, isHosting = true)
                    } else {
                        existing.add(DiscoveredHost(device, name, isHosting = true))
                    }
                    state.copy(discoveredHosts = existing)
                }
            },
            onError = {
                _uiState.update { state -> state.copy(errorResId = R.string.lobby_bt_permission_denied) }
            }
        )
    }

    fun onPermissionDenied() {
        _uiState.update { it.copy(errorResId = R.string.lobby_bt_permission_denied) }
    }

    fun connectTo(device: BluetoothDevice) {
        bleDiscovery.stopScanning()
        _uiState.update { it.copy(errorResId = null) }
        GameSession.lastDevice = device
        btManager.connect(device)
        subscribeToEvents()
    }

    override fun onCleared() {
        super.onCleared()
        bleDiscovery.stopAdvertising()
        bleDiscovery.stopScanning()
        // No btManager.disconnect() here — game VMs own the connection lifetime
    }
}
