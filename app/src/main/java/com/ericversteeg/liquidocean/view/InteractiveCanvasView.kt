package com.ericversteeg.liquidocean.view

import android.content.Context
import android.util.AttributeSet
import android.view.*
import androidx.annotation.RequiresApi
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

    var interactiveCanvas = InteractiveCanvas()

    private fun commonInit() {
        interactiveCanvas.updateDeviceViewport(context, interactiveCanvas.rows / 2F, interactiveCanvas.cols / 2F)
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        // Let the ScaleGestureDetector inspect all events.
        mPanDetector.onTouchEvent(ev)
        mScaleDetector.onTouchEvent(ev)

        return true
    }

    // panning

    private val mGestureListener = object : GestureDetector.SimpleOnGestureListener() {

        override fun onScroll(
            e1: MotionEvent,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {

            // Log.i("Drag distance", "x=$distanceX, y=$distanceY")
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
            mScaleFactor = Math.max(0.1f, Math.min(mScaleFactor, 10.0f))

            interactiveCanvas.ppu = (interactiveCanvas.basePpu * mScaleFactor).toInt()
            interactiveCanvas.updateDeviceViewport(context)
            interactiveCanvas.drawCallbackListener?.notifyRedraw()

            return true
        }
    }

    private val mScaleDetector = ScaleGestureDetector(context, scaleListener)
}