package com.matrixwarez.pt.listener

interface SelectedObjectListener {
    fun onObjectSelected()

    fun onSelectedObjectMoveStart()
    fun onSelectedObjectMoved()
    fun onSelectedObjectMoveEnd()
}