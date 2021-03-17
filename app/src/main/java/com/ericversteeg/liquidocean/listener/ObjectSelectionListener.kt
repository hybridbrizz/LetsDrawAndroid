package com.ericversteeg.liquidocean.listener

import android.graphics.Point
import android.graphics.PointF

interface ObjectSelectionListener {
    fun onObjectSelectionBoundsChanged(upperLeft: PointF, lowerRight: PointF)
    fun onObjectSelectionEnded()
}