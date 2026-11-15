package com.fraccetta.scopabriscola.bluetooth

import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream

class BtServer(private val context: Context) {
    private val _channel = Channel<String>(Channel.UNLIMITED)
    val incoming: Flow<String> = _channel.receiveAsFlow()
    private var outputStream: OutputStream? = null
    private var serverSocket: BluetoothServerSocket? = null

    fun start(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            try {
                val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                val adapter = bluetoothManager.adapter ?: return@launch
                serverSocket = adapter.listenUsingRfcommWithServiceRecord(
                    "ScopaBriscola",
                    BluetoothConstants.SERVICE_UUID
                )
                val socket = serverSocket?.accept() ?: return@launch
                outputStream = socket.outputStream
                val reader = BufferedReader(InputStreamReader(socket.inputStream))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    line?.let { _channel.trySend(it) }
                }
            } catch (_: Exception) {
                // Connection closed or error
            } finally {
                _channel.close()
            }
        }
    }

    fun send(line: String) {
        try {
            outputStream?.write(line.toByteArray())
            outputStream?.flush()
        } catch (_: Exception) {
            // ignore send errors
        }
    }

    fun stop() {
        try {
            serverSocket?.close()
        } catch (_: Exception) {
            // ignore
        }
        serverSocket = null
        outputStream = null
    }
}
