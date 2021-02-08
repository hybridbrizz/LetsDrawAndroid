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

    companion object {
        var semiPaint = Paint()
        var semiLightPaint = Paint()
        var semiDarkPaint = Paint()
        val greenPaint = Paint()
        val lightGreenPaint = Paint()
        val altGreenPaint = Paint()
        val lightAltGreenPaint = Paint()
        val whitePaint = Paint()
        val redPaint = Paint()
        val yellowPaint = Paint()
        val lightYellowPaint = Paint()
        val lightRedPaint = Paint()
        val blackPaint = Paint()
        val thirdGray = Paint()
        val twoThirdGray = Paint()
        val photoshopGray = Paint()
        val classicGrayLight = Paint()
        val classicGrayDark = Paint()
        val chessTan = Paint()
        val linePaint = Paint()
    }

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

    enum class Type {
        NONE,
        BACK,
        PAINT_CLOSE,
        YES,
        NO,
        PAINT,
        RECENT_COLORS,
        RECENT_COLOR,
        PLAY,
        OPTIONS,
        STATS,
        EXIT,
        SINGLE,
        WORLD,
        BACKGROUND_BLACK,
        BACKGROUND_WHITE,
        BACKGROUND_PHOTOSHOP,
        BACKGROUND_CLASSIC,
        BACKGROUND_GRAY_THIRDS,
        BACKGROUND_CHESS
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
        set(value) {
            field = value
            invalidate()
        }

    var representingColor: Int? = null
        set(value) {
            field = value
            invalidate()
        }

    var colorMode = ColorMode.NONE
    set(value) {
        field = value
        invalidate()
    }

    var pxWidth = 0
    var pxHeight = 0

    val menuButtonRows = 4
    val menuButtonCols = 26

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
        semiPaint.color = Color.parseColor("#99FFFFFF")
        semiLightPaint.color = Color.parseColor("#33FFFFFF")
        semiDarkPaint.color = Color.parseColor("#33000000")

        greenPaint.color = Color.parseColor("#05AD2E")
        altGreenPaint.color = Color.parseColor("#42ff7b")

        whitePaint.color = Color.WHITE
        yellowPaint.color = Color.parseColor("#FAD452")
        redPaint.color = Color.parseColor("#FA3A47")

        lightGreenPaint.color = Color.parseColor("#62AD6C")
        lightAltGreenPaint.color = Color.parseColor("#B0FFC5")
        lightYellowPaint.color = Color.parseColor("#FAE38D")
        lightRedPaint.color = Color.parseColor("#FB7E87")

        blackPaint.color = Color.parseColor("#FF000000")
        thirdGray.color = Color.parseColor("#FFAAAAAA")
        twoThirdGray.color = Color.parseColor("#FF555555")
        photoshopGray.color = Color.parseColor("#FFCCCCCC")

        classicGrayLight.color = Color.parseColor("#FF666666")
        classicGrayDark.color = Color.parseColor("#FF333333")

        chessTan.color = Color.parseColor("#FFb59870")

        linePaint.color = Color.WHITE
        linePaint.strokeWidth = 1F
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas?.apply {
            save()

            if (type == Type.PAINT_CLOSE) {
                drawPaintCloseAction(touchState == TouchState.ACTIVE, canvas)
            }
            else if (type == Type.BACK) {
                drawBackAction(touchState == TouchState.ACTIVE, canvas)
            }
            else if (type == Type.YES) {
                drawYesAction(touchState == TouchState.ACTIVE, colorMode == ColorMode.COLOR, canvas)
            }
            else if (type == Type.NO) {
                drawNoAction(touchState == TouchState.ACTIVE,colorMode == ColorMode.COLOR, canvas)
            }
            else if (type == Type.PAINT) {
                drawPaintAction(touchState == TouchState.ACTIVE, canvas)
            }
            else if (type == Type.RECENT_COLORS) {
                drawRecentColorsAction(touchState == TouchState.ACTIVE, canvas)
            }
            else if (type == Type.RECENT_COLOR) {
                drawRecentColorAction(touchState == TouchState.ACTIVE, canvas)
            }
            else if (type == Type.PLAY) {
                drawPlayAction(touchState == TouchState.ACTIVE, canvas)
            }
            else if (type == Type.OPTIONS) {
                drawOptionsAction(touchState == TouchState.ACTIVE, canvas)
            }
            else if (type == Type.STATS) {
                drawStatsAction(touchState == TouchState.ACTIVE, canvas)
            }
            else if (type == Type.EXIT) {
                drawExitAction(touchState == TouchState.ACTIVE, canvas)
            }
            else if (type == Type.SINGLE) {
                drawSingleAction(touchState == TouchState.ACTIVE, canvas)
            }
            else if (type == Type.WORLD) {
                drawWorldAction(touchState == TouchState.ACTIVE, canvas)
            }
            else if (type == Type.BACKGROUND_WHITE || type == Type.BACKGROUND_BLACK || type == Type.BACKGROUND_GRAY_THIRDS ||
                type == Type.BACKGROUND_PHOTOSHOP || type == Type.BACKGROUND_CLASSIC || type == Type.BACKGROUND_CHESS) {
                drawBackgroundOptionAction(type, touchState == TouchState.ACTIVE, canvas)
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
        else if (ev.action == MotionEvent.ACTION_CANCEL) {
            touchState = TouchState.INACTIVE
        }

        return super.onTouchEvent(ev)
    }

    private fun drawPaintCloseAction(light: Boolean, canvas: Canvas) {
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

    private fun drawBackAction(light: Boolean, canvas: Canvas) {
        rows = 5
        cols = 7

        var paint = semiLightPaint
        if (SessionSettings.instance.darkIcons) {
            paint = semiDarkPaint
        }

        if (light) {
            paint = lightYellowPaint
        }

        canvas.apply {
            // row 1
            drawPixel(2, 0, paint, canvas)

            // row 2
            drawPixel(1, 1, paint, canvas)

            // row 3
            drawPixel(6, 2, paint, canvas)
            drawPixel(5, 2, paint, canvas)
            drawPixel(4, 2, paint, canvas)
            drawPixel(3, 2, paint, canvas)
            drawPixel(2, 2, paint, canvas)
            drawPixel(0, 2, paint, canvas)

            // row 4
            drawPixel(1, 3, paint, canvas)

            // row 5
            drawPixel(2, 4, paint, canvas)
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

    private fun drawPaintAction(light: Boolean, canvas: Canvas) {
        rows = 4
        cols = 4

        var primaryPaint = whitePaint
        var accentPaint = altGreenPaint
        if (light) {
            accentPaint = lightAltGreenPaint
        }

        if (SessionSettings.instance.darkIcons) {
            primaryPaint = blackPaint
        }

        canvas.apply {
            // row 1
            drawPixel(3, 0, accentPaint, canvas)

            // row 2
            drawPixel(2, 1, primaryPaint, canvas)

            // row 3
            drawPixel(1, 2, primaryPaint, canvas)

            // row 4
            drawPixel(0, 3, primaryPaint, canvas)
        }
    }

    private fun drawRecentColorsAction(light: Boolean, canvas: Canvas) {
        rows = 3
        cols = 3

        var paint = semiPaint

        if (SessionSettings.instance.darkIcons) {
            paint = semiDarkPaint
        }

        if (light) {
            paint = altGreenPaint
        }

        canvas.apply {
            // row 1
            drawPixel(0, 0, paint, canvas)
            drawPixel(1, 0, paint, canvas)
            drawPixel(2, 0, paint, canvas)

            // row 2
            drawPixel(0, 1, paint, canvas)
            drawPixel(2, 1, paint, canvas)

            // row 3
            drawPixel(0, 2, paint, canvas)
            drawPixel(1, 2, paint, canvas)
            drawPixel(2, 2, paint, canvas)
        }
    }

    private fun drawRecentColorAction(light: Boolean, canvas: Canvas) {
        rows = 4
        cols = 4

        var paint = semiPaint

        if (SessionSettings.instance.darkIcons) {
            paint = semiDarkPaint
        }

        if (light) {
            paint = altGreenPaint
        }

        val colorPaint = Paint()

        representingColor?.apply {
            colorPaint.color = this
        }


        canvas.apply {
            // row 1
            drawPixel(0, 0, paint, canvas)
            drawPixel(1, 0, paint, canvas)
            drawPixel(2, 0, paint, canvas)
            drawPixel(3, 0, paint, canvas)

            // row 2
            drawPixel(0, 1, paint, canvas)
            drawPixel(1, 1, colorPaint, canvas)
            drawPixel(2, 1, colorPaint, canvas)
            drawPixel(3, 1, paint, canvas)

            // row 3
            drawPixel(0, 2, paint, canvas)
            drawPixel(1, 2, colorPaint, canvas)
            drawPixel(2, 2, colorPaint, canvas)
            drawPixel(3, 2, paint, canvas)

            // row 4
            drawPixel(0, 3, paint, canvas)
            drawPixel(1, 3, paint, canvas)
            drawPixel(2, 3, paint, canvas)
            drawPixel(3, 3, paint, canvas)
        }
    }

    // menu

    private fun drawPlayAction(selected: Boolean, canvas: Canvas) {
        rows = menuButtonRows
        cols = menuButtonCols

        var paint = whitePaint
        if (selected) {
            paint = altGreenPaint
        }

        val colorPaint = Paint()

        representingColor?.apply {
            colorPaint.color = this
        }

        canvas.apply {
            // row 1
            drawPixel(0, 0, paint, canvas)
            drawPixel(1, 0, paint, canvas)
            drawPixel(4, 0, paint, canvas)
            drawPixel(10, 0, paint, canvas)
            drawPixel(13, 0, paint, canvas)
            drawPixel(15, 0, paint, canvas)

            // row 2
            drawPixel(0, 1, paint, canvas)
            drawPixel(2, 1, paint, canvas)
            drawPixel(4, 1, paint, canvas)
            drawPixel(9, 1, paint, canvas)
            drawPixel(11, 1, paint, canvas)
            drawPixel(13, 1, paint, canvas)
            drawPixel(15, 1, paint, canvas)

            // row 3
            drawPixel(0, 2, paint, canvas)
            drawPixel(1, 2, paint, canvas)
            drawPixel(4, 2, paint, canvas)
            drawPixel(8, 2, paint, canvas)
            drawPixel(10, 2, paint, canvas)
            drawPixel(11, 2, paint, canvas)
            drawPixel(14, 2, paint, canvas)

            // row 4
            drawPixel(0, 3, paint, canvas)
            drawPixel(4, 3, paint, canvas)
            drawPixel(5, 3, paint, canvas)
            drawPixel(6, 3, paint, canvas)
            drawPixel(8, 3, paint, canvas)
            drawPixel(11, 3, paint, canvas)
            drawPixel(14, 3, paint, canvas)
        }
    }

    private fun drawOptionsAction(selected: Boolean, canvas: Canvas) {
        rows = menuButtonRows
        cols = menuButtonCols

        var paint = whitePaint
        if (selected) {
            paint = altGreenPaint
        }

        val colorPaint = Paint()

        representingColor?.apply {
            colorPaint.color = this
        }

        canvas.apply {
            // row 1
            drawPixel(0, 0, paint, canvas)
            drawPixel(1, 0, paint, canvas)
            drawPixel(2, 0, paint, canvas)
            drawPixel(4, 0, paint, canvas)
            drawPixel(5, 0, paint, canvas)
            drawPixel(8, 0, paint, canvas)
            drawPixel(9, 0, paint, canvas)
            drawPixel(10, 0, paint, canvas)
            drawPixel(12, 0, paint, canvas)
            drawPixel(14, 0, paint, canvas)
            drawPixel(15, 0, paint, canvas)
            drawPixel(16, 0, paint, canvas)
            drawPixel(18, 0, paint, canvas)
            drawPixel(21, 0, paint, canvas)
            drawPixel(23, 0, paint, canvas)
            drawPixel(24, 0, paint, canvas)
            drawPixel(25, 0, paint, canvas)

            // row 2
            drawPixel(0, 1, paint, canvas)
            drawPixel(2, 1, paint, canvas)
            drawPixel(4, 1, paint, canvas)
            drawPixel(6, 1, paint, canvas)
            drawPixel(9, 1, paint, canvas)
            drawPixel(12, 1, paint, canvas)
            drawPixel(14, 1, paint, canvas)
            drawPixel(16, 1, paint, canvas)
            drawPixel(18, 1, paint, canvas)
            drawPixel(19, 1, paint, canvas)
            drawPixel(21, 1, paint, canvas)
            drawPixel(24, 1, paint, canvas)

            // row 3
            drawPixel(0, 2, paint, canvas)
            drawPixel(2, 2, paint, canvas)
            drawPixel(4, 2, paint, canvas)
            drawPixel(5, 2, paint, canvas)
            drawPixel(9, 2, paint, canvas)
            drawPixel(12, 2, paint, canvas)
            drawPixel(14, 2, paint, canvas)
            drawPixel(16, 2, paint, canvas)
            drawPixel(18, 2, paint, canvas)
            drawPixel(20, 2, paint, canvas)
            drawPixel(21, 2, paint, canvas)
            drawPixel(25, 2, paint, canvas)

            // row 4
            drawPixel(0, 3, paint, canvas)
            drawPixel(1, 3, paint, canvas)
            drawPixel(2, 3, paint, canvas)
            drawPixel(4, 3, paint, canvas)
            drawPixel(9, 3, paint, canvas)
            drawPixel(12, 3, paint, canvas)
            drawPixel(14, 3, paint, canvas)
            drawPixel(15, 3, paint, canvas)
            drawPixel(16, 3, paint, canvas)
            drawPixel(18, 3, paint, canvas)
            drawPixel(21, 3, paint, canvas)
            drawPixel(23, 3, paint, canvas)
            drawPixel(24, 3, paint, canvas)
            drawPixel(25, 3, paint, canvas)
        }
    }

    private fun drawStatsAction(selected: Boolean, canvas: Canvas) {
        rows = menuButtonRows
        cols = menuButtonCols

        var paint = whitePaint
        if (selected) {
            paint = altGreenPaint
        }

        val colorPaint = Paint()

        representingColor?.apply {
            colorPaint.color = this
        }

        canvas.apply {
            // row 1
            drawPixel(0, 0, paint, canvas)
            drawPixel(1, 0, paint, canvas)
            drawPixel(2, 0, paint, canvas)
            drawPixel(4, 0, paint, canvas)
            drawPixel(5, 0, paint, canvas)
            drawPixel(6, 0, paint, canvas)
            drawPixel(10, 0, paint, canvas)
            drawPixel(13, 0, paint, canvas)
            drawPixel(14, 0, paint, canvas)
            drawPixel(15, 0, paint, canvas)
            drawPixel(17, 0, paint, canvas)
            drawPixel(18, 0, paint, canvas)
            drawPixel(19, 0, paint, canvas)


            // row 2
            drawPixel(1, 1, paint, canvas)
            drawPixel(5, 1, paint, canvas)
            drawPixel(9, 1, paint, canvas)
            drawPixel(11, 1, paint, canvas)
            drawPixel(14, 1, paint, canvas)
            drawPixel(18, 1, paint, canvas)

            // row 3
            drawPixel(2, 2, paint, canvas)
            drawPixel(5, 2, paint, canvas)
            drawPixel(8, 2, paint, canvas)
            drawPixel(10, 2, paint, canvas)
            drawPixel(11, 2, paint, canvas)
            drawPixel(14, 2, paint, canvas)
            drawPixel(19, 2, paint, canvas)

            // row 4
            drawPixel(0, 3, paint, canvas)
            drawPixel(1, 3, paint, canvas)
            drawPixel(2, 3, paint, canvas)
            drawPixel(5, 3, paint, canvas)
            drawPixel(8, 3, paint, canvas)
            drawPixel(11, 3, paint, canvas)
            drawPixel(14, 3, paint, canvas)
            drawPixel(17, 3, paint, canvas)
            drawPixel(18, 3, paint, canvas)
            drawPixel(19, 3, paint, canvas)

        }
    }

    private fun drawExitAction(selected: Boolean, canvas: Canvas) {
        rows = menuButtonRows
        cols = menuButtonCols

        var paint = whitePaint
        if (selected) {
            paint = altGreenPaint
        }

        val colorPaint = Paint()

        representingColor?.apply {
            colorPaint.color = this
        }

        canvas.apply {
            // row 1
            drawPixel(0, 0, paint, canvas)
            drawPixel(1, 0, paint, canvas)
            drawPixel(2, 0, paint, canvas)
            drawPixel(4, 0, paint, canvas)
            drawPixel(6, 0, paint, canvas)
            drawPixel(8, 0, paint, canvas)
            drawPixel(9, 0, paint, canvas)
            drawPixel(10, 0, paint, canvas)
            drawPixel(12, 0, paint, canvas)
            drawPixel(13, 0, paint, canvas)
            drawPixel(14, 0, paint, canvas)

            // row 2
            drawPixel(0, 1, paint, canvas)
            drawPixel(1, 1, paint, canvas)
            drawPixel(5, 1, paint, canvas)
            drawPixel(9, 1, paint, canvas)
            drawPixel(13, 1, paint, canvas)

            // row 3
            drawPixel(0, 2, paint, canvas)
            drawPixel(5, 2, paint, canvas)
            drawPixel(9, 2, paint, canvas)
            drawPixel(13, 2, paint, canvas)

            // row 4
            drawPixel(0, 3, paint, canvas)
            drawPixel(1, 3, paint, canvas)
            drawPixel(2, 3, paint, canvas)
            drawPixel(4, 3, paint, canvas)
            drawPixel(6, 3, paint, canvas)
            drawPixel(8, 3, paint, canvas)
            drawPixel(9, 3, paint, canvas)
            drawPixel(10, 3, paint, canvas)
            drawPixel(13, 3, paint, canvas)
        }
    }

    private fun drawSingleAction(selected: Boolean, canvas: Canvas) {
        rows = menuButtonRows
        cols = menuButtonCols

        var paint = whitePaint
        if (selected) {
            paint = altGreenPaint
        }

        val colorPaint = Paint()

        representingColor?.apply {
            colorPaint.color = this
        }

        canvas.apply {
            // row 1
            drawPixel(0, 0, paint, canvas)
            drawPixel(1, 0, paint, canvas)
            drawPixel(2, 0, paint, canvas)
            drawPixel(4, 0, paint, canvas)
            drawPixel(6, 0, paint, canvas)
            drawPixel(9, 0, paint, canvas)
            drawPixel(11, 0, paint, canvas)
            drawPixel(12, 0, paint, canvas)
            drawPixel(13, 0, paint, canvas)
            drawPixel(15, 0, paint, canvas)
            drawPixel(19, 0, paint, canvas)
            drawPixel(20, 0, paint, canvas)
            drawPixel(21, 0, paint, canvas)

            // row 2
            drawPixel(1, 1, paint, canvas)
            drawPixel(4, 1, paint, canvas)
            drawPixel(6, 1, paint, canvas)
            drawPixel(7, 1, paint, canvas)
            drawPixel(9, 1, paint, canvas)
            drawPixel(11, 1, paint, canvas)
            drawPixel(15, 1, paint, canvas)
            drawPixel(19, 1, paint, canvas)
            drawPixel(20, 1, paint, canvas)

            // row 3
            drawPixel(2, 2, paint, canvas)
            drawPixel(4, 2, paint, canvas)
            drawPixel(6, 2, paint, canvas)
            drawPixel(8, 2, paint, canvas)
            drawPixel(9, 2, paint, canvas)
            drawPixel(11, 2, paint, canvas)
            drawPixel(13, 2, paint, canvas)
            drawPixel(15, 2, paint, canvas)
            drawPixel(19, 2, paint, canvas)

            // row 4
            drawPixel(0, 3, paint, canvas)
            drawPixel(1, 3, paint, canvas)
            drawPixel(2, 3, paint, canvas)
            drawPixel(4, 3, paint, canvas)
            drawPixel(6, 3, paint, canvas)
            drawPixel(9, 3, paint, canvas)
            drawPixel(11, 3, paint, canvas)
            drawPixel(12, 3, paint, canvas)
            drawPixel(13, 3, paint, canvas)
            drawPixel(15, 3, paint, canvas)
            drawPixel(16, 3, paint, canvas)
            drawPixel(17, 3, paint, canvas)
            drawPixel(19, 3, paint, canvas)
            drawPixel(20, 3, paint, canvas)
            drawPixel(21, 3, paint, canvas)
        }
    }

    private fun drawWorldAction(selected: Boolean, canvas: Canvas) {
        rows = menuButtonRows
        cols = menuButtonCols

        var paint = whitePaint
        if (selected) {
            paint = altGreenPaint
        }

        val colorPaint = Paint()

        representingColor?.apply {
            colorPaint.color = this
        }

        canvas.apply {
            // row 1
            drawPixel(0, 0, paint, canvas)
            drawPixel(4, 0, paint, canvas)
            drawPixel(6, 0, paint, canvas)
            drawPixel(7, 0, paint, canvas)
            drawPixel(8, 0, paint, canvas)
            drawPixel(10, 0, paint, canvas)
            drawPixel(11, 0, paint, canvas)
            drawPixel(14, 0, paint, canvas)
            drawPixel(18, 0, paint, canvas)
            drawPixel(19, 0, paint, canvas)

            // row 2
            drawPixel(0, 1, paint, canvas)
            drawPixel(4, 1, paint, canvas)
            drawPixel(6, 1, paint, canvas)
            drawPixel(8, 1, paint, canvas)
            drawPixel(10, 1, paint, canvas)
            drawPixel(12, 1, paint, canvas)
            drawPixel(14, 1, paint, canvas)
            drawPixel(18, 1, paint, canvas)
            drawPixel(20, 1, paint, canvas)

            // row 3
            drawPixel(0, 2, paint, canvas)
            drawPixel(1, 2, paint, canvas)
            drawPixel(3, 2, paint, canvas)
            drawPixel(4, 2, paint, canvas)
            drawPixel(6, 2, paint, canvas)
            drawPixel(8, 2, paint, canvas)
            drawPixel(10, 2, paint, canvas)
            drawPixel(11, 2, paint, canvas)
            drawPixel(14, 2, paint, canvas)
            drawPixel(18, 2, paint, canvas)
            drawPixel(20, 2, paint, canvas)

            // row 4
            drawPixel(0, 3, paint, canvas)
            drawPixel(2, 3, paint, canvas)
            drawPixel(4, 3, paint, canvas)
            drawPixel(6, 3, paint, canvas)
            drawPixel(7, 3, paint, canvas)
            drawPixel(8, 3, paint, canvas)
            drawPixel(10, 3, paint, canvas)
            drawPixel(12, 3, paint, canvas)
            drawPixel(14, 3, paint, canvas)
            drawPixel(15, 3, paint, canvas)
            drawPixel(16, 3, paint, canvas)
            drawPixel(18, 3, paint, canvas)
            drawPixel(19, 3, paint, canvas)
        }
    }

    // single player backgrounds

    private fun drawBackgroundOptionAction(type: ActionButtonView.Type, selected: Boolean, canvas: Canvas) {
        rows = 9
        cols = 13

        var paint1 = redPaint
        var paint2 = redPaint

        when (type) {
            Type.BACKGROUND_WHITE -> {
                paint1 = whitePaint
                paint2 = whitePaint
            }
            Type.BACKGROUND_BLACK -> {
                paint1 = blackPaint
                paint2 = blackPaint
            }
            Type.BACKGROUND_GRAY_THIRDS -> {
                paint1 = thirdGray
                paint2 = twoThirdGray
            }
            Type.BACKGROUND_PHOTOSHOP -> {
                paint1 = whitePaint
                paint2 = photoshopGray
            }
            Type.BACKGROUND_CLASSIC -> {
                paint1 = classicGrayLight
                paint2 = classicGrayDark
            }
            Type.BACKGROUND_CHESS -> {
                paint1 = chessTan
                paint2 = blackPaint
            }
        }

        if (selected) {
            paint1 = altGreenPaint
            paint2 = altGreenPaint
        }

        canvas.apply {
            var flip = false
            for (y in 0 until rows) {
                for (x in 0 until cols) {
                    if ((x % 2 == 0 && !flip) || (flip && x % 2 == 1)) {
                        drawPixel(x, y, paint1, canvas)
                    }
                    else {
                        drawPixel(x, y, paint2, canvas)
                    }
                }
                flip = !flip
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