package com.matrixwarez.pt.service

import android.content.Context
import com.matrixwarez.pt.helper.Utils
import com.matrixwarez.pt.model.Server
import com.matrixwarez.pt.model.SessionSettings
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

    fun getServerList(completionHandler: (statusCode: Int, list: List<Server>) -> Unit) {
        service.getServerList().enqueue(object: Callback<List<Server>> {
            override fun onResponse(call: Call<List<Server>>, response: Response<List<Server>>) {
                val list = response.body()
                list?.let {
                    it.forEach { server ->
                        server.uuid = SessionSettings.instance.publicServerUniqueIds[server.id.toString()] ?: ""
                    }
                }
                completionHandler.invoke(response.code(), list ?: listOf())
            }

            override fun onFailure(call: Call<List<Server>>, t: Throwable) {
                completionHandler.invoke(0, listOf())
            }
        })
    }

    fun getPrivateServerList(context: Context, keys: List<String>, completionHandler: (statusCode: Int, list: List<Server>) -> Unit) {
        service.getPrivateServerList(keys).enqueue(object: Callback<List<Server>> {
            override fun onResponse(call: Call<List<Server>>, response: Response<List<Server>>) {
                val list = response.body()
                list?.let {
                    SessionSettings.instance.syncServerStatus(context, it)
                }
                completionHandler.invoke(response.code(), SessionSettings.instance.servers)
            }

            override fun onFailure(call: Call<List<Server>>, t: Throwable) {
                completionHandler.invoke(0, listOf())
            }
        })
    }

    fun getPrivateAdminServerList(context: Context, keys: List<String>, completionHandler: (statusCode: Int, list: List<Server>) -> Unit) {
        service.getPrivateAdminServerList(keys).enqueue(object: Callback<List<Server>> {
            override fun onResponse(call: Call<List<Server>>, response: Response<List<Server>>) {
                val list = response.body()
                list?.let {
                    SessionSettings.instance.syncServerStatus(context, it)
                }
                completionHandler.invoke(response.code(), SessionSettings.instance.servers)
            }

            override fun onFailure(call: Call<List<Server>>, t: Throwable) {
                completionHandler.invoke(0, listOf())
            }
        })
    }
}