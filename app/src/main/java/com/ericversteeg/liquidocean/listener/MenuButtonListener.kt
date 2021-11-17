package com.ericversteeg.liquidocean.listener


interface MenuButtonListener {
    fun onMenuButtonSelected(index: Int, route: Int = -1)
}