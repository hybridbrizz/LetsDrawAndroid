package com.ericversteeg.liquidocean.model

import com.google.gson.annotations.SerializedName

class Server {
    @SerializedName("id")
    var id = -1

    @SerializedName("name")
    var name = ""

    @SerializedName("base_url")
    var baseUrl = ""

    @SerializedName("pixel_interval")
    var pixelInterval = -1

    @SerializedName("max_pixels")
    var maxPixels = -1

    @SerializedName("access_key")
    var accessKey = ""

    @SerializedName("admin_key")
    var adminKey = ""

    @SerializedName("is_admin")
    var isAdmin = false

    fun serviceBaseUrl(): String {
        return buildUrl(baseUrl, 5000)
    }

    fun canvasSocketUrl(): String {
        return buildUrl(baseUrl, 5010)
    }

    fun queueSocketUrl(): String {
        return buildUrl(baseUrl, 5020)
    }

    fun serviceAltBaseUrl(): String {
        return buildUrl(baseUrl, 5030)
    }

    private fun buildUrl(baseUrl: String, port: Int): String {
        return String.format("%s:%d/", baseUrl, port)
    }
}