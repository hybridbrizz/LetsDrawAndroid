package com.matrixwarez.pt.service

import com.matrixwarez.pt.model.Server
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface ServerRetrofitService {

    @GET("api/v1/find/server/{access_key}")
    fun getServer(@Header("key0") key0: String, @Path("access_key") accessKey: String): Call<Server>

    @GET("api/v1/find/private/server/{access_key}")
    fun getPrivateServer(@Header("key0") key0: String, @Path("access_key") accessKey: String): Call<Server>

    @GET("api/v1/serverlist")
    fun getServerList(): Call<List<Server>>

    @POST("api/v1/private/serverlist")
    fun getPrivateServerList(@Body keys: List<String>): Call<List<Server>>

    @POST("api/v1/private/admin/serverlist")
    fun getPrivateAdminServerList(@Body keys: List<String>): Call<List<Server>>
}