package com.ericversteeg.liquidocean.listener

interface SocketConnectCallback {
    fun onSocketConnect()
    fun onSocketConnectError()
    fun onSocketDisconnect()
}