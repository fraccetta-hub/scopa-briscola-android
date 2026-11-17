package com.fraccetta.scopabriscola.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import java.util.UUID

class BleDiscovery(private val context: Context) {

    companion object {
        val SERVICE_UUID: UUID = UUID.fromString("6764620a-9dad-11d1-80b4-00c04fd430c8")
        private const val MAX_NAME_BYTES = 16
    }

    private val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter

    private var advertiseCallback: AdvertiseCallback? = null
    private var scanCallback: ScanCallback? = null

    fun isAdvertisingSupported(): Boolean =
        adapter?.isMultipleAdvertisementSupported == true

    fun startAdvertising(playerName: String, onError: (String) -> Unit = {}) {
        if (!isAdvertisingSupported()) return  // device doesn't support BLE advertising — silent skip

        val advertiser = try { adapter?.bluetoothLeAdvertiser } catch (_: Exception) { null }
        if (advertiser == null) return

        val nameBytes = playerName.toByteArray(Charsets.UTF_8).take(MAX_NAME_BYTES).toByteArray()

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setConnectable(false)
            .setTimeout(0)
            .build()

        val data = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(SERVICE_UUID))
            .addServiceData(ParcelUuid(SERVICE_UUID), nameBytes)
            .build()

        val cb = object : AdvertiseCallback() {
            override fun onStartFailure(errorCode: Int) {
                if (errorCode != AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED) {
                    onError("Advertise error $errorCode")
                }
            }
        }
        advertiseCallback = cb

        try {
            advertiser.startAdvertising(settings, data, cb)
        } catch (_: SecurityException) {
            onError("BLE permission denied")
        }
    }

    fun stopAdvertising() {
        advertiseCallback?.let {
            try { adapter?.bluetoothLeAdvertiser?.stopAdvertising(it) } catch (_: Exception) {}
        }
        advertiseCallback = null
    }

    fun startScanning(onFound: (BluetoothDevice, String) -> Unit, onError: (String) -> Unit = {}) {
        val scanner = try { adapter?.bluetoothLeScanner } catch (_: Exception) { null }
        if (scanner == null) return  // BLE not supported — silent skip

        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(SERVICE_UUID))
            .build()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        val seen = mutableSetOf<String>()
        val cb = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val addr = result.device.address
                if (seen.contains(addr)) return
                val nameBytes = result.scanRecord?.getServiceData(ParcelUuid(SERVICE_UUID))
                val name = nameBytes?.toString(Charsets.UTF_8)?.trim()
                    ?: result.device.name?.takeIf { it.isNotEmpty() }
                    ?: return
                seen.add(addr)
                onFound(result.device, name)
            }
            override fun onScanFailed(errorCode: Int) { onError("Scan error $errorCode") }
        }
        scanCallback = cb

        try {
            scanner.startScan(listOf(filter), settings, cb)
        } catch (_: SecurityException) {
            onError("BLE permission denied")
        }
    }

    fun stopScanning() {
        scanCallback?.let {
            try { adapter?.bluetoothLeScanner?.stopScan(it) } catch (_: Exception) {}
        }
        scanCallback = null
        }
}
