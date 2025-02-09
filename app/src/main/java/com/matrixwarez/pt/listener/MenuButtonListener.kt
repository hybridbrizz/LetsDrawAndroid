package com.matrixwarez.pt.listener

import com.matrixwarez.pt.model.Server


interface MenuButtonListener {
    fun onMenuButtonSelected(index: Int, route: Int = -1)
    fun onServerSelected(server: Server)
}