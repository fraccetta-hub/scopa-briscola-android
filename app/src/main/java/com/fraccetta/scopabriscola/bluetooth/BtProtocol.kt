package com.fraccetta.scopabriscola.bluetooth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
sealed class BtMessage {
    @Serializable
    @SerialName("HANDSHAKE")
    data class Handshake(val playerName: String, val role: String) : BtMessage()

    @Serializable
    @SerialName("PLAY_CARD")
    data class PlayCard(val cardAssetName: String) : BtMessage()

    @Serializable
    @SerialName("CHOOSE_CAPTURE")
    data class ChooseCapture(val cardAssetNames: List<String>) : BtMessage()

    @Serializable
    @SerialName("GAME_STATE")
    data class GameState(val stateJson: String) : BtMessage()

    @Serializable
    @SerialName("NEXT_ROUND")
    object NextRound : BtMessage()

    @Serializable
    @SerialName("PING")
    object Ping : BtMessage()

    @Serializable
    @SerialName("SET_ARENA")
    data class SetArena(val arenaName: String) : BtMessage()

    @Serializable
    @SerialName("CHAT")
    data class Chat(val messageKey: String) : BtMessage()
}

object BtProtocol {
    private val json = Json {
        classDiscriminator = "type"
        encodeDefaults = true
    }

    fun encode(msg: BtMessage): String = json.encodeToString(BtMessage.serializer(), msg) + "\n"

    fun decode(line: String): BtMessage = json.decodeFromString(BtMessage.serializer(), line.trim())
}
