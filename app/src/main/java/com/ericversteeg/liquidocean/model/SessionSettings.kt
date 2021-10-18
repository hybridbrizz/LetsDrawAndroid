package com.ericversteeg.liquidocean.model

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Point
import androidx.collection.ArraySet
import com.ericversteeg.liquidocean.R
import com.ericversteeg.liquidocean.helper.Utils
import com.ericversteeg.liquidocean.listener.PaintQtyListener
import com.ericversteeg.liquidocean.view.ActionButtonView
import com.ericversteeg.liquidocean.view.ArtView
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

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
        for (listener in paintQtyListeners) {
            listener?.paintQtyChanged(field)
        }
    }

    var startTimeMillis = 0L

    val maxPaintAmt = 1000

    var paintQtyListeners: MutableSet<PaintQtyListener?> = ArraySet()

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

    var artShowcase: MutableList<MutableList<InteractiveCanvas.RestorePoint>>? = null
    var showcaseIndex = 0

    var numRecentColors = 8

    var arrJsonStr = ""

    lateinit var chunk1: Array<IntArray>
    lateinit var chunk2: Array<IntArray>
    lateinit var chunk3: Array<IntArray>
    lateinit var chunk4: Array<IntArray>

    var firstContributorName = ""
    var secondContributorName = ""
    var thirdContributorName = ""

    var colorIndicatorWidth = 2

    var colorIndicatorFill = false

    var colorIndicatorSquare = false

    var colorIndicatorOutline = true

    var gridLineMode = 0

    var canvasGridLineColor = -1

    var closePaintBackButtonColor = ActionButtonView.yellowPaint.color

    var menuBackgroundResId = 0

    var showPaintBar = false
    var showPaintCircle = true

    var paintBarColor = 0

    var tablet = false

    var shortTermPixels: MutableList<InteractiveCanvas.ShortTermPixel> = ArrayList()

    var rightHanded = false
    var selectedHand = false

    var smallActionButtons = false

    var pincodeSet = false

    var firstLaunch = true

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

        ed.putString("art_showcase_json", artShowcaseJsonString())

        ed.putInt("num_recent_colors", numRecentColors)

        ed.putInt("color_indicator_width_2", colorIndicatorWidth)

        ed.putBoolean("color_indicator_fill", colorIndicatorFill)

        ed.putBoolean("color_indicator_outline", colorIndicatorOutline)

        ed.putInt("grid_line_mode", gridLineMode)

        ed.putInt("canvas_grid_line_color", canvasGridLineColor)

        ed.putInt("close_paint_back_button_color", closePaintBackButtonColor)

        ed.putBoolean("color_indicator_square", colorIndicatorSquare)

        ed.putBoolean("show_paint_bar", showPaintBar)

        ed.putBoolean("show_paint_circle", showPaintCircle)

        ed.putInt("paint_bar_color", paintBarColor)

        ed.putBoolean("right_handed", rightHanded)

        ed.putBoolean("selected_hand", selectedHand)

        ed.putBoolean("small_action_buttons", smallActionButtons)

        ed.putBoolean("pin_code_set", pincodeSet)

        ed.putBoolean("first_launch", firstLaunch)

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

        panelBackgroundResId = getSharedPrefs(context).getInt("panel_texture_id", R.drawable.amb_9)

        emittersEnabled = getSharedPrefs(context).getBoolean("emitters", false)

        canvasLockBorder = getSharedPrefs(context).getBoolean("lock_border", false)

        canvasLockBorderColor = getSharedPrefs(context).getInt("lock_border_color", Color.parseColor("#66FF0000"))

        promptToExit = getSharedPrefs(context).getBoolean("prompt_to_exit", false)

        backgroundColorsIndex = getSharedPrefs(context).getInt("background_colors_index", 0)

        displayName = getSharedPrefs(context).getString("display_name", "")!!

        artShowcase = loadArtShowcase(getSharedPrefs(context).getString("art_showcase_json", null))

        numRecentColors = getSharedPrefs(context).getInt("num_recent_colors", 16)

        colorIndicatorWidth = getSharedPrefs(context).getInt("color_indicator_width_2", 4)

        colorIndicatorFill = getSharedPrefs(context).getBoolean("color_indicator_fill", false)

        colorIndicatorOutline = getSharedPrefs(context).getBoolean("color_indicator_outline", false)

        gridLineMode = getSharedPrefs(context).getInt("grid_line_mode", 0)

        canvasGridLineColor = getSharedPrefs(context).getInt("canvas_grid_line_color", -1)

        closePaintBackButtonColor = getSharedPrefs(context).getInt("close_paint_back_button_color", ActionButtonView.yellowPaint.color)

        colorIndicatorSquare = getSharedPrefs(context).getBoolean("color_indicator_square", true)

        showPaintBar = getSharedPrefs(context).getBoolean("show_paint_bar", true)

        showPaintCircle = getSharedPrefs(context).getBoolean("show_paint_circle", false)

        paintBarColor = getSharedPrefs(context).getInt("paint_bar_color", Color.parseColor("#FFAAAAAA"))

        rightHanded = getSharedPrefs(context).getBoolean("right_handed", false)

        selectedHand = getSharedPrefs(context).getBoolean("selected_hand", false)

        smallActionButtons = getSharedPrefs(context).getBoolean("small_action_buttons", false)

        pincodeSet = getSharedPrefs(context).getBoolean("pin_code_set", false)

        firstLaunch = getSharedPrefs(context).getBoolean("first_launch", true)
    }

    fun addShortTermPixels(pixels: List<InteractiveCanvas.ShortTermPixel>) {
        shortTermPixels.addAll(pixels)
    }

    fun updateShortTermPixels() {
        var removeList: MutableList<InteractiveCanvas.ShortTermPixel> = ArrayList()
        for (shortTermPixel in shortTermPixels) {
            if ((System.currentTimeMillis() - shortTermPixel.time) > (1000 * 60 * 2)) {
                removeList.add(shortTermPixel)
            }
        }

        shortTermPixels.removeAll(removeList)
    }

    private fun artShowcaseJsonString(): String? {
        artShowcase?.apply {
            val ret = arrayOfNulls<Array<Map<String, Int>?>>(size)

            for (i in this.indices) {
                val art = this[i]

                val restorePoints = arrayOfNulls<Map<String, Int>>(art.size)
                for (j in art.indices) {
                    val restorePoint = art[j]
                    val map = HashMap<String, Int>()

                    map["x"] = restorePoint.point.x
                    map["y"] = restorePoint.point.y
                    map["color"] = restorePoint.color

                    restorePoints[j] = map
                }

                ret[i] = restorePoints
            }

            return JSONArray(ret).toString()
        }

        return null
    }

    private fun loadArtShowcase(jsonStr: String?): MutableList<MutableList<InteractiveCanvas.RestorePoint>>? {
        jsonStr?.apply {
            if (this == "[null]") return null

            val showcase: MutableList<MutableList<InteractiveCanvas.RestorePoint>> = ArrayList()

            val jsonArray = JSONArray(jsonStr)

            for (i in 0 until jsonArray.length()) {
                val artJsonArray = jsonArray.getJSONArray(i)

                val art = ArrayList<InteractiveCanvas.RestorePoint>()

                for (j in 0 until artJsonArray.length()) {
                    val jsonObj = artJsonArray.getJSONObject(j)
                    art.add(InteractiveCanvas.RestorePoint(Point(jsonObj.getInt("x"), jsonObj.getInt("y")), jsonObj.getInt("color"), jsonObj.getInt("color")))
                }

                showcase.add(art)
            }

            return showcase
        }

        return null
    }

    fun resetCanvasLockBorderColor() {
        canvasLockBorderColor = defaultCanvasLockBorderColor
    }

    fun addToShowcase(art: List<InteractiveCanvas.RestorePoint>) {
        if (artShowcase == null) {
            artShowcase = ArrayList()
        }

        artShowcase?.apply {
            if (size < 10) {
                add(art.toMutableList())
            }
            else {
                val rIndex = (Math.random() * size).toInt()
                removeAt(rIndex)
                add(rIndex, art.toMutableList())
            }
        }
    }

    fun defaultArtShowcase(resources: Resources) {
        addToShowcase(ArtView.artFromJsonResource(resources, R.raw.leaf_json))
        addToShowcase(ArtView.artFromJsonResource(resources, R.raw.water_drop_json))
        addToShowcase(ArtView.artFromJsonResource(resources, R.raw.doughnut_json))
        addToShowcase(ArtView.artFromJsonResource(resources, R.raw.bird_json))
        addToShowcase(ArtView.artFromJsonResource(resources, R.raw.hfs_json))
        addToShowcase(ArtView.artFromJsonResource(resources, R.raw.paint_bucket_json))
        addToShowcase(ArtView.artFromJsonResource(resources, R.raw.fire_badge_json))
        addToShowcase(ArtView.artFromJsonResource(resources, R.raw.fries_json))
    }

    companion object {
        val instance = SessionSettings()
    }
}