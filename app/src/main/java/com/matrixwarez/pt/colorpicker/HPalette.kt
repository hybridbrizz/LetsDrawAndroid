package com.matrixwarez.pt.colorpicker

import android.R.attr.radius
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Region
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.widget.FrameLayout
import com.matrixwarez.pt.R
import java.lang.Float.min
import java.nio.IntBuffer
import kotlin.math.max


class HPalette: FrameLayout {

    interface HColorSelectionListener {
        fun onHChanged()
    }

    private val logging = false

    private val resolutionConstant = 4

    private var w = 0

    private lateinit var pixels: IntArray

    private var pcv: PickedColorValues? = null

    var hSelectionListener: HColorSelectionListener? = null

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
        setWillNotDraw(false)
    }

    fun resize() {
        w = width / resolutionConstant

        pixels = IntArray(w)

        Log.d("Test redraw", "init: w = $width")
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val sTime = System.currentTimeMillis()

        Log.d("Test redraw", "outer draw: w = $width")

        canvas.apply {
            save()

            if (w > 0) {
                pcv?.let {
                    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), Paint().apply { color = Color.BLACK })

                    val cornerRadius = 5 * resources.displayMetrics.density

                    // 1. Create a Path for the Rounded Rectangle
                    val path = Path()
                    path.addRoundRect(
                        RectF(
                            0f,
                            0f,
                            width.toFloat(),
                            height.toFloat(),
                        ), cornerRadius, cornerRadius, Path.Direction.CW
                    )

                    // 2. Clip the Canvas
                    canvas.clipPath(path)

                    Log.d("Test redraw", "inner draw: w = $width")
                    drawHuePalette(canvas, it)
                }
            }

            restore()
        }

        val duration = System.currentTimeMillis() - sTime

        if (logging) {
            Log.i("Hue Fps = ", (1000 / duration.toFloat()).toString())
        }

        moveIndicator()
    }

    private fun drawHuePalette(canvas: Canvas, pcv: PickedColorValues) {
        val wf = w.toFloat()

        for (x in 0 until w) {
            val h = x / wf * (pcv.maxValue * 360)

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
            pcv?.let {
                it.h = max(min(event.x / width.toFloat() * it.maxValue, it.maxValue), it.minValue)

                hSelectionListener?.onHChanged()

                moveIndicator()
            }

            true
        } else {
            super.onTouchEvent(event)
        }
    }

    fun moveIndicator() {
        pcv?.let {
            val indicator = findViewById<SBIndicator>(R.id.h_indicator)
            indicator.x = it.h / it.maxValue * width - indicator.width / 2
        }
    }

    fun setPCV(pcv: PickedColorValues) {
        this.pcv = pcv
        invalidate()
    }
}