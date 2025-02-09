package com.matrixwarez.pt.listener

import org.json.JSONArray

interface PixelHistoryCallback {
    fun onHistoryJsonResponse(historyJson: JSONArray)
}