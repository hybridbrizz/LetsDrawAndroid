package com.ericversteeg.liquidocean.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.annotation.RequiresApi
import com.ericversteeg.liquidocean.helper.PanelThemeConfig
import com.ericversteeg.liquidocean.helper.Utils
import com.ericversteeg.liquidocean.listener.PaintSelectionListener
import com.ericversteeg.liquidocean.model.SessionSettings

class PaintColorIndicator : View {

    var paintSelectionListeners: MutableList<PaintSelectionListener> = ArrayList()

    lateinit var panelThemeConfig: PanelThemeConfig

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
            if (SessionSettings.instance.colorIndicatorFill || SessionSettings.instance.colorIndicatorSquare) {
                paint.style = Paint.Style.FILL_AND_STROKE
            }

            paint.strokeWidth = ringSizeFromOption(context, SessionSettings.instance.colorIndicatorWidth).toFloat()
            paint.color = SessionSettings.instance.paintColor

            val borderPaint = Paint()
            borderPaint.style = Paint.Style.STROKE
            borderPaint.strokeWidth = 1F

            borderPaint.color = panelThemeConfig.paintColorIndicatorLineColor

            if (SessionSettings.instance.colorIndicatorSquare) {
                val w = squareSizeFromOption(context, SessionSettings.instance.colorIndicatorWidth)
                it.drawRect((width / 2 - w / 2).toFloat(), (height / 2 - w / 2).toFloat(),
                    (width / 2 + w / 2).toFloat(), (height / 2 + w / 2).toFloat(), paint)

                paint.strokeWidth = 1F
            }
            else if (SessionSettings.instance.colorIndicatorFill) {
                // circle
                val w = circleSizeFromOption(context, SessionSettings.instance.colorIndicatorWidth)
                it.drawCircle(width / 2F, height / 2F, w.toFloat(), paint)

                paint.strokeWidth = 1F
            }
            else {
                var radius = (width / 3F)

                val w = SessionSettings.instance.colorIndicatorWidth
                if (w == 4) {
                    radius = width * 0.38F
                }
                else if (w == 5) {
                    radius = width * 0.43F
                }

                it.drawCircle(width / 2F, height / 2F, radius, paint)
            }

            if (SessionSettings.instance.colorIndicatorOutline) {
                var radius = (width / 3F)

                val w = SessionSettings.instance.colorIndicatorWidth
                if (!SessionSettings.instance.colorIndicatorFill && !SessionSettings.instance.colorIndicatorSquare && w > 3) {

                    if (w == 4) {
                        radius = width * 0.38F
                    }
                    else if (w == 5) {
                        radius = width * 0.43F
                    }
                }
                // inner border
                it.drawCircle(
                    width / 2F,
                    height / 2F,
                    radius - (paint.strokeWidth / 2 + borderPaint.strokeWidth / 2),
                    borderPaint
                )

                // outer border
                it.drawCircle(
                    width / 2F,
                    height / 2F,
                    radius + (paint.strokeWidth / 2 + borderPaint.strokeWidth / 2),
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

    fun ringSizeFromOption(context: Context, widthVal: Int): Int {
        return when (widthVal) {
            1 -> Utils.dpToPx(context, 12)
            2 -> Utils.dpToPx(context, 14)
            3 -> Utils.dpToPx(context, 16)
            4 -> Utils.dpToPx(context, 16)
            5 -> Utils.dpToPx(context, 16)
            else -> Utils.dpToPx(context, 12)
        }
    }

    private fun circleSizeFromOption(context: Context, widthVal: Int): Int {
        return when (widthVal) {
            1 -> (width * 0.3).toInt()
            2 -> (width * 0.35).toInt()
            3 -> (width * 0.4).toInt()
            4 -> (width * 0.45).toInt()
            5 -> (width * 0.5).toInt()
            else -> (width * 0.2).toInt()
        }
    }

    private fun squareSizeFromOption(context: Context, widthVal: Int): Int {
        return when (widthVal) {
            1 -> (width * 0.6).toInt()
            2 -> (width * 0.7).toInt()
            3 -> (width * 0.8).toInt()
            4 -> (width * 0.9).toInt()
            5 -> width
            else -> (width * 0.4).toInt()
        }
    }
}