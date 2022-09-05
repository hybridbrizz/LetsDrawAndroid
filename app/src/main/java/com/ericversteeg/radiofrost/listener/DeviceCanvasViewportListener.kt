package com.ericversteeg.radiofrost.listener

import android.graphics.RectF

interface DeviceCanvasViewportListener {
    fun onDeviceViewportUpdate(viewport: RectF)
}