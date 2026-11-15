package com.fraccetta.scopabriscola.bluetooth

import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Test

class BtProtocolTest {

    @Test
    fun encodeDecodeRoundtrip() {
        val msg = BtMessage.PlayCard("asso_denari")
        val encoded = BtProtocol.encode(msg)
        val decoded = BtProtocol.decode(encoded)
        assertEquals(msg, decoded)
    }

    @Test
    fun encodeDecodeHandshake() {
        val msg = BtMessage.Handshake("Mario", "SERVER")
        assertEquals(msg, BtProtocol.decode(BtProtocol.encode(msg)))
    }

    @Test
    fun encodeDecodeChooseCapture() {
        val msg = BtMessage.ChooseCapture(listOf("asso_denari", "tre_coppe"))
        assertEquals(msg, BtProtocol.decode(BtProtocol.encode(msg)))
    }

    @Test
    fun encodeDecodeGameState() {
        val msg = BtMessage.GameState("{\"stateKey\":\"value\"}")
        assertEquals(msg, BtProtocol.decode(BtProtocol.encode(msg)))
    }

    @Test
    fun encodeDecodeNextRound() {
        val msg = BtMessage.NextRound
        assertEquals(msg, BtProtocol.decode(BtProtocol.encode(msg)))
    }

    @Test
    fun encodeDecodePing() {
        val msg = BtMessage.Ping
        assertEquals(msg, BtProtocol.decode(BtProtocol.encode(msg)))
    }

    @Test
    fun encodeEndsWithNewline() {
        val msg = BtMessage.PlayCard("asso_denari")
        val encoded = BtProtocol.encode(msg)
        assert(encoded.endsWith("\n")) { "encoded message should end with newline" }
    }

    @Test(expected = SerializationException::class)
    fun decodeInvalidThrows() {
        BtProtocol.decode("not-json")
    }
}
