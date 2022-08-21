package com.ericversteeg.liquidocean.service

import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface CanvasRetrofitServiceAlt {

    @GET("api/v1/devices/{uuid}/logip")
    fun logIp(@Header("key1") key1: String, @Path("uuid") uuid: String): Call<JsonObject>

    @GET("api/v1/devices/{deviceId}/ban")
    fun banDeviceIps(@Header("key1") key1: String, @Path("deviceId") deviceId: Int): Call<JsonObject>
}