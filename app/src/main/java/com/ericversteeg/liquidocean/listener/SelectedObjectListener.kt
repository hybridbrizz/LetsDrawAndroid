package com.ericversteeg.liquidocean.listener

interface SelectedObjectListener {
    fun onObjectSelected()

    fun onSelectedObjectMoveStart()
    fun onSelectedObjectMoved()
    fun onSelectedObjectMoveEnd()
}