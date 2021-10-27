package com.ericversteeg.liquidocean.model

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Point
import android.graphics.RectF
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.ericversteeg.liquidocean.helper.TrustAllSSLCertsDebug
import com.ericversteeg.liquidocean.helper.Utils
import com.ericversteeg.liquidocean.listener.*
import com.ericversteeg.liquidocean.view.ActionButtonView
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import io.socket.engineio.client.transports.Polling
import io.socket.engineio.client.transports.WebSocket
import org.json.JSONArray
import org.json.JSONObject
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
    val autoFarGridLineThreshold = 25

    var deviceViewport: RectF? = null

    var drawCallbackListener: InteractiveCanvasDrawerCallback? = null
    var scaleCallbackListener: InteractiveCanvasScaleCallback? = null
    var paintSelectionListener: PaintSelectionListener? = null
    var recentColorsListener: RecentColorsListener? = null
    var artExportListener: ArtExportListener? = null

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

    val numBackgrounds = 6

    var numConnect = 0
    lateinit var connectingTimer: Timer

    var summary: MutableList<RestorePoint> = ArrayList()

    companion object {
        var GRID_LINE_MODE_ON = 0
        var GRID_LINE_MODE_OFF = 1

        val BACKGROUND_BLACK = 0
        val BACKGROUND_WHITE = 1
        val BACKGROUND_GRAY_THIRDS = 2
        val BACKGROUND_PHOTOSHOP = 3
        val BACKGROUND_CLASSIC = 4
        val BACKGROUND_CHESS = 5
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
                "arr_single",
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

                drawCallbackListener?.notifyRedraw()
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

    private fun initSinglePlayPixels(type: ActionButtonView.Type) {
        var paint1 = ActionButtonView.redPaint
        var paint2 = ActionButtonView.redPaint

        when (type) {
            ActionButtonView.Type.BACKGROUND_WHITE -> {
                paint1 = ActionButtonView.whitePaint
                paint2 = ActionButtonView.whitePaint
            }
            ActionButtonView.Type.BACKGROUND_BLACK -> {
                paint1 = ActionButtonView.blackPaint
                paint2 = ActionButtonView.blackPaint
            }
            ActionButtonView.Type.BACKGROUND_GRAY_THIRDS -> {
                paint1 = ActionButtonView.thirdGray
                paint2 = ActionButtonView.twoThirdGray
            }
            ActionButtonView.Type.BACKGROUND_PHOTOSHOP -> {
                paint1 = ActionButtonView.whitePaint
                paint2 = ActionButtonView.photoshopGray
            }
            ActionButtonView.Type.BACKGROUND_CLASSIC -> {
                paint1 = ActionButtonView.classicGrayLight
                paint2 = ActionButtonView.classicGrayDark
            }
            ActionButtonView.Type.BACKGROUND_CHESS -> {
                paint1 = ActionButtonView.chessTan
                paint2 = ActionButtonView.blackPaint
            }
        }

        for (i in 0 until rows - 1) {
            for (j in 0 until cols - 1) {
                var paint = paint2
                if ((i + j) % 2 == 0) {
                    paint = paint1
                }
                arr[j][i] = paint.color
            }
        }

        if ((paint1.color == Color.BLACK && paint2.color == Color.BLACK) || (paint1.color == Color.WHITE && paint2.color == Color.WHITE)) {
            arr[rows / 2 - 4][cols / 2 - 4] = ActionButtonView.altGreenPaint.color
        }
    }

    private fun initPixels(arrJsonStr: String) {
        val outerArray = JSONArray(arrJsonStr)

        for (i in 0 until outerArray.length()) {
            val innerArr = outerArray.getJSONArray(i)
            for (j in 0 until innerArr.length()) {
                val color = innerArr.getInt(j)
                arr[i][j] = color

                if (color != 0) {
                    summary.add(RestorePoint(Point(j, i), color, color))
                }
            }
        }

        drawCallbackListener?.notifyRedraw()
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
                var color = 0
                arr[j][i] = color
            }
        }
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
        }

        return Color.RED
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

    fun isBackground(unitPoint: Point): Boolean {
        return arr[unitPoint.y][unitPoint.x] == 0
    }

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
            drawCallbackListener?.notifyRedraw()
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
            /*StatTracker.instance.reportEvent(context,
                StatTracker.EventType.PIXEL_PAINTED_SINGLE,
                restorePoints.size
            )*/

            for (restorePoint in restorePoints) {
                summary.add(RestorePoint(restorePoint.point, restorePoint.newColor, restorePoint.newColor))
            }
        }

        updateRecentColors()
        recentColorsListener?.onNewRecentColors(recentColorsList.toTypedArray())
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

    fun unitInRestorePoints(unitPoint: Point, list: List<RestorePoint>): RestorePoint? {
        for(restorePoint: RestorePoint in list) {
            if (restorePoint.point.x == unitPoint.x && restorePoint.point.y == unitPoint.y) {
                return restorePoint
            }
        }

        return null
    }

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

        val canvasCenterXPx = (canvasCenterX * ppu).toInt()
        val canvasCenterYPx = (canvasCenterY * ppu).toInt()

        var top = (canvasCenterYPx - screenHeight / 2) / ppu.toFloat()
        var bottom = (canvasCenterYPx + screenHeight / 2) / ppu.toFloat()
        var left = (canvasCenterXPx - screenWidth / 2) / ppu.toFloat()
        var right = (canvasCenterXPx + screenWidth / 2) / ppu.toFloat()


        if (top < 0 || bottom > rows || left < 0 || right > cols) {
            if (fromScale) {
                scaleCallbackListener?.notifyScaleCancelled()
                return
            }
        }

        deviceViewport = RectF(left, top, right, bottom)
    }

    fun updateDeviceViewport(context: Context, fromScale: Boolean = false) {
        deviceViewport?.apply {
            updateDeviceViewport(context, (left + right) / 2, (top + bottom) / 2, fromScale)
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

    fun translateBy(context: Context, x: Float, y: Float) {
        deviceViewport?.apply {
            var margin = Utils.dpToPx(context, 200) / ppu

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

            left += dX
            right += dX
            top += dY
            bottom += dY
        }

        drawCallbackListener?.notifyRedraw()
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
            ed.putString("arr_single", jsonArr.toString())
            ed.putString(
                "recent_colors_single",
                JSONArray(recentColorsList.toTypedArray()).toString()
            )
            ed.putInt("grid_line_color", getGridLineColor())
        }

        ed.apply()
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

    fun pixelIdForUnitPoint(unitPoint: Point): Int {
        if (realmId == 2) {
            return (unitPoint.y * cols + unitPoint.x) + 1
        }
        else {
            return (unitPoint.y * cols + unitPoint.x) + 1 + (512 * 512)
        }
    }

    fun exportSelection(startUnit: Point, endUnit: Point) {
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

        artExportListener?.onArtExported(pixelsOut)
    }

    fun exportSelection(onePointWithin: Point) {
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

    fun getBackgroundColors(index: Int): List<Int> {
        when (index) {
            BACKGROUND_BLACK -> return listOf(0, 0)
            BACKGROUND_WHITE -> return listOf(Color.WHITE, Color.WHITE)
            BACKGROUND_GRAY_THIRDS -> return listOf(ActionButtonView.thirdGray.color, ActionButtonView.twoThirdGray.color)
            BACKGROUND_PHOTOSHOP -> return listOf(ActionButtonView.whitePaint.color, ActionButtonView.photoshopGray.color)
            BACKGROUND_CLASSIC ->  return listOf(ActionButtonView.classicGrayLight.color, ActionButtonView.classicGrayDark.color)
            BACKGROUND_CHESS -> return listOf(ActionButtonView.chessTan.color, ActionButtonView.blackPaint.color)
        }

        return listOf()
    }

    class RestorePoint(var point: Point, var color: Int, var newColor: Int)

    class ShortTermPixel(var restorePoint: RestorePoint) {
        var time = 0L
        init {
            time = System.currentTimeMillis()
        }
    }
}