package com.fraccetta.scopabriscola.ui.lobby

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fraccetta.scopabriscola.bluetooth.BtManager
import com.fraccetta.scopabriscola.bluetooth.BtMessage
import com.fraccetta.scopabriscola.bluetooth.BtState
import com.fraccetta.scopabriscola.bluetooth.ConnectionEvent
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
    val error: String? = null
)

@HiltViewModel
class LobbyViewModel @Inject constructor(
    private val btManager: BtManager,
    private val userPrefs: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(LobbyUiState())
    val uiState: StateFlow<LobbyUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<String>()
    val navigationEvent: SharedFlow<String> = _navigationEvent.asSharedFlow()

    private var isServer = false
    private var gameType = ""

    init {
        viewModelScope.launch {
            btManager.state.collect { btState ->
                _uiState.update { it.copy(btState = btState) }
            }
        }
        viewModelScope.launch {
            btManager.events.collect { event ->
                when (event) {
                    is ConnectionEvent.Connected -> {
                        val playerName = userPrefs.playerName.first().ifEmpty { "Giocatore" }
                        val role = if (isServer) "SERVER" else "CLIENT"
                        btManager.send(BtMessage.Handshake(playerName, role))
                    }
                    is ConnectionEvent.MessageReceived -> {
                        val msg = event.msg
                        if (msg is BtMessage.Handshake) {
                            _uiState.update { it.copy(remoteName = msg.playerName) }
                            val destination = if (gameType == "briscola") "game/briscola" else "game/scopa"
                            _navigationEvent.emit(destination)
                        }
                    }
                    is ConnectionEvent.Lost -> {
                        _uiState.update { it.copy(error = "Connessione persa") }
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
        _uiState.update { it.copy(error = null) }
        btManager.startHosting()
    }

    fun startScanning() {
        isServer = false
        _uiState.update { it.copy(error = null) }
        btManager.startScanning()
        val paired: Set<BluetoothDevice>? = try {
            @Suppress("DEPRECATION")
            BluetoothAdapter.getDefaultAdapter()?.bondedDevices
        } catch (e: SecurityException) {
            _uiState.update { it.copy(error = "Permesso Bluetooth negato") }
            null
        }
        _uiState.update { it.copy(pairedDevices = paired?.toList() ?: emptyList()) }
    }

    fun connectTo(device: BluetoothDevice) {
        _uiState.update { it.copy(error = null) }
        btManager.connect(device)
    }

    override fun onCleared() {
        super.onCleared()
        btManager.disconnect()
    }
}
