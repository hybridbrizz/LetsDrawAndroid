package com.ericversteeg.liquidocean.model

import android.util.Log
import com.ericversteeg.liquidocean.helper.TrustAllSSLCertsDebug
import com.ericversteeg.liquidocean.helper.Utils
import com.ericversteeg.liquidocean.listener.SocketConnectCallback
import com.ericversteeg.liquidocean.listener.SocketStatusCallback
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import io.socket.engineio.client.transports.WebSocket
import java.util.*

class InteractiveCanvasSocket {

    companion object {
        val instance = InteractiveCanvasSocket()
    }

    private var socket: Socket? = null

    var socketConnectCallback: SocketConnectCallback? = null

    private var manualDisconnect = false

    fun startSocket(server: Server) {
        Log.i("Canvas Socket", "Connecting to socket... (${socketConnectCallback?.javaClass?.simpleName})")
        val opts = IO.Options()
        opts.transports = arrayOf(WebSocket.NAME)
        opts.reconnectionAttempts = 0
        //socket = TrustAllSSLCertsDebug.getAllCertsIOSocket()
        socket = IO.socket(server.canvasSocketUrl(), opts)

        socket?.connect()

        socket?.on(Socket.EVENT_CONNECT, Emitter.Listener {
            //Log.i("Socket", "Socket connected!")

            socketConnectCallback?.onSocketConnect()
        })

        socket?.on(Socket.EVENT_CONNECT_ERROR) {
            //Log.i("Socket", "Socket connect error!")

            socketConnectCallback?.onSocketDisconnect(true)
            socket?.disconnect()
        }

        socket?.on(Socket.EVENT_DISCONNECT) {
            Log.i("Socket", "Socket disconnected!")

            socketConnectCallback?.onSocketDisconnect(!manualDisconnect)

            manualDisconnect = false
        }
    }

    fun disconnect() {
        manualDisconnect = true
        socket?.disconnect()
    }

    fun isConnected(): Boolean {
        return socket?.connected() ?: false
    }

    fun requireSocket(): Socket {
        return socket!!
    }
}