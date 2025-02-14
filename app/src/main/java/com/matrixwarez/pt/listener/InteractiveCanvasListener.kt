package com.matrixwarez.pt.listener

interface InteractiveCanvasListener {
    fun notifyPixelsReady()

    fun notifyPaintColorUpdate(color: Int)

    fun notifyPaintActionStarted()

    fun notifyPaintingStarted()
    fun notifyPaintingEnded()

    fun isPaletteFragmentOpen(): Boolean
    fun notifyClosePaletteFragment()

    fun notifyDeviceViewportUpdate()

    fun notifyUpdateCanvasSummary()
    fun onDeviceViewportUpdate()

    fun notifySocketLatency(ms: String)
}