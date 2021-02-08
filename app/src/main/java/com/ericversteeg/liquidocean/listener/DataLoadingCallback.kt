package com.ericversteeg.liquidocean.listener

import com.ericversteeg.liquidocean.view.ActionButtonView

interface DataLoadingCallback {
    fun onDataLoaded(world: Boolean)
}