package com.matrixwarez.pt.listener

interface SocketConnectCallback {
    fun onSocketConnect()
    fun onSocketDisconnect(error: Boolean)
}