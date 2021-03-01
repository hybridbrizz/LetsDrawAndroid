package com.ericversteeg.liquidocean.model

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.ericversteeg.liquidocean.helper.Utils
import com.ericversteeg.liquidocean.listener.AchievementListener
import org.json.JSONObject
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class StatTracker {

    enum class EventType {
        PIXEL_PAINTED_WORLD,
        PIXEL_PAINTED_SINGLE,
        PAINT_RECEIVED,
        PIXEL_OVERWRITE_OUT,
        PIXEL_OVERWRITE_IN,
        WORLD_XP
    }

    var activity: Activity? = null

    var achievementListener: AchievementListener? = null
    val achievementDisplayInterval = 8000L

    // world
    var totalPaintAccrued = 0
    val totalPaintAccruedKey = "total_paint_accrued"

    var numPixelsPaintedWorld = 0
    set(value) {
        field = value
        worldXp = value * 20
    }
    val numPixelsPaintedWorldKey = "num_pixels_painted_world"
    var worldXp = 0
    val worldXpKey = "world_xp"
    var numPixelOverwritesOut = 0
    val numPixelOverwritesOutKey = "num_pixel_overwrites_out"
    var numPixelOverwritesIn = 0
    val numPixelOverwritesInKey = "num_pixel_overwrites_in"

    // single play
    var numPixelsPaintedSingle = 0
    val numPixelsPaintedSingleKey = "num_pixels_painted_single"

    // achievement thresholds
    val pixelsWorldThresholds = intArrayOf(50, 500, 1000, 2000, 3000, 4000, 5000, 6000, 7000, 8000,
        9000, 10000, 15000, 20000, 30000, 40000, 50000, 60000, 70000, 80000, 90000, 100000,
        150000, 200000, 250000, 300000, 350000, 400000, 450000, 500000, 550000, 600000,
        650000, 700000, 750000, 800000, 850000, 900000, 950000, 1000000)

    val pixelSingleThresholds = intArrayOf(100, 250, 500, 1000, 1500, 2500, 5000, 10000)

    val paintAccruedThresholds = intArrayOf(10000, 50000, 100000, 250000, 500000, 1000000, 5000000)

    val overwritesInThresholds = intArrayOf(1, 10, 50, 100, 250, 500, 1000, 5000)
    val overwritesOutThresholds = intArrayOf(1, 10, 50, 100, 250, 500, 1000, 5000, 10000)

    val levelThresholds = intArrayOf(5000, 11000, 18000, 28000, 45000, 80000, 115000, 160000, 200000, 245000, 300000, 360000, 435000, 550000, 675000, 820000, 960000, 1120000, 1300000)

    private var achievementQueue = ArrayList<Map<EventType, Int>>()

    fun reportEvent(context: Context, eventType: EventType, amt: Int) {
        val numPixelsPaintedWorldOld = numPixelsPaintedWorld
        val numPixelsPaintedSingleOld = numPixelsPaintedSingle
        val totalPaintAccruedOld = totalPaintAccrued
        val numPixelOverwritesInOld = numPixelOverwritesIn
        val numPixelOverwritesOutOld = numPixelOverwritesOut

        when (eventType) {
            EventType.PIXEL_PAINTED_WORLD -> {
                numPixelsPaintedWorld += amt
                checkAchievements(eventType, numPixelsPaintedWorldOld, numPixelsPaintedWorld)
            }
            EventType.PIXEL_PAINTED_SINGLE -> {
                numPixelsPaintedSingle += amt
                checkAchievements(eventType, numPixelsPaintedSingleOld, numPixelsPaintedSingle)
            }
            EventType.PAINT_RECEIVED -> {
                totalPaintAccrued = amt
                checkAchievements(eventType, totalPaintAccruedOld, totalPaintAccrued)
            }
            EventType.PIXEL_OVERWRITE_IN -> {
                numPixelOverwritesIn = amt
                checkAchievements(eventType, numPixelOverwritesInOld, numPixelOverwritesIn)
            }
            EventType.PIXEL_OVERWRITE_OUT -> {
                numPixelOverwritesOut = amt
                checkAchievements(eventType, numPixelOverwritesOutOld, numPixelOverwritesOut)
            }
        }

        if (eventType != EventType.PAINT_RECEIVED && eventType != EventType.PIXEL_OVERWRITE_IN &&
                eventType != EventType.PIXEL_OVERWRITE_OUT) {
            sendDeviceStat(context, eventType)
        }
    }

    fun getStatValue(key: String): Int {
        when (key) {
            totalPaintAccruedKey -> return totalPaintAccrued
            numPixelsPaintedWorldKey -> return numPixelsPaintedWorld
            numPixelsPaintedSingleKey -> return numPixelsPaintedSingle
            numPixelOverwritesInKey -> return numPixelOverwritesIn
            numPixelOverwritesOutKey -> return numPixelOverwritesOut
        }

        return -1
    }

    fun load(context: Context) {
        val sp = SessionSettings.instance.getSharedPrefs(context)
        totalPaintAccrued = sp.getInt(totalPaintAccruedKey, 0)
        numPixelsPaintedWorld = sp.getInt(numPixelsPaintedWorldKey, 0)
        numPixelOverwritesOut = sp.getInt(numPixelOverwritesOutKey, 0)
        numPixelOverwritesIn = sp.getInt(numPixelOverwritesInKey, 0)
        numPixelsPaintedSingle = sp.getInt(numPixelsPaintedSingleKey, 0)
    }

    fun save(context: Context) {
        val ed = SessionSettings.instance.getSharedPrefs(context).edit()

        ed.putInt(totalPaintAccruedKey, totalPaintAccrued)
        ed.putInt(numPixelsPaintedWorldKey, numPixelsPaintedWorld)
        ed.putInt(numPixelOverwritesOutKey, numPixelOverwritesOut)
        ed.putInt(numPixelOverwritesInKey, numPixelOverwritesIn)
        ed.putInt(numPixelsPaintedSingleKey, numPixelsPaintedSingle)

        ed.apply()
    }

    private fun sendDeviceStat(context: Context, eventType: EventType) {
        val uniqueId = SessionSettings.instance.uniqueId

        val requestQueue = Volley.newRequestQueue(context)

        uniqueId?.apply {
            val requestParams = HashMap<String, Int>()

            when (eventType) {
                EventType.PIXEL_PAINTED_WORLD -> requestParams["wt"] = numPixelsPaintedWorld
                EventType.PIXEL_PAINTED_SINGLE -> requestParams["st"] = numPixelsPaintedSingle
                EventType.PAINT_RECEIVED -> requestParams["tp"] = totalPaintAccrued
                EventType.PIXEL_OVERWRITE_IN -> requestParams["oi"] = numPixelOverwritesIn
                EventType.PIXEL_OVERWRITE_OUT -> requestParams["oo"] = numPixelOverwritesOut
            }

            val paramsJson = JSONObject(requestParams as Map<String, Int>)

            val request = JsonObjectRequest(
                Request.Method.POST,
                Utils.baseUrlApi + "/api/v1/devices/${SessionSettings.instance.uniqueId}",
                paramsJson,
                { response ->
                    Log.i("Stat Tracker", "State updated.")
                },
                { error ->
                    Log.i("Stat Tracker", "State update failed.")
                })

            requestQueue.add(request)
        }
    }

    fun getAchievementProgressString(eventType: EventType): String {
        return when (eventType) {
            EventType.PIXEL_PAINTED_SINGLE -> thresholdsPassed(numPixelsPaintedSingle, pixelSingleThresholds)
            EventType.PIXEL_PAINTED_WORLD -> thresholdsPassed(numPixelsPaintedWorld, pixelsWorldThresholds)
            EventType.PIXEL_OVERWRITE_IN -> thresholdsPassed(numPixelOverwritesIn, overwritesInThresholds)
            EventType.PIXEL_OVERWRITE_OUT -> thresholdsPassed(numPixelOverwritesOut, overwritesOutThresholds)
            EventType.PAINT_RECEIVED -> thresholdsPassed(totalPaintAccrued, paintAccruedThresholds)
            EventType.WORLD_XP -> getWorldLevel().toString()
        }
    }

    private fun thresholdsPassed(progress: Int, thresholds: IntArray): String {
        for ((count, threshold) in thresholds.withIndex()) {
            if (threshold > progress) {
                return "$count / ${thresholds.size}"
            }
        }
        return "${thresholds.size} / ${thresholds.size}"
    }

    fun getWorldLevel(): Int {
        for ((count, threshold) in levelThresholds.withIndex()) {
            if (threshold > worldXp) {
                return count + 1
            }
        }
        return levelThresholds.size + 1
    }

    private fun checkAchievements(eventType: EventType, oldVal: Int, newVal: Int) {
        when (eventType) {
            EventType.PIXEL_PAINTED_WORLD -> {
                for (i in pixelsWorldThresholds.indices) {
                    val threshold = pixelsWorldThresholds[pixelsWorldThresholds.size - 1- i]
                    if (threshold in (oldVal + 1) until newVal + 1) {
                        enqueueAchievement(eventType, threshold)
                        return
                    }
                }

                val oldXp = oldVal * 20
                val newXp = newVal * 20

                for (i in levelThresholds.indices) {
                    val threshold = levelThresholds[levelThresholds.size - 1 - i]
                    if (threshold in (oldXp + 1) until newXp + 1) {
                        enqueueAchievement(EventType.WORLD_XP, threshold)
                        return
                    }
                }
            }
            EventType.PIXEL_PAINTED_SINGLE -> {
                for (i in pixelSingleThresholds.indices) {
                    val threshold = pixelSingleThresholds[pixelSingleThresholds.size - 1- i]
                    if (threshold in (oldVal + 1) until newVal + 1) {
                        enqueueAchievement(eventType, threshold)
                        return
                    }
                }
            }
            EventType.PAINT_RECEIVED -> {
                for (i in paintAccruedThresholds.indices) {
                    val threshold = paintAccruedThresholds[paintAccruedThresholds.size - 1- i]
                    if (threshold in (oldVal + 1) until newVal + 1) {
                        enqueueAchievement(eventType, threshold)
                        return
                    }
                }
            }
            EventType.PIXEL_OVERWRITE_IN -> {
                for (i in overwritesInThresholds.indices) {
                    val threshold = overwritesInThresholds[overwritesInThresholds.size - 1- i]
                    if (threshold in (oldVal + 1) until newVal + 1) {
                        enqueueAchievement(eventType, threshold)
                        return
                    }
                }
            }
            EventType.PIXEL_OVERWRITE_OUT -> {
                for (i in overwritesOutThresholds.indices) {
                    val threshold = overwritesOutThresholds[overwritesOutThresholds.size - 1- i]
                    if (threshold in (oldVal + 1) until newVal + 1) {
                        enqueueAchievement(eventType, threshold)
                        return
                    }
                }
            }
        }
    }

    private fun enqueueAchievement(eventType: EventType, threshold: Int) {
        val map = HashMap<EventType, Int>()
        map[eventType] = threshold
        achievementQueue.add(map)

        if (eventType == EventType.WORLD_XP || eventType == EventType.PIXEL_PAINTED_WORLD ||
            eventType == EventType.PIXEL_PAINTED_SINGLE) {
            activity?.apply {
                displayAchievements(this)
            }
        }
    }

    fun displayAchievements(activity: Activity) {
        Log.i("Queue size is ", achievementQueue.size.toString())
        if (achievementQueue.size > 0) {
            val timer = Timer()

            timer.schedule(object : TimerTask() {
                override fun run() {
                    activity.runOnUiThread {
                        if (achievementQueue.size == 0) {
                            timer.cancel()
                        }
                        else {
                            val nextAchievement = achievementQueue.removeAt(0)

                            achievementListener?.onDisplayAchievement(
                                nextAchievement,
                                achievementDisplayInterval
                            )
                        }
                    }
                }
            }, 0, achievementDisplayInterval)
        }
    }

    companion object {
        val instance = StatTracker()
    }
}