package com.ericversteeg.liquidocean.listener

interface SocketEventListener {
    fun onSocketDisconnect()

    fun onSocketError()
}