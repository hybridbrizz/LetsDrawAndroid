package com.matrixwarez.pt.model

import android.graphics.Color
import androidx.core.graphics.ColorUtils

class ErrorPixel(val x: Int, val y: Int, private val duration: Float, val startColor: Int = Color.RED) {
    var isActive = true
    private val startTime = System.currentTimeMillis()

    fun getColor(): Int {
        val percent =  (System.currentTimeMillis() - startTime) / 1000f / duration
        if (percent >= 1f) {
            isActive = false
            return Color.TRANSPARENT
        }

        val opacity = ((1f - percent) * 255).toInt()
        return ColorUtils.setAlphaComponent(startColor, opacity)
    }
}