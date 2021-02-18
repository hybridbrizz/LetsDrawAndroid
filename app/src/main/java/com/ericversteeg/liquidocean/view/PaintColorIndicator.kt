package com.ericversteeg.liquidocean.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.annotation.RequiresApi
import com.ericversteeg.liquidocean.helper.Utils
import com.ericversteeg.liquidocean.listener.PaintSelectionListener
import com.ericversteeg.liquidocean.model.SessionSettings

class PaintColorIndicator : View {

    var paintSelectionListeners: MutableList<PaintSelectionListener> = ArrayList()

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

        paintSelected(color)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas?.let {
            it.save()
            val paint = Paint()
            paint.style = Paint.Style.STROKE
            if (SessionSettings.instance.colorIndicatorFill) {
                paint.style = Paint.Style.FILL_AND_STROKE
            }

            paint.strokeWidth = SessionSettings.instance.colorIndicatorWidth
            paint.color = SessionSettings.instance.paintColor

            val borderPaint = Paint()
            borderPaint.style = Paint.Style.STROKE
            borderPaint.strokeWidth = 1F
            borderPaint.color = Color.WHITE

            // circle
            it.drawCircle(width / 2F, height / 2F, width / 3F, paint)

            if (Utils.isColorDark(paint.color)) {

            }

            if (SessionSettings.instance.colorIndicatorOutline) {
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

    fun paintSelected(color: Int) {
        for (listener in paintSelectionListeners) {
            listener.onPaintSelected(color)
        }
    }
}