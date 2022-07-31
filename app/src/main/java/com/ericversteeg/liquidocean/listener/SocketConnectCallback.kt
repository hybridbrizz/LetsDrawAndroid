package com.ericversteeg.liquidocean.listener

interface SocketConnectCallback {
    fun onSocketConnect()
    fun onSocketDisconnect(error: Boolean)
}