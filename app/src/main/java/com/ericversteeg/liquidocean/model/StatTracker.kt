package com.ericversteeg.liquidocean.model

import android.content.Context
import android.util.Log
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.ericversteeg.liquidocean.helper.Utils
import org.json.JSONObject

class StatTracker {

    enum class EventType {
        PIXEL_PAINTED_WORLD,
        PIXEL_PAINTED_SINGLE,
        PAINT_RECEIVED,
        PIXEL_OVERWRITE_OUT,
        PIXEL_OVERWRITE_IN
    }

    // world
    var totalPaintAccrued = 0
    val totalPaintAccruedKey = "total_paint_accrued"

    var numPixelsPaintedWorld = 0
    val numPixelsPaintedWorldKey = "num_pixels_painted_world"
    var numPixelOverwritesOut = 0
    val numPixelOverwritesOutKey = "num_pixel_overwrites_out"
    var numPixelOverwritesIn = 0
    val numPixelOverwritesInKey = "num_pixel_overwrites_in"

    // single play
    var numPixelsPaintedSingle = 0
    val numPixelsPaintedSingleKey = "num_pixels_painted_single"

    fun reportEvent(context: Context, eventType: EventType, amt: Int) {
        when (eventType) {
            EventType.PIXEL_PAINTED_WORLD -> {
                numPixelsPaintedWorld += amt
            }
            EventType.PIXEL_PAINTED_SINGLE -> {
                numPixelsPaintedSingle += amt
            }
            EventType.PAINT_RECEIVED -> {
                totalPaintAccrued += amt
            }
            EventType.PIXEL_OVERWRITE_IN -> {
                numPixelOverwritesIn += amt
            }
            EventType.PIXEL_OVERWRITE_OUT -> {
                numPixelOverwritesOut += amt
            }
        }

        sendDeviceStat(context, eventType)
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

    fun sendDeviceStat(context: Context, eventType: EventType) {
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

    companion object {
        val instance = StatTracker()
    }
}