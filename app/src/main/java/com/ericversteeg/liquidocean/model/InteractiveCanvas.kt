package com.ericversteeg.liquidocean.model

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Point
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.annotation.RequiresApi
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.ericversteeg.liquidocean.helper.Utils
import com.ericversteeg.liquidocean.listener.*
import com.ericversteeg.liquidocean.view.ActionButtonView
import io.socket.client.Socket
import org.json.JSONArray
import org.json.JSONObject
import java.lang.Exception
import java.net.URISyntaxException
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.set
import kotlin.math.floor


class InteractiveCanvas(var context: Context, val sessionSettings: SessionSettings) {

    var rows = 1024
    var cols = 1024

    lateinit var arr: Array<IntArray>

    val basePpu = 100
    var ppu = basePpu

    val autoCloseGridLineThreshold = 50

    var deviceViewport: RectF? = null

    var interactiveCanvasListener: InteractiveCanvasListener? = null
    var interactiveCanvasDrawer: InteractiveCanvasDrawer? = null
    var scaleCallbackListener: InteractiveCanvasScaleCallback? = null
    var recentColorsListener: RecentColorsListener? = null
    var artExportListener: ArtExportListener? = null
    var deviceCanvasViewportResetListener: DeviceCanvasViewportResetListener? = null
    var selectedObjectListener: SelectedObjectListener? = null

    var recentColorsList: MutableList<Int> = ArrayList()

    var restorePoints = ArrayList<RestorePoint>()

    val maxScaleFactor = 10.0F
    val minScaleFactor = 0.15F

    val startScaleFactor = 0.5f
    var lastScaleFactor = startScaleFactor

    var lastSelectedUnitPoint = Point(0, 0)

    var world = false
    set(value) {
        field = value
        initType()
    }
    var realmId = 0

    // socket.io websocket for handling real-time pixel updates

    var receivedPaintRecently = false

    val numBackgrounds = 7

    var numConnect = 0
    lateinit var connectingTimer: Timer

    var summary: MutableList<RestorePoint> = ArrayList()

    var selectedPixels: List<RestorePoint>? = null
    set(value) {
        field = value

        if (value != null) {
            startSelectedPixels = copyPixels(value)
        }
    }

    lateinit var startSelectedPixels: List<RestorePoint>

    lateinit var startSelectedStartUnit: Point
    lateinit var startSelectedEndUnit: Point

    lateinit var cSelectedStartUnit: Point
    lateinit var cSelectedEndUnit: Point

    enum class Direction {
        UP,
        DOWN,
        LEFT,
        RIGHT
    }

    class RestorePoint(var point: Point, var color: Int, var newColor: Int)

    class ShortTermPixel(var restorePoint: RestorePoint) {
        var time = 0L
        init {
            time = System.currentTimeMillis()
        }
    }

    companion object {
        var rows = 1024
        var cols = 1024

        const val GRID_LINE_MODE_ON = 0
        const val GRID_LINE_MODE_OFF = 1

        const val BACKGROUND_BLACK = 0
        const val BACKGROUND_WHITE = 1
        const val BACKGROUND_GRAY_THIRDS = 2
        const val BACKGROUND_PHOTOSHOP = 3
        const val BACKGROUND_CLASSIC = 4
        const val BACKGROUND_CHESS = 5
        const val BACKGROUND_CUSTOM = 6

        fun importCanvasFromJson(context: Context, jsonString: String): Boolean {
            try {
                val jsonArray = JSONArray(jsonString)
                for (i in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.getJSONObject(i)

                    val x = jsonObject.getInt("x")
                    val y = jsonObject.getInt("y")
                    jsonObject.getInt("color")

                    if (x < 0 || x > cols - 1 || y < 0 || y > rows - 1) {
                        return false
                    }
                }
            }
            catch (ex: Exception) {
                return false
            }

            val ed = SessionSettings.instance.getSharedPrefs(context).edit()
            ed.putString("arr_canvas", jsonString)

            ed.apply()

            return true
        }

        fun exportCanvasToJson(arr: Array<IntArray>): String {
            val pixelList: MutableList<Map<String, Int>> = ArrayList()
            for (y in 0 until rows) {
                for (x in 0 until cols) {
                    val color = arr[y][x]

                    if (color != 0) {
                        val map = HashMap<String, Int>()
                        with(map) {
                            put("x", x)
                            put("y", y)
                            put("color", color)
                        }
                        pixelList.add(map)
                    }
                }
            }
            return JSONArray(pixelList.toTypedArray()).toString()
        }
    }

    private fun initType() {
        if (world) {
            // dev
            if (realmId == 2) {
                val arrJsonStr = sessionSettings.arrJsonStr

                if (arrJsonStr == "") {
                    Log.i("Error", "Error displaying canvas, arrJsonStr not set.")
                }
                else {
                    arr = Array(rows) { IntArray(cols) }

                    initPixels(arrJsonStr)
                    sessionSettings.arrJsonStr = ""
                }
            }
            // world
            else if (realmId == 1) {
                rows = 1024
                cols = 1024
                arr = Array(rows) { IntArray(cols) }

                initChunkPixelsFromMemory()
            }

            try {

                // socket.emit("my_event", "test")

                registerForSocketEvents(InteractiveCanvasSocket.instance.socket)

                // showConnectingAttempts()

            } catch (e: URISyntaxException) {

            }

            val recentColorsJsonStr = sessionSettings.getSharedPrefs(context).getString(
                "recent_colors",
                null
            )

            if (recentColorsJsonStr != null) {
                val recentColorsArr = JSONArray(recentColorsJsonStr)
                val sizeDiff = sessionSettings.numRecentColors - recentColorsArr.length()

                if (sizeDiff < 0) {
                    for (i in 0 until sessionSettings.numRecentColors) {
                        // because the most recent is at the end of the list
                        recentColorsList.add(recentColorsArr.getInt(-sizeDiff + i))
                    }
                }
                else {
                    for (i in 0 until recentColorsArr.length()) {
                        recentColorsList.add(recentColorsArr.getInt(i))
                    }

                    if (sizeDiff > 0) {
                        val gridLineColor = getGridLineColor()
                        for (i in 0 until sizeDiff) {
                            recentColorsList.add(0, gridLineColor)
                        }
                    }
                }
            }
            else {
                val gridLineColor = getGridLineColor()
                for (i in 0 until sessionSettings.numRecentColors) {
                    // default to size - 1 of the grid line color
                    if (i < sessionSettings.numRecentColors - 1) {
                        if (gridLineColor == Color.BLACK) {
                            recentColorsList.add(Color.BLACK)
                        }
                        else {
                            recentColorsList.add(Color.WHITE)
                        }
                    }
                    // and 1 of the opposite color
                    else {
                        if (gridLineColor == Color.BLACK) {
                            recentColorsList.add(Color.WHITE)
                        }
                        else {
                            recentColorsList.add(Color.BLACK)
                        }
                    }
                }
            }

            // short term pixels
            for (shortTermPixel in sessionSettings.shortTermPixels) {
                val x = shortTermPixel.restorePoint.point.x
                val y = shortTermPixel.restorePoint.point.y

                arr[y][x] = shortTermPixel.restorePoint.color
            }
        }
        // single play
        else {
            arr = Array(rows) { IntArray(cols) }

            val arrJsonStr = sessionSettings.getSharedPrefs(context).getString(
                "arr_canvas",
                null
            )

            if (arrJsonStr != null) {
                initPixels(arrJsonStr)
            }
            else {
                initDefault()
            }

            val recentColorsJsonStr = sessionSettings.getSharedPrefs(context).getString(
                "recent_colors_single",
                null
            )

            if (recentColorsJsonStr != null) {
                val recentColorsArr = JSONArray(recentColorsJsonStr)
                val sizeDiff = sessionSettings.numRecentColors - recentColorsArr.length()

                if (sizeDiff < 0) {
                    for (i in 0 until sessionSettings.numRecentColors) {
                        // because the most recent is at the end of the list
                        recentColorsList.add(recentColorsArr.getInt(-sizeDiff + i))
                    }
                }
                else {
                    for (i in 0 until recentColorsArr.length()) {
                        recentColorsList.add(recentColorsArr.getInt(i))
                    }

                    if (sizeDiff > 0) {
                        val gridLineColor = getGridLineColor()
                        for (i in 0 until sizeDiff) {
                            recentColorsList.add(0, gridLineColor)
                        }
                    }
                }
            }
            else {
                val gridLineColor = getGridLineColor()
                for (i in 0 until sessionSettings.numRecentColors) {
                    // default to size - 1 of the grid line color
                    if (i < sessionSettings.numRecentColors - 1) {
                        if (gridLineColor == Color.BLACK) {
                            recentColorsList.add(Color.BLACK)
                        }
                        else {
                            recentColorsList.add(Color.WHITE)
                        }
                    }
                    // and 1 of the opposite color
                    else {
                        if (gridLineColor == Color.BLACK) {
                            recentColorsList.add(Color.WHITE)
                        }
                        else {
                            recentColorsList.add(Color.BLACK)
                        }
                    }
                }
            }
        }
    }

    private fun registerForSocketEvents(socket: Socket?) {
        socket?.on("pixels_commit") {
            (context as Activity?)?.runOnUiThread(Runnable {
                val shortTermPixels: MutableList<ShortTermPixel> = ArrayList()

                val pixelsJsonArr = it[0] as JSONArray
                for (i in 0 until pixelsJsonArr.length()) {
                    val pixelObj = pixelsJsonArr.get(i) as JSONObject

                    var sameRealm = false

                    // update color
                    var unit1DIndex = pixelObj.getInt("id") - 1

                    if (unit1DIndex < (512 * 512) && realmId == 2) {
                        sameRealm = true
                    }
                    else if (unit1DIndex >= (512 * 512) && realmId == 1) {
                        sameRealm = true
                    }

                    // adjust from the absolute pixel id (on top of dev pixels in table (for now))
                    if (realmId == 1) {
                        unit1DIndex -= (512 * 512)
                    }

                    if (sameRealm) {
                        val y = unit1DIndex / cols
                        val x = unit1DIndex % cols

                        val color = pixelObj.getInt("color")

                        arr[y][x] = color

                        shortTermPixels.add(ShortTermPixel(RestorePoint(Point(x, y), color, color)))
                    }
                }

                sessionSettings.addShortTermPixels(shortTermPixels)

                interactiveCanvasDrawer?.notifyRedraw()
            })
        }

        socket?.on("paint_qty") {
            val deviceJsonObject = it[0] as JSONObject
            sessionSettings.dropsAmt = deviceJsonObject.getInt("paint_qty")
        }

        socket?.on("add_paint") {
            sessionSettings.dropsAmt++
        }

        socket?.on("add_paint_canvas_setup") {
            if (!receivedPaintRecently) {
                receivedPaintRecently = true
                Log.i("Flag flipped", "flipped")

                Log.i("Drops amt before", sessionSettings.dropsAmt.toString())
                sessionSettings.dropsAmt += 50
                Log.i("Drops amt after", sessionSettings.dropsAmt.toString())
                if (sessionSettings.dropsAmt > 1000) {
                    sessionSettings.dropsAmt = 1000
                }
                Timer().schedule(object: TimerTask() {
                    override fun run() {
                        Log.i("Flag flipped back", "flipped back")
                        receivedPaintRecently = false
                    }
                }, 1000 * 60)
            }
        }
    }

    /*fun showConnectingAttempts() {
        connectingTimer = Timer()
        connectingTimer.schedule(object: TimerTask() {
            override fun run() {
                (context as Activity).runOnUiThread {
                    numConnect += 1
                    Toast.makeText(context, "Connecting to socket $numConnect", Toast.LENGTH_SHORT).show()

                    if (socket != null && socket!!.connected()) {
                        connectingTimer.cancel()
                    }
                }
            }
        }, 0, 5000)
    }*/

    private fun initPixels(pixelsJsonStr: String) {
        val jsonArray = JSONArray(pixelsJsonStr)

        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)

            val x = jsonObject.getInt("x")
            val y = jsonObject.getInt("y")

            val color = jsonObject.getInt("color")

            arr[y][x] = color

            if (color != 0) {
                summary.add(RestorePoint(Point(x, y), color, color))
            }
        }

        interactiveCanvasDrawer?.notifyRedraw()
    }

    private fun initChunkPixelsFromMemory() {
        for (i in arr.indices) {
            lateinit var chunk: Array<IntArray>

            if (i < rows / 4) {
                chunk = sessionSettings.chunk1
            }
            else if (i < rows / 2) {
                chunk = sessionSettings.chunk2
            }
            else if (i < rows - (rows / 4)) {
                chunk = sessionSettings.chunk3
            }
            else {
                chunk = sessionSettings.chunk4
            }

            for (j in arr[i].indices) {
                arr[i][j] = chunk[i % 256][j]
            }
        }
    }

    private fun initDefault() {
        for (i in 0 until rows - 1) {
            for (j in 0 until cols - 1) {
                val color = 0
                arr[j][i] = color
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun saveUnits(context: Context) {
        val jsonArr = JSONArray(arr)

        val ed = sessionSettings.getSharedPrefs(context).edit()

        if (world) {
            ed.putString("arr", jsonArr.toString())
            ed.putString("recent_colors", JSONArray(recentColorsList.toTypedArray()).toString())
        }
        else {
            ed.putString("arr_canvas", exportCanvasToJson(arr))
            ed.putString(
                "recent_colors_single",
                JSONArray(recentColorsList.toTypedArray()).toString()
            )
            ed.putInt("grid_line_color", getGridLineColor())
        }

        ed.apply()
    }

    fun getGridLineColor(): Int {
        if (sessionSettings.canvasGridLineColor != -1) {
            return sessionSettings.canvasGridLineColor
        }

        when (sessionSettings.backgroundColorsIndex) {
            BACKGROUND_BLACK -> return Color.WHITE
            BACKGROUND_WHITE -> return Color.BLACK
            BACKGROUND_GRAY_THIRDS -> return Color.WHITE
            BACKGROUND_PHOTOSHOP -> return Color.BLACK
            BACKGROUND_CLASSIC -> return Color.WHITE
            BACKGROUND_CHESS -> return Color.WHITE
            BACKGROUND_CUSTOM -> return Color.WHITE
        }

        return Color.RED
    }

    fun getBackgroundColors(index: Int): List<Int> {
        when (index) {
            BACKGROUND_BLACK -> return listOf(0, 0)
            BACKGROUND_WHITE -> return listOf(Color.WHITE, Color.WHITE)
            BACKGROUND_GRAY_THIRDS -> return listOf(ActionButtonView.thirdGray.color, ActionButtonView.twoThirdGray.color)
            BACKGROUND_PHOTOSHOP -> return listOf(ActionButtonView.whitePaint.color, ActionButtonView.photoshopGray.color)
            BACKGROUND_CLASSIC ->  return listOf(ActionButtonView.classicGrayLight.color, ActionButtonView.classicGrayDark.color)
            BACKGROUND_CHESS -> return listOf(ActionButtonView.chessTan.color, ActionButtonView.blackPaint.color)
            BACKGROUND_CUSTOM -> return listOf(SessionSettings.instance.canvasBackgroundPrimaryColor,
                SessionSettings.instance.canvasBackgroundSecondaryColor)
        }

        return listOf()
    }

    fun updateRecentColors() {
        var colorIndex = -1
        for (restorePoint in restorePoints) {
            var contains = false
            for (i in 0 until recentColorsList.size) {
                if (restorePoint.newColor == recentColorsList[i]) {
                    contains = true
                    colorIndex = i
                }
            }
            if (!contains) {
                if (recentColorsList.size == sessionSettings.numRecentColors) {
                    recentColorsList.removeAt(0)
                }
                recentColorsList.add(restorePoint.newColor)
            }
            else {
                recentColorsList.removeAt(colorIndex)
                recentColorsList.add(restorePoint.newColor)
            }
        }
    }

    fun isBackground(unitPoint: Point): Boolean {
        return arr[unitPoint.y][unitPoint.x] == 0
    }

    // painting
    fun paintUnitOrUndo(unitPoint: Point, mode: Int = 0, redraw: Boolean = true) {
        val restorePoint = unitInRestorePoints(unitPoint)
        if (mode == 0) {
            if (restorePoint == null && (sessionSettings.dropsAmt > 0 || !world)) {
                if (unitPoint.x in 0 until cols && unitPoint.y in 0 until rows) {
                    val unitColor = arr[unitPoint.y][unitPoint.x]

                    if (sessionSettings.paintColor != unitColor) {
                        Log.i("Interactive Canvas", "Paint!")
                        // paint
                        restorePoints.add(
                            RestorePoint(
                                unitPoint,
                                arr[unitPoint.y][unitPoint.x],
                                sessionSettings.paintColor
                            )
                        )
                        arr[unitPoint.y][unitPoint.x] = sessionSettings.paintColor

                        if (world) {
                            sessionSettings.dropsAmt -= 1
                        }
                    }
                }
            }
        }
        else if (mode == 1) {
            if (restorePoint != null) {
                if (unitPoint.x in 0 until cols && unitPoint.y in 0 until rows) {
                    // undo
                    restorePoints.remove(restorePoint)
                    arr[unitPoint.y][unitPoint.x] = restorePoint.color

                    if (world) {
                        sessionSettings.dropsAmt += 1
                    }
                }
            }
        }

        if (redraw) {
            interactiveCanvasDrawer?.notifyRedraw()
        }
    }

    // sends pixel updates to the web server
    fun commitPixels() {
        if (world) {
            val arr = arrayOfNulls<Map<String, Int>>(restorePoints.size)

            for((index, restorePoint) in restorePoints.withIndex()) {
                val map = HashMap<String, Int>()
                if (realmId == 2) {
                    map["id"] = (restorePoint.point.y * cols + restorePoint.point.x) + 1
                }
                else if (realmId == 1) {
                    map["id"] = (restorePoint.point.y * cols + restorePoint.point.x) + 1 + (512 * 512)
                }
                map["color"] = restorePoint.newColor

                arr[index] = map
            }

            val reqObj = JSONObject()
            val jsonArr = JSONArray(arr)

            reqObj.put("uuid", sessionSettings.uniqueId)
            reqObj.put("pixels", jsonArr)

            InteractiveCanvasSocket.instance.socket?.emit("pixels_event", reqObj)

            StatTracker.instance.reportEvent(context,
                StatTracker.EventType.PIXEL_PAINTED_WORLD,
                restorePoints.size
            )
        }
        else {
            for (restorePoint in restorePoints) {
                summary.add(RestorePoint(restorePoint.point, restorePoint.newColor, restorePoint.newColor))
            }
        }

        clearRestorePoints()

        updateRecentColors()
        recentColorsListener?.onNewRecentColors(recentColorsList.toTypedArray())
    }

    fun undoPendingPaint() {
        for(restorePoint: RestorePoint in restorePoints) {
            arr[restorePoint.point.y][restorePoint.point.x] = restorePoint.color
        }
    }

    fun clearRestorePoints() {
        restorePoints = ArrayList()
    }

    fun unitInRestorePoints(unitPoint: Point): RestorePoint? {
        for(restorePoint: RestorePoint in restorePoints) {
            if (restorePoint.point.x == unitPoint.x && restorePoint.point.y == unitPoint.y) {
                return restorePoint
            }
        }

        return null
    }

    private fun unitInRestorePoints(unitPoint: Point, list: List<RestorePoint>): RestorePoint? {
        for(restorePoint: RestorePoint in list) {
            if (restorePoint.point.x == unitPoint.x && restorePoint.point.y == unitPoint.y) {
                return restorePoint
            }
        }

        return null
    }

    // viewport
    fun updateDeviceViewport(
        context: Context,
        canvasCenterX: Float,
        canvasCenterY: Float,
        fromScale: Boolean = false
    ) {
        val displayMetrics = DisplayMetrics()
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        windowManager.defaultDisplay.getRealMetrics(displayMetrics)

        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        val canvasCenterXPx = (canvasCenterX * ppu)
        val canvasCenterYPx = (canvasCenterY * ppu)

        val top = (canvasCenterYPx - screenHeight / 2) / ppu.toFloat()
        val bottom = (canvasCenterYPx + screenHeight / 2) / ppu.toFloat()
        val left = (canvasCenterXPx - screenWidth / 2) / ppu.toFloat()
        val right = (canvasCenterXPx + screenWidth / 2) / ppu.toFloat()

        if (top < 0 || bottom > rows || left < 0 || right > cols) {
            if (fromScale) {
                scaleCallbackListener?.notifyScaleCancelled()
                return
            }
        }

        deviceViewport = RectF(left, top, right, bottom)

        val w = right - left
        val h = bottom - top

        // error! reset the canvas viewport
        if (w <= 0 || h <= 0) {
            deviceCanvasViewportResetListener?.resetDeviceCanvasViewport()
        }

        // selected object
        if (selectedPixels != null) {
            selectedObjectListener?.onSelectedObjectMoved()
        }

        interactiveCanvasListener?.onDeviceViewportUpdate()
        interactiveCanvasDrawer?.notifyRedraw()
    }

    fun updateDeviceViewport(context: Context, fromScale: Boolean = false) {
        deviceViewport?.apply {
            updateDeviceViewport(context, (left + right) / 2, (top + bottom) / 2, fromScale)
        }
    }

    fun translateBy(context: Context, x: Float, y: Float) {
        deviceViewport?.apply {
            val margin = Utils.dpToPx(context, 200) / ppu

            var dX = x / ppu
            var dY = y / ppu

            val leftBound = -margin
            if (left + dX < leftBound) {
                val diff = left - leftBound
                dX = diff
            }

            val rightBound = cols + margin
            if (right + dX > rightBound) {
                val diff = rightBound - right
                dX = diff
            }

            val topBound = -margin
            if (top + dY < topBound) {
                val diff = top - topBound
                dY = diff
            }

            val bottomBound = rows + margin
            if (bottom + dY > bottomBound) {
                val diff = bottomBound - bottom
                dY = diff
            }

            updateDeviceViewport(context, centerX() + dX, centerY() + dY)
        }
    }

    fun getScreenSpaceForUnit(x: Int, y: Int): RectF {
        deviceViewport?.apply {
            val offsetX = (x - left) * ppu
            val offsetY = (y - top) * ppu

            return RectF(offsetX, offsetY, offsetX + ppu, offsetY + ppu)
        }

        return RectF(0F, 0F, 0F, 0F)
    }

    fun canvasScreenBounds(): Rect {
        deviceViewport?.apply {
            val topLeftScreen = unitToScreenPoint(0F, 0F)
            val bottomRightScreen = unitToScreenPoint(cols.toFloat(), rows.toFloat())

            return Rect(topLeftScreen!!.x, topLeftScreen.y, bottomRightScreen!!.x, bottomRightScreen.y)
        }

        return Rect()
    }

    fun screenPointToUnit(x: Float, y: Float): Point? {
        deviceViewport?.apply {
            val topViewportPx = top * ppu
            val leftViewportPx = left * ppu

            val absXPx = leftViewportPx + x
            val absYPx = topViewportPx + y

            val absX = absXPx / ppu
            val absY = absYPx / ppu

            val point = Point()
            point.x = floor(absX).toInt()
            point.y = floor(absY).toInt()

            return point
        }

        return null
    }

    fun unitToScreenPoint(x: Float, y: Float): Point? {
        deviceViewport?.apply {
            val topViewportPx = top * ppu
            val leftViewportPx = left * ppu

            val absXPx = x * ppu
            val absYPx = y * ppu

            val point = Point()
            point.x = (absXPx - leftViewportPx).toInt()
            point.y = (absYPx - topViewportPx).toInt()

            return point
        }

        return null
    }

    fun unitInBounds(point: Point): Boolean {
        return point.x in 0 until cols && point.y in 0 until rows
    }

    fun pixelIdForUnitPoint(unitPoint: Point): Int {
        if (realmId == 2) {
            return (unitPoint.y * cols + unitPoint.x) + 1
        }
        else {
            return (unitPoint.y * cols + unitPoint.x) + 1 + (512 * 512)
        }
    }

    // object move
    fun startMoveSelection(startUnit: Point, endUnit: Point): Boolean {
        if (!unitInBounds(startUnit) || !unitInBounds(endUnit)) {
            return false
        }

        val pixels = getPixels(startUnit, endUnit)
        if (pixels.isEmpty()) {
            return false
        }

        selectedPixels = pixels

        val startAndEndUnits = getStartAndEndUnits(selectedPixels!!)

        startSelectedStartUnit = startAndEndUnits.first
        cSelectedStartUnit = Point(startSelectedStartUnit.x, startSelectedStartUnit.y)

        startSelectedEndUnit = startAndEndUnits.second
        cSelectedEndUnit = Point(startSelectedEndUnit.x, startSelectedEndUnit.y)

        selectedObjectListener?.onObjectSelected()
        selectedObjectListener?.onSelectedObjectMoveStart()

        interactiveCanvasDrawer?.notifyRedraw()

        return true
    }

    fun startMoveSelection(onePointWithin: Point): Boolean {
        if (!unitInBounds(onePointWithin)) {
            return false
        }

        val pixels = getPixelsInForm(onePointWithin)
        if (pixels.isEmpty()) {
            return false
        }

        selectedPixels = pixels

        val startAndEndUnits = getStartAndEndUnits(pixels)
        startSelectedStartUnit = startAndEndUnits.first
        cSelectedStartUnit = Point(startSelectedStartUnit.x, startSelectedStartUnit.y)

        startSelectedEndUnit = startAndEndUnits.second
        cSelectedEndUnit = Point(startSelectedEndUnit.x, startSelectedEndUnit.y)

        selectedObjectListener?.onObjectSelected()
        selectedObjectListener?.onSelectedObjectMoveStart()

        interactiveCanvasDrawer?.notifyRedraw()

        return true
    }

    fun moveSelection(direction: Direction): Boolean {
        selectedPixels?.apply {
            val moved = movePixels(this, direction)
            if (moved) {
                selectedObjectListener?.onSelectedObjectMoved()
                interactiveCanvasDrawer?.notifyRedraw()
            }
        }
        return false
    }

    fun endMoveSelection(confirm: Boolean) {
        if (confirm) {
            for (pixel in startSelectedPixels) {
                val x = pixel.point.x
                val y = pixel.point.y

                arr[y][x] = 0
            }

            selectedPixels?.apply {
                for (pixel in this) {
                    val x = pixel.point.x
                    val y = pixel.point.y

                    arr[y][x] = pixel.color
                }
            }
        }

        selectedPixels = null

        selectedObjectListener?.onSelectedObjectMoveEnd()
        interactiveCanvasDrawer?.notifyRedraw()
    }

    fun cancelMoveSelectedObject() {
        endMoveSelection(false)
    }

    private fun movePixels(pixels: List<RestorePoint>, direction: Direction): Boolean {
        // check bounds
        for (pixel in pixels) {
            when (direction) {
                Direction.UP -> {
                    if (pixel.point.y < 1) {
                        return false
                    }
                }
                Direction.DOWN -> {
                    if (pixel.point.y > rows - 2) {
                        return false
                    }
                }
                Direction.LEFT -> {
                    if (pixel.point.x < 1) {
                        return false
                    }
                }
                Direction.RIGHT -> {
                    if (pixel.point.x > cols - 2) {
                        return false
                    }
                }
            }
        }

        // move pixels
        for (pixel in pixels) {
            when (direction) {
                Direction.UP -> {
                    pixel.point.y -= 1
                }
                Direction.DOWN -> {
                    pixel.point.y += 1
                }
                Direction.LEFT -> {
                    pixel.point.x -= 1
                }
                Direction.RIGHT -> {
                    pixel.point.x += 1
                }
            }
        }

        when (direction) {
            Direction.UP -> {
                cSelectedStartUnit.y -= 1
                cSelectedEndUnit.y -= 1
            }
            Direction.DOWN -> {
                cSelectedStartUnit.y += 1
                cSelectedEndUnit.y += 1
            }
            Direction.LEFT -> {
                cSelectedStartUnit.x -= 1
                cSelectedEndUnit.x -= 1
            }
            Direction.RIGHT -> {
                cSelectedStartUnit.x += 1
                cSelectedEndUnit.x += 1
            }
        }

        return true
    }

    fun hasSelectedObjectMoved(): Boolean {
        return startSelectedStartUnit.x != cSelectedStartUnit.x || startSelectedStartUnit.y != cSelectedStartUnit.y
    }

    // art export
    fun exportSelection(startUnit: Point, endUnit: Point) {
        if (!unitInBounds(startUnit) || !unitInBounds(endUnit)) {
            return
        }

        artExportListener?.onArtExported(getPixels(startUnit, endUnit))
    }

    fun exportSelection(onePointWithin: Point) {
        if (!unitInBounds(onePointWithin)) {
            return
        }

        artExportListener?.onArtExported(getPixelsInForm(onePointWithin))
    }

    private fun getPixelsInForm(unitPoint: Point): List<RestorePoint> {
        val pixels: MutableList<RestorePoint> = ArrayList()

        stepPixelsInForm(unitPoint.x, unitPoint.y, pixels, 0)

        return pixels
    }

    private fun stepPixelsInForm(x: Int, y: Int, pixelsOut: MutableList<RestorePoint>, depth: Int) {
        // a background color
        // or already in list
        // or out of bounds
        if (x < 0 || x > cols - 1 || y < 0 || y > rows - 1 ||
            arr[y][x] == 0 || unitInRestorePoints(Point(x, y), pixelsOut) != null || depth > 10000) {
            return
        }
        else {
            pixelsOut.add(RestorePoint(Point(x, y), arr[y][x], arr[y][x]))
        }

        // left
        stepPixelsInForm(x - 1, y, pixelsOut, depth + 1)
        // top
        stepPixelsInForm(x, y - 1, pixelsOut, depth + 1)
        // right
        stepPixelsInForm(x + 1, y, pixelsOut, depth + 1)
        // bottom
        stepPixelsInForm(x, y + 1, pixelsOut, depth + 1)
        // top-left
        stepPixelsInForm(x - 1, y - 1, pixelsOut, depth + 1)
        // top-right
        stepPixelsInForm(x + 1, y - 1, pixelsOut, depth + 1)
        // bottom-left
        stepPixelsInForm(x - 1, y + 1, pixelsOut, depth + 1)
        // bottom-right
        stepPixelsInForm(x + 1, y + 1, pixelsOut, depth + 1)
    }

    private fun getPixels(startUnit: Point, endUnit: Point): List<RestorePoint> {
        val pixelsOut: MutableList<RestorePoint> = ArrayList()

        var numLeadingCols = 0
        var numTrailingCols = 0

        var numLeadingRows = 0
        var numTrailingRows = 0

        var before = true
        for (x in startUnit.x..endUnit.x) {
            var clear = true
            for (y in startUnit.y..endUnit.y) {
                if (arr[y][x] != 0) {
                    clear = false
                    before = false
                }
            }

            if (clear && before) {
                numLeadingCols += 1
            }
        }

        before = true
        for (x in endUnit.x downTo startUnit.x) {
            var clear = true
            for (y in startUnit.y..endUnit.y) {
                if (arr[y][x] != 0) {
                    clear = false
                    before = false
                }
            }

            if (clear && before) {
                numTrailingCols += 1
            }
        }

        before = true
        for (y in startUnit.y..endUnit.y) {
            var clear = true
            for (x in startUnit.x..endUnit.x) {
                if (arr[y][x] != 0) {
                    clear = false
                    before = false
                }
            }

            if (clear && before) {
                numLeadingRows += 1
            }
        }

        before = true
        for (y in endUnit.y downTo startUnit.y) {
            var clear = true
            for (x in startUnit.x..endUnit.x) {
                if (arr[y][x] != 0) {
                    clear = false
                    before = false
                }
            }

            if (clear && before) {
                numTrailingRows += 1
            }
        }

        for (x in (startUnit.x + numLeadingCols)..(endUnit.x - numTrailingCols)) {
            for (y in (startUnit.y + numLeadingRows)..(endUnit.y - numTrailingRows)) {
                pixelsOut.add(RestorePoint(Point(x, y), arr[y][x], arr[y][x]))
            }
        }

        return pixelsOut
    }

    private fun getStartAndEndUnits(pixels: List<RestorePoint>): Pair<Point, Point> {
        var minX = cols
        var maxX = -1
        var minY = rows
        var maxY = -1

        for (pixel in pixels) {
            val x = pixel.point.x
            val y = pixel.point.y

            if (x < minX) {
                minX = x
            }

            if (x > maxX) {
                maxX = x
            }

            if (y < minY) {
                minY = y
            }

            if (y > maxY) {
                maxY = y
            }
        }

        return Pair(Point(minX, minY), Point(maxX, maxY))
    }

    private fun copyPixels(pixels: List<RestorePoint>): List<RestorePoint> {
        val list: MutableList<RestorePoint> = ArrayList()
        for (pixel in pixels) {
            list.add(RestorePoint(Point(pixel.point.x, pixel.point.y), pixel.color, pixel.newColor))
        }
        return list
    }

    fun getPixelHistory(pixelId: Int, callback: PixelHistoryCallback?) {
        val requestQueue = Volley.newRequestQueue(context)
        val request = object: JsonObjectRequest(
            Request.Method.GET,
            Utils.baseUrlApi + "/api/v1/canvas/pixels/${pixelId}/history",
            null,
            { response ->
                (context as Activity?)?.runOnUiThread {
                    callback?.onHistoryJsonResponse(response.getJSONArray("data"))
                }
            },
            { error ->
                (context as Activity?)?.runOnUiThread {

                }
            }) {

            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = "application/json; charset=utf-8"
                headers["key1"] = Utils.key1
                return headers
            }
        }

        requestQueue.add(request)
    }
}