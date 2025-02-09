package com.matrixwarez.pt.listener

import android.graphics.Rect

interface SelectedObjectMoveView {
    fun showSelectedObjectMoveButtons(bounds: Rect)
    fun updateSelectedObjectMoveButtons(bounds: Rect)

    fun hideSelectedObjectMoveButtons()

    fun selectedObjectMoveEnded()
}