package com.ericversteeg.liquidocean.listener

interface InteractiveCanvasDrawerCallback {
    fun notifyRedraw()

    fun notifyPaintColorUpdate(color: Int)
    fun notifyPaintQtyUpdate(qty: Int)
}