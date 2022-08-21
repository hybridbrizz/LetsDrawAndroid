package com.ericversteeg.liquidocean.service

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.http.*

interface CanvasRetrofitService {

    @GET("api/v1/recent/pixels/{time}")
    fun getRecentPixels(@Header("key1") key1: String, @Path("time") time: Long): Call<JsonArray>

    @GET("api/v1/device/paintqty/{uuid}")
    fun getPaintQty(@Header("key1") key1: String, @Path("uuid") uuid: String): Call<JsonObject>

    @GET("api/v1/canvas/1/pixels/{chunk_id}")
    fun getChunkPixels(@Header("key1") key1: String, @Path("chunk_id") chunkId: Int): Call<JsonArray>

    @GET("api/v1/devices/{uuid}/setip")
    fun logIp(@Header("key1") key1: String, @Path("uuid") uuid: String): Call<JsonObject>
}