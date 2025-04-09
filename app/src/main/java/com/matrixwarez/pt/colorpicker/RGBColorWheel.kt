package com.matrixwarez.pt.colorpicker

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ComposeShader
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.PorterDuff
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import com.matrixwarez.pt.R
import com.matrixwarez.pt.helper.Utils
import java.lang.Float.min
import java.nio.IntBuffer
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin


class RGBColorWheel: FrameLayout {

    interface RGBSelectionListener {
        fun onRGBChanged()
    }

    var rgbSelectionListener: RGBSelectionListener? = null

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
        setWillNotDraw(false)
//        clipChildren = false
//        clipToOutline = false
//        clipToPadding = false
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        Log.d("Test Specific Test", "w = ${canvas.width}")

        canvas.apply {
            save()

            pcv?.let {
                drawColorWheel(canvas, it)
            }

            restore()
        }

        moveIndicator()
    }

    /**
     * Draws a color wheel on the Canvas with brightness value from PickedColorValues
     */
    private fun drawColorWheel(canvas: Canvas, pcv: PickedColorValues) {
        val center = PointF(width / 2f, height / 2f)
        val radius = Math.min(width, height) / 2f

        // Create a paint object for our drawing
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Step 1: Create hue colors for the sweep gradient (going around the circle)
        val hueColors = IntArray(361) // 0-360 degrees
        for (i in hueColors.indices) {
            hueColors[i] = Color.HSVToColor(floatArrayOf(i.toFloat(), 1f, pcv.b))
        }

        // Create the sweep gradient for hues (going around the circle clockwise)
        // Note: Android's SweepGradient starts at 3 o'clock position and goes clockwise
        val sweepGradient = SweepGradient(
            center.x, center.y,
            hueColors,
            null // Positions are evenly distributed
        )

        // Step 2: Create a radial gradient mask from transparent white at center to opaque white at edge
        // This will be used as a porter-duff mask with the hue gradient
        val transparentWhite = Color.argb((255 - (pcv.b * 255)).roundToInt(), 255, 255, 255)
        val opaqueWhite = Color.argb(255, 255, 255, 255)

        val saturationMask = RadialGradient(
            center.x, center.y, radius,
            transparentWhite, opaqueWhite,
            Shader.TileMode.CLAMP
        )

        // Step 3: Combine the two gradients using a ComposeShader
        val wheelShader = ComposeShader(
            sweepGradient,
            saturationMask,
            PorterDuff.Mode.DST_IN
        )

        // Apply the shader to the paint
        paint.shader = wheelShader

        // Draw a white circle first to ensure center is white/low saturation
        paint.shader = null
        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
        canvas.drawCircle(center.x, center.y, radius, paint)

        // Now draw the color wheel over it
        paint.shader = wheelShader
        canvas.drawCircle(center.x, center.y, radius, paint)

        // Optional: draw a subtle border around the wheel for definition
        paint.shader = null
        paint.color = Color.LTGRAY
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawCircle(center.x, center.y, radius, paint)
    }

    /**
     * Returns the color at the given point in the color wheel
     */
    fun colorAt(point: PointF): Int? {
        val center = PointF(width / 2f, height / 2f)
        val radius = Math.min(width, height) / 2f

        // Calculate distance from center
        val dx = point.x - center.x
        val dy = point.y - center.y
        val distance = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()

        // Check if the point is within the wheel's radius
        if (distance > radius) {
            return null // Point is outside the color wheel
        }

        // Calculate angle (in radians, 0 is to the right, increasing counterclockwise)
        var angle = Math.atan2(dy.toDouble(), dx.toDouble()).toFloat()

        // Convert to positive angle (0 to 2π)
        if (angle < 0) {
            angle += 2.0f * Math.PI.toFloat()
        }

        // Convert angle to hue (0 to 360)
        val hue = (angle / (2.0f * Math.PI.toFloat())) * 360f

        // Calculate saturation based on distance from center
        val saturation = distance / radius

        // Create HSV color
        val hsv = floatArrayOf(hue, saturation, pcv?.b ?: 1.0f)
        return Color.HSVToColor(hsv)
    }

    /**
     * Returns the point for a given color based on hue and saturation
     */
    fun pointForColor(h: Float, s: Float): PointF {
        val center = PointF(width / 2f, height / 2f)
        val radius = Math.min(width, height) / 2f

        // Convert hue to angle (0 to 2π)
        val angle = h * 2.0f * Math.PI.toFloat()

        // Calculate the distance from center based on saturation
        // Full saturation (1.0) is at the edge of the wheel
        val distance = s * radius

        // Calculate x, y coordinates from angle and distance
        val x = center.x + distance * Math.cos(angle.toDouble()).toFloat()
        val y = center.y + distance * Math.sin(angle.toDouble()).toFloat()

        return PointF(x, y)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
            pcv?.let { pcv ->
                val color = colorAt(PointF(event.x, event.y))

                color?.let {
                    val hsb = floatArrayOf(0f, 0f, 0f)
                    Color.colorToHSV(color, hsb)

                    pcv.h = hsb[0] / 360f
                    pcv.s = hsb[1]

                    rgbSelectionListener?.onRGBChanged()

                    moveIndicator()
                }
            }

            true
        } else {
            super.onTouchEvent(event)
        }
    }

    fun moveIndicator() {
        pcv?.let {
            val indicator = findViewById<SBIndicator>(R.id.sb_indicator)
            val point = pointForColor(it.h, it.s)
            indicator.x = point.x - indicator.width / 2
            indicator.y = point.y - indicator.height / 2
        }
    }

    fun setPCV(pcv: PickedColorValues) {
        this.pcv = pcv
        postInvalidate()
    }
}