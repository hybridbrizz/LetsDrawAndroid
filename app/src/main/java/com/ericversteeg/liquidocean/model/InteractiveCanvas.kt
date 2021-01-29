package com.ericversteeg.liquidocean.model

import android.graphics.Color
import android.graphics.RectF
import com.ericversteeg.liquidocean.listener.InteractiveCanvasDrawerCallback

class InteractiveCanvas {
    val rows = 2048
    val cols = 2048

    var arr =  arrayOfNulls<Array<Int>>(rows)

    var ppu = 100

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