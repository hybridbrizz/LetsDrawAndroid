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

    var socket: Socket? = null
    var checkEventTimeout = 20000L
    var checkStatusReceived = false

    var socketStatusCallback: SocketStatusCallback? = null
    var socketConnectCallback: SocketConnectCallback? = null

    fun startSocket(server: Server) {
        val opts = IO.Options()
        opts.transports = arrayOf(WebSocket.NAME)
        opts.reconnectionAttempts = 0
        //socket = TrustAllSSLCertsDebug.getAllCertsIOSocket()
        socket = IO.socket(server.canvasSocketUrl(), opts)

        socket?.connect()

        socket?.on(Socket.EVENT_CONNECT, Emitter.Listener {
            Log.i("Socket", "Socket connected!")

            socketConnectCallback?.onSocketConnect()
        })

        socket?.on(Socket.EVENT_CONNECT_ERROR) {
            Log.i("Socket", "Socket connect error!")

            socket?.disconnect()
            socketConnectCallback?.onSocketConnectError()
        }

        socket?.on(Socket.EVENT_DISCONNECT) {
            Log.i("Socket", "Socket disconnected!")
        }
    }

    fun isConnected(): Boolean {
        return socket?.connected() ?: false
    }

    fun checkSocketStatus() {
        socket?.emit("check_event")

        checkStatusReceived = false
        Timer().schedule(object : TimerTask() {
            override fun run() {
                if (!checkStatusReceived) {
                    socketStatusCallback?.onSocketStatusError()
                }
            }
        }, checkEventTimeout)
    }
}