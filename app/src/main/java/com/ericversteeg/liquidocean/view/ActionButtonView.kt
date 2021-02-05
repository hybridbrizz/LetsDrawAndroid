package com.ericversteeg.liquidocean.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import androidx.annotation.RequiresApi
import com.ericversteeg.liquidocean.helper.SessionSettings
import com.ericversteeg.liquidocean.listener.PaintQtyListener

class ActionButtonView: View {

    var rows = 0
    set(value) {
        field = value
        pxHeight = height / value
    }

    var cols = 0
        set(value) {
            field = value
            pxWidth = width / value
        }

    val greenPaint = Paint()
    val lightGreenPaint = Paint()
    val whitePaint = Paint()
    val redPaint = Paint()
    val yellowPaint = Paint()
    val lightYellowPaint = Paint()
    val lightRedPaint = Paint()
    val linePaint = Paint()

    enum class Type {
        NONE,
        BACK,
        YES,
        NO
    }

    enum class TouchState {
        ACTIVE,
        INACTIVE
    }

    enum class ColorMode {
        COLOR,
        NONE
    }

    var type = Type.NONE
    var touchState = TouchState.INACTIVE

    var colorMode = ColorMode.NONE
    set(value) {
        field = value
        invalidate()
    }

    var pxWidth = 0
    var pxHeight = 0

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

    @RequiresApi(21)
    constructor(context: Context, attributeSet: AttributeSet, v0: Int, v1: Int) : super(
        context,
        attributeSet,
        v0,
        v1
    ) {
        commonInit()
    }

    private fun commonInit() {
        greenPaint.color = Color.parseColor("#05AD2E")
        whitePaint.color = Color.WHITE
        yellowPaint.color = Color.parseColor("#FAD452")
        redPaint.color = Color.parseColor("#FA3A47")

        lightGreenPaint.color = Color.parseColor("#62AD6C")
        lightYellowPaint.color = Color.parseColor("#FAE38D")
        lightRedPaint.color = Color.parseColor("#FB7E87")

        linePaint.color = Color.WHITE
        linePaint.strokeWidth = 1F
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas?.apply {
            save()

            if (type == Type.BACK) {
                drawBackAction(touchState == TouchState.ACTIVE, canvas)
            }
            else if (type == Type.YES) {
                drawYesAction(touchState == TouchState.ACTIVE, colorMode == ColorMode.COLOR, canvas)
            }
            else if (type == Type.NO) {
                drawNoAction(touchState == TouchState.ACTIVE,colorMode == ColorMode.COLOR, canvas)
            }

            restore()
        }
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            touchState = TouchState.ACTIVE

        }
        else if(ev.action == MotionEvent.ACTION_UP) {
            touchState = TouchState.INACTIVE
        }

        invalidate()

        return super.onTouchEvent(ev)
    }

    private fun drawBackAction(light: Boolean, canvas: Canvas) {
        rows = 5
        cols = 7

        var paint = yellowPaint
        if (light) {
            paint = lightYellowPaint
        }

        canvas.apply {
            // row 1
            drawPixel(4, 0, paint, canvas)

            // row 2
            drawPixel(5, 1, paint, canvas)

            // row 3
            drawPixel(0, 2, paint, canvas)
            drawPixel(1, 2, paint, canvas)
            drawPixel(2, 2, paint, canvas)
            drawPixel(3, 2, paint, canvas)
            drawPixel(4, 2, paint, canvas)
            drawPixel(6, 2, paint, canvas)

            // row 4
            drawPixel(5, 3, paint, canvas)

            // row 5
            drawPixel(4, 4, paint, canvas)
        }
    }

    private fun drawYesAction(light: Boolean, color: Boolean, canvas: Canvas) {
        rows = 5
        cols = 7

        var paint = greenPaint
        if (light) {
            paint = lightGreenPaint
        }

        if (!color) {
            paint = whitePaint

            val outValue = TypedValue()
            context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
            setBackgroundResource(outValue.resourceId)
        }

        canvas.apply {
            // row 1
            drawPixel(6, 0, paint, canvas)

            // row 2
            drawPixel(5, 1, paint, canvas)

            // row 3
            drawPixel(0, 2, paint, canvas)
            drawPixel(4, 2, paint, canvas)

            // row 4
            drawPixel(1, 3, paint, canvas)
            drawPixel(3, 3, paint, canvas)

            // row 5
            drawPixel(2, 4, paint, canvas)
        }
    }

    private fun drawNoAction(light: Boolean, color: Boolean, canvas: Canvas) {
        rows = 5
        cols = 5

        var paint = redPaint
        if (light) {
            paint = lightRedPaint
        }

        if (!color) {
            paint = whitePaint

            val outValue = TypedValue()
            context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
            setBackgroundResource(outValue.resourceId)
        }

        canvas.apply {
            // row 1
            drawPixel(0, 0, paint, canvas)
            drawPixel(4, 0, paint, canvas)

            // row 2
            drawPixel(1, 1, paint, canvas)
            drawPixel(3, 1, paint, canvas)

            // row 3
            drawPixel(2, 2, paint, canvas)

            // row 4
            drawPixel(1, 3, paint, canvas)
            drawPixel(3, 3, paint, canvas)

            // row 5
            drawPixel(0, 4, paint, canvas)
            drawPixel(4, 4, paint, canvas)
        }
    }

    private fun rectForPixel(x: Int, y: Int): RectF {
        val top = y * pxHeight
        val left = x * pxWidth

        return RectF(left.toFloat(), top.toFloat(), (left + pxWidth).toFloat(), (top + pxHeight).toFloat())
    }

    private fun drawPixel(x: Int, y: Int, paint: Paint, canvas: Canvas) {
        canvas.drawRect(rectForPixel(x, y), paint)
    }
}