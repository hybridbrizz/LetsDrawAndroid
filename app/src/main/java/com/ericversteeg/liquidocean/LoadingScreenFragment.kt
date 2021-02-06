package com.ericversteeg.liquidocean

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.ericversteeg.liquidocean.helper.SessionSettings
import com.ericversteeg.liquidocean.listener.DataLoadingCallback
import kotlinx.android.synthetic.main.fragment_loading_screen.*
import org.json.JSONArray
import org.json.JSONObject

class LoadingScreenFragment : Fragment() {

    var doneLoadingPixels = false
    var doneLoadingPaintQty = false
    var doneSendingDeviceId = false

    var dataLoadingCallback: DataLoadingCallback? = null

    private lateinit var requestQueue: RequestQueue

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_loading_screen, container, false)

        // setup views here

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requestQueue = Volley.newRequestQueue(context)

        context?.apply {
            // load session settings
            SessionSettings.instance.load(this)

            // sync paint qty or register device
            if (SessionSettings.instance.sentUniqueId) {
                getPaintQty()
            }
            else {
                sendDeviceId()
            }

            // load pixel data
            downloadCanvasPixels(this)
        }
    }

    private fun initPixels(arrJsonStr: String) {
        val arr = Array(512) { IntArray(512) }

        val outerArray = JSONArray(arrJsonStr)

        for (i in 0 until outerArray.length()) {
            val innerArr = outerArray.getJSONArray(i)
            for (j in 0 until innerArr.length()) {
                val color = innerArr.getInt(j)
                arr[i][j] = color
            }
        }

        val jsonArr = JSONArray(arr)

        if (context != null) {
            val ed = SessionSettings.instance.getSharedPrefs(context!!).edit()
            ed.putString("arr", jsonArr.toString())
            ed.apply()
        }
        else {
            Log.i("Loading error", "Error loading pixel data, context was null inside loading fragment.")
        }
    }

    private fun downloadCanvasPixels(context: Context) {
        val jsonObjRequest: StringRequest = object : StringRequest(
            Method.GET,
            "http://192.168.200.69:5000/api/v1/canvas/pixels",
            Response.Listener { response ->
                initPixels(response)

                doneLoadingPixels = true
                downloadFinished()
            },
            Response.ErrorListener { error ->
                error.message?.apply {
                    Log.i("Error", this)
                }
            }) {
            override fun getBodyContentType(): String {
                return "application/x-www-form-urlencoded; charset=UTF-8"
            }

            override fun getParams(): Map<String, String> {
                val params: MutableMap<String, String> = HashMap()
                val pixelData = SessionSettings.instance.getSharedPrefs(context).getString(
                    "arr",
                    ""
                )

                pixelData?.apply {
                    params["arr"] = this
                }

                return params
            }
        }

        jsonObjRequest.retryPolicy = DefaultRetryPolicy(30000, 3, 1.0f)

        requestQueue.add(jsonObjRequest)
    }

    private fun sendDeviceId() {
        context?.apply {
            val uniqueId = SessionSettings.instance.uniqueId

            uniqueId?.apply {
                val requestParams = HashMap<String, String>()

                requestParams["uuid"] = uniqueId

                val paramsJson = JSONObject(requestParams as Map<String, String>)

                val request = JsonObjectRequest(
                    Request.Method.POST,
                    "http://192.168.200.69:5000/api/v1/devices/register",
                    paramsJson,
                    { response ->
                        SessionSettings.instance.dropsAmt = response.getInt("paint_qty")
                        SessionSettings.instance.sentUniqueId = true

                        doneSendingDeviceId = true
                        downloadFinished()
                    },
                    { error ->
                        Log.i("Error", error.message!!)
                    })

                requestQueue.add(request)
            }
        }
    }

    private fun getPaintQty() {
        context?.apply {
            val uniqueId = SessionSettings.instance.uniqueId

            uniqueId?.apply {
                val request = JsonObjectRequest(
                    Request.Method.GET,
                    "http://192.168.200.69:5000/api/v1/devices/$uniqueId/paint",
                    null,
                    { response ->
                        SessionSettings.instance.dropsAmt = response.getInt("paint_qty")

                        doneLoadingPaintQty = true
                        downloadFinished()
                    },
                    { error ->
                        error.message?.apply {
                            Log.i("Error", this)
                        }
                    })

                requestQueue.add(request)
            }
        }
    }

    private fun downloadFinished() {
        updateNumLoaded()
        if (loadingDone()) {
            dataLoadingCallback?.onDataLoaded()
        }
    }

    private fun updateNumLoaded() {
        status_text.text = "Loading ${getNumLoaded()} / 2"
    }

    private fun loadingDone(): Boolean {
        return (doneLoadingPaintQty || doneSendingDeviceId) && doneLoadingPixels
    }

    private fun getNumLoaded(): Int {
        var num = 0
        if (doneLoadingPixels || doneSendingDeviceId) {
            num++
        }

        if (doneLoadingPaintQty) {
            num++
        }

        return num
    }
}