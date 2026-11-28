package com.fraccetta.scopabriscola.online

import com.fraccetta.scopabriscola.bluetooth.BtMessage
import com.fraccetta.scopabriscola.bluetooth.BtProtocol
import com.fraccetta.scopabriscola.bluetooth.ConnectionEvent
import com.fraccetta.scopabriscola.bluetooth.GameTransport
import com.fraccetta.scopabriscola.data.GameSession
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

data class RoomInfo(
    val code: String,
    val hostName: String,
    val gameType: String,
    val timestamp: Long
)

class OnlineManager : GameTransport {

    private val db = Firebase.database.reference
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _events = MutableSharedFlow<ConnectionEvent>(replay = 0, extraBufferCapacity = 64)
    override val events: Flow<ConnectionEvent> = _events

    private var roomCode: String? = null
    private var isServer: Boolean = false
    private var hostPlayerName: String? = null
    private var incomingListener: ChildEventListener? = null
    private var clientJoinListener: ValueEventListener? = null
    private var openRoomsListener: ValueEventListener? = null

    // Host: creates room, writes to open_rooms list
    fun startHosting(gameType: String, hostName: String): String {
        val code = generateCode()
        roomCode = code
        isServer = true
        hostPlayerName = hostName

        val roomRef = db.child("rooms").child(code)
        roomRef.child("meta").setValue(
            mapOf("game" to gameType, "hostName" to hostName, "state" to "WAITING")
        )

        // Publish to open rooms; auto-remove on host disconnect
        val openRoomRef = db.child("open_rooms").child(hostName)
        openRoomRef.setValue(
            mapOf("code" to code, "hostName" to hostName, "game" to gameType, "timestamp" to System.currentTimeMillis())
        )
        openRoomRef.onDisconnect().removeValue()
        roomRef.onDisconnect().removeValue()

        // Listen for client joining
        clientJoinListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val clientName = snapshot.child("meta").child("clientName").getValue(String::class.java)
                if (clientName != null) {
                    roomRef.removeEventListener(this)
                    clientJoinListener = null
                    openRoomRef.removeValue()
                    GameSession.opponentName = clientName
                    GameSession.partnerName = clientName
                    scope.launch { _events.emit(ConnectionEvent.Connected) }
                    listenForMessages(code, isServerSide = true)
                }
            }
            override fun onCancelled(error: DatabaseError) {
                scope.launch { _events.emit(ConnectionEvent.Lost) }
            }
        }
        roomRef.addValueEventListener(clientJoinListener!!)

        return code
    }

    // Client: connects by joining a room from the browser
    fun connect(code: String, clientName: String, onRoomNotFound: () -> Unit) {
        roomCode = code
        isServer = false

        val roomRef = db.child("rooms").child(code)
        roomRef.child("meta").get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                onRoomNotFound()
                return@addOnSuccessListener
            }
            val hostName = snapshot.child("hostName").getValue(String::class.java)
            GameSession.opponentName = hostName
            GameSession.partnerName = hostName
            roomRef.child("meta").child("clientName").setValue(clientName)
            scope.launch { _events.emit(ConnectionEvent.Connected) }
            listenForMessages(code, isServerSide = false)
        }.addOnFailureListener {
            scope.launch { _events.emit(ConnectionEvent.Lost) }
        }
    }

    // Real-time room browser — filtered by gameType family
    fun listenForOpenRooms(gameType: String, onUpdate: (List<RoomInfo>) -> Unit) {
        stopListeningForRooms()
        openRoomsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val staleThreshold = System.currentTimeMillis() - 5 * 60 * 1000L
                val rooms = mutableListOf<RoomInfo>()
                for (child in snapshot.children) {
                    val code = child.child("code").getValue(String::class.java) ?: continue
                    val hostName = child.child("hostName").getValue(String::class.java) ?: continue
                    val game = child.child("game").getValue(String::class.java) ?: continue
                    val ts = child.child("timestamp").getValue(Long::class.java) ?: 0L
                    if (ts < staleThreshold) continue
                    val sameFamily = gameType.uppercase().let { gt ->
                        game.uppercase() == gt ||
                        (gt.startsWith("SCOPONE") && game.uppercase().startsWith("SCOPONE"))
                    }
                    if (sameFamily) rooms.add(RoomInfo(code, hostName, game, ts))
                }
                onUpdate(rooms.sortedByDescending { it.timestamp })
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        db.child("open_rooms").addValueEventListener(openRoomsListener!!)
    }

    fun stopListeningForRooms() {
        openRoomsListener?.let { db.child("open_rooms").removeEventListener(it) }
        openRoomsListener = null
    }

    private fun listenForMessages(code: String, isServerSide: Boolean) {
        val channel = if (isServerSide) "c2s" else "s2c"
        val channelRef = db.child("rooms").child(code).child(channel)

        var initialized = false
        incomingListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                if (!initialized) { initialized = true; return }
                val msgJson = snapshot.getValue(String::class.java) ?: return
                try {
                    val msg = BtProtocol.decode(msgJson)
                    if (msg is BtMessage.Ping) return
                    scope.launch { _events.emit(ConnectionEvent.MessageReceived(msg)) }
                } catch (_: Exception) {}
            }
            override fun onChildChanged(s: DataSnapshot, p: String?) {}
            override fun onChildRemoved(s: DataSnapshot) {}
            override fun onChildMoved(s: DataSnapshot, p: String?) {}
            override fun onCancelled(e: DatabaseError) {
                scope.launch { _events.emit(ConnectionEvent.Lost) }
            }
        }
        channelRef.push().setValue("__init__")
        channelRef.addChildEventListener(incomingListener!!)
    }

    override fun send(msg: BtMessage) {
        val code = roomCode ?: return
        val channel = if (isServer) "s2c" else "c2s"
        val msgJson = BtProtocol.encode(msg).trim()
        db.child("rooms").child(code).child(channel).push().setValue(msgJson)
    }

    override fun disconnect() {
        val code = roomCode ?: return
        incomingListener?.let {
            val channel = if (isServer) "c2s" else "s2c"
            db.child("rooms").child(code).child(channel).removeEventListener(it)
        }
        clientJoinListener?.let {
            db.child("rooms").child(code).removeEventListener(it)
        }
        hostPlayerName?.let { db.child("open_rooms").child(it).removeValue() }
        if (isServer) {
            db.child("rooms").child(code).removeValue()
        }
        stopListeningForRooms()
        roomCode = null
        isServer = false
        hostPlayerName = null
        incomingListener = null
        clientJoinListener = null
    }

    private fun generateCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { chars.random() }.joinToString("")
    }
}
