package com.ericversteeg.liquidocean.model

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Point
import android.util.Log
import androidx.collection.ArraySet
import com.ericversteeg.liquidocean.R
import com.ericversteeg.liquidocean.annotation.Mockable
import com.ericversteeg.liquidocean.helper.Utils
import com.ericversteeg.liquidocean.listener.PaintQtyListener
import com.ericversteeg.liquidocean.view.ActionButtonView
import com.ericversteeg.liquidocean.view.ArtView
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

@Mockable
class SessionSettings {

    private val spKey = "MyPrefs"

    var gson = Gson()

    var uniqueId: String? = null
    var deviceId: Int = -1

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

    val maxPaintAmt = 1

    var paintQtyListeners: MutableSet<PaintQtyListener?> = ArraySet()

    var darkIcons = false

    var xp = 0

    val panelResIds = intArrayOf(
        R.drawable.metal_floor_1,
        R.drawable.metal_floor_2,
        R.drawable.foil,
        R.drawable.rainbow_foil,
        R.drawable.wood_texture_light,
        R.drawable.fall_leaves,
        R.drawable.grass,
        R.drawable.amb_6,
        R.drawable.water_texture,
        R.drawable.space_texture,
        R.drawable.crystal_8,
        R.drawable.crystal_10,
        R.drawable.crystal_1,
        R.drawable.crystal_2,
        R.drawable.crystal_4,
        R.drawable.crystal_5,
        R.drawable.crystal_6,
        R.drawable.crystal_7,
        R.drawable.crystal_3,
        R.drawable.amb_2,
        R.drawable.amb_3,
        R.drawable.amb_4,
        R.drawable.amb_5,
        R.drawable.amb_7,
        R.drawable.amb_8,
        R.drawable.amb_9,
        R.drawable.amb_10,
        R.drawable.amb_11,
        R.drawable.amb_12,
        R.drawable.amb_13,
        R.drawable.amb_14,
        R.drawable.amb_15
    )

    var panelBackgroundResIndex = 0

    private val defaultCanvasLockBorderColor = 0

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

    var addPaintInterval = 3

    var displayName = ""

    var artShowcase: MutableList<MutableList<InteractiveCanvas.RestorePoint>>? = null
    var showcaseIndex = 0

    var numRecentColors = 8

    var arrJsonStr = ""

    lateinit var chunk1: String
    lateinit var chunk2: String
    lateinit var chunk3: String
    lateinit var chunk4: String

    var firstContributorName = ""
    var secondContributorName = ""
    var thirdContributorName = ""

    var colorIndicatorWidth = 2

    var colorIndicatorFill = false

    var colorIndicatorSquare = false

    var colorIndicatorOutline = true

    var gridLineMode = 0

    var canvasGridLineColor = -1

    var canvasBackgroundPrimaryColor = 0

    var canvasBackgroundSecondaryColor = 0

    var frameColor = -1

    var closePaintBackButtonColor = -1

    var menuBackgroundResId = 0

    var showPaintBar = false
    var showPaintCircle = true

    var paintBarColor = 0

    var tablet = false

    var shortTermPixels: MutableList<InteractiveCanvas.ShortTermPixel> = ArrayList()

    var rightHanded = false
    var selectedHand = false

    var smallActionButtons = false

    var lockPaintPanel = false

    var pincodeSet = false

    var firstLaunch = true

    var palettes: MutableList<Palette> = ArrayList()

    var selectedPaletteIndex = 0
    set(value) {
        field = value

        palette = palettes[selectedPaletteIndex]
    }

    lateinit var palette: Palette

    var lastDrawFrameWidth = 0
    var lastDrawFrameHeight = 0

    var restoreDeviceViewportLeft = 0F
    var restoreDeviceViewportTop = 0F
    var restoreDeviceViewportRight = 0F
    var restoreDeviceViewportBottom = 0F

    var restoreCanvasScaleFactor = 0F

    var restoreDeviceViewportCenterX = 0F
    var restoreDeviceViewportCenterY = 0F

    var toolboxOpen = false
    var paintPanelOpen = false

    var canvasOpen = false

    var boldActionButtons = true

    var colorPaletteSize = 4

    var servers = LinkedList<Server>()
    var lastVisitedServer: Server? = null

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

        ed.putInt("panel_texture_index", panelBackgroundResIndex)

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

        ed.putInt("canvas_background_primary_color", canvasBackgroundPrimaryColor)

        ed.putInt("canvas_background_secondary_color", canvasBackgroundSecondaryColor)

        ed.putInt("frame_color", frameColor)

        ed.putInt("close_paint_back_button_color", closePaintBackButtonColor)

        ed.putBoolean("color_indicator_square", colorIndicatorSquare)

        ed.putBoolean("show_paint_bar", showPaintBar)

        ed.putBoolean("show_paint_circle", showPaintCircle)

        ed.putInt("paint_bar_color", paintBarColor)

        ed.putBoolean("right_handed", rightHanded)

        ed.putBoolean("selected_hand", selectedHand)

        ed.putBoolean("small_action_buttons", smallActionButtons)

        ed.putBoolean("lock_paint_panel", lockPaintPanel)

        ed.putBoolean("pin_code_set", pincodeSet)

        ed.putBoolean("first_launch", firstLaunch)

        ed.putString("palettes", palettesJsonStr())

        ed.putInt("selected_palette_index", selectedPaletteIndex)

        ed.putFloat("restore_device_viewport_left", restoreDeviceViewportLeft)

        ed.putFloat("restore_device_viewport_top", restoreDeviceViewportTop)

        ed.putFloat("restore_device_viewport_right", restoreDeviceViewportRight)

        ed.putFloat("restore_device_viewport_bottom", restoreDeviceViewportBottom)

        ed.putFloat("restore_canvas_scale_factor", restoreCanvasScaleFactor)

        ed.putFloat("restore_device_viewport_center_x", restoreDeviceViewportCenterX)

        ed.putFloat("restore_device_viewport_center_y", restoreDeviceViewportCenterY)

        ed.putBoolean("toolbox_open", toolboxOpen)

        ed.putBoolean("paint_panel_open", paintPanelOpen)

        ed.putBoolean("canvas_open", canvasOpen)

        ed.putBoolean("bold_action_buttons", boldActionButtons)

        ed.putInt("color_palette_size", colorPaletteSize)

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

        uniqueId =
            getSharedPrefs(context).getString("installation_id", UUID.randomUUID().toString())
        sentUniqueId = getSharedPrefs(context).getBoolean("sent_uuid", false)

        xp = getSharedPrefs(context).getInt("xp", 0)

        googleAuth = getSharedPrefs(context).getBoolean("google_auth", false)

        panelBackgroundResIndex = getSharedPrefs(context).getInt("panel_texture_index", 25)

        emittersEnabled = getSharedPrefs(context).getBoolean("emitters", false)

        canvasLockBorder = getSharedPrefs(context).getBoolean("lock_border", false)

        canvasLockBorderColor =
            getSharedPrefs(context).getInt("lock_border_color", Color.parseColor("#66FF0000"))

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

        canvasBackgroundPrimaryColor = getSharedPrefs(context).getInt("canvas_background_primary_color", 0)

        canvasBackgroundSecondaryColor = getSharedPrefs(context).getInt("canvas_background_secondary_color", 0)

        frameColor = getSharedPrefs(context).getInt("frame_color", Color.GRAY)

        closePaintBackButtonColor = getSharedPrefs(context).getInt(
            "close_paint_back_button_color",
            -1
        )

        colorIndicatorSquare = getSharedPrefs(context).getBoolean("color_indicator_square", true)

        showPaintBar = getSharedPrefs(context).getBoolean("show_paint_bar", true)

        showPaintCircle = getSharedPrefs(context).getBoolean("show_paint_circle", false)

        paintBarColor =
            getSharedPrefs(context).getInt("paint_bar_color", Color.parseColor("#FFAAAAAA"))

        rightHanded = getSharedPrefs(context).getBoolean("right_handed", false)

        selectedHand = getSharedPrefs(context).getBoolean("selected_hand", false)

        smallActionButtons = getSharedPrefs(context).getBoolean("small_action_buttons", false)

        lockPaintPanel = getSharedPrefs(context).getBoolean("lock_paint_panel", false)

        pincodeSet = getSharedPrefs(context).getBoolean("pin_code_set", false)

        firstLaunch = getSharedPrefs(context).getBoolean("first_launch", true)

        palettes = palettesFromJsonString(getSharedPrefs(context).getString("palettes", "[]")!!).toMutableList()

        //palettes = ArrayList()

        // recent colors palette
        palettes.add(0, Palette("Recent Color"))

        selectedPaletteIndex = getSharedPrefs(context).getInt("selected_palette_index", 0)

        //selectedPaletteIndex = 0

        /*val palette1 = Palette("Palette 1")
        palette1.addColor(-1)

        palettes.add(palette1)

        val palette2 = Palette("Palette 2")
        palette2.addColor(0)
        palette2.addColor(103040)

        palettes.add(palette2)*/

        restoreDeviceViewportLeft = getSharedPrefs(context).getFloat("restore_device_viewport_left", 0F)
        restoreDeviceViewportTop = getSharedPrefs(context).getFloat("restore_device_viewport_top", 0F)
        restoreDeviceViewportRight = getSharedPrefs(context).getFloat("restore_device_viewport_right", 0F)
        restoreDeviceViewportBottom = getSharedPrefs(context).getFloat("restore_device_viewport_bottom", 0F)

        restoreDeviceViewportCenterX = getSharedPrefs(context).getFloat("restore_device_viewport_center_x", 0F)
        restoreDeviceViewportCenterY = getSharedPrefs(context).getFloat("restore_device_viewport_center_y", 0F)

        restoreCanvasScaleFactor = getSharedPrefs(context).getFloat("restore_canvas_scale_factor", 0F)

        toolboxOpen = getSharedPrefs(context).getBoolean("toolbox_open", false)

        paintPanelOpen = getSharedPrefs(context).getBoolean("paint_panel_open", false)

        canvasOpen = getSharedPrefs(context).getBoolean("canvas_open", false)

        boldActionButtons = getSharedPrefs(context).getBoolean("bold_action_buttons", true)

        colorPaletteSize = getSharedPrefs(context).getInt("color_palette_size", 4)

        initServerList(context)
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
        //addToShowcase(ArtView.artFromJsonResource(resources, R.raw.fire_badge_json))
        addToShowcase(ArtView.artFromJsonResource(resources, R.raw.fries_json))
    }

    fun addPalette(name: String) {
        palettes.add(Palette((name)))
    }

    fun removePalette(pos: Int) {
        palettes.removeAt(pos)
    }

    private fun palettesJsonStr(): String {
        val palettesArray = arrayOfNulls<Map<Any?, Any?>>(palettes.size - 1)

        for (i in palettesArray.indices) {
            palettesArray[i] = palettes[i + 1].toMap()
        }

        return JSONArray(palettesArray).toString()
    }

    private fun palettesFromJsonString(jsonStr: String): List<Palette> {
        val palettes: MutableList<Palette> = ArrayList()

        val palettesJsonArr = JSONArray(jsonStr)
        for (i in 0 until palettesJsonArr.length()) {
            val paletteJsonObj = palettesJsonArr.getJSONObject(i)

            val name = paletteJsonObj.getString("name")
            val colorsJsonArr = paletteJsonObj.getJSONArray("colors")

            val palette = Palette(name)

            for (j in 0 until colorsJsonArr.length()) {
                val color = colorsJsonArr.getInt(j)
                palette.addColor(color)
            }

            palettes.add(palette)
        }

        return palettes
    }

    private fun initServerList(context: Context) {
        val sp = getSharedPrefs(context)
        val set = sp.getStringSet("servers_json", mutableSetOf())!!

        for (jsonStr in set) {
            val server = gson.fromJson(jsonStr, Server::class.java)
            servers.add(server)
        }

        servers.sortBy { it.name }
    }

    fun addServer(context: Context, server: Server) {
        val sp = getSharedPrefs(context)
        val set = sp.getStringSet("servers_json", mutableSetOf())!!
        val cp = mutableSetOf<String>().also { it.addAll(set) }

        val str = gson.toJson(server, Server::class.java)
        cp.add(str)

        sp.edit().putStringSet("servers_json", cp).apply()

        servers.add(server)
        servers.sortBy { it.name }
    }

    companion object {
        val instance = SessionSettings()
    }
}