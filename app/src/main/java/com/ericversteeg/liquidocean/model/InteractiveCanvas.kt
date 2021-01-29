package com.ericversteeg.liquidocean.model

import android.content.Context
import android.graphics.Color
import android.graphics.RectF
import android.util.DisplayMetrics
import android.view.WindowManager
import com.ericversteeg.liquidocean.listener.InteractiveCanvasDrawerCallback

class InteractiveCanvas {
    val rows = 2048
    val cols = 2048

    var arr =  arrayOfNulls<Array<Int>>(rows)

    val basePpu = 100
    var ppu = basePpu

    var deviceViewport: RectF? = null

    var drawCallbackListener: InteractiveCanvasDrawerCallback? = null

    init {
        for (i in 0 until rows - 1) {
            var row = arrayOfNulls<Int>(cols)
            for (j in 0 until cols - 1) {
                var color = Color.parseColor("#FF333333")
                if ((i + j) % 2 == 0) {
                    color = Color.parseColor("#FF666666")
                }
                row[j] = color
            }
        }
    }

    fun updateDeviceViewport(context: Context, canvasCenterX: Float, canvasCenterY: Float) {
        val displayMetrics = DisplayMetrics()
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        windowManager.defaultDisplay.getMetrics(displayMetrics)

        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        val canvasCenterXPx = (canvasCenterX * ppu).toInt()
        val canvasCenterYPx = (canvasCenterY * ppu).toInt()

        val top = (canvasCenterYPx - screenHeight / 2) / ppu.toFloat()
        val bottom = (canvasCenterYPx + screenHeight / 2) / ppu.toFloat()
        val left = (canvasCenterXPx - screenWidth / 2) / ppu.toFloat()
        val right = (canvasCenterXPx + screenWidth / 2) / ppu.toFloat()

        deviceViewport = RectF(left, top, right, bottom)
    }

    fun updateDeviceViewport(context: Context) {
        deviceViewport?.apply {
            updateDeviceViewport(context, (left + right) / 2, (top + bottom) / 2)
        }
    }

    fun translateBy(x: Float, y: Float) {
        deviceViewport?.apply {
            var dX = x / ppu
            var dY = y / ppu

            if (left + dX < 0) {
                val diff = left
                dX = diff
            }

            if (right + dX > cols) {
                val diff = cols - right
                dX = diff
            }

            if (top + dY < 0) {
                val diff = top
                dY = diff
            }

            if (bottom + dY > rows) {
                val diff = rows - bottom
                dY = diff
            }

            left += dX
            right += dX
            top += dY
            bottom += dY
        }

        drawCallbackListener?.notifyRedraw()
    }
}