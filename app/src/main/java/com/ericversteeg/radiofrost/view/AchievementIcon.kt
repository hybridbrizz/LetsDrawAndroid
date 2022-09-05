package com.ericversteeg.radiofrost.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.annotation.RequiresApi
import com.ericversteeg.radiofrost.model.StatTracker

class AchievementIcon: View {

    var single1 = intArrayOf(
        1, 1, 1, 1, 1,
        1, 1, 1, 0, 1,
        1, 1, 1, 1, 1,
        1, 1, 1, 1, 1,
        1, 1, 1, 1, 1
    )

    var single2 = intArrayOf(
        1, 1, 1, 1, 1,
        1, 1, 1, 0, 1,
        1, 1, 0, 1, 1,
        1, 1, 1, 1, 1,
        1, 1, 1, 1, 1
    )

    var single3 = intArrayOf(
        1, 1, 1, 1, 1,
        1, 1, 1, 0, 1,
        1, 1, 0, 1, 1,
        1, 1, 1, 0, 1,
        1, 1, 1, 1, 1
    )

    var single4 = intArrayOf(
        1, 1, 0, 1, 1,
        1, 1, 1, 0, 1,
        1, 1, 0, 1, 1,
        1, 1, 1, 0, 1,
        1, 1, 1, 1, 1
    )

    var single5 = intArrayOf(
        1, 1, 0, 1, 1,
        1, 0, 1, 0, 1,
        1, 1, 0, 1, 1,
        1, 1, 1, 0, 1,
        1, 1, 1, 1, 1
    )

    var single6 = intArrayOf(
        1, 1, 0, 1, 1,
        1, 0, 1, 0, 1,
        1, 1, 0, 1, 0,
        1, 1, 1, 0, 1,
        1, 1, 1, 1, 1
    )

    var single7 = intArrayOf(
        1, 1, 0, 1, 1,
        1, 0, 1, 0, 1,
        1, 1, 0, 1, 0,
        1, 0, 1, 0, 1,
        1, 1, 1, 1, 1
    )

    var single8 = intArrayOf(
        1, 1, 0, 1, 1,
        1, 0, 1, 0, 1,
        0, 1, 0, 1, 0,
        1, 0, 1, 0, 1,
        1, 1, 1, 1, 1
    )

    var world1 = intArrayOf(
        1, 1, 1, 1, 0,
        1, 1, 1, 1, 1,
        1, 1, 1, 1, 1,
        1, 1, 1, 1, 1,
        1, 1, 1, 1, 1
    )

    var world2 = intArrayOf(
        1, 1, 1, 1, 0,
        1, 1, 1, 1, 0,
        1, 1, 1, 1, 1,
        1, 1, 1, 1, 1,
        1, 1, 1, 1, 1
    )

    var world3 = intArrayOf(
        1, 1, 1, 0, 0,
        1, 1, 1, 1, 0,
        1, 1, 1, 1, 1,
        1, 1, 1, 1, 1,
        1, 1, 1, 1, 1
    )

    var world4 = intArrayOf(
        1, 1, 0, 0, 0,
        1, 1, 1, 1, 0,
        1, 1, 1, 1, 1,
        1, 1, 1, 1, 1,
        1, 1, 1, 1, 1
    )

    var world5 = intArrayOf(
        1, 1, 0, 0, 0,
        1, 1, 1, 1, 0,
        1, 1, 1, 1, 0,
        1, 1, 1, 1, 1,
        1, 1, 1, 1, 1
    )

    var world6 = intArrayOf(
        1, 1, 0, 0, 0,
        1, 1, 1, 0, 0,
        1, 1, 1, 1, 0,
        1, 1, 1, 1, 1,
        1, 1, 1, 1, 1
    )

    var world7 = intArrayOf(
        0, 1, 0, 0, 0,
        1, 1, 1, 0, 0,
        1, 1, 1, 1, 0,
        1, 1, 1, 1, 1,
        1, 1, 1, 1, 1
    )

    var world8 = intArrayOf(
        0, 1, 0, 0, 0,
        0, 1, 1, 0, 0,
        1, 1, 1, 1, 0,
        1, 1, 1, 1, 1,
        1, 1, 1, 1, 1
    )

    var world9 = intArrayOf(
        0, 0, 0, 0, 0,
        0, 1, 1, 0, 0,
        1, 1, 1, 1, 0,
        1, 1, 1, 1, 1,
        1, 1, 1, 1, 1
    )

    var world10 = intArrayOf(
        0, 0, 0, 0, 0,
        0, 1, 1, 0, 0,
        0, 1, 1, 1, 0,
        1, 1, 1, 1, 1,
        1, 1, 1, 1, 1
    )

    var world11 = intArrayOf(
        0, 0, 0, 0, 0,
        0, 0, 1, 0, 0,
        0, 1, 1, 1, 0,
        1, 1, 1, 1, 1,
        1, 1, 1, 1, 1
    )

    var world12 = intArrayOf(
        0, 0, 0, 0, 0,
        0, 0, 1, 0, 0,
        0, 1, 1, 1, 0,
        1, 1, 1, 1, 1,
        1, 1, 1, 1, 0
    )

    var world13 = intArrayOf(
        0, 0, 0, 0, 0,
        0, 0, 1, 0, 0,
        0, 1, 1, 1, 0,
        1, 1, 1, 1, 0,
        1, 1, 1, 1, 0
    )

    var world14 = intArrayOf(
        0, 0, 0, 0, 0,
        0, 0, 1, 0, 0,
        0, 1, 1, 1, 0,
        1, 1, 1, 1, 0,
        1, 1, 1, 0, 0
    )

    var world15 = intArrayOf(
        0, 0, 0, 0, 0,
        0, 0, 1, 0, 0,
        0, 1, 1, 1, 0,
        1, 1, 1, 1, 0,
        1, 1, 0, 0, 0
    )

    var world16 = intArrayOf(
        0, 0, 0, 0, 0,
        0, 0, 1, 0, 0,
        0, 1, 1, 1, 0,
        1, 1, 1, 0, 0,
        1, 1, 0, 0, 0
    )

    var world17 = intArrayOf(
        0, 0, 0, 0, 0,
        0, 0, 1, 0, 0,
        0, 1, 1, 1, 0,
        1, 1, 1, 0, 0,
        0, 1, 0, 0, 0
    )

    var world18 = intArrayOf(
        0, 0, 0, 0, 0,
        0, 0, 1, 0, 0,
        0, 1, 1, 1, 0,
        0, 1, 1, 0, 0,
        0, 1, 0, 0, 0
    )

    var world19 = intArrayOf(
        0, 0, 0, 0, 0,
        0, 0, 1, 0, 0,
        0, 1, 1, 1, 0,
        0, 1, 1, 0, 0,
        0, 0, 0, 0, 0
    )

    var world20 = intArrayOf(
        0, 0, 0, 0, 0,
        0, 0, 1, 0, 0,
        0, 1, 1, 1, 0,
        0, 0, 1, 0, 0,
        0, 0, 0, 0, 0
    )

    var world21 = intArrayOf(
        0, 0, 0, 0, 0,
        0, 0, 1, 0, 0,
        0, 1, 0, 1, 0,
        0, 0, 1, 0, 0,
        0, 0, 0, 0, 0
    )

    var world22 = intArrayOf(
        0, 0, 0, 0, 0,
        0, 1, 1, 0, 0,
        0, 1, 0, 1, 0,
        0, 0, 1, 0, 0,
        0, 0, 0, 0, 0
    )

    var world23 = intArrayOf(
        0, 0, 0, 0, 0,
        0, 1, 1, 1, 0,
        0, 1, 0, 1, 0,
        0, 0, 1, 0, 0,
        0, 0, 0, 0, 0
    )

    var world24 = intArrayOf(
        0, 0, 0, 0, 0,
        0, 1, 1, 1, 0,
        0, 1, 0, 1, 0,
        0, 0, 1, 1, 0,
        0, 0, 0, 0, 0
    )

    var world25 = intArrayOf(
        0, 0, 0, 0, 0,
        0, 1, 1, 1, 0,
        0, 1, 0, 1, 0,
        0, 1, 1, 1, 0,
        0, 0, 0, 0, 0
    )

    var world26 = intArrayOf(
        0, 0, 0, 0, 0,
        0, 1, 1, 1, 0,
        0, 1, 1, 1, 0,
        0, 1, 1, 1, 0,
        0, 0, 0, 0, 0
    )

    var world27 = intArrayOf(
        0, 0, 1, 0, 0,
        0, 1, 1, 1, 0,
        0, 1, 1, 1, 0,
        0, 1, 1, 1, 0,
        0, 0, 0, 0, 0
    )

    var world28 = intArrayOf(
        0, 0, 1, 0, 0,
        0, 1, 1, 1, 0,
        0, 1, 1, 1, 0,
        0, 1, 1, 1, 0,
        0, 0, 1, 0, 0
    )

    var world29 = intArrayOf(
        0, 0, 1, 0, 0,
        0, 1, 1, 1, 0,
        0, 1, 1, 1, 1,
        0, 1, 1, 1, 0,
        0, 0, 1, 0, 0
    )

    var world30 = intArrayOf(
        0, 0, 1, 0, 0,
        0, 1, 1, 1, 0,
        1, 1, 1, 1, 1,
        0, 1, 1, 1, 0,
        0, 0, 1, 0, 0
    )

    var world31 = intArrayOf(
        0, 0, 1, 0, 0,
        0, 1, 1, 1, 0,
        1, 1, 0, 1, 1,
        0, 1, 1, 1, 0,
        0, 0, 1, 0, 0
    )

    var world32 = intArrayOf(
        0, 0, 1, 0, 0,
        0, 1, 1, 1, 0,
        1, 0, 0, 0, 1,
        0, 1, 1, 1, 0,
        0, 0, 1, 0, 0
    )

    var world33 = intArrayOf(
        0, 0, 1, 0, 0,
        0, 1, 0, 1, 0,
        1, 0, 0, 0, 1,
        0, 1, 0, 1, 0,
        0, 0, 1, 0, 0
    )

    var world34 = intArrayOf(
        1, 0, 1, 0, 0,
        0, 1, 0, 1, 0,
        1, 0, 0, 0, 1,
        0, 1, 0, 1, 0,
        0, 0, 1, 0, 0
    )

    var world35 = intArrayOf(
        1, 0, 1, 0, 1,
        0, 1, 0, 1, 0,
        1, 0, 0, 0, 1,
        0, 1, 0, 1, 0,
        0, 0, 1, 0, 0
    )

    var world36 = intArrayOf(
        1, 0, 1, 0, 1,
        0, 1, 0, 1, 0,
        1, 0, 0, 0, 1,
        0, 1, 0, 1, 0,
        1, 0, 1, 0, 0
    )

    var world37 = intArrayOf(
        1, 0, 1, 0, 1,
        0, 1, 0, 1, 0,
        1, 0, 0, 0, 1,
        0, 1, 0, 1, 0,
        1, 0, 1, 0, 1
    )

    var world38 = intArrayOf(
        1, 0, 1, 0, 1,
        0, 1, 0, 1, 0,
        1, 0, 0, 0, 1,
        0, 0, 0, 0, 0,
        1, 0, 1, 0, 1
    )

    var world39 = intArrayOf(
        1, 0, 1, 0, 1,
        0, 0, 0, 0, 0,
        1, 0, 0, 0, 1,
        0, 0, 0, 0, 0,
        1, 0, 1, 0, 1
    )

    var world40 = intArrayOf(
        1, 0, 1, 0, 1,
        0, 1, 0, 1, 0,
        1, 0, 2, 0, 1,
        0, 1, 0, 1, 0,
        1, 0, 1, 0, 1
    )

    var oIn1 = intArrayOf(
        1, 1, 1, 1, 1,
        1, 0, 1, 1, 1,
        1, 1, 1, 1, 1,
        1, 1, 1, 1, 1,
        1, 1, 1, 1, 1
    )

    var oIn2 = intArrayOf(
        1, 1, 1, 1, 1,
        1, 0, 1, 1, 1,
        1, 0, 1, 1, 1,
        1, 1, 1, 1, 1,
        1, 1, 1, 1, 1
    )

    var oIn3 = intArrayOf(
        1, 1, 1, 1, 1,
        1, 0, 0, 1, 1,
        1, 0, 1, 1, 1,
        1, 1, 1, 1, 1,
        1, 1, 1, 1, 1
    )

    var oIn4 = intArrayOf(
        1, 1, 1, 1, 1,
        1, 0, 0, 1, 1,
        1, 0, 0, 1, 1,
        1, 1, 1, 1, 1,
        1, 1, 1, 1, 1
    )

    var oIn5 = intArrayOf(
        1, 1, 1, 1, 1,
        1, 0, 0, 1, 1,
        1, 0, 0, 1, 1,
        1, 0, 1, 1, 1,
        1, 1, 1, 1, 1
    )

    var oIn6 = intArrayOf(
        1, 1, 1, 1, 1,
        1, 0, 0, 1, 1,
        1, 0, 0, 1, 1,
        1, 0, 0, 1, 1,
        1, 1, 1, 1, 1
    )

    var oIn7 = intArrayOf(
        1, 1, 1, 1, 1,
        1, 0, 0, 0, 1,
        1, 0, 0, 1, 1,
        1, 0, 0, 1, 1,
        1, 1, 1, 1, 1
    )

    var oIn8 = intArrayOf(
        1, 1, 1, 1, 1,
        1, 0, 0, 0, 1,
        1, 0, 0, 0, 1,
        1, 0, 0, 1, 1,
        1, 1, 1, 1, 1
    )

    var oOut1 = intArrayOf(
        1, 1, 1, 1, 1,
        1, 1, 1, 0, 1,
        1, 1, 1, 1, 1,
        1, 1, 1, 1, 1,
        1, 1, 1, 1, 1
    )

    var oOut2 = intArrayOf(
        1, 1, 1, 1, 1,
        1, 1, 1, 0, 1,
        1, 1, 1, 0, 1,
        1, 1, 1, 1, 1,
        1, 1, 1, 1, 1
    )

    var oOut3 = intArrayOf(
        1, 1, 1, 1, 1,
        1, 1, 0, 0, 1,
        1, 1, 1, 0, 1,
        1, 1, 1, 1, 1,
        1, 1, 1, 1, 1
    )

    var oOut4 = intArrayOf(
        1, 1, 1, 1, 1,
        1, 1, 0, 0, 1,
        1, 1, 0, 0, 1,
        1, 1, 1, 1, 1,
        1, 1, 1, 1, 1
    )

    var oOut5 = intArrayOf(
        1, 1, 1, 1, 1,
        1, 1, 0, 0, 1,
        1, 1, 0, 0, 1,
        1, 1, 1, 0, 1,
        1, 1, 1, 1, 1
    )

    var oOut6 = intArrayOf(
        1, 1, 1, 1, 1,
        1, 1, 0, 0, 1,
        1, 1, 0, 0, 1,
        1, 1, 0, 0, 1,
        1, 1, 1, 1, 1
    )

    var oOut7 = intArrayOf(
        1, 1, 1, 1, 1,
        1, 0, 0, 0, 1,
        1, 1, 0, 0, 1,
        1, 1, 0, 0, 1,
        1, 1, 1, 1, 1
    )

    var oOut8 = intArrayOf(
        1, 1, 1, 1, 1,
        1, 0, 0, 0, 1,
        1, 0, 0, 0, 1,
        1, 1, 0, 0, 1,
        1, 1, 1, 1, 1
    )

    var oOut9 = intArrayOf(
        1, 1, 1, 1, 1,
        1, 0, 0, 0, 1,
        1, 0, 1, 0, 1,
        1, 0, 0, 0, 1,
        1, 1, 1, 1, 1
    )

    var paint1 = intArrayOf(
        1, 1, 1, 1, 1,
        1, 1, 1, 1, 1,
        1, 1, 1, 1, 1,
        1, 1, 1, 0, 1,
        1, 1, 1, 1, 1
    )

    var paint2 = intArrayOf(
        1, 1, 1, 1, 1,
        1, 1, 1, 1, 1,
        1, 1, 1, 1, 1,
        1, 1, 0, 0, 1,
        1, 1, 1, 1, 1
    )

    var paint3 = intArrayOf(
        1, 1, 1, 1, 1,
        1, 1, 1, 1, 1,
        1, 1, 1, 1, 1,
        1, 0, 0, 0, 1,
        1, 1, 1, 1, 1
    )

    var paint4 = intArrayOf(
        1, 1, 1, 1, 1,
        1, 1, 1, 1, 1,
        1, 1, 1, 0, 1,
        1, 0, 0, 0, 1,
        1, 1, 1, 1, 1
    )

    var paint5 = intArrayOf(
        1, 1, 1, 1, 1,
        1, 1, 1, 1, 1,
        1, 0, 1, 0, 1,
        1, 0, 0, 0, 1,
        1, 1, 1, 1, 1
    )

    var paint6 = intArrayOf(
        1, 1, 1, 1, 1,
        1, 1, 1, 1, 1,
        1, 0, 0, 0, 1,
        1, 0, 0, 0, 1,
        1, 1, 1, 1, 1
    )

    var paint7 = intArrayOf(
        1, 1, 1, 1, 1,
        1, 0, 0, 0, 1,
        1, 0, 0, 0, 1,
        1, 0, 0, 0, 1,
        1, 1, 1, 1, 1
    )

    companion object {
        val blackPaint = Paint()
        val whitePaint = Paint()
        var bluePaint = Paint()
    }

    var pxWidth = 0
    var pxHeight = 0

    lateinit var eventType: StatTracker.EventType
    var thresholds = 0

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
        blackPaint.color = Color.parseColor("#ff000000")
        whitePaint.color = Color.WHITE
        bluePaint.color = Color.parseColor("#ff1a1ed7")
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        pxWidth = width / 5
        pxHeight = height / 5

        canvas?.apply {
            save()

            if (thresholds != 0) {
                drawIcon(canvas)
            }

            restore()
        }
    }

    fun setType(achievementType: StatTracker.EventType, thresholds: Int) {
        this.eventType = achievementType
        this.thresholds = thresholds

        invalidate()
    }

    fun drawIcon(canvas: Canvas) {
        if (thresholds == 0) {
            return
        }

        lateinit var icon: IntArray

        val singleIcons = arrayOf(single1, single2, single3, single4, single5, single6, single7, single8)

        val worldIcons = arrayOf(world1, world2, world3, world4, world5, world6, world7, world8, world9, world10,
        world11, world12, world13, world14, world15, world16, world17, world18, world19, world20, world21, world22,
        world23, world24, world25, world26, world27, world28, world29, world30, world31, world32, world33, world34,
        world35, world36, world37, world38, world39, world40)

        val pixelIn = arrayOf(oIn1, oIn2, oIn3, oIn4, oIn5, oIn6, oIn7, oIn8)

        val pixelOut = arrayOf(oOut1, oOut2, oOut3, oOut4, oOut5, oOut6, oOut7, oOut8, oOut9)

        val paint = arrayOf(paint1, paint2, paint3, paint4, paint5, paint6, paint7)

        if (eventType == StatTracker.EventType.PIXEL_PAINTED_SINGLE) {
            icon = singleIcons[thresholds - 1]
        }
        else if (eventType == StatTracker.EventType.PIXEL_PAINTED_WORLD) {
            icon = worldIcons[thresholds - 1]
        }
        else if (eventType == StatTracker.EventType.PIXEL_OVERWRITE_IN) {
            icon = pixelIn[thresholds - 1]
        }
        else if (eventType == StatTracker.EventType.PIXEL_OVERWRITE_OUT) {
            icon = pixelOut[thresholds - 1]
        }
        else if (eventType == StatTracker.EventType.PAINT_RECEIVED) {
            icon = paint[thresholds - 1]
        }

        for (y in 0 until 5) {
            for (x in 0 until 5) {
                if (icon[y * 5 + x] == 1) {
                    drawPixel(x, y, blackPaint, canvas)
                }
                else if (icon[y * 5 + x] == 2) {
                    drawPixel(x, y, bluePaint, canvas)
                }
                else {
                    drawPixel(x, y, whitePaint, canvas)
                }
            }
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