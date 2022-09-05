package com.ericversteeg.radiofrost.listener

import org.json.JSONArray

interface PixelHistoryCallback {
    fun onHistoryJsonResponse(historyJson: JSONArray)
}