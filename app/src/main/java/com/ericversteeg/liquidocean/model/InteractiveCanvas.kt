package com.ericversteeg.liquidocean.model

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Point
import android.graphics.PointF
import android.graphics.RectF
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.annotation.RequiresApi
import com.ericversteeg.liquidocean.helper.SessionSettings
import com.ericversteeg.liquidocean.listener.InteractiveCanvasDrawerCallback
import org.json.JSONArray
import java.io.File
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

    init {
        val arrJson = SessionSettings.instance.getSharedPrefs(context).getString("arr", null)

        if (arrJson == null) {
            loadDefault()
        }
        else {
            val outerArray = JSONArray(arrJson)

            for (i in 0 until outerArray.length()) {
                val innerArr = outerArray.getJSONArray(i)
                for (j in 0 until innerArr.length()) {
                    val color = innerArr.getInt(j)
                    arr[i][j] = color
                }
            }
        }
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

    fun paintUnitOrUndo(unitPoint: Point) {
        val restorePoint = unitInRestorePoints(unitPoint)
        if (restorePoint != null) {
            // undo
            restorePoints.remove(restorePoint)
            arr[unitPoint.y][unitPoint.x] = restorePoint.color
        }
        else {
            restorePoints.add(RestorePoint(unitPoint, arr[unitPoint.y][unitPoint.x]))
            arr[unitPoint.y][unitPoint.x] = SessionSettings.instance.paintColor
        }

        drawCallbackListener?.notifyRedraw()
    }

    fun undoPendingPaint() {
        for(restorePoint: RestorePoint in restorePoints) {
            arr[restorePoint.point.y][restorePoint.point.x] = restorePoint.color
        }
    }

    fun clearRestorePoints() {
        restorePoints = ArrayList()
    }

    private fun unitInRestorePoints(unitPoint: Point): RestorePoint? {
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

        windowManager.defaultDisplay.getMetrics(displayMetrics)

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
        ed.commit()
    }

    class RestorePoint(var point: Point, var color: Int)
}