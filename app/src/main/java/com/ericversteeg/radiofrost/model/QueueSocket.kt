package com.ericversteeg.radiofrost.model

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import io.socket.engineio.client.transports.WebSocket

class QueueSocket {

    companion object {
        val instance = QueueSocket()

        const val interval = 10
    }

    interface SocketListener {
        fun onQueueConnect()
        fun onQueueConnectError()
        fun onAddedToQueue(pos: Int)
        fun onServiceReady()
    }

    var socket: Socket? = null

    var socketListener: SocketListener? = null

    var gson = Gson()

    fun startSocket(server: Server) {
        val opts = IO.Options()
        opts.transports = arrayOf(WebSocket.NAME)
        opts.reconnectionAttempts = 0
        //socket = TrustAllSSLCertsDebug.getAllCertsIOSocket()
        socket = IO.socket(server.queueSocketUrl(), opts)

        socket?.connect()

        socket?.on(Socket.EVENT_CONNECT, Emitter.Listener {
            Log.i("Queue Socket", "Queue connected!")
            socket?.emit("add_to_queue", socket?.id())
            socketListener?.onQueueConnect()
        })

        socket?.on(Socket.EVENT_CONNECT_ERROR) {
            Log.i("Queue Socket", "Queue connect error!")
            socket?.disconnect()
            socketListener?.onQueueConnectError()
        }

        socket?.on("added_to_queue") {
            Log.i("Queue Socket", "Added to queue!")
            socketListener?.onAddedToQueue(it[0].toString().toInt())
        }

        socket?.on("service_ready") {
            Log.i("Queue Socket", "Service ready!")

            val jsonStr = it[0].toString()
            val jsonObj = gson.fromJson(jsonStr, JsonObject::class.java)

            if (jsonObj == null || !jsonObj.has("start") || !jsonObj.get("start").asBoolean) {
                return@on
            }

            socketListener?.onServiceReady()
        }

        socket?.on(Socket.EVENT_DISCONNECT) {
            Log.i("Socket", "Queue disconnected!")
        }
    }

    fun isConnected(): Boolean {
        return socket?.connected() ?: false
    }
}