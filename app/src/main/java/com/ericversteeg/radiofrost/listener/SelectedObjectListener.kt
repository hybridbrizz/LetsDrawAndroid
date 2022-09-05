package com.ericversteeg.radiofrost.listener

interface SelectedObjectListener {
    fun onObjectSelected()

    fun onSelectedObjectMoveStart()
    fun onSelectedObjectMoved()
    fun onSelectedObjectMoveEnd()
}