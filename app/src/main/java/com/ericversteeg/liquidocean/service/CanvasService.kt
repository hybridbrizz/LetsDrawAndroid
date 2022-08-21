package com.ericversteeg.liquidocean.service

import com.ericversteeg.liquidocean.helper.Utils
import com.ericversteeg.liquidocean.model.Server
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class CanvasService(server: Server) {

    private var retrofit = Retrofit.Builder()
        .baseUrl(server.serviceBaseUrl())
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private var retrofitAlt = Retrofit.Builder()
        .baseUrl(server.serviceAltBaseUrl())
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val service = retrofit.create(CanvasRetrofitService::class.java)
    private val serviceAlt = retrofitAlt.create(CanvasRetrofitServiceAlt::class.java)

    fun getRecentPixels(since: Long, completionHandler: (jsonArray: JsonArray?) -> Unit) {
        service.getRecentPixels(Utils.key1, since).enqueue(object: Callback<JsonArray> {
            override fun onResponse(call: Call<JsonArray>, response: Response<JsonArray>) {
                completionHandler.invoke(response.body())
            }

            override fun onFailure(call: Call<JsonArray>, t: Throwable) {
                completionHandler.invoke(null)
            }
        })
    }

    fun getPaintQty(uuid: String, completionHandler: (jsonObj: JsonObject?) -> Unit) {
        service.getPaintQty(Utils.key1, uuid).enqueue(object: Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                completionHandler.invoke(response.body())
            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                completionHandler.invoke(null)
            }
        })
    }

    fun getChunkPixels(chunkId: Int, completionHandler: (string: JsonArray?) -> Unit) {
        service.getChunkPixels(Utils.key1, chunkId).enqueue(object: Callback<JsonArray> {
            override fun onResponse(call: Call<JsonArray>, response: Response<JsonArray>) {
                completionHandler.invoke(response.body())
            }

            override fun onFailure(call: Call<JsonArray>, t: Throwable) {
                completionHandler.invoke(null)
            }
        })
    }

    fun logIp(uuid: String, completionHandler: (jsonObj: JsonObject?) -> Unit) {
        serviceAlt.logIp(Utils.key1, uuid).enqueue(object: Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                completionHandler.invoke(response.body())
            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                completionHandler.invoke(null)
            }
        })
    }

    fun banDeviceIps(deviceId: Int, completionHandler: (jsonObj: JsonObject?) -> Unit) {
        serviceAlt.banDeviceIps(Utils.key1, deviceId).enqueue(object: Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                completionHandler.invoke(response.body())
            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                completionHandler.invoke(null)
            }
        })
    }
}