package com.fraccetta.scopabriscola.ui.lobby

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fraccetta.scopabriscola.R
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

data class LobbyUiState(
    val btState: BtState = BtState.Idle,
    val pairedDevices: List<BluetoothDevice> = emptyList(),
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
                        btManager.send(BtMessage.Handshake(playerName, role))
                    }
                    is ConnectionEvent.MessageReceived -> {
                        val msg = event.msg
                        if (msg is BtMessage.Handshake) {
                            _uiState.update { it.copy(remoteName = msg.playerName) }
                            GameSession.isServer = isServer
                            GameSession.localPlayer = if (isServer) 1 else 2
                            GameSession.opponentName = msg.playerName
                            val destination = if (gameType == "briscola") "game/briscola" else "game/scopa"
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
        btManager.startHosting()
        subscribeToEvents()
    }

    fun startScanning() {
        isServer = false
        _uiState.update { it.copy(errorResId = null) }
        btManager.startScanning()
        val paired: Set<BluetoothDevice>? = try {
            @Suppress("DEPRECATION")
            BluetoothAdapter.getDefaultAdapter()?.bondedDevices
        } catch (e: SecurityException) {
            _uiState.update { it.copy(errorResId = R.string.lobby_bt_permission_denied) }
            null
        }
        _uiState.update { it.copy(pairedDevices = paired?.toList() ?: emptyList()) }
    }

    fun onPermissionDenied() {
        _uiState.update { it.copy(errorResId = R.string.lobby_bt_permission_denied) }
    }

    fun connectTo(device: BluetoothDevice) {
        _uiState.update { it.copy(errorResId = null) }
        btManager.connect(device)
        subscribeToEvents()
    }

    override fun onCleared() {
        super.onCleared()
        btManager.disconnect()
    }
}
