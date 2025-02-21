package com.matrixwarez.pt.model

import com.google.gson.annotations.SerializedName

class Server {
    @SerializedName("id")
    var id = -1

    @SerializedName("name")
    var name = ""

    @SerializedName("size")
    var size = 0

    @SerializedName("max_send")
    var maxSend = 10

    @SerializedName("base_url")
    var baseUrl = ""

    @SerializedName("icon_url")
    var iconUrl = ""

    @SerializedName("icon_link")
    var iconLink = ""

    @SerializedName("color")
    var color = 0

    @SerializedName("banner_text")
    var bannerText = ""

    @SerializedName("show_banner")
    var showBanner = false

    @SerializedName("pixel_interval")
    var pixelInterval = -1L

    @SerializedName("max_pixels")
    var maxPixels = -1

    @SerializedName("pixels_amt")
    var pixelsAmt = -1

    @SerializedName("access_key")
    var accessKey = ""

    @SerializedName("admin_key")
    var adminKey = ""

    @SerializedName("is_admin")
    var isAdmin = false

    @SerializedName("uuid")
    var uuid: String = ""

    @SerializedName("api_port")
    var apiPort = 0

    @SerializedName("alt_port")
    var altPort = 0

    @SerializedName("socket_port")
    var socketPort = 0

    @SerializedName("queue_port")
    var queuePort = 0

    @SerializedName("online")
    var online = false

    @SerializedName("connection_count")
    var connectionCount = 0

    @SerializedName("max_connections")
    var maxConnections = 0

    fun serviceBaseUrl(): String {
        return buildUrl(baseUrl, apiPort)
    }

    fun canvasSocketUrl(): String {
        return buildUrl(baseUrl, socketPort)
    }

    fun queueSocketUrl(): String {
        return buildUrl(baseUrl, queuePort)
    }

    fun serviceAltBaseUrl(): String {
        return buildUrl(baseUrl, altPort)
    }

    private fun buildUrl(baseUrl: String, port: Int): String {
        return String.format("%s:%d/", baseUrl, port)
    }

    override fun equals(other: Any?): Boolean {
        return other != null &&
                other is Server &&
                (!other.isAdmin && !isAdmin && other.accessKey == accessKey ||
                 other.isAdmin && isAdmin && other.adminKey == adminKey)
    }
}