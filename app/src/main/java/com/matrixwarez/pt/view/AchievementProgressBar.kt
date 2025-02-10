package com.matrixwarez.pt.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.annotation.RequiresApi
import com.matrixwarez.pt.listener.PaintActionListener
import com.matrixwarez.pt.listener.PaintBarActionListener
import com.matrixwarez.pt.model.SessionSettings
import com.matrixwarez.pt.listener.PaintQtyListener
import java.util.*

class AchievementProgressBar: View, PaintQtyListener, PaintActionListener {

    val rows = 3
    val cols = 13

    val greenPaint = Paint()
    val whitePaint = Paint()
    val bluePaint = Paint()
    val brownPaint = Paint()
    val lightGrayPaint = Paint()
    val linePaint = Paint()

    var world = true

    var flashingError = false

    var firstClickTime = 0L

    var actionListener: PaintBarActionListener? = null

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
        greenPaint.color = Color.parseColor("#42ff7b")
        whitePaint.color = Color.WHITE
        bluePaint.color = Color.parseColor("#84baff")
        brownPaint.color = Color.parseColor("#633d21")
        lightGrayPaint.color = Color.parseColor("#e6ebe6")

        linePaint.color = Color.WHITE
        linePaint.strokeWidth = 1F
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event?.action == MotionEvent.ACTION_DOWN) {
            /* if (firstClickTime == 0L) {
                firstClickTime = System.currentTimeMillis()
            }
            else {
                if (System.currentTimeMillis() - firstClickTime < 500L) {
                    actionListener?.onPaintBarDoubleTapped()
                }

                firstClickTime = 0L
            } */

            actionListener?.onPaintBarDoubleTapped()
        }

        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.apply {
            save()

            drawPixelBorder(this)

            /*drawRect(rectForPixel(0, 1), greenPaint)
            drawRect(rectForPixel(1, 1), brownPaint)
            drawRect(rectForPixel(2, 1), brownPaint)
            drawRect(rectForPixel(3, 1), brownPaint)*/

            restore()
        }
    }

    private fun rectForPixel(x: Int, y: Int): RectF {
        val pxWidth = (width / cols)
        val pxHeight = (height / rows)

        val top = y * pxHeight
        val left = x * pxWidth

        return RectF(left.toFloat(), top.toFloat(), (left + pxWidth).toFloat(), (top + pxHeight).toFloat())
    }

    private fun drawPixelBorder(canvas: Canvas) {
        canvas.apply {
            // top and bottom
            for (x in 1 until cols - 1) {
                drawRect(rectForPixel(x, 0), whitePaint)
            }

            for (x in 1 until cols - 1) {
                drawRect(rectForPixel(x, 2), whitePaint)
            }

            // decor
            drawRect(rectForPixel(6, 0), lightGrayPaint)
            drawRect(rectForPixel(2, 2), lightGrayPaint)
            drawRect(rectForPixel(8, 2), lightGrayPaint)

            // ends
            drawRect(rectForPixel(0, 1), greenPaint)
            drawRect(rectForPixel(cols - 1, 1), whitePaint)

            // quantity
            drawQuantity(this)
        }
    }

    private fun drawQuantity(canvas: Canvas) {
        val pxWidth = (width / cols)
        val pxHeight = (height / rows)

        var relQty = SessionSettings.instance.dropsAmt / SessionSettings.instance.maxPaintAmt.toFloat()

        if (!world) {
            relQty = 1F
        }

        val numPixels = cols - 2
        val qtyPer = 1F / numPixels
        var curProg = 0F

        canvas.apply {
            for (x in 1 until cols - 1) {
                if (relQty > curProg) {
                    if (world) {
                        drawRect(rectForPixel((cols - 1) - x, 1), bluePaint)
                    }
                    else {
                        drawRect(rectForPixel((cols - 1) - x, 1), ActionButtonView.twoThirdGray)
                    }
                }
                else {
                    if (flashingError) {
                        drawRect(rectForPixel((cols - 1) - x, 1), ActionButtonView.redPaint)
                    }
                    else {
                        drawRect(rectForPixel((cols - 1) - x, 1), brownPaint)
                    }
                }

                if (x < cols - 2) {
                    drawLine(((cols - 1 - x) * pxWidth).toFloat(), pxHeight.toFloat(), ((cols - 1 - x) * pxWidth).toFloat(), (pxHeight * 2).toFloat(), linePaint)
                }

                curProg += qtyPer
            }
        }
    }

    private fun flashError() {
        flashingError = true
        invalidate()
        Timer().schedule(object: TimerTask() {
            override fun run() {
                flashingError = false
                invalidate()
            }

        }, 1500)
    }

    override fun paintQtyChanged(qty: Int) {
        invalidate()
    }

    override fun onPaintStart() {
        if (SessionSettings.instance.dropsAmt == 0) {
            flashError()
        }
    }
}