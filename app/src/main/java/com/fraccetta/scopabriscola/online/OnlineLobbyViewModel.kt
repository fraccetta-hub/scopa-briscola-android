package com.fraccetta.scopabriscola.online

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fraccetta.scopabriscola.bluetooth.ConnectionEvent
import com.fraccetta.scopabriscola.data.GameSession
import com.fraccetta.scopabriscola.data.prefs.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class OnlineLobbyState {
    object Idle : OnlineLobbyState()
    data class Hosting(val playerName: String) : OnlineLobbyState()
    data class BrowsingRooms(val rooms: List<RoomInfo>) : OnlineLobbyState()
    object Connecting : OnlineLobbyState()
    object Connected : OnlineLobbyState()
    object Error : OnlineLobbyState()
}

@HiltViewModel
class OnlineLobbyViewModel @Inject constructor(
    private val onlineManager: OnlineManager,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _state = MutableStateFlow<OnlineLobbyState>(OnlineLobbyState.Idle)
    val state: StateFlow<OnlineLobbyState> = _state.asStateFlow()

    fun hostGame(gameType: String) {
        viewModelScope.launch {
            val name = userPreferences.playerName.first().ifBlank { "Host" }
            GameSession.isServer = true
            GameSession.isSolo = false
            GameSession.isOnline = true
            GameSession.localPlayerName = name
            applyGameTypeSession(gameType, isServer = true)
            // Start collecting BEFORE startHosting to avoid missing Connected event
            val collectJob = launch {
                onlineManager.events.collect { event ->
                    when (event) {
                        is ConnectionEvent.Connected -> {
                            _state.value = OnlineLobbyState.Connected
                            return@collect
                        }
                        is ConnectionEvent.Lost -> {
                            _state.value = OnlineLobbyState.Error
                            return@collect
                        }
                        else -> {}
                    }
                }
            }
            onlineManager.startHosting(gameType, name)
            _state.value = OnlineLobbyState.Hosting(name)
        }
    }

    fun startBrowsing(gameType: String) {
        _state.value = OnlineLobbyState.BrowsingRooms(emptyList())
        onlineManager.listenForOpenRooms(gameType) { rooms ->
            _state.value = OnlineLobbyState.BrowsingRooms(rooms)
        }
    }

    fun joinRoom(room: RoomInfo, gameType: String) {
        viewModelScope.launch {
            val name = userPreferences.playerName.first().ifBlank { "Guest" }
            _state.value = OnlineLobbyState.Connecting
            GameSession.isServer = false
            GameSession.isSolo = false
            GameSession.isOnline = true
            GameSession.localPlayerName = name
            applyGameTypeSession(gameType, isServer = false)
            onlineManager.stopListeningForRooms()

            onlineManager.connect(room.code, name,
                onRoomNotFound = { startBrowsing(gameType) }
            )

            onlineManager.events.collect { event ->
                when (event) {
                    is ConnectionEvent.Connected -> {
                        _state.value = OnlineLobbyState.Connected
                        return@collect
                    }
                    is ConnectionEvent.Lost -> {
                        _state.value = OnlineLobbyState.Error
                        return@collect
                    }
                    else -> {}
                }
            }
        }
    }

    fun reset() {
        onlineManager.disconnect()
        _state.value = OnlineLobbyState.Idle
    }

    private fun applyGameTypeSession(gameType: String, isServer: Boolean) {
        when (gameType.uppercase()) {
            "SCOPA" -> {
                GameSession.localPlayer = if (isServer) 1 else 2
            }
            "BRISCOLA" -> {
                GameSession.localPlayer = if (isServer) 1 else 2
            }
            "SCOPONE_TEAM" -> {
                GameSession.scoponeBtMode = 1
                GameSession.scoponeLocalPlayer = if (isServer) 0 else 2
            }
        }
    }
}
