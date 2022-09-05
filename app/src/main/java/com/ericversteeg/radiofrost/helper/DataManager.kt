package com.ericversteeg.radiofrost.helper

import android.app.Activity
import android.content.Context
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.ericversteeg.radiofrost.model.SessionSettings
import com.ericversteeg.radiofrost.model.StatTracker
import org.json.JSONObject

class DataManager {

    companion object {
        fun getDeviceInfo(context: Context) {
            val requestQueue = Volley.newRequestQueue(context)

            val uniqueId = SessionSettings.instance.uniqueId

            val request = object: JsonObjectRequest(
                Request.Method.GET,
                "api/v1/devices/$uniqueId/info",
                null,
                { response ->
                    SessionSettings.instance.dropsAmt = response.getInt("paint_qty")
                    SessionSettings.instance.xp = response.getInt("xp")

                    SessionSettings.instance.displayName = response.getString("name")

                    StatTracker.instance.numPixelsPaintedWorld = response.getInt("wt")
                    StatTracker.instance.numPixelsPaintedSingle = response.getInt("st")

                    // server-side event sync
                    StatTracker.instance.reportEvent(context, StatTracker.EventType.PAINT_RECEIVED, response.getInt("tp"))
                    StatTracker.instance.reportEvent(context, StatTracker.EventType.PIXEL_OVERWRITE_IN, response.getInt("oi"))
                    StatTracker.instance.reportEvent(context, StatTracker.EventType.PIXEL_OVERWRITE_OUT, response.getInt("oo"))

                    StatTracker.instance.displayAchievements(context as Activity)
                },
                { error ->

                }) {

                override fun getHeaders(): MutableMap<String, String> {
                    val headers = HashMap<String, String>()
                    headers["Content-Type"] = "application/json; charset=utf-8"
                    headers["key1"] = Utils.key1
                    return headers
                }
            }

            requestQueue.add(request)
        }

        fun sendDeviceId(context: Context) {
            val requestQueue = Volley.newRequestQueue(context)

            val uniqueId = SessionSettings.instance.uniqueId

            uniqueId?.apply {
                val requestParams = HashMap<String, String>()

                requestParams["uuid"] = uniqueId

                val paramsJson = JSONObject(requestParams as Map<String, String>)

                val request = object: JsonObjectRequest(
                    Request.Method.POST,
                    "api/v1/devices/register",
                    paramsJson,
                    { response ->
                        SessionSettings.instance.dropsAmt = response.getInt("paint_qty")
                        SessionSettings.instance.xp = response.getInt("xp")

                        StatTracker.instance.numPixelsPaintedWorld = response.getInt("wt")
                        StatTracker.instance.numPixelsPaintedSingle = response.getInt("st")
                        StatTracker.instance.totalPaintAccrued = response.getInt("tp")
                        StatTracker.instance.numPixelOverwritesIn = response.getInt("oi")
                        StatTracker.instance.numPixelOverwritesOut = response.getInt("oo")

                        SessionSettings.instance.sentUniqueId = true
                    },
                    { error ->

                    }) {

                    override fun getHeaders(): MutableMap<String, String> {
                        val headers = HashMap<String, String>()
                        headers["Content-Type"] = "application/json; charset=utf-8"
                        headers["key1"] = Utils.key1
                        return headers
                    }
                }

                request.tag = "download"
                requestQueue.add(request)
            }
        }
    }
}