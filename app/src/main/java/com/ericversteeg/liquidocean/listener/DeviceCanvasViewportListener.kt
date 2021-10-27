package com.ericversteeg.liquidocean.listener

import android.graphics.RectF

interface DeviceCanvasViewportListener {
    fun onDeviceViewportUpdate(viewport: RectF)
}