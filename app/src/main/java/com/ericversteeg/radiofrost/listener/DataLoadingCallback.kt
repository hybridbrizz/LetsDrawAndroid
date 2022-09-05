package com.ericversteeg.radiofrost.listener

import com.ericversteeg.radiofrost.model.Server

interface DataLoadingCallback {
    fun onDataLoaded(world: Boolean, realmId: Int)
    fun onDataLoaded(server: Server)
    fun onConnectionError()
}