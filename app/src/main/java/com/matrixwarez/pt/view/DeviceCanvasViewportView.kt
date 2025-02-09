package com.matrixwarez.pt.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.annotation.RequiresApi
import com.matrixwarez.pt.model.InteractiveCanvas

class DeviceCanvasViewportView: View {

    private var deviceViewport: RectF? = null
    private var baseDeviceViewport: RectF? = null

    val viewportLinePaint = Paint()

    private var interactiveCanvas: InteractiveCanvas? = null

    private var viewportScaleFactor = 0F
    var oldScaleFactor = 0F

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
        viewportLinePaint.color = Color.WHITE
        viewportLinePaint.strokeWidth = 3F
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            moveDeviceViewport(PointF(ev.x, ev.y))
            return true
        }
        else if (ev.action == MotionEvent.ACTION_MOVE) {
            if (ev.x.toInt() in 0 until width && ev.y.toInt() in 0 until height) {
                moveDeviceViewport(PointF(ev.x, ev.y))
                return true
            }
        }

        return false
    }

    // scaling
    private val scaleListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            interactiveCanvas?.apply {
                viewportScaleFactor *= detector.scaleFactor

                // Don't let the object get too small or too large.
                viewportScaleFactor = Math.max(minScaleFactor, Math.min(viewportScaleFactor, maxScaleFactor))

                baseDeviceViewport?.apply {
                    deviceViewport = scaledViewport(this, viewportScaleFactor)
                    interactiveCanvasDrawer?.notifyRedraw()
                    //viewportListener?.onDeviceViewportUpdate(scaledViewport(this, viewportScaleFactor))
                }
            }

            return true
        }
    }

    fun scaledViewport(baseViewport: RectF, scaleFactor: Float): RectF {
        val scaledViewport = RectF()

        scaledViewport.left = baseViewport.left
        scaledViewport.top = baseViewport.top
        scaledViewport.right = baseViewport.right
        scaledViewport.bottom = baseViewport.bottom

        scaledViewport.left = scaledViewport.centerX() - (scaledViewport.centerX() - scaledViewport.left) * scaleFactor
        scaledViewport.top = scaledViewport.centerY() - (scaledViewport.centerY() - scaledViewport.top) * scaleFactor
        scaledViewport.right = scaledViewport.centerX() - (scaledViewport.centerX() - scaledViewport.right) * scaleFactor
        scaledViewport.bottom = scaledViewport.centerY() - (scaledViewport.centerY() - scaledViewport.bottom) * scaleFactor

        return scaledViewport
    }

    private val mScaleDetector = ScaleGestureDetector(context, scaleListener)

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        drawToCanvas(canvas)
    }

    private fun drawToCanvas(canvas: Canvas?) {
        canvas?.apply {
            save()
            deviceViewport?.apply {
                canvas.drawLine(left, top, left, bottom, viewportLinePaint)
                canvas.drawLine(left, top, right, top, viewportLinePaint)
                canvas.drawLine(right, top, right, bottom, viewportLinePaint)
                canvas.drawLine(left, bottom, right, bottom, viewportLinePaint)
            }
            restore()
        }
    }

    fun deviceViewport(deviceViewport: RectF?, interactiveCanvasRows: Int, interactiveCanvasCols: Int) {
        deviceViewport?.apply {
            val scaledViewport = RectF()

            scaledViewport.left = left * (width / interactiveCanvasCols.toFloat())
            scaledViewport.top = top * (height / interactiveCanvasRows.toFloat())
            scaledViewport.right = right * (width / interactiveCanvasCols.toFloat())
            scaledViewport.bottom = bottom * (height / interactiveCanvasRows.toFloat())

            this@DeviceCanvasViewportView.deviceViewport = scaledViewport
        }
    }

    fun updateDeviceViewport() {
        interactiveCanvas?.apply {
            updateDeviceViewport(this)
        }
    }

    fun updateDeviceViewport(interactiveCanvas: InteractiveCanvas) {
        this.interactiveCanvas = interactiveCanvas
        viewportScaleFactor = interactiveCanvas.lastScaleFactor

        deviceViewport(interactiveCanvas.deviceViewport, interactiveCanvas.rows, interactiveCanvas.cols)

        baseDeviceViewport = deviceViewport

        invalidate()
    }

    private fun moveDeviceViewport(pointInView: PointF) {
        interactiveCanvas?.apply {
            this@DeviceCanvasViewportView.deviceViewport?.apply {
                val dX = pointInView.x - centerX()
                val dY = pointInView.y - centerY()

                left += dX
                top += dY
                right += dX
                bottom += dY

                var cX = 0F
                var cY = 0F

                if (left < 0) {
                    cX = -left
                }
                if (top < 0) {
                    cY = -top
                }
                if (right > width) {
                    cX = -(right - width)
                }
                if (bottom > height) {
                    cY = -(bottom - height)
                }

                if (cX != 0F) {
                    left += cX
                    right += cX
                }

                if (cY != 0F) {
                    top += cY
                    bottom += cY
                }

                val scaledViewport = RectF()

                scaledViewport.left = left * (interactiveCanvas!!.cols.toFloat() / width)
                scaledViewport.top = top * (interactiveCanvas!!.rows.toFloat() / height)
                scaledViewport.right = right * (interactiveCanvas!!.cols.toFloat() / width)
                scaledViewport.bottom = bottom * (interactiveCanvas!!.rows.toFloat() / height)

                updateDeviceViewport(context, scaledViewport.centerX(), scaledViewport.centerY())
                //viewportListener?.onDeviceViewportUpdate(scaledViewport)
            }
        }
        invalidate()
    }
}