package com.matrixwarez.pt.listener

import android.graphics.Point

interface PixelHistoryListener {
    // fun onPixelHistoryJson(screenPoint: Point, history: JSONArray)

    fun showPixelHistoryFragmentPopover(screenPoint: Point)
    fun showDrawFrameConfigFragmentPopover(screenPoint: Point)
}