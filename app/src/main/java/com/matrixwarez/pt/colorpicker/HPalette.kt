package com.matrixwarez.pt.colorpicker

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import java.lang.Float.min
import java.nio.IntBuffer
import kotlin.math.max


class HPalette: View {

    interface HColorSelectionListener {
        fun onHColorSelected(hue: Float)
    }

    interface HIndicatorCallback {
        fun indicatorStartPosition(x: Float)
        fun moveIndicatorToPosition(x: Float)
    }

    private val logging = false

    private val resolutionConstant = 4

    private val minHue = 0F
    private val maxHue = 360F

    private var w = 0

    private lateinit var pixels: IntArray

    var hue = 0F
    private set

    var hColorSelectionListener: HColorSelectionListener? = null
    var indicatorCallback: HIndicatorCallback? = null

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

    private fun commonInit() {

    }

    fun init() {
        w = width / resolutionConstant

        pixels = IntArray(w)
    }

    fun setH(h: Float) {
        hue = h

        hColorSelectionListener?.onHColorSelected(hue)

        indicatorCallback?.indicatorStartPosition(hue / maxHue * width)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        val sTime = System.currentTimeMillis()

        canvas?.apply {
            save()

            if (w > 0) {
                drawHuePalette(canvas)
            }

            restore()
        }

        val duration = System.currentTimeMillis() - sTime

        if (logging) {
            Log.i("Hue Fps = ", (1000 / duration.toFloat()).toString())
        }
    }

    private fun drawHuePalette(canvas: Canvas) {
        val wf = w.toFloat()

        for (x in 0 until w) {
            val h = x / wf * maxHue

            val color = ColorUtility.colorFromH(h)

            pixels[x] = ColorUtility.getAndroidBitmapFormatRGBA8888(color)
        }

        var bitmap = Bitmap.createBitmap(w, 1, Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(IntBuffer.wrap(pixels))

        bitmap = Bitmap.createScaledBitmap(bitmap, width, height, false)

        canvas.drawBitmap(bitmap, 0F, 0F, null)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
            val h = event.x / width.toFloat() * maxHue

            setH(max(min(h, maxHue), minHue))

            true
        } else {
            super.onTouchEvent(event)
        }
    }
}