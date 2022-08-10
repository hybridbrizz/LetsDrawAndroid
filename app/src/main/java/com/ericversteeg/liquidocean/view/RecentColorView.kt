package com.ericversteeg.liquidocean.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.util.AttributeSet
import android.view.View
import androidx.annotation.RequiresApi

class RecentColorView: View {

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context): super(context) {

    }

    constructor(context: Context, attributeSet: AttributeSet): super(context, attributeSet) {

    }

    var color: Int? = null

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas?.apply {
            val colorPaint = Paint()
            if (color != null) {
                colorPaint.color = color!!
            }
            else {
                colorPaint.color = Color.WHITE
            }
            drawRect(0F, 0F, width.toFloat(), height.toFloat(), colorPaint)
            val borderPaint = Paint()
            borderPaint.strokeWidth = 1F
            borderPaint.color = Color.parseColor("#333333")
            drawLine(0F, 0F, width.toFloat(), 0F, borderPaint)
            drawLine(0F, height.toFloat(), width.toFloat(), height.toFloat(), borderPaint)
            drawLine(0F, 0F, 0F, height.toFloat(), borderPaint)
            drawLine(width.toFloat(), 0F, width.toFloat(), height.toFloat(), borderPaint)
        }
    }
}