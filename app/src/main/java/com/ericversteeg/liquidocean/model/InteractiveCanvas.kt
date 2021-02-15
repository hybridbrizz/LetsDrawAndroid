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
import androidx.annotation.RequiresApi
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.ericversteeg.liquidocean.helper.TrustAllSSLCertsDebug
import com.ericversteeg.liquidocean.helper.Utils
import com.ericversteeg.liquidocean.listener.*
import com.ericversteeg.liquidocean.view.ActionButtonView
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONArray
import org.json.JSONObject
import java.net.URISyntaxException
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.set
import kotlin.math.floor


class InteractiveCanvas(var context: Context) {
    val rows = 512
    val cols = 512

    val arr = Array(rows) { IntArray(cols) }

    val basePpu = 100
    var ppu = basePpu

    val gridLineThreshold = 50

    var deviceViewport: RectF? = null

    var drawCallbackListener: InteractiveCanvasDrawerCallback? = null
    var scaleCallbackListener: InteractiveCanvasScaleCallback? = null
    var paintSelectionListener: PaintSelectionListener? = null
    var recentColorsListener: RecentColorsListener? = null
    var artExportListener: ArtExportListener? = null

    var recentColorsList: MutableList<Int> = ArrayList()
    val maxRecents = 8

    var restorePoints = ArrayList<RestorePoint>()

    val maxScaleFactor = 10.0F
    val minScaleFactor = 0.15F

    val startScaleFactor = 0.5f

    var lastSelectedUnitPoint = Point(0, 0)

    var world = false
    set(value) {
        field = value
        initType()
    }

    // socket.io websocket for handling real-time pixel updates
    private lateinit var socket: Socket
    var checkEventTimeout = 20000L
    var checkStatusReceived = false

    var socketStatusCallback: SocketStatusCallback? = null

    var receivedPaintRecently = false

    val BACKGROUND_BLACK = 0
    val BACKGROUND_WHITE = 1
    val BACKGROUND_GRAY_THIRDS = 2
    val BACKGROUND_PHOTOSHOP = 3
    val BACKGROUND_CLASSIC = 4
    val BACKGROUND_CHESS = 5

    val numBackgrounds = 6

    private fun initType() {
        if (world) {
            val arrJsonStr = SessionSettings.instance.getSharedPrefs(context).getString("arr", null)

            if (arrJsonStr == null) {
                Log.i("Error", "Error displaying canvas, no data in shared prefs to display.")
            }
            else {
                initPixels(arrJsonStr)
            }

            try {
                socket = TrustAllSSLCertsDebug.getAllCertsIOSocket()

                socket.connect()

                socket.on(Socket.EVENT_CONNECT, Emitter.Listener {
                    Log.i("okay", it.toString())

                    //val map = HashMap<String, String>()
                    //map["data"] = "connected to the SocketServer android..."
                    //socket.emit("my_event", gson.toJson(map))

                    if (!checkStatusReceived) {
                        socket.emit("check_event")
                    }
                })

                socket.on(Socket.EVENT_CONNECT_ERROR) {
                    Log.i("Error", it.toString())
                }

                socket.on(Socket.EVENT_DISCONNECT) {
                    Log.i("Socket", "Socket disconnected.")
                }

                // socket.emit("my_event", "test")

                registerForSocketEvents(socket)

            } catch (e: URISyntaxException) {

            }

            val recentColorsJsonStr = SessionSettings.instance.getSharedPrefs(context).getString(
                "recent_colors",
                null
            )

            if (recentColorsJsonStr != null) {
                val recentColorsArr = JSONArray(recentColorsJsonStr)
                for (i in 0 until recentColorsArr.length()) {
                    recentColorsList.add(recentColorsArr.getInt(i))
                }
            }
            else {
                recentColorsList.add(Color.BLACK)
                recentColorsList.add(Color.BLACK)
                recentColorsList.add(Color.BLACK)
                recentColorsList.add(Color.BLACK)
                recentColorsList.add(Color.BLACK)
                recentColorsList.add(Color.BLACK)
                recentColorsList.add(Color.BLACK)
                recentColorsList.add(Color.WHITE)
            }
        }
        // single play
        else {
            val arrJsonStr = SessionSettings.instance.getSharedPrefs(context).getString(
                "arr_single",
                null
            )

            if (arrJsonStr != null) {
                initPixels(arrJsonStr)
            }
            else {
                initDefault()
            }

            val recentColorsJsonStr = SessionSettings.instance.getSharedPrefs(context).getString(
                "recent_colors_single",
                null
            )

            if (recentColorsJsonStr != null) {
                val recentColorsArr = JSONArray(recentColorsJsonStr)
                for (i in 0 until recentColorsArr.length()) {
                    recentColorsList.add(recentColorsArr.getInt(i))
                }
            }
            else {
                if (getGridLineColor() == Color.BLACK) {
                    recentColorsList.add(Color.BLACK)
                    recentColorsList.add(Color.BLACK)
                    recentColorsList.add(Color.BLACK)
                    recentColorsList.add(Color.BLACK)
                    recentColorsList.add(Color.BLACK)
                    recentColorsList.add(Color.BLACK)
                    recentColorsList.add(Color.BLACK)
                    recentColorsList.add(Color.WHITE)
                }
                else {
                    recentColorsList.add(Color.WHITE)
                    recentColorsList.add(Color.WHITE)
                    recentColorsList.add(Color.WHITE)
                    recentColorsList.add(Color.WHITE)
                    recentColorsList.add(Color.WHITE)
                    recentColorsList.add(Color.WHITE)
                    recentColorsList.add(Color.WHITE)
                    recentColorsList.add(Color.BLACK)
                }
            }
        }
    }

    private fun registerForSocketEvents(socket: Socket) {
        socket.on("pixels_commit") {
            (context as Activity).runOnUiThread(Runnable {
                val pixelsJsonArr = it[0] as JSONArray
                for (i in 0 until pixelsJsonArr.length()) {
                    val pixelObj = pixelsJsonArr.get(i) as JSONObject

                    // update color
                    val unit1DIndex = pixelObj.getInt("id") - 1

                    val y = unit1DIndex / cols
                    val x = unit1DIndex % cols

                    arr[y][x] = pixelObj.getInt("color")
                }

                drawCallbackListener?.notifyRedraw()
            })
        }

        socket.on("paint_qty") {
            val deviceJsonObject = it[0] as JSONObject
            SessionSettings.instance.dropsAmt = deviceJsonObject.getInt("paint_qty")
        }

        socket.on("add_paint") {
            SessionSettings.instance.dropsAmt++
        }

        socket.on("add_paint_canvas_setup") {
            if (!receivedPaintRecently) {
                SessionSettings.instance.dropsAmt += 50
                if (SessionSettings.instance.dropsAmt > 1000) {
                    SessionSettings.instance.dropsAmt = 1000
                }

                receivedPaintRecently = true
                Timer().schedule(object: TimerTask() {
                    override fun run() {
                        receivedPaintRecently = false
                    }
                }, 1000 * 10)
            }
        }

        socket.on("check_success") {
            checkStatusReceived = true
        }
    }

    fun checkSocketStatus() {
        socket.emit("check_event")

        checkStatusReceived = false
        Timer().schedule(object : TimerTask() {
            override fun run() {
                if (!checkStatusReceived) {
                    socketStatusCallback?.onSocketStatusError()
                }
            }
        }, checkEventTimeout)
    }

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
            }
        }

        drawCallbackListener?.notifyRedraw()
    }

    private fun initDefault() {
        for (i in 0 until rows - 1) {
            for (j in 0 until cols - 1) {
                var color = 0
                arr[j][i] = color
            }
        }

        arr[rows / 2][cols / 2] = Color.parseColor("#FF00FF00")
    }

    fun getGridLineColor(): Int {
        when (SessionSettings.instance.backgroundColorsIndex) {
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

            return Point(floor(absX).toInt(), floor(absY).toInt())
        }

        return null
    }

    fun paintUnitOrUndo(unitPoint: Point, mode: Int = 0) {
        val restorePoint = unitInRestorePoints(unitPoint)
        if (mode == 0) {
            if (restorePoint == null && (SessionSettings.instance.dropsAmt > 0 || !world)) {
                if (unitPoint.x in 0 until cols && unitPoint.y in 0 until rows) {
                    val unitColor = arr[unitPoint.y][unitPoint.x]

                    if (SessionSettings.instance.paintColor != unitColor) {
                        Log.i("Interactive Canvas", "Paint!")
                        // paint
                        restorePoints.add(
                            RestorePoint(
                                unitPoint,
                                arr[unitPoint.y][unitPoint.x],
                                SessionSettings.instance.paintColor
                            )
                        )
                        arr[unitPoint.y][unitPoint.x] = SessionSettings.instance.paintColor

                        if (world) {
                            SessionSettings.instance.dropsAmt -= 1
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
                        SessionSettings.instance.dropsAmt += 1
                    }
                }
            }
        }

        drawCallbackListener?.notifyRedraw()
    }

    // sends pixel updates to the web server
    fun commitPixels() {
        if (world) {
            val arr = arrayOfNulls<Map<String, Int>>(restorePoints.size)

            for((index, restorePoint) in restorePoints.withIndex()) {
                val map = HashMap<String, Int>()
                map["id"] = (restorePoint.point.y * cols + restorePoint.point.x) + 1
                map["color"] = restorePoint.newColor

                arr[index] = map
            }

            val reqObj = JSONObject()
            val jsonArr = JSONArray(arr)

            reqObj.put("uuid", SessionSettings.instance.uniqueId)
            reqObj.put("pixels", jsonArr)

            socket.emit("pixels_event", reqObj)

            StatTracker.instance.reportEvent(context,
                StatTracker.EventType.PIXEL_PAINTED_WORLD,
                restorePoints.size
            )
        }
        else {
            StatTracker.instance.reportEvent(context,
                StatTracker.EventType.PIXEL_PAINTED_SINGLE,
                restorePoints.size
            )
        }

        updateRecentColors()
        recentColorsListener?.onNewRecentColors(recentColorsList.toTypedArray())
    }

    private fun updateRecentColors() {
        for (restorePoint in restorePoints) {
            var contains = false
            for (i in 0 until recentColorsList.size) {
                if (restorePoint.newColor == recentColorsList[i]) {
                    recentColorsList.removeAt(i)
                    recentColorsList.add(restorePoint.newColor)

                    contains = true
                }
            }
            if (!contains) {
                if (recentColorsList.size == maxRecents) {
                    recentColorsList.removeAt(0)
                }
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

        val ed = SessionSettings.instance.getSharedPrefs(context).edit()

        if (world) {
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
        val request = JsonObjectRequest(
            Request.Method.GET,
            Utils.baseUrlApi + "/api/v1/canvas/pixels/${pixelId}/history",
            null,
            { response ->
                (context as Activity).runOnUiThread {
                    callback?.onHistoryJsonResponse(response.getJSONArray("data"))
                }
            },
            { error ->
                (context as Activity).runOnUiThread {

                }
            })

        requestQueue.add(request)

    }

    fun pixelIdForUnitPoint(unitPoint: Point): Int {
        return (unitPoint.y * cols + unitPoint.x) + 1
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
}