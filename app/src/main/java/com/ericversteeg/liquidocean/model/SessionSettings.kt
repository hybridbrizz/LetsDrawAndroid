package com.ericversteeg.liquidocean.model

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import com.ericversteeg.liquidocean.R
import com.ericversteeg.liquidocean.listener.PaintQtyListener
import java.util.*
import kotlin.collections.ArrayList

class SessionSettings {

    private val spKey = "MyPrefs"

    var uniqueId: String? = null

    var googleAuth = false

    var sentUniqueId = false

    var paintColor = Color.WHITE

    var backgroundColorsIndex = 0

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

    var xp = 0

    var panelBackgroundResId = R.drawable.wood_texture_light

    private val defaultCanvasLockBorderColor = Color.parseColor("#66FF0000")

    var emittersEnabled = true
    var canvasLockBorder = true
    var canvasLockBorderColor = defaultCanvasLockBorderColor

    var promptToExit = false

    var timeSync = 0L
    set(value) {
        field = value
        nextPaintTime = System.currentTimeMillis() + value * 1000
    }

    var nextPaintTime = 0L

    var displayName = ""

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

        ed.putInt("xp", xp)

        ed.putBoolean("google_auth", googleAuth)

        ed.putInt("panel_texture_id", panelBackgroundResId)

        ed.putBoolean("emitters", emittersEnabled)

        ed.putBoolean("lock_border", canvasLockBorder)

        ed.putInt("lock_border_color", canvasLockBorderColor)

        ed.putBoolean("prompt_to_exit", promptToExit)

        ed.putInt("background_colors_index", backgroundColorsIndex)

        ed.putString("display_name", displayName)

        ed.apply()
    }

    fun saveLastPaintColor(context: Context, world: Boolean) {
        val ed = getSharedPrefs(context).edit()
        if (world) {
            ed.putInt("last_world_paint_color", paintColor)
        }
        else {
            ed.putInt("last_single_paint_color", paintColor)
        }
        ed.apply()
    }

    fun load(context: Context) {
        paintColor = getSharedPrefs(context).getInt("paint_color", Color.WHITE)

        dropsAmt = getSharedPrefs(context).getInt("drops_amt", 0)

        startTimeMillis = getSharedPrefs(context).getLong("start_time", System.currentTimeMillis())

        uniqueId = getSharedPrefs(context).getString("installation_id", UUID.randomUUID().toString())
        sentUniqueId = getSharedPrefs(context).getBoolean("sent_uuid", false)

        xp = getSharedPrefs(context).getInt("xp", 0)

        googleAuth = getSharedPrefs(context).getBoolean("google_auth", false)

        panelBackgroundResId = getSharedPrefs(context).getInt("panel_texture_id", R.drawable.wood_texture_light)

        emittersEnabled = getSharedPrefs(context).getBoolean("emitters", true)

        canvasLockBorder = getSharedPrefs(context).getBoolean("lock_border", true)

        canvasLockBorderColor = getSharedPrefs(context).getInt("lock_border_color", Color.parseColor("#66FF0000"))

        promptToExit = getSharedPrefs(context).getBoolean("prompt_to_exit", false)

        backgroundColorsIndex = getSharedPrefs(context).getInt("background_colors_index", 0)

        displayName = getSharedPrefs(context).getString("display_name", "")!!
    }

    fun resetCanvasLockBorderColor() {
        canvasLockBorderColor = defaultCanvasLockBorderColor
    }

    companion object {
        val instance = SessionSettings()
    }
}