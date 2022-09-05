package com.ericversteeg.radiofrost.colorpicker

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


class SBPalette: View {

    interface SBColorSelectionListener {
        fun onSBColorSelected(saturation: Float, brightness: Float)
    }

    interface SBIndicatorCallback {
        fun indicatorStartPosition(x: Float, y: Float)
        fun moveIndicatorToPosition(x: Float, y: Float)
    }

    private val logging = false

    private val resolutionConstant = 8

    private val minSb = 0F
    private val maxSb = 1F

    private var w = 0
    private var h = 0

    private lateinit var pixels: IntArray

    private var hue = 0F
    set(value) {
        field = value

        invalidate()
    }

    var saturation = 0.5F
    private set

    var brightness = 0.5F
    private set

    var sbColorSelectionListener: SBColorSelectionListener? = null
    var indicatorCallback: SBIndicatorCallback? = null

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
        h = height / resolutionConstant

        pixels = IntArray(w * h)
    }

    fun setSB(s: Float, b: Float) {
        saturation = s
        brightness = b

        sbColorSelectionListener?.onSBColorSelected(saturation, brightness)

        indicatorCallback?.indicatorStartPosition(saturation * width, height - (brightness * height))
    }

    fun setH(h: Float) {
        hue = h
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        val sTime = System.currentTimeMillis()

        canvas?.apply {
            save()

            drawSBSquare(hue, canvas)

            restore()
        }

        val duration = System.currentTimeMillis() - sTime

        if (logging) {
            Log.i("Fps = ", (1000 / duration.toFloat()).toString())
        }
    }

    private fun drawSBSquare(hue: Float, canvas: Canvas) {
        if (w == 0 || h == 0) return

        val wf = w.toFloat()
        val hf = h.toFloat()

        for (y in 0 until h) {
            for (x in 0 until w) {
                val s = x / wf
                val br = maxSb - (y / hf)

                val color = ColorUtility.colorFromHSB(hue, s, br)

                pixels[y * w + x] = ColorUtility.getAndroidBitmapFormatRGBA8888(color)
            }
        }

        var bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(IntBuffer.wrap(pixels))

        bitmap = Bitmap.createScaledBitmap(bitmap, width, height, false)

        canvas.drawBitmap(bitmap, 0F, 0F, null)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
            val s = event.x / width.toFloat()
            val b = 1 - (event.y / height.toFloat())

            setSB(max(min(s, maxSb), minSb), max(min(b, maxSb), minSb))

            true
        } else {
            super.onTouchEvent(event)
        }
    }
}