package com.matrixwarez.pt.colorpicker

import android.graphics.Color

class PickedColorValues(var h: Float = 0f, var s: Float = 0f, var b: Float = 0f) {
    val minValue = 0f
    val maxValue = 1f

    fun toColor(): Int {
        return Color.HSVToColor(floatArrayOf(h * 360f, s, b))
    }

    fun setFromColor(color: Int) {
        val hsbValues = FloatArray(3)
        Color.colorToHSV(color, hsbValues)

        h = hsbValues[0] / 360f
        s = hsbValues[1]
        b = hsbValues[2]
    }

    companion object {
        fun fromColor(color: Int): PickedColorValues {
            val hsbValues = FloatArray(3)
            Color.colorToHSV(color, hsbValues)

            return PickedColorValues(
                h = hsbValues[0] / 360f,
                s = hsbValues[1],
                b = hsbValues[2]
            )
        }
    }
}