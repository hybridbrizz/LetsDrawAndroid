package com.matrixwarez.pt.view

import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.*
import androidx.annotation.RequiresApi
import com.matrixwarez.pt.helper.Utils
import com.matrixwarez.pt.listener.*
import com.matrixwarez.pt.model.SessionSettings
import com.matrixwarez.pt.model.InteractiveCanvas
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

class InteractiveCanvasView : SurfaceView, InteractiveCanvasDrawer, InteractiveCanvasScaleCallback, SelectedObjectListener {

    enum class Mode {
        EXPLORING,
        PAINTING,
        PAINT_SELECTION,
        EXPORTING,
        OBJECT_MOVE_SELECTION,
        OBJECT_MOVING
    }

    private var mode = Mode.EXPLORING

    var undo = false

    var oldScaleFactor = 0F
    var oldPpu = 0

    val paint = Paint()
    private val gridLinePaint = Paint()
    private val gridLinePaintAlt = Paint()

    var paintActionListener: PaintActionListener? = null

    var lastPanOrScaleTime = 0L

    var pixelHistoryListener: PixelHistoryListener? = null
    var gestureListener: InteractiveCanvasGestureListener? = null

    var objectSelectionListener: ObjectSelectionListener? = null
    var selectedObjectView: SelectedObjectView? = null
    var selectedObjectMoveView: SelectedObjectMoveView? = null

    var canvasEdgeTouchListener: CanvasEdgeTouchListener? = null

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
        interactiveCanvas.selectedObjectListener = this

        // surface view holder
        holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                interactiveCanvas.interactiveCanvasDrawer = this@InteractiveCanvasView
                drawInteractiveCanvas(holder)
            }

            override fun surfaceDestroyed(p0: SurfaceHolder) {
                interactiveCanvas.interactiveCanvasDrawer = null
            }

            override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {

            }
        })
    }

    fun setInitialPositionAndScale() {
        SessionSettings.instance.loadViewportInfo(context)

        // scale
        if (SessionSettings.instance.restoreCanvasScaleFactor != 0F) {
            interactiveCanvas.lastScaleFactor = SessionSettings.instance.restoreCanvasScaleFactor
        }

        scaleFactor = interactiveCanvas.lastScaleFactor
        interactiveCanvas.ppu = (interactiveCanvas.basePpu * scaleFactor).toInt()

        // position
        if (SessionSettings.instance.restoreDeviceViewportCenterX == 0F && SessionSettings.instance.restoreDeviceViewportCenterY == 0F) {
            interactiveCanvas.updateDeviceViewport(
                context,
                interactiveCanvas.rows / 2F, interactiveCanvas.cols / 2F
            )
        }
        else {
            interactiveCanvas.updateDeviceViewport(context, SessionSettings.instance.restoreDeviceViewportCenterX, SessionSettings.instance.restoreDeviceViewportCenterY)
        }
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {

        if (mode == Mode.EXPLORING) {
            // pass event into gesture detectors
            mPanDetector.onTouchEvent(ev)
            mScaleDetector.onTouchEvent(ev)
            mTapDetector.onTouchEvent(ev)
        }
        else if (mode == Mode.PAINTING) {
            if(ev.action == MotionEvent.ACTION_DOWN) {
                interactiveCanvas.interactiveCanvasListener?.apply {
                    if (isPaletteFragmentOpen()) {
                        notifyClosePaletteFragment()
                        return false
                    }
                }

                interactiveCanvas.interactiveCanvasListener?.notifyPaintActionStarted()

                if ((ev.x > width - Utils.dpToPx(context, 50) && !SessionSettings.instance.rightHanded) || (ev.x < Utils.dpToPx(context, 50) && SessionSettings.instance.rightHanded)) {
                    canvasEdgeTouchListener?.onTouchCanvasEdge()
                    return false
                }

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
                    interactiveCanvas.interactiveCanvasListener?.notifyPaintingStarted()
                }
                else if (interactiveCanvas.restorePoints.size == 0) {
                    interactiveCanvas.interactiveCanvasListener?.notifyPaintingEnded()
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
                    interactiveCanvas.interactiveCanvasListener?.notifyPaintingStarted()
                }
                else if (interactiveCanvas.restorePoints.size == 0) {
                    interactiveCanvas.interactiveCanvasListener?.notifyPaintingEnded()
                }
            }
        }
        else if (mode == Mode.PAINT_SELECTION) {
            if (ev.action == MotionEvent.ACTION_DOWN) {
                val unitPoint = interactiveCanvas.screenPointToUnit(ev.x, ev.y)
                unitPoint?.apply {
                    if (unitPoint.x in 0 until interactiveCanvas.cols && unitPoint.y in 0 until interactiveCanvas.rows) {
                        SessionSettings.instance.paintColor = interactiveCanvas.arr[y][x]
                        interactiveCanvas.interactiveCanvasListener?.notifyPaintColorUpdate(SessionSettings.instance.paintColor)
                    }
                }
            }
        }
        else if (mode == Mode.EXPORTING || mode == Mode.OBJECT_MOVE_SELECTION) {
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

                        if (unitPoint != null && interactiveCanvas.unitInBounds(unitPoint)) {
                            if (mode == Mode.EXPORTING) {
                                if (interactiveCanvas.server.isAdmin) {
                                    interactiveCanvas.erasePixels(unitPoint, unitPoint)
                                }
                                else {
                                    interactiveCanvas.exportSelection(unitPoint)
                                }
                            }
                            else if (mode == Mode.OBJECT_MOVE_SELECTION) {
                                val valid = interactiveCanvas.startMoveSelection(unitPoint)

                                if (valid) {
                                    mode = Mode.OBJECT_MOVING
                                }
                            }
                        }
                    }
                    else {
                        val minX = min(unitPoint.x, objectSelectionStartUnit.x)
                        val minY = min(unitPoint.y, objectSelectionStartUnit.y)

                        val maxX = max(unitPoint.x, objectSelectionStartUnit.x)
                        val maxY = max(unitPoint.y, objectSelectionStartUnit.y)

                        val startUnit = Point(minX, minY)
                        val endUnit = Point(minX, minY)

                        if (interactiveCanvas.unitInBounds(startUnit) && interactiveCanvas.unitInBounds(endUnit)) {
                            if (mode == Mode.EXPORTING) {
                                if (interactiveCanvas.server.isAdmin) {
                                    interactiveCanvas.erasePixels(Point(minX, minY), Point(maxX, maxY))
                                }
                                else {
                                    interactiveCanvas.exportSelection(Point(minX, minY), Point(maxX, maxY))
                                }
                            }
                            else if (mode == Mode.OBJECT_MOVE_SELECTION) {
                                val valid = interactiveCanvas.startMoveSelection(Point(minX, minY), Point(maxX, maxY))

                                if (valid) {
                                    mode = Mode.OBJECT_MOVING
                                }
                            }
                        }
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
        else if (mode == Mode.OBJECT_MOVING) {
            mPanDetector.onTouchEvent(ev)
            mScaleDetector.onTouchEvent(ev)
        }

        return true
    }

    // panning
    private val mGestureListener = object : GestureDetector.SimpleOnGestureListener() {

        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {

            interactiveCanvas.translateBy(context, distanceX, distanceY)

            lastPanOrScaleTime = System.currentTimeMillis()

            gestureListener?.onInteractiveCanvasPan()

            return true
        }
    }

    private val mPanDetector = GestureDetector(context, mGestureListener)

    // scaling
    var scaleFactor = 1f

    private val scaleListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            oldScaleFactor = scaleFactor
            scaleFactor *= detector.scaleFactor

            // Don't let the object get too small or too large.
            scaleFactor = Math.max(interactiveCanvas.minScaleFactor, Math.min(scaleFactor, interactiveCanvas.maxScaleFactor))

            oldPpu = interactiveCanvas.ppu
            interactiveCanvas.ppu = (interactiveCanvas.basePpu * scaleFactor).toInt()

            interactiveCanvas.updateDeviceViewport(context, true)

            interactiveCanvas.lastScaleFactor = scaleFactor
            lastPanOrScaleTime = System.currentTimeMillis()

            gestureListener?.onInteractiveCanvasScale()

            return true
        }
    }

    private val mScaleDetector = ScaleGestureDetector(context, scaleListener)

    // pixel tap & long press
    private val mTapListener = object : GestureDetector.SimpleOnGestureListener() {

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            e.apply {
                if (System.currentTimeMillis() - lastPanOrScaleTime > 100) {
                    val unitPoint = interactiveCanvas.screenPointToUnit(x, y)

                    if (unitPoint != null) {
                        interactiveCanvas.lastSelectedUnitPoint = unitPoint

                        if (interactiveCanvas.world) {
                            pixelHistoryListener?.showPixelHistoryFragmentPopover(Point(x.toInt(), y.toInt()))
                        }
                    }
                }
            }

            return true
        }

        override fun onLongPress(e: MotionEvent) {
            e.apply {
                if (System.currentTimeMillis() - lastPanOrScaleTime > 500) {
                    val unitPoint = interactiveCanvas.screenPointToUnit(x, y)

                    if (unitPoint != null) {
                        interactiveCanvas.lastSelectedUnitPoint = unitPoint

                        if (!interactiveCanvas.world) {
                            pixelHistoryListener?.showDrawFrameConfigFragmentPopover(Point(x.toInt(), y.toInt()))
                        }
                    }
                }
            }
        }
    }

    private val mTapDetector = GestureDetector(context, mTapListener)

    override fun notifyScaleCancelled() {
        scaleFactor = oldScaleFactor
        interactiveCanvas.ppu = oldPpu
    }

    // modes
    fun startPainting() {
        if (mode == Mode.OBJECT_MOVE_SELECTION || mode == Mode.OBJECT_MOVING) {
            interactiveCanvas.cancelMoveSelectedObject()
        }

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

        interactiveCanvas.interactiveCanvasDrawer?.notifyRedraw()
        mode = Mode.EXPLORING
    }

    fun startPaintSelection() {
        mode = Mode.PAINT_SELECTION
    }

    fun endPaintSelection() {
        mode = Mode.PAINTING
    }

    fun startExport() {
        mode = Mode.EXPORTING
    }

    fun endExport() {
        mode = Mode.EXPLORING
    }

    fun startObjectMove() {
        mode = Mode.OBJECT_MOVE_SELECTION
    }

    fun endObjectMove() {
        mode = Mode.EXPLORING
    }

    fun isExporting(): Boolean {
        return mode == Mode.EXPORTING
    }

    fun isObjectMoving(): Boolean {
        return mode == Mode.OBJECT_MOVING
    }

    fun isObjectMoveSelection(): Boolean {
        return mode == Mode.OBJECT_MOVE_SELECTION
    }

    // interactive canvas selected object listener
    override fun onObjectSelected() {

    }

    override fun onSelectedObjectMoveStart() {
        val bounds = screenBoundsForSelectedObject()
        selectedObjectMoveView?.showSelectedObjectMoveButtons(bounds)
    }

    override fun onSelectedObjectMoved() {
        val bounds = screenBoundsForSelectedObject()
        selectedObjectMoveView?.updateSelectedObjectMoveButtons(bounds)

        val cX = (bounds.left + bounds.right) / 2
        val cY = (bounds.top + bounds.bottom) / 2

        if (interactiveCanvas.hasSelectedObjectMoved()) {
            selectedObjectView?.showSelectedObjectYesAndNoButtons(Point(cX, cY))
        }
        else {
            selectedObjectView?.hideSelectedObjectYesAndNoButtons()
        }
    }

    override fun onSelectedObjectMoveEnd() {
        endObjectMove()

        selectedObjectMoveView?.hideSelectedObjectMoveButtons()
        selectedObjectMoveView?.selectedObjectMoveEnded()

        selectedObjectView?.hideSelectedObjectYesAndNoButtons()
        selectedObjectView?.selectedObjectEnded()
    }

    private fun screenBoundsForSelectedObject(): Rect {
        val selectedStartUnit = interactiveCanvas.cSelectedStartUnit
        val selectedStartUnitScreen = interactiveCanvas.unitToScreenPoint(selectedStartUnit.x.toFloat(), selectedStartUnit.y.toFloat())

        val selectedEndUnit = interactiveCanvas.cSelectedEndUnit
        val selectedEndUnitScreen = interactiveCanvas.unitToScreenPoint(selectedEndUnit.x.toFloat(), selectedEndUnit.y.toFloat())

        val bounds = Rect(selectedStartUnitScreen!!.x, selectedStartUnitScreen.y,
            selectedEndUnitScreen!!.x + interactiveCanvas.ppu, selectedEndUnitScreen.y + interactiveCanvas.ppu)

        val minWidth = Utils.dpToPx(context, 60)
        val minHeight = Utils.dpToPx(context, 60)

        if (bounds.width() < minWidth) {
            val diff = minWidth - bounds.width()
            bounds.left -= diff / 2
            bounds.right += diff / 2
        }

        if (bounds.height() < minHeight) {
            val diff = minHeight - bounds.height()
            bounds.top -= diff / 2
            bounds.bottom += diff / 2
        }

        return bounds
    }

    // canvas frames
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

    private val coroutineScope = CoroutineScope(Dispatchers.Main.immediate)
    private var redrawLoopJob: Job? = null

    // draw canvas
    override fun notifyRedraw() {
        when (interactiveCanvas.errorPixels.isNotEmpty() && redrawLoopJob == null) {
            true -> {
                redrawLoopJob = coroutineScope.launch {
                    while (interactiveCanvas.errorPixels.isNotEmpty()) {
                        drawInteractiveCanvas(holder)
                        withContext(Dispatchers.Default) {
                            delay((1000 / 30f).roundToLong())
                        }
                    }
                    redrawLoopJob?.cancel()
                    redrawLoopJob = null
                }
            }
            false -> {
                drawInteractiveCanvas(holder)
            }
        }
    }

    fun drawInteractiveCanvas(holder: SurfaceHolder) {
        Log.d("Draw Interactive Canvas", "Redraw")

        paint.color = Color.parseColor("#FFFFFFFF")

        val canvas = holder.lockCanvas()

        val deviceViewport = interactiveCanvas.deviceViewport!!
        val ppu = interactiveCanvas.ppu

        canvas.drawARGB(255, 0, 0, 0)

        drawUnits(canvas)
        drawErrors(canvas)
        drawGridLines(canvas, deviceViewport, ppu)

        holder.unlockCanvasAndPost(canvas)
    }

    private fun drawGridLines(canvas: Canvas, deviceViewport: RectF, ppu: Int) {
        val gridLineMode = SessionSettings.instance.gridLineMode
        if (gridLineMode == InteractiveCanvas.GRID_LINE_MODE_ON && interactiveCanvas.ppu >= interactiveCanvas.autoCloseGridLineThreshold) {

            val gridLineColor = interactiveCanvas.getGridLineColor()

            gridLinePaint.strokeWidth = 1f
            gridLinePaint.color = gridLineColor

            gridLinePaintAlt.strokeWidth = 1f
            gridLinePaintAlt.color = gridLineColor

            if (!interactiveCanvas.world) {
                gridLinePaint.color = interactiveCanvas.getGridLineColor()
                gridLinePaintAlt.color = interactiveCanvas.getGridLineColor()
            }

            val unitsWide = canvas.width / interactiveCanvas.ppu
            val unitsTall = canvas.height / interactiveCanvas.ppu

            val gridXOffsetPx = (ceil(deviceViewport.left) - deviceViewport.left) * ppu
            val gridYOffsetPx = (ceil(deviceViewport.top) - deviceViewport.top) * ppu

            for (y in 0..unitsTall) {
                if (y % 2 == 0) {
                    canvas.drawLine(
                        0F,
                        floor(y * ppu.toFloat() + gridYOffsetPx),
                        canvas.width.toFloat(),
                        floor(y * ppu.toFloat() + gridYOffsetPx),
                        gridLinePaint
                    )
                }
                else {
                    canvas.drawLine(
                        0F,
                        floor(y * ppu.toFloat() + gridYOffsetPx),
                        canvas.width.toFloat(),
                        floor(y * ppu.toFloat() + gridYOffsetPx),
                        gridLinePaintAlt
                    )
                }
            }

            for (x in 0..unitsWide) {
                if (x % 2 == 0) {
                    canvas.drawLine(
                        x * ppu.toFloat() + gridXOffsetPx,
                        0F,
                        x * ppu.toFloat() + gridXOffsetPx,
                        canvas.height.toFloat(),
                        gridLinePaint
                    )
                }
                else {
                    canvas.drawLine(
                        x * ppu.toFloat() + gridXOffsetPx,
                        0F,
                        x * ppu.toFloat() + gridXOffsetPx,
                        canvas.height.toFloat(),
                        gridLinePaintAlt
                    )
                }
            }
        }

    }

    private fun drawErrors(canvas: Canvas) {
        val errorPixels = interactiveCanvas.errorPixels

        errorPixels.forEach { pixel ->
            val rect = interactiveCanvas.getScreenSpaceForUnit(
                pixel.x,
                pixel.y
            )
            paint.color = pixel.getColor()
            canvas.drawRect(rect, paint)
        }

        var i = 0
        while (i < errorPixels.size) {
            val pixel = errorPixels[i]
            if (!pixel.isActive) {
                errorPixels.removeAt(i)
                i--
            }
            i++
        }
    }

    private fun drawUnits(canvas: Canvas) {
        val interactiveCanvas = interactiveCanvas

        interactiveCanvas.deviceViewport?.apply {
            val startUnitIndexX = floor(left).toInt()
            val endUnitIndexX = ceil(right).toInt()
            val startUnitIndexY = floor(top).toInt()
            val endUnitIndexY = ceil(bottom).toInt()

            val rangeX = endUnitIndexX - startUnitIndexX
            val rangeY = endUnitIndexY - startUnitIndexY

            paint.color = Color.BLACK

            val isObjectSelected = interactiveCanvas.selectedPixels != null

            val backgroundColors = interactiveCanvas.getBackgroundColors(SessionSettings.instance.backgroundColorsIndex)

            for (x in 0..rangeX) {
                for (y in 0..rangeY) {
                    val unitX = x + startUnitIndexX
                    val unitY = y + startUnitIndexY

                    val inGrid = unitX >= 0 && unitX < interactiveCanvas.cols && unitY >= 0 && unitY < interactiveCanvas.rows

                    if (inGrid) {
                        val color = interactiveCanvas.arr[unitY][unitX]

                        // background
                        if (color == 0) {
                            if ((unitX + unitY) % 2 == 0) {
                                paint.color = backgroundColors[0]
                            }
                            else {
                                paint.color = backgroundColors[1]
                            }
                        }
                        else {
                            paint.color = interactiveCanvas.arr[unitY][unitX]
                        }
                    }
                    else {
                        paint.color = Color.BLACK
                    }
                    val rect = interactiveCanvas.getScreenSpaceForUnit(
                        x + startUnitIndexX,
                        y + startUnitIndexY
                    )

                    if (isObjectSelected) {
                        paint.color = Utils.brightenColor(paint.color, -0.5F)
                    }
                    canvas.drawRect(rect, paint)
                }
            }

            // selected object
            interactiveCanvas.selectedPixels?.apply {
                for (pixel in this) {
                    val x = pixel.point.x
                    val y = pixel.point.y

                    if (x in startUnitIndexX..endUnitIndexX && y in startUnitIndexY..endUnitIndexY) {
                        paint.color = pixel.color
                        val rect = interactiveCanvas.getScreenSpaceForUnit(x, y)

                        canvas.drawRect(rect, paint)
                    }
                }
            }
        }
    }
}