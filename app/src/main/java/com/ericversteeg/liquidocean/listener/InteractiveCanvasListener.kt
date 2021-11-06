package com.ericversteeg.liquidocean.listener

interface InteractiveCanvasListener {
    fun notifyRedraw()

    fun notifyPaintColorUpdate(color: Int)

    fun notifyPaintActionStarted()

    fun notifyPaintingStarted()
    fun notifyPaintingEnded()

    fun isPaletteFragmentOpen(): Boolean
    fun notifyClosePaletteFragment()
}