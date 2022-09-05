package com.ericversteeg.radiofrost.service

import com.ericversteeg.radiofrost.model.Server
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface ServerRetrofitService {

    @GET("api/v1/find/server/{access_key}")
    fun getServer(@Header("key0") key0: String, @Path("access_key") accessKey: String): Call<Server>

}