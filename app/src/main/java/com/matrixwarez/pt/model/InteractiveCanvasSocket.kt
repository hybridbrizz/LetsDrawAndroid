package com.matrixwarez.pt.model

import android.util.Log
import com.matrixwarez.pt.listener.SocketConnectCallback
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import io.socket.engineio.client.transports.WebSocket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InteractiveCanvasSocket {

    companion object {
        val instance = InteractiveCanvasSocket()
    }

    private var socket: Socket? = null

    var socketConnectCallback: SocketConnectCallback? = null

    fun startSocket(server: Server) {
        Log.i("Canvas Socket", "Connecting to socket... (${socketConnectCallback?.javaClass?.simpleName})")
        val opts = IO.Options()
        opts.transports = arrayOf(WebSocket.NAME)
        opts.reconnectionAttempts = 0
        //socket = TrustAllSSLCertsDebug.getAllCertsIOSocket()
        socket = IO.socket(server.canvasSocketUrl(), opts)

        socket?.connect()

        socket?.on(Socket.EVENT_CONNECT, Emitter.Listener {
            socket?.emit("connect2")

            socketConnectCallback?.onSocketConnect()
        })

        socket?.on(Socket.EVENT_CONNECT_ERROR) {
            //Log.i("Socket", "Socket connect error!")

            socketConnectCallback?.onSocketDisconnect(true)
            //socket?.disconnect()
        }

        socket?.on(Socket.EVENT_DISCONNECT) {
            Log.i("Socket", "Socket disconnected!")

            socketConnectCallback?.onSocketDisconnect(false)
        }
    }

    fun disconnect() {
        socket?.disconnect()
    }

    fun isConnected(): Boolean {
        return socket?.connected() ?: false
    }

    fun requireSocket(): Socket {
        return socket!!
    }
}