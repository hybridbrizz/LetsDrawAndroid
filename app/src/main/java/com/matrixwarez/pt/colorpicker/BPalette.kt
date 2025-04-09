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


class BPalette: FrameLayout {

    interface BColorSelectionListener {
        fun onBChanged()
    }

    private val logging = false

    private val resolutionConstant = 4

    private var w = 0

    private lateinit var pixels: IntArray

    private var pcv: PickedColorValues? = null

    var bSelectionListener: BColorSelectionListener? = null

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

        pixels = IntArray(w)

        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.apply {
            save()

            if (w > 0) {
                pcv?.let {
                    drawSPalette(canvas, it)
                }
            }

            restore()
        }

        moveIndicator()
    }

    private fun drawSPalette(canvas: Canvas, pcv: PickedColorValues) {
        val wf = w.toFloat()

        for (x in 0 until w) {
            val b = x / wf * pcv.maxValue

            val color = ColorUtility.colorFromB(b, pcv)

            pixels[x] = ColorUtility.getAndroidBitmapFormatRGBA8888(color)
        }

        var bitmap = Bitmap.createBitmap(w, 1, Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(IntBuffer.wrap(pixels))

        bitmap = Bitmap.createScaledBitmap(bitmap, width, height, false)

        canvas.drawBitmap(bitmap, 0F, 0F, null)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
            pcv?.let {
                it.b = max(min(event.x / width.toFloat() * it.maxValue, it.maxValue), it.minValue)

                bSelectionListener?.onBChanged()

                moveIndicator()
            }

            true
        } else {
            super.onTouchEvent(event)
        }
    }

    fun moveIndicator() {
        pcv?.let {
            val indicator = findViewById<SBIndicator>(R.id.b_indicator)
            indicator.x = it.b / it.maxValue * width - indicator.width / 2
        }
    }

    fun setPCV(pcv: PickedColorValues) {
        this.pcv = pcv
        invalidate()
    }
}