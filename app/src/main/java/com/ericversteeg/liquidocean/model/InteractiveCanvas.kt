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
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.ericversteeg.liquidocean.helper.SessionSettings
import com.ericversteeg.liquidocean.listener.InteractiveCanvasDrawerCallback
import com.google.gson.Gson
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.net.URI
import java.net.URISyntaxException
import java.util.*
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLSession
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

    var restorePoints = ArrayList<RestorePoint>()

    var gson = Gson()

    // socket.io websocket for handling real-time pixel updates
    private lateinit var socket: Socket

    init {
        val arrJsonStr = SessionSettings.instance.getSharedPrefs(context).getString("arr", null)

        if (arrJsonStr == null) {
            // loadDefault()
            downloadCanvasPixels(context)
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
    }

    private fun registerForSocketEvents(socket: Socket) {
        socket.on("pixels_commit", object : Emitter.Listener {
            override fun call(vararg args: Any) {
                (context as Activity).runOnUiThread(Runnable {
                    val pixelsJsonArr = args[0] as JSONArray
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
        })

        socket.on("paint_qty") {
            val deviceJsonObject = it[0] as JSONObject
            SessionSettings.instance.dropsAmt = deviceJsonObject.getInt("paint_qty")
            drawCallbackListener?.notifyPaintQtyUpdate(SessionSettings.instance.dropsAmt)
        }

        socket.on("add_paint") {
            SessionSettings.instance.dropsAmt++
            drawCallbackListener?.notifyPaintQtyUpdate(SessionSettings.instance.dropsAmt)
        }

        socket.on("add_paint_canvas_setup") {
            SessionSettings.instance.dropsAmt += 50
            drawCallbackListener?.notifyPaintQtyUpdate(SessionSettings.instance.dropsAmt)
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

    private fun downloadCanvasPixels(context: Context) {
        val requestQueue = Volley.newRequestQueue(context)

        val jsonObjRequest: StringRequest = object : StringRequest(
            Method.GET,
            "http://192.168.200.69:5000/api/v1/canvas/pixels",
            Response.Listener { response ->
                initPixels(response)
            },
            Response.ErrorListener { error ->
                error.message?.apply {
                    Log.i("Error", this)
                }
            }) {
            override fun getBodyContentType(): String {
                return "application/x-www-form-urlencoded; charset=UTF-8"
            }

            override fun getParams(): Map<String, String> {
                val params: MutableMap<String, String> = HashMap()
                val pixelData = SessionSettings.instance.getSharedPrefs(context).getString(
                    "arr",
                    ""
                )

                pixelData?.apply {
                    params["arr"] = this
                }

                return params
            }
        }

        jsonObjRequest.retryPolicy = DefaultRetryPolicy(30000, 3, 1.0f)

        requestQueue.add(jsonObjRequest)
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

    fun updateDeviceViewport(context: Context, canvasCenterX: Float, canvasCenterY: Float) {
        val displayMetrics = DisplayMetrics()
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        windowManager.defaultDisplay.getRealMetrics(displayMetrics)

        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        val canvasCenterXPx = (canvasCenterX * ppu).toInt()
        val canvasCenterYPx = (canvasCenterY * ppu).toInt()

        val top = (canvasCenterYPx - screenHeight / 2) / ppu.toFloat()
        val bottom = (canvasCenterYPx + screenHeight / 2) / ppu.toFloat()
        val left = (canvasCenterXPx - screenWidth / 2) / ppu.toFloat()
        val right = (canvasCenterXPx + screenWidth / 2) / ppu.toFloat()

        deviceViewport = RectF(left, top, right, bottom)
    }

    fun updateDeviceViewport(context: Context) {
        deviceViewport?.apply {
            updateDeviceViewport(context, (left + right) / 2, (top + bottom) / 2)
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
        ed.apply()
    }

    class RestorePoint(var point: Point, var color: Int, var newColor: Int)
}