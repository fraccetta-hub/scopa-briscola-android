package com.fraccetta.scopabriscola.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
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

class BtClient(private val context: Context) {
    private val _channel = Channel<String>(Channel.UNLIMITED)
    val incoming: Flow<String> = _channel.receiveAsFlow()
    private val _connected = Channel<Unit>(Channel.CONFLATED)
    val connected: Flow<Unit> = _connected.receiveAsFlow()
    private var outputStream: OutputStream? = null
    private var socket: BluetoothSocket? = null

    fun bondedDevices(): Set<BluetoothDevice> {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        return bluetoothManager.adapter?.bondedDevices ?: emptySet()
    }

    fun connect(device: BluetoothDevice, scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            try {
                val s = device.createInsecureRfcommSocketToServiceRecord(BluetoothConstants.SERVICE_UUID)
                s.connect()
                socket = s
                outputStream = s.outputStream
                _connected.trySend(Unit)   // signal: socket established
                val reader = BufferedReader(InputStreamReader(s.inputStream))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    line?.let { _channel.trySend(it) }
                }
            } catch (_: Exception) {
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

    fun disconnect() {
        try { socket?.close() } catch (_: Exception) {}
        socket = null
        outputStream = null
    }
}
