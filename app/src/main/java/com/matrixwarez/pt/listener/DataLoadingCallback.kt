package com.matrixwarez.pt.listener

import com.matrixwarez.pt.model.Server

interface DataLoadingCallback {
    fun onDataLoaded(world: Boolean, realmId: Int)
    fun onDataLoaded(server: Server)
    fun onConnectionError()
}