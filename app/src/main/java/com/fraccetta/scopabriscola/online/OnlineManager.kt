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

class OnlineManager : GameTransport {

    private val db = Firebase.database.reference
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _events = MutableSharedFlow<ConnectionEvent>(replay = 0, extraBufferCapacity = 64)
    override val events: Flow<ConnectionEvent> = _events

    private var roomCode: String? = null
    private var isServer: Boolean = false
    private var incomingListener: ChildEventListener? = null
    private var clientJoinListener: ValueEventListener? = null

    // Host: creates room, returns code, waits for client to join
    fun startHosting(gameType: String, hostName: String): String {
        val code = generateCode()
        roomCode = code
        isServer = true

        val roomRef = db.child("rooms").child(code)
        roomRef.child("meta").setValue(
            mapOf("game" to gameType, "hostName" to hostName, "state" to "WAITING")
        )

        // Listen for client joining
        clientJoinListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val clientName = snapshot.child("meta").child("clientName").getValue(String::class.java)
                if (clientName != null) {
                    roomRef.removeEventListener(this)
                    clientJoinListener = null
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

    // Client: joins an existing room by code
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

    private fun listenForMessages(code: String, isServerSide: Boolean) {
        // Server listens on c2s, client listens on s2c
        val channel = if (isServerSide) "c2s" else "s2c"
        val channelRef = db.child("rooms").child(code).child(channel)

        // skip existing messages — only listen to newly added
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
        // Add a sentinel so the first real message is not skipped
        channelRef.push().setValue("__init__")
        channelRef.addChildEventListener(incomingListener!!)
    }

    override fun send(msg: BtMessage) {
        val code = roomCode ?: return
        // Server sends on s2c, client sends on c2s
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
        if (isServer) {
            db.child("rooms").child(code).removeValue()
        }
        roomCode = null
        incomingListener = null
        clientJoinListener = null
    }

    private fun generateCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { chars.random() }.joinToString("")
    }
}
