package com.fraccetta.scopabriscola.data

object GameSession {
    @Volatile var isServer: Boolean = false
    @Volatile var localPlayerName: String? = null
    @Volatile var opponentName: String? = null
    @Volatile var localPlayer: Int = 1
    @Volatile var lastDevice: android.bluetooth.BluetoothDevice? = null
    @Volatile var lastSessionPoints: Int = 0
    @Volatile var lastSessionScope: Int = 0
    @Volatile var lastMaxConsecutiveScope: Int = 0
    @Volatile var isSolo: Boolean = false
    // Scopone Scientifico fields
    @Volatile var scoponeLocalPlayer: Int = 0  // 0-3, which player this device controls
    @Volatile var scoponeBtMode: Int = 0       // 0=offline, 1=humans+AI, 2=user+AI vs BT+AI
    @Volatile var partnerName: String? = null  // BT partner name (mode 1)
}
