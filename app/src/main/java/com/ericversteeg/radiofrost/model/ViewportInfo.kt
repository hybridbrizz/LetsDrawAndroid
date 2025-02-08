package com.ericversteeg.radiofrost.model

import com.google.gson.annotations.SerializedName

class ViewportInfo {
    @SerializedName("items")
    var items = mutableListOf<ViewportInfoItem>()
}

class ViewportInfoItem {
    @SerializedName("server_id")
    var serverId = -1

    @SerializedName("scale_factor")
    var scaleFactor = 0f

    @SerializedName("center_x")
    var centerX = 0f

    @SerializedName("center_y")
    var centerY = 0f
}