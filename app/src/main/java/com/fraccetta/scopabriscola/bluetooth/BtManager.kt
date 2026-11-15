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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

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

    private data class Tagged(val gen: Int, val event: ConnectionEvent)
    private val _tagged = MutableSharedFlow<Tagged>(replay = 1, extraBufferCapacity = 64)
    private val generation = AtomicInteger(0)

    val events: Flow<ConnectionEvent> get() {
        val gen = generation.get()
        return _tagged.filter { it.gen == gen }.map { it.event }
    }

    private var btServer: BtServer? = null
    private var btClient: BtClient? = null

    fun startHosting() {
        val gen = generation.incrementAndGet()
        _tagged.resetReplayCache()
        _state.value = BtState.Hosting
        val server = BtServer(appContext)
        btServer = server
        server.start(scope)
        scope.launch {
            try {
                server.accepted.first()   // wait until socket accepted
            } catch (_: Exception) {
                _state.value = BtState.Disconnected
                _tagged.emit(Tagged(gen, ConnectionEvent.Lost))
                return@launch
            }
            _state.value = BtState.Connected("")   // name updated when Handshake arrives
            _tagged.emit(Tagged(gen, ConnectionEvent.Connected))
            startKeepalive()
            server.incoming.collect { line ->
                try {
                    val msg = BtProtocol.decode(line)
                    if (msg is BtMessage.Handshake) {
                        _state.value = BtState.Connected(msg.playerName)
                    }
                    _tagged.emit(Tagged(gen, ConnectionEvent.MessageReceived(msg)))
                } catch (_: Exception) {}
            }
            _state.value = BtState.Disconnected
            _tagged.emit(Tagged(gen, ConnectionEvent.Lost))
        }
    }

    fun startScanning() {
        _state.value = BtState.Scanning
    }

    fun connect(device: BluetoothDevice) {
        val gen = generation.incrementAndGet()
        _tagged.resetReplayCache()
        _state.value = BtState.Scanning
        val client = BtClient(appContext)
        btClient = client
        client.connect(device, scope)
        scope.launch {
            try {
                client.connected.first()   // wait until socket connected
            } catch (_: Exception) {
                _state.value = BtState.Disconnected
                _tagged.emit(Tagged(gen, ConnectionEvent.Lost))
                return@launch
            }
            _state.value = BtState.Connected(device.name ?: device.address)
            _tagged.emit(Tagged(gen, ConnectionEvent.Connected))
            startKeepalive()
            client.incoming.collect { line ->
                try {
                    val msg = BtProtocol.decode(line)
                    _tagged.emit(Tagged(gen, ConnectionEvent.MessageReceived(msg)))
                } catch (_: Exception) {}
            }
            _state.value = BtState.Disconnected
            _tagged.emit(Tagged(gen, ConnectionEvent.Lost))
        }
    }

    fun send(msg: BtMessage) {
        val line = BtProtocol.encode(msg)
        scope.launch(Dispatchers.IO) {
            btServer?.send(line)
            btClient?.send(line)
        }
    }

    fun disconnect() {
        btServer?.stop()
        btClient?.disconnect()
        btServer = null
        btClient = null
        _state.value = BtState.Disconnected
        // No resetReplayCache here — old session's Lost carries stale generation and is filtered out
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
