package com.ericversteeg.radiofrost.colorpicker

import android.graphics.Color

object ColorUtility {

    private var colorValues = FloatArray(3)

    fun colorFromH(hue: Float): Int {
        colorValues[0] = hue
        colorValues[1] = 1.0F
        colorValues[2] = 1.0F

        return Color.HSVToColor(colorValues)
    }

    fun colorFromHSB(hue: Float, saturation: Float, brightness: Float): Int {
        colorValues[0] = hue
        colorValues[1] = saturation
        colorValues[2] = brightness

        return Color.HSVToColor(colorValues)
    }

    fun getAndroidBitmapFormatRGBA8888(color: Int): Int {
        val a = (color shr 24) and 255
        val r = (color shr 16) and 255
        val g = (color shr 8) and 255
        val b = color and 255

        return (a shl 24) or (b shl 16) or (g shl 8) or r
    }
}