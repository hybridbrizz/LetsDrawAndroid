package com.ericversteeg.liquidocean.view

import android.content.Context
import android.graphics.Color
import android.graphics.Point
import android.graphics.PointF
import android.graphics.RectF
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.*
import androidx.annotation.RequiresApi
import com.ericversteeg.liquidocean.listener.*
import com.ericversteeg.liquidocean.model.SessionSettings
import com.ericversteeg.liquidocean.model.InteractiveCanvas
import org.json.JSONArray
import java.util.*
import kotlin.math.max
import kotlin.math.min

class InteractiveCanvasView : SurfaceView, InteractiveCanvasScaleCallback, DeviceCanvasViewportListener {

    enum class Mode {
        EXPLORING,
        PAINTING,
        PAINT_SELECTION,
        EXPORTING
    }

    private var mode = Mode.EXPLORING
    private var lastMode = Mode.EXPLORING

    var undo = false

    var oldScaleFactor = 0F
    var oldPpu = 0

    var paintActionListener: PaintActionListener? = null

    var lastPanOrScaleTime = 0L

    var pixelHistoryListener: PixelHistoryListener? = null
    var gestureListener: InteractiveCanvasGestureListener? = null

    var objectSelectionListener: ObjectSelectionListener? = null

    lateinit var objectSelectionStartUnit: Point
    lateinit var objectSelectionStartPoint: PointF

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

    var interactiveCanvas = InteractiveCanvas(context, SessionSettings.instance)

    private fun commonInit() {
        interactiveCanvas.scaleCallbackListener = this

        mScaleFactor = interactiveCanvas.startScaleFactor
        interactiveCanvas.ppu = (interactiveCanvas.basePpu * mScaleFactor).toInt()

        //interactiveCanvas.updateDeviceViewport(context, interactiveCanvas.rows / 2F, interactiveCanvas.cols / 2F)

        /*Timer().schedule(object: TimerTask() {
            override fun run() {
                val rT = (Math.random() * 20 + 1).toInt()
                Timer().schedule(object: TimerTask() {
                    override fun run() {
                        simulateDraw()
                    }
                }, 1000L * rT)
            }

        }, 3000)*/
    }

    fun simulateDraw() {
        val rSmallAmt = (Math.random() * 20 + 2).toInt()
        val rBigAmt = (Math.random() * 100 + 50).toInt()

        startPainting()

        val r = (Math.random() * 10).toInt()
        if (r < 2) {
            for (i in 0 until rBigAmt) {
                val rX = (Math.random() * interactiveCanvas.cols).toInt()
                val rY = (Math.random() * interactiveCanvas.rows).toInt()
                interactiveCanvas.paintUnitOrUndo(Point(rX, rY))
            }
        }
        else {
            for (i in 0 until rSmallAmt) {
                val rX = (Math.random() * interactiveCanvas.cols).toInt()
                val rY = (Math.random() * interactiveCanvas.rows).toInt()
                interactiveCanvas.paintUnitOrUndo(Point(rX, rY))
            }
        }

        endPainting(true)
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {

        if (mode == Mode.EXPLORING) {
            // Let the ScaleGestureDetector inspect all events.
            mPanDetector.onTouchEvent(ev)
            mScaleDetector.onTouchEvent(ev)
            mTapDetector.onTouchEvent(ev)
        }
        else if (mode == Mode.PAINTING) {
            interactiveCanvas.drawCallbackListener?.apply {
                if (isPaletteFragmentOpen()) {
                    notifyClosePaletteFragment()
                    return false
                }
            }

            if(ev.action == MotionEvent.ACTION_DOWN) {
                interactiveCanvas.drawCallbackListener?.notifyCloseRecentColors()

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

                    paintActionListener?.onPaintStart()
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
                    // Log.i("Unit Tap", "Tapped on unit $unitPoint")

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
                    if (unitPoint.x in 0 until interactiveCanvas.cols && unitPoint.y in 0 until interactiveCanvas.rows) {
                        SessionSettings.instance.paintColor = interactiveCanvas.arr[y][x]
                        interactiveCanvas.drawCallbackListener?.notifyPaintColorUpdate(SessionSettings.instance.paintColor)
                    }
                }
            }
        }
        else if (mode == Mode.EXPORTING) {
            if (ev.action == MotionEvent.ACTION_DOWN) {
                val unitPoint = interactiveCanvas.screenPointToUnit(ev.x, ev.y)

                if (unitPoint != null) {
                    objectSelectionStartUnit = unitPoint
                    objectSelectionStartPoint = PointF(ev.x, ev.y)
                }
            }
            else if (ev.action == MotionEvent.ACTION_UP) {
                val unitPoint = interactiveCanvas.screenPointToUnit(ev.x, ev.y)

                if (unitPoint != null) {
                    if (unitPoint.x == objectSelectionStartUnit.x && unitPoint.y == objectSelectionStartUnit.y) {
                        val unitPoint = interactiveCanvas.screenPointToUnit(ev.x, ev.y)

                        if (unitPoint != null) {
                            interactiveCanvas.exportSelection(unitPoint)
                        }
                    }
                    else {
                        val minX = min(unitPoint.x, objectSelectionStartUnit.x)
                        val minY = min(unitPoint.y, objectSelectionStartUnit.y)

                        val maxX = max(unitPoint.x, objectSelectionStartUnit.x)
                        val maxY = max(unitPoint.y, objectSelectionStartUnit.y)

                        interactiveCanvas.exportSelection(Point(minX, minY), Point(maxX, maxY))
                    }
                    objectSelectionListener?.onObjectSelectionEnded()
                }
            }
            else if (ev.action == MotionEvent.ACTION_MOVE) {
                val unitPoint = interactiveCanvas.screenPointToUnit(ev.x, ev.y)

                if (unitPoint != null) {
                    val minX = min(ev.x, objectSelectionStartPoint.x)
                    val minY = min(ev.y, objectSelectionStartPoint.y)

                    val maxX = max(ev.x, objectSelectionStartPoint.x)
                    val maxY = max(ev.y, objectSelectionStartPoint.y)

                    objectSelectionListener?.onObjectSelectionBoundsChanged(PointF(minX, minY), PointF(maxX, maxY))
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
            interactiveCanvas.translateBy(context, distanceX, distanceY)

            lastPanOrScaleTime = System.currentTimeMillis()

            gestureListener?.onInteractiveCanvasPan()

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
            mScaleFactor = Math.max(interactiveCanvas.minScaleFactor, Math.min(mScaleFactor, interactiveCanvas.maxScaleFactor))

            oldPpu = interactiveCanvas.ppu
            interactiveCanvas.ppu = (interactiveCanvas.basePpu * mScaleFactor).toInt()

            interactiveCanvas.updateDeviceViewport(context, true)
            interactiveCanvas.drawCallbackListener?.notifyRedraw()

            interactiveCanvas.lastScaleFactor = mScaleFactor
            lastPanOrScaleTime = System.currentTimeMillis()

            gestureListener?.onInteractiveCanvasScale()

            return true
        }
    }

    private val mScaleDetector = ScaleGestureDetector(context, scaleListener)

    // pixel tap
    private val mTapListener = object : GestureDetector.SimpleOnGestureListener() {

        override fun onSingleTapUp(e: MotionEvent?): Boolean {
            e?.apply {
                if (System.currentTimeMillis() - lastPanOrScaleTime > 500) {
                    val unitPoint = interactiveCanvas.screenPointToUnit(x, y)

                    if (unitPoint != null) {
                        interactiveCanvas.lastSelectedUnitPoint = unitPoint

                        if (interactiveCanvas.world) {
                            pixelHistoryListener?.showPixelHistoryFragmentPopover(Point(x.toInt(), y.toInt()))
                        }
                        else {
                            pixelHistoryListener?.showDrawFrameConfigFragmentPopover(Point(x.toInt(), y.toInt()))
                        }
                    }
                }
            }

            return true
        }
    }

    private val mTapDetector = GestureDetector(context, mTapListener)

    override fun notifyScaleCancelled() {
        mScaleFactor = oldScaleFactor
        interactiveCanvas.ppu = oldPpu
    }

    fun startExport() {
        mode = Mode.EXPORTING
    }

    fun endExport() {
        mode = Mode.EXPLORING
    }

    fun isExporting(): Boolean {
        return mode == Mode.EXPORTING
    }

    fun createDrawFrame(centerX: Int, centerY: Int, width: Int, height: Int, color: Int) {
        startPainting()

        val oldColor = SessionSettings.instance.paintColor
        SessionSettings.instance.paintColor = color

        var minX = centerX - width / 2
        var maxX = centerX + width / 2 + 1
        var minY = centerY - height / 2
        var maxY = centerY + height / 2 + 1

        if (width % 2 != 0) {
            minX = centerX - (width + 1) / 2
            maxX = centerX + (width + 1) / 2
        }

        if (height % 2 != 0) {
            minY = centerY - (height + 1) / 2
            maxY = centerY + (height + 1) / 2
        }

        // left
        for (y in minY..maxY) {
            interactiveCanvas.paintUnitOrUndo(Point(minX, y), redraw = false)
        }
        // right
        for (y in minY..maxY) {
            interactiveCanvas.paintUnitOrUndo(Point(maxX, y), redraw = false)
        }
        // top
        for (x in minX..maxX) {
            interactiveCanvas.paintUnitOrUndo(Point(x, minY), redraw = false)
        }
        // bottom
        for (x in minX..maxX) {
            interactiveCanvas.paintUnitOrUndo(Point(x, maxY), redraw = false)
        }

        SessionSettings.instance.paintColor = oldColor

        endPainting(true)
    }

    override fun onDeviceViewportUpdate(viewport: RectF) {
        interactiveCanvas.deviceViewport = viewport
        interactiveCanvas.drawCallbackListener?.notifyRedraw()
    }
}