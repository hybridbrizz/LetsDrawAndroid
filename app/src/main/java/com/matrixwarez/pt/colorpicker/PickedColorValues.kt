package com.matrixwarez.pt.colorpicker

import android.graphics.Color

class PickedColorValues(var h: Float = 0f, var s: Float = 0f, var b: Float = 0f) {
    val minValue = 0f
    val maxValue = 1f

    fun toColor(): Int {
        return Color.HSVToColor(floatArrayOf(h * 360f, s, b))
    }
}