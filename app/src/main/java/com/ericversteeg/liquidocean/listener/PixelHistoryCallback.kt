package com.ericversteeg.liquidocean.listener

import org.json.JSONArray

interface PixelHistoryCallback {
    fun onHistoryJsonResponse(historyJson: JSONArray)
}