package com.matrixwarez.pt.listener

import android.graphics.RectF

interface DeviceCanvasViewportListener {
    fun onDeviceViewportUpdate(viewport: RectF)
}