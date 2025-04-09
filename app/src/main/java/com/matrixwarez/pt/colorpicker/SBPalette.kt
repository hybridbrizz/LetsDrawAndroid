package com.matrixwarez.pt.colorpicker

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import com.matrixwarez.pt.R
import com.matrixwarez.pt.helper.Utils
import java.lang.Float.min
import java.nio.IntBuffer
import kotlin.math.max


class SBPalette: FrameLayout {

    interface SBSelectionListener {
        fun onSBChanged()
    }

    private val logging = false

    private val resolutionConstant = 8

    private var w = 0
    private var h = 0

    private lateinit var pixels: IntArray

    var sbSelectionListener: SBSelectionListener? = null

    private var pcv: PickedColorValues? = null

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

    fun resize() {
        w = width / resolutionConstant
        h = height / resolutionConstant

        pixels = IntArray(w * h)

        requestLayout()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val sTime = System.currentTimeMillis()

        canvas.apply {
            save()

            pcv?.let {
                drawSBSquare(canvas, it)
            }

            restore()
        }

        val duration = System.currentTimeMillis() - sTime

        if (logging) {
            Log.i("Fps = ", (1000 / duration.toFloat()).toString())
        }

        moveIndicator()
    }

    private fun drawSBSquare(canvas: Canvas, pcv: PickedColorValues) {
        if (w == 0 || h == 0) return

        val wf = w.toFloat()
        val hf = h.toFloat()

        for (y in 0 until h) {
            for (x in 0 until w) {
                val s = x / wf
                val br = pcv.maxValue - (y / hf)

                val color = ColorUtility.colorFromHSB(pcv.h, s, br)

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
            pcv?.let {
                it.s = max(min(event.x / width.toFloat(), it.maxValue), it.minValue)
                it.b = max(min(1 - (event.y / height.toFloat()), it.maxValue), it.minValue)

                sbSelectionListener?.onSBChanged()

                moveIndicator()
            }

            true
        } else {
            super.onTouchEvent(event)
        }
    }

    fun moveIndicator() {
        pcv?.let {
            val indicator = findViewById<SBIndicator>(R.id.sb_indicator)
            indicator.x = it.s / it.maxValue * width - indicator.width / 2
            indicator.y = height - (it.b / it.maxValue * height) - indicator.width / 2
        }
    }

    fun setPCV(pcv: PickedColorValues) {
        this.pcv = pcv
        postInvalidate()
    }
}