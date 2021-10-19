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

class PaintColorIndicator : View, ActionButtonView.TouchStateListener {

    var paintSelectionListeners: MutableList<PaintSelectionListener> = ArrayList()

    lateinit var panelThemeConfig: PanelThemeConfig

    var topLayer = false
    set(value) {
        field = value

        alpha = 0F
    }

    var activeState = false

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

            if (topLayer) {
                if (SessionSettings.instance.colorIndicatorOutline) {
                    var paint = Paint()
                    paint.strokeWidth = 2F

                    if (!SessionSettings.instance.colorIndicatorSquare && !SessionSettings.instance.colorIndicatorFill) {
                        paint.strokeWidth = ringSizeFromOption(
                            context,
                            SessionSettings.instance.colorIndicatorWidth
                        ).toFloat()
                    }

                    val borderPaint = Paint()
                    borderPaint.style = Paint.Style.STROKE
                    borderPaint.strokeWidth = 2F

                    if (panelThemeConfig.paintColorIndicatorLineColor == ActionButtonView.blackPaint.color) {
                        borderPaint.color = ActionButtonView.twoThirdGray.color
                    }
                    else if (panelThemeConfig.paintColorIndicatorLineColor == ActionButtonView.whitePaint.color) {
                        borderPaint.color = ActionButtonView.thirdGray.color
                    }

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

                    if (paint.strokeWidth > 2) {
                        // inner border
                        it.drawCircle(
                            width / 2F,
                            height / 2F,
                            radius - (paint.strokeWidth / 2 + borderPaint.strokeWidth / 2),
                            borderPaint
                        )
                    }

                    // outer border
                    it.drawCircle(
                        width / 2F,
                        height / 2F,
                        radius + (paint.strokeWidth / 2 + borderPaint.strokeWidth / 2),
                        borderPaint
                    )
                }
            }
            else {
                val paint = Paint()
                paint.style = Paint.Style.STROKE
                if (SessionSettings.instance.colorIndicatorFill || SessionSettings.instance.colorIndicatorSquare) {
                    paint.style = Paint.Style.FILL_AND_STROKE
                }

                paint.strokeWidth = ringSizeFromOption(
                    context,
                    SessionSettings.instance.colorIndicatorWidth
                ).toFloat()
                paint.color = SessionSettings.instance.paintColor

                val darkColor = isColorDark(paint.color)
                val lightColor = isColorLight(paint.color)

                val borderPaint = Paint()
                borderPaint.style = Paint.Style.STROKE
                borderPaint.strokeWidth = 2F

                borderPaint.color = panelThemeConfig.paintColorIndicatorLineColor

                if (darkColor) {
                    borderPaint.color = Color.WHITE
                }
                else if (lightColor) {
                    borderPaint.color = Color.BLACK
                }

                if (SessionSettings.instance.colorIndicatorSquare) {
                    paint.strokeWidth = 2F

                    var padding = 0

                    if (paint.color == 0) {
                        paint.strokeWidth = 10F

                        paint.color = borderPaint.color
                        paint.style = Paint.Style.STROKE

                        padding = (paint.strokeWidth / 2).toInt()
                    }

                    val w = squareSizeFromOption(
                        context,
                        SessionSettings.instance.colorIndicatorWidth
                    )
                    it.drawRect(
                        (width / 2 - w / 2).toFloat() + padding,
                        (height / 2 - w / 2).toFloat() + padding,
                        (width / 2 + w / 2).toFloat() - padding,
                        (height / 2 + w / 2).toFloat() - padding,
                        paint
                    )
                }
                else if (SessionSettings.instance.colorIndicatorFill) {
                    paint.strokeWidth = 2F

                    var padding = 0

                    if (paint.color == 0) {
                        paint.strokeWidth = 10F

                        paint.color = borderPaint.color
                        paint.style = Paint.Style.STROKE

                        padding = (paint.strokeWidth / 2).toInt()
                    }

                    // circle
                    val w = circleSizeFromOption(
                        context,
                        SessionSettings.instance.colorIndicatorWidth
                    )
                    it.drawCircle(width / 2F, height / 2F, w.toFloat() - padding, paint)
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

                    var padding = 0

                    if (paint.color == 0) {
                        paint.strokeWidth = 10F

                        paint.color = borderPaint.color
                        paint.style = Paint.Style.STROKE

                        padding = (paint.strokeWidth / 2).toInt()
                    }

                    it.drawCircle(width / 2F, height / 2F, radius - padding, paint)
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

                    if (paint.strokeWidth > 10) {
                        // inner border
                        it.drawCircle(
                            width / 2F,
                            height / 2F,
                            radius - (paint.strokeWidth / 2 + borderPaint.strokeWidth / 2),
                            borderPaint
                        )
                    }

                    // outer border
                    it.drawCircle(
                        width / 2F,
                        height / 2F,
                        radius + (paint.strokeWidth / 2 + borderPaint.strokeWidth / 2),
                        borderPaint
                    )
                }
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
            5 -> (width * 0.49).toInt()
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

    override fun onTouchStateChanged(touchState: ActionButtonView.TouchState) {
        if (touchState == ActionButtonView.TouchState.ACTIVE) {
            activeState = true
            alpha = 0F
            animate().alphaBy(1F).setDuration(100).withEndAction {
                if (!activeState) {
                    alpha = 0F
                }
            }
        }
        else if (touchState == ActionButtonView.TouchState.INACTIVE) {
            activeState = false
            alpha = 0F
        }
    }

    companion object {
        fun isColorDark(color: Int): Boolean {
            val darkness =
                1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
            return (darkness > 0.85)
        }

        fun isColorLight(color: Int): Boolean {
            val darkness =
                1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
            return (darkness < 0.15)
        }
    }
}