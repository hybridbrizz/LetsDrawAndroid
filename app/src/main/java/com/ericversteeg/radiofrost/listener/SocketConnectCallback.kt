package com.ericversteeg.radiofrost.listener

interface SocketConnectCallback {
    fun onSocketConnect()
    fun onSocketDisconnect(error: Boolean)
}