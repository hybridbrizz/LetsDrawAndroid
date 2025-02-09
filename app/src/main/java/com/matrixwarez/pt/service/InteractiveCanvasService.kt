package com.matrixwarez.pt.service

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header

interface InteractiveCanvasService {

    @GET("canvas/1/pixels/1")
    fun getChunkPixels(@Header("key1") key1: String): Call<String>
}