package com.ericversteeg.liquidocean.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.annotation.RequiresApi
import com.ericversteeg.liquidocean.helper.SessionSettings
import com.ericversteeg.liquidocean.helper.Utils

class PaintColorIndicator : View {

    constructor(context: Context) : super(context) {
        commonInit()
    }

    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet) {
        commonInit()
    }

    constructor(context: Context, attributeSet: AttributeSet, v0: Int) : super(
        context,
        attributeSet,
        v0
    ) {
        commonInit()
    }

    @RequiresApi(21) constructor(context: Context, attributeSet: AttributeSet, v0: Int, v1: Int) : super(
        context,
        attributeSet,
        v0,
        v1
    ) {
        commonInit()
    }

    private fun commonInit() {

    }

    fun setPaintColor(color: Int) {
        SessionSettings.instance.paintColor = color
        invalidate()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas?.let {
            it.save()
            val paint = Paint()
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 25F
            paint.color = SessionSettings.instance.paintColor

            val borderPaint = Paint()
            borderPaint.style = Paint.Style.STROKE
            borderPaint.strokeWidth = 1F
            borderPaint.color = Color.WHITE

            // circle
            it.drawCircle(width / 2F, height / 2F, width / 3F, paint)

            if (Utils.isColorDark(paint.color)) {
                // inner border
                it.drawCircle(
                    width / 2F,
                    height / 2F,
                    (width / 3F) - (paint.strokeWidth / 2 + borderPaint.strokeWidth / 2),
                    borderPaint
                )

                // outer border
                it.drawCircle(
                    width / 2F,
                    height / 2F,
                    (width / 3F) + (paint.strokeWidth / 2 + borderPaint.strokeWidth / 2),
                    borderPaint
                )
            }

            it.restore()
        }
    }
}