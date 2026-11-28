package com.fraccetta.scopabriscola.bluetooth

import kotlinx.coroutines.flow.Flow

interface GameTransport {
    val events: Flow<ConnectionEvent>
    fun send(msg: BtMessage)
    fun disconnect()
}
