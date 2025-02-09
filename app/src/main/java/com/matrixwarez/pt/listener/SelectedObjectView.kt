package com.matrixwarez.pt.listener

import android.graphics.Point

interface SelectedObjectView {
    fun showSelectedObjectYesAndNoButtons(screenPoint: Point)
    fun hideSelectedObjectYesAndNoButtons()

    fun selectedObjectEnded()
}