package com.ericversteeg.liquidocean.listener

import com.ericversteeg.liquidocean.view.ActionButtonView

interface MenuButtonListener {
    fun onMenuButtonSelected(index: Int, route: Int = -1)

    fun onSingleBackgroundOptionSelected(type: ActionButtonView.Type)
}