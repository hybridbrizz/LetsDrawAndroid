package com.ericversteeg.liquidocean.listener

import com.ericversteeg.liquidocean.model.Server

interface DataLoadingCallback {
    fun onDataLoaded(world: Boolean, realmId: Int)
    fun onDataLoaded(server: Server)
    fun onConnectionError()
}