package com.ericversteeg.liquidocean.service

import com.google.gson.JsonArray
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface CanvasRetrofitService {

    @GET("api/v1/recent/pixels/{time}")
    fun getRecentPixels(@Header("key1") key1: String, @Path("time") time: Long): Call<JsonArray>

}