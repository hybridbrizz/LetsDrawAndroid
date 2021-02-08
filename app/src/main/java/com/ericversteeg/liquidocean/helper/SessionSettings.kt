package com.ericversteeg.liquidocean.helper

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.util.Log
import com.ericversteeg.liquidocean.listener.PaintQtyListener
import com.ericversteeg.liquidocean.model.InteractiveCanvas
import java.util.*
import kotlin.collections.ArrayList

class SessionSettings {

    private val spKey = "MyPrefs"

    var uniqueId: String? = null
    var sentUniqueId = false

    var paintColor = Color.WHITE

    var dropsAmt = 0
    set(value) {
        field = value
        for (x in paintQtyListeners.indices) {
            paintQtyListeners[x]?.paintQtyChanged(field)
        }
    }

    var startTimeMillis = 0L

    val maxPaintAmt = 1000

    var paintQtyListeners: MutableList<PaintQtyListener?> = ArrayList()

    var darkIcons = false

    fun getSharedPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(spKey, Context.MODE_PRIVATE)
    }

    fun save(context: Context) {
        val ed = getSharedPrefs(context).edit()
        ed.putInt("paint_color", paintColor)

        ed.putInt("drops_amt", dropsAmt)

        ed.putLong("start_time", startTimeMillis)

        /*interactiveCanvas.deviceViewport?.apply {
            ed.putFloat("viewport_left", left)
            ed.putFloat("viewport_top", top)
            ed.putFloat("viewport_right", right)
            ed.putFloat("viewport_bottom", bottom)
        }*/

        uniqueId?.apply {
            ed.putString("installation_id", uniqueId)
        }

        ed.putBoolean("sent_uuid", sentUniqueId)

        ed.apply()
    }

    fun load(context: Context) {
        paintColor = getSharedPrefs(context).getInt("paint_color", Color.WHITE)

        dropsAmt = getSharedPrefs(context).getInt("drops_amt", 0)

        startTimeMillis = getSharedPrefs(context).getLong("start_time", System.currentTimeMillis())

        uniqueId = getSharedPrefs(context).getString("installation_id", UUID.randomUUID().toString())
        sentUniqueId = getSharedPrefs(context).getBoolean("sent_uuid", false)
    }

    companion object {
        val instance = SessionSettings()
    }
}