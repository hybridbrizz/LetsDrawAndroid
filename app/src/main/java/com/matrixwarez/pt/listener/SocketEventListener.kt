package com.matrixwarez.pt.listener

interface SocketEventListener {
    fun onSocketDisconnect()

    fun onSocketError()
}