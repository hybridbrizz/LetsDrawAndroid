package com.ericversteeg.liquidocean.view

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.*
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintSet
import com.ericversteeg.liquidocean.helper.SessionSettings
import com.ericversteeg.liquidocean.listener.InteractiveCanvasScaleCallback
import com.ericversteeg.liquidocean.model.InteractiveCanvas
import org.json.JSONArray
import kotlin.math.min

class InteractiveCanvasView : SurfaceView, InteractiveCanvasScaleCallback {

    enum class Mode {
        EXPLORING,
        PAINTING,
        PAINT_SELECTION
    }

    private var mode = Mode.EXPLORING
    private var lastMode = Mode.EXPLORING

    var undo = false

    var oldScaleFactor = 0F
    var oldPpu = 0

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

    var interactiveCanvas = InteractiveCanvas(context)

    private fun commonInit() {
        interactiveCanvas.scaleCallbackListener = this
        interactiveCanvas.updateDeviceViewport(context, interactiveCanvas.rows / 2F, interactiveCanvas.cols / 2F)
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {

        if (mode == Mode.EXPLORING) {
            // Let the ScaleGestureDetector inspect all events.
            mPanDetector.onTouchEvent(ev)
            mScaleDetector.onTouchEvent(ev)
        }
        else if (mode == Mode.PAINTING) {

            if(ev.action == MotionEvent.ACTION_DOWN) {
                val unitPoint = interactiveCanvas.screenPointToUnit(ev.x, ev.y)

                unitPoint?.apply {
                    Log.i("Unit Tap", "Tapped on unit $unitPoint")

                    undo = interactiveCanvas.unitInRestorePoints(this) != null

                    if (undo) {
                        // undo
                        interactiveCanvas.paintUnitOrUndo(unitPoint, 1)
                    }
                    else {
                        // paint
                        interactiveCanvas.paintUnitOrUndo(unitPoint)
                    }
                }

                if (interactiveCanvas.restorePoints.size == 1) {
                    interactiveCanvas.drawCallbackListener?.notifyPaintingStarted()
                }
                else if (interactiveCanvas.restorePoints.size == 0) {
                    interactiveCanvas.drawCallbackListener?.notifyPaintingEnded()
                }
            }
            else if(ev.action == MotionEvent.ACTION_MOVE) {
                val unitPoint = interactiveCanvas.screenPointToUnit(ev.x, ev.y)

                unitPoint?.apply {
                    Log.i("Unit Tap", "Tapped on unit $unitPoint")

                    if (undo) {
                        // undo
                        interactiveCanvas.paintUnitOrUndo(unitPoint, 1)
                    }
                    else {
                        // paint
                        interactiveCanvas.paintUnitOrUndo(unitPoint)
                    }
                }

                if (interactiveCanvas.restorePoints.size == 1) {
                    interactiveCanvas.drawCallbackListener?.notifyPaintingStarted()
                }
                else if (interactiveCanvas.restorePoints.size == 0) {
                    interactiveCanvas.drawCallbackListener?.notifyPaintingEnded()
                }
            }
        }
        else if (mode == Mode.PAINT_SELECTION) {
            if (ev.action == MotionEvent.ACTION_DOWN) {
                val unitPoint = interactiveCanvas.screenPointToUnit(ev.x, ev.y)
                unitPoint?.apply {
                    SessionSettings.instance.paintColor = interactiveCanvas.arr[y][x]
                    interactiveCanvas.drawCallbackListener?.notifyPaintColorUpdate(SessionSettings.instance.paintColor)
                }
            }
        }

        return true
    }

    fun startPainting() {
        mode = Mode.PAINTING
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun endPainting(accept: Boolean) {
        if (!accept) {
            interactiveCanvas.undoPendingPaint()
            SessionSettings.instance.dropsAmt += interactiveCanvas.restorePoints.size
        }
        else {
            // before restore points are cleared
            interactiveCanvas.commitPixels()
        }

        interactiveCanvas.clearRestorePoints()

        interactiveCanvas.drawCallbackListener?.notifyRedraw()
        mode = Mode.EXPLORING
    }

    fun startPaintSelection() {
        mode = Mode.PAINT_SELECTION
    }

    fun endPaintSelection() {
        mode = Mode.PAINTING
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
            oldScaleFactor = mScaleFactor
            mScaleFactor *= detector.scaleFactor

            // Don't let the object get too small or too large.
            mScaleFactor = Math.max(0.1f, Math.min(mScaleFactor, 10.0f))

            oldPpu = interactiveCanvas.ppu
            interactiveCanvas.ppu = (interactiveCanvas.basePpu * mScaleFactor).toInt()

            interactiveCanvas.updateDeviceViewport(context, true)
            interactiveCanvas.drawCallbackListener?.notifyRedraw()

            return true
        }
    }

    private val mScaleDetector = ScaleGestureDetector(context, scaleListener)

    override fun notifyScaleCancelled() {
        mScaleFactor = oldScaleFactor
        interactiveCanvas.ppu = oldPpu
    }
}