package com.ericversteeg.radiofrost.listener

interface SocketEventListener {
    fun onSocketDisconnect()

    fun onSocketError()
}