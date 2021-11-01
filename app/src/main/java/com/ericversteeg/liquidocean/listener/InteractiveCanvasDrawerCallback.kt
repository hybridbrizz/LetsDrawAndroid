package com.ericversteeg.liquidocean.listener

interface InteractiveCanvasDrawerCallback {
    fun notifyRedraw()

    fun notifyPaintColorUpdate(color: Int)

    fun notifyPaintActionStarted()
    fun notifyClosePaletteFragment()

    fun notifyPaintingStarted()
    fun notifyPaintingEnded()

    fun isPaletteFragmentOpen(): Boolean
}