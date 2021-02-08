package com.ericversteeg.liquidocean.listener

import com.ericversteeg.liquidocean.view.ActionButtonView

interface MenuButtonListener {
    fun onMenuButtonSelected(index: Int)

    fun onSingleBackgroundOptionSelected(type: ActionButtonView.Type)
}