package com.ericversteeg.radiofrost.listener

import com.ericversteeg.radiofrost.model.Server


interface MenuButtonListener {
    fun onMenuButtonSelected(index: Int, route: Int = -1)
    fun onServerSelected(server: Server)
}