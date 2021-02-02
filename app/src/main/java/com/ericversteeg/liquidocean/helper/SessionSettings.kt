package com.ericversteeg.liquidocean.helper

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.RectF
import com.ericversteeg.liquidocean.model.InteractiveCanvas

class SessionSettings {

    private val spKey = "MyPrefs"

    var paintColor = Color.WHITE
    var dropsAmt = 0
    var dropsUsed = 0
    var startTimeMillis = 0L

    fun getSharedPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(spKey, Context.MODE_PRIVATE)
    }

    fun save(context: Context, interactiveCanvas: InteractiveCanvas) {
        val ed = getSharedPrefs(context).edit()
        ed.putInt("paint_color", paintColor)
        ed.putInt("drops_amt", dropsAmt)
        ed.putInt("drops_used", dropsUsed)
        ed.putLong("start_time", startTimeMillis)

        interactiveCanvas.deviceViewport?.apply {
            ed.putFloat("viewport_left", left)
            ed.putFloat("viewport_top", top)
            ed.putFloat("viewport_right", right)
            ed.putFloat("viewport_bottom", bottom)
        }

        interactiveCanvas.saveUnits(context)

        ed.apply()
    }

    fun load(context: Context, interactiveCanvas: InteractiveCanvas) {
        paintColor = getSharedPrefs(context).getInt("paint_color", Color.WHITE)
        // dropsAmt = getSharedPrefs(context).getInt("drops_amt", 0)
        dropsUsed = getSharedPrefs(context).getInt("drops_used", 0)
        startTimeMillis = getSharedPrefs(context).getLong("start_time", System.currentTimeMillis())

        val left = getSharedPrefs(context).getFloat("viewport_left", -1F)
        val top = getSharedPrefs(context).getFloat("viewport_top", 0F)
        val right = getSharedPrefs(context).getFloat("viewport_right", 0F)
        val bottom = getSharedPrefs(context).getFloat("viewport_bottom", 0F)

        if (left >= 0F) {
            // interactiveCanvas.deviceViewport = RectF(left, top, right, bottom)
        }
    }

    companion object {
        val instance = SessionSettings()
    }
}