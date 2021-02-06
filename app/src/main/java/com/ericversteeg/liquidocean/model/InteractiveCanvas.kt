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
import com.android.volley.toolbox.Volley
import com.ericversteeg.liquidocean.helper.SessionSettings
import com.ericversteeg.liquidocean.listener.InteractiveCanvasDrawerCallback
import com.ericversteeg.liquidocean.listener.PaintSelectionListener
import com.ericversteeg.liquidocean.listener.InteractiveCanvasScaleCallback
import com.ericversteeg.liquidocean.listener.RecentColorsListener
import com.google.gson.Gson
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONArray
import org.json.JSONObject
import java.net.URISyntaxException
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
    val initialScaleFactor = 0.25f

    var deviceViewport: RectF? = null

    var drawCallbackListener: InteractiveCanvasDrawerCallback? = null
    var scaleCallbackListener: InteractiveCanvasScaleCallback? = null
    var paintSelectionListener: PaintSelectionListener? = null
    var recentColorsListener: RecentColorsListener? = null

    var recentColorsList: MutableList<Int> = ArrayList()
    val maxRecents = 8

    var restorePoints = ArrayList<RestorePoint>()

    var gson = Gson()

    // socket.io websocket for handling real-time pixel updates
    private lateinit var socket: Socket

    init {
        val arrJsonStr = SessionSettings.instance.getSharedPrefs(context).getString("arr", null)

        if (arrJsonStr == null) {
            Log.i("Error", "Error displaying canvas, no data in shared prefs to display.")
        }
        else {
            initPixels(arrJsonStr)
        }

        try {
            socket = IO.socket("http://192.168.200.69:5010")

            socket.connect()

            socket.on(Socket.EVENT_CONNECT, Emitter.Listener {
                Log.i("okay", it.toString())

                //val map = HashMap<String, String>()
                //map["data"] = "connected to the SocketServer android..."
                //socket.emit("my_event", gson.toJson(map))
            })

            socket.on(Socket.EVENT_CONNECT_ERROR) {
                Log.i("Error", it.toString())
            }

            // socket.emit("my_event", "test")

            registerForSocketEvents(socket)
        } catch (e: URISyntaxException) {

        }

        val recentColorsJsonStr = SessionSettings.instance.getSharedPrefs(context).getString("recent_colors", null)

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
            SessionSettings.instance.dropsAmt += 50
            if (SessionSettings.instance.dropsAmt > 1000) {
                SessionSettings.instance.dropsAmt = 1000
            }
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

    private fun loadDefault() {
        for (i in 0 until rows - 1) {
            for (j in 0 until cols - 1) {
                var color = Color.parseColor("#FF333333")
                if ((i + j) % 2 == 0) {
                    color = Color.parseColor("#FF666666")
                }
                arr[j][i] = color
            }
        }

        arr[rows / 2][cols / 2] = Color.parseColor("#FF00FF00")
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
            if (restorePoint == null && SessionSettings.instance.dropsAmt > 0) {
                // paint
                restorePoints.add(
                    RestorePoint(
                        unitPoint,
                        arr[unitPoint.y][unitPoint.x],
                        SessionSettings.instance.paintColor
                    )
                )
                arr[unitPoint.y][unitPoint.x] = SessionSettings.instance.paintColor

                SessionSettings.instance.dropsAmt -= 1
            }
        }
        else if (mode == 1) {
            if (restorePoint != null) {
                // undo
                restorePoints.remove(restorePoint)
                arr[unitPoint.y][unitPoint.x] = restorePoint.color

                SessionSettings.instance.dropsAmt += 1
            }
        }

        drawCallbackListener?.notifyRedraw()
    }

    // sends pixel updates to the web server
    fun commitPixels() {
        val requestQueue = Volley.newRequestQueue(context)

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

        updateRecentColors()
        recentColorsListener?.onNewRecentColors(recentColorsList.toTypedArray())

        /*val request = JsonArrayRequest(
            Request.Method.POST,
            "http://192.168.200.69:5000/api/v1/canvas/pixels",
            jsonArr,
            { response ->
                Log.i("Foo", "Success")
            },
            { error ->
                Log.i("Error", error.message!!)
            })

        requestQueue.add(request)*/
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

    fun updateDeviceViewport(context: Context, canvasCenterX: Float, canvasCenterY: Float, fromScale: Boolean = false) {
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

    fun translateBy(x: Float, y: Float) {
        deviceViewport?.apply {
            var dX = x / ppu
            var dY = y / ppu

            if (left + dX < 0) {
                val diff = left
                dX = diff
            }

            if (right + dX > cols) {
                val diff = cols - right
                dX = diff
            }

            if (top + dY < 0) {
                val diff = top
                dY = diff
            }

            if (bottom + dY > rows) {
                val diff = rows - bottom
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
        ed.putString("arr", jsonArr.toString())
        ed.putString("recent_colors", JSONArray(recentColorsList.toTypedArray()).toString())

        ed.apply()
    }

    class RestorePoint(var point: Point, var color: Int, var newColor: Int)
}