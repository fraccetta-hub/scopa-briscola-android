package com.fraccetta.scopabriscola.bluetooth

import android.bluetooth.BluetoothDevice
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class BtState {
    object Idle : BtState()
    object Hosting : BtState()
    object Scanning : BtState()
    data class Connected(val remoteDevice: String) : BtState()
    object Disconnected : BtState()
}

sealed class ConnectionEvent {
    object Connected : ConnectionEvent()
    data class MessageReceived(val msg: BtMessage) : ConnectionEvent()
    object Lost : ConnectionEvent()
}

class BtManager(@androidx.annotation.NonNull context: Context) {
    private val appContext: Context = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow<BtState>(BtState.Idle)
    val state: StateFlow<BtState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<ConnectionEvent>(replay = 1, extraBufferCapacity = 64)
    val events: Flow<ConnectionEvent> = _events.asSharedFlow()

    private var btServer: BtServer? = null
    private var btClient: BtClient? = null

    fun startHosting() {
        _events.resetReplayCache()
        _state.value = BtState.Hosting
        val server = BtServer(appContext)
        btServer = server
        server.start(scope)
        scope.launch {
            server.incoming.collect { line ->
                try {
                    val msg = BtProtocol.decode(line)
                    if (msg is BtMessage.Handshake) {
                        _state.value = BtState.Connected(msg.playerName)
                        _events.emit(ConnectionEvent.Connected)
                        startKeepalive()
                    }
                    _events.emit(ConnectionEvent.MessageReceived(msg))
                } catch (_: Exception) {
                    // ignore malformed lines
                }
            }
            _state.value = BtState.Disconnected
            _events.emit(ConnectionEvent.Lost)
        }
    }

    fun startScanning() {
        _state.value = BtState.Scanning
    }

    fun connect(device: BluetoothDevice) {
        _events.resetReplayCache()
        val client = BtClient(appContext)
        btClient = client
        client.connect(device, scope)
        scope.launch {
            client.incoming.collect { line ->
                try {
                    val msg = BtProtocol.decode(line)
                    if (_state.value is BtState.Scanning) {
                        _state.value = BtState.Connected(device.name ?: device.address)
                        _events.emit(ConnectionEvent.Connected)
                        startKeepalive()
                    }
                    _events.emit(ConnectionEvent.MessageReceived(msg))
                } catch (_: Exception) {
                    // ignore malformed lines
                }
            }
            _state.value = BtState.Disconnected
            _events.emit(ConnectionEvent.Lost)
        }
    }

    fun send(msg: BtMessage) {
        val line = BtProtocol.encode(msg)
        btServer?.send(line)
        btClient?.send(line)
    }

    fun disconnect() {
        btServer?.stop()
        btClient?.disconnect()
        btServer = null
        btClient = null
        _state.value = BtState.Disconnected
        _events.resetReplayCache()
    }

    private fun startKeepalive() {
        scope.launch {
            while (_state.value is BtState.Connected) {
                delay(15_000L)
                if (_state.value is BtState.Connected) {
                    send(BtMessage.Ping)
                }
            }
        }
    }
}
