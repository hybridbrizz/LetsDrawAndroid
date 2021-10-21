package com.ericversteeg.liquidocean.listener

import android.graphics.Point
import org.json.JSONArray

interface PixelHistoryListener {
    // fun onPixelHistoryJson(screenPoint: Point, history: JSONArray)

    fun showPixelHistoryFragmentPopover(screenPoint: Point)
    fun showDrawFrameConfigFragmentPopover(screenPoint: Point)
}