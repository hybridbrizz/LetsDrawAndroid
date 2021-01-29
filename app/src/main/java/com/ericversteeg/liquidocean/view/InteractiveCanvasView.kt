package com.ericversteeg.liquidocean.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import androidx.annotation.RequiresApi
import androidx.core.graphics.xor
import androidx.core.view.ViewCompat
import com.ericversteeg.liquidocean.listener.InteractiveCanvasDrawerCallback
import com.ericversteeg.liquidocean.model.InteractiveCanvas

class InteractiveCanvasView : SurfaceView {

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

    @RequiresApi(21) constructor(context: Context, attributeSet: AttributeSet, v0: Int, v1: Int) : super(
        context,
        attributeSet,
        v0,
        v1
    ) {
        commonInit()
    }

    // The ‘active pointer’ is the one currently moving our object.
    private var mActivePointerId = 20

    var mLastTouchX = 0F
    var mLastTouchY = 0F

    var mPosX = 0F
    var mPosY = 0F

    private val INVALID_POINTER_ID = 23

    private var axisXMin = 0F
    private var axisYMin = 0F
    private var axisXMax = 0F
    private var axisYMax = 0F

    var interactiveCanvas = InteractiveCanvas()

    private fun commonInit() {
        val displayMetrics = DisplayMetrics()
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        windowManager.defaultDisplay.getMetrics(displayMetrics)
        axisYMax = displayMetrics.heightPixels.toFloat()
        axisXMax = displayMetrics.widthPixels.toFloat()

        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        val canvasWidthPx = interactiveCanvas.rows * interactiveCanvas.ppu
        val canvasHeightPx = interactiveCanvas.cols * interactiveCanvas.ppu

        val top = (canvasHeightPx / 2 - screenHeight / 2) / interactiveCanvas.ppu.toFloat()
        val bottom = (canvasHeightPx / 2 + screenHeight / 2) / interactiveCanvas.ppu.toFloat()
        val left = (canvasWidthPx / 2 - screenWidth / 2) / interactiveCanvas.ppu.toFloat()
        val right = (canvasWidthPx / 2 + screenWidth / 2) / interactiveCanvas.ppu.toFloat()

        interactiveCanvas.deviceViewport = RectF(left, top, right, bottom)
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        // Let the ScaleGestureDetector inspect all events.
        mPanDetector.onTouchEvent(ev)
       // mScaleDetector.onTouchEvent(ev)

        return true
    }

    // panning

    // The current viewport. This rectangle represents the currently visible
    // chart domain and range.
    private val mCurrentViewport = RectF(axisXMin, axisYMin, axisXMax, axisYMax)

    // The current destination rectangle (in pixel coordinates) into which the
    // chart data should be drawn.
    private val mContentRect = RectF(mCurrentViewport.left, mCurrentViewport.top, mCurrentViewport.right, mCurrentViewport.bottom)

    private val mGestureListener = object : GestureDetector.SimpleOnGestureListener() {

        override fun onScroll(
            e1: MotionEvent,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {

            Log.i("Drag distance", "x=$distanceX, y=$distanceY")
            interactiveCanvas.translateBy(distanceX, distanceY)

            return true
        }
    }

    private val mPanDetector = GestureDetector(context, mGestureListener)

    // scaling

    private var mScaleFactor = 1f

    private val scaleListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            mScaleFactor *= detector.scaleFactor

            // Don't let the object get too small or too large.
            mScaleFactor = Math.max(0.1f, Math.min(mScaleFactor, 5.0f))

            val canvas = holder.lockCanvas()
            canvas.scale(2F, 2F)
            holder.unlockCanvasAndPost(canvas)

            return true
        }
    }

    private val mScaleDetector = ScaleGestureDetector(context, scaleListener)

    // viewport setting

    private fun setViewportTopLeft(x: Float, y: Float) {
        /*
         * Constrains within the scroll range. The scroll range is simply the viewport
         * extremes (AXIS_X_MAX, etc.) minus the viewport size. For example, if the
         * extremes were 0 and 10, and the viewport size was 2, the scroll range would
         * be 0 to 8.
         */

        val curWidth: Float = mCurrentViewport.width()
        val curHeight: Float = mCurrentViewport.height()

        /*val newX: Float = Math.max(axisXMin, Math.min(x, axisXMax - curWidth))
        val newY: Float = Math.max(axisYMin + curHeight, Math.min(y, axisYMax))

        mCurrentViewport.set(newX, newY - curHeight, newX + curWidth, newY)*/

        mCurrentViewport.set(x, y, x + curWidth, y + curHeight)
    }
}