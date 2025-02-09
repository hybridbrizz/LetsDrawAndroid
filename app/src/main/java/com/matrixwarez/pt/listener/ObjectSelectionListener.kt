package com.matrixwarez.pt.listener

import android.graphics.PointF

interface ObjectSelectionListener {
    fun onObjectSelectionBoundsChanged(upperLeft: PointF, lowerRight: PointF)
    fun onObjectSelectionEnded()
}