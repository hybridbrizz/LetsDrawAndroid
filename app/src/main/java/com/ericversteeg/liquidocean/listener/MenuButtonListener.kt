package com.ericversteeg.liquidocean.listener

import com.ericversteeg.liquidocean.model.Server


interface MenuButtonListener {
    fun onMenuButtonSelected(index: Int, route: Int = -1)
    fun onServerSelected(server: Server)
}