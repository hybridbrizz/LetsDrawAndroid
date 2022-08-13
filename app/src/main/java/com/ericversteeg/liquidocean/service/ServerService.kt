package com.ericversteeg.liquidocean.service

import com.ericversteeg.liquidocean.helper.Utils
import com.ericversteeg.liquidocean.model.Server
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ServerService {

    private val retrofit = Retrofit.Builder()
        .baseUrl(Utils.baseServersUrl)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val service = retrofit.create(ServerRetrofitService::class.java)

    private val key0 = "MYCEJUCNZ6AVZAVDZBHKJJYM6OIWQVDOC1OU7RZP"

    fun getServer(accessKey: String, completionHandler: (statusCode: Int, server: Server?) -> Unit) {
        service.getServer(key0, accessKey).enqueue(object: Callback<Server> {
            override fun onResponse(call: Call<Server>, response: Response<Server>) {
                completionHandler.invoke(response.code(), response.body())
            }

            override fun onFailure(call: Call<Server>, t: Throwable) {
                completionHandler.invoke(0, null)
            }
        })
    }
}