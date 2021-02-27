package com.ericversteeg.liquidocean.fragment

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.fragment.app.Fragment
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.ericversteeg.liquidocean.R
import com.ericversteeg.liquidocean.helper.Utils
import com.ericversteeg.liquidocean.listener.DataLoadingCallback
import com.ericversteeg.liquidocean.model.SessionSettings
import com.ericversteeg.liquidocean.model.StatTracker
import kotlinx.android.synthetic.main.fragment_loading_screen.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import kotlin.collections.HashMap

class LoadingScreenFragment : Fragment() {

    var doneLoadingPixels = false
    var doneLoadingPaintQty = false
    var doneSendingDeviceId = false
    var doneLoadingTopContributors = false

    var dataLoadingCallback: DataLoadingCallback? = null

    private lateinit var requestQueue: RequestQueue

    var world = false

    var lastDotsStr = ""

    var timer = Timer()

    val gameTips = arrayOf(
        "You can turn several features on / off in the Options menu.",
        "All drawings can be exported to a PNG files. Simply choose the object selector in the toolbox, tap an object, then select the share or save feature.",
        "Anything you create on the world canvas is automatically saved and shared with others.",
        "Like your level, paint, and others stats? Back your account up and sync across multiple devices by signing into Google or Facebook (we do not collect or store any personal information).",
        "Tap on any pixel on the world canvas to view a history of edits for that position.",
        "No violence, racism, profanity, or nudity of any kind is allowed on the world canvas.",
        "Anyone can get started painting on the world canvas in 5 minutes or less. Simply wait for the next Paint Cycle."
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_loading_screen, container, false)

        // setup views here

        return view
    }

    override fun onPause() {
        super.onPause()

        timer.cancel()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requestQueue = Volley.newRequestQueue(context)

        updateNumLoaded()

        val rIndex = (Math.random() * gameTips.size).toInt()
        game_tip_text.text = "Tip: ${gameTips[rIndex]}"

        context?.apply {
            // sync paint qty or register device
            if (SessionSettings.instance.sentUniqueId) {
                getDeviceInfo()
            }
            else {
                sendDeviceId()
            }

            downloadCanvasPixels(this)

            getTopContributors()

            globe_art.jsonResId = R.raw.globe_json
        }

        timer.schedule(object : TimerTask() {
            override fun run() {
                activity?.runOnUiThread {
                    if (lastDotsStr == "" || lastDotsStr == "." || lastDotsStr == "..") {
                        lastDotsStr = "$lastDotsStr."
                    } else {
                        lastDotsStr = ""
                    }
                    dots_title?.text = lastDotsStr
                }
            }
        }, 200, 200)
    }

    private fun drawWorldCanvas() {
        val conf: Bitmap.Config = Bitmap.Config.ARGB_8888 // see other conf types
        val bitmap: Bitmap = Bitmap.createBitmap(
            world_canvas_preview.width,
            world_canvas_preview.height,
            conf
        ) // this creates a MUTABLE bitmap

        val canvas = Canvas(bitmap)
        val unitSize = world_canvas_preview.height.toFloat() / 512

        val paint = Paint()

        context?.apply {
            val arrJsonStr = SessionSettings.instance.getSharedPrefs(this).getString("arr", null)
            if (arrJsonStr != null) {
                val outerArray = JSONArray(arrJsonStr)
                for (i in 0 until outerArray.length()) {
                    val innerArr = outerArray.getJSONArray(i)
                    for (j in 0 until innerArr.length()) {
                        val color = innerArr.getInt(j)
                        paint.color = color
                        canvas.drawRect(j * unitSize, i * unitSize, unitSize, unitSize, paint)
                    }
                }
            }
        }
    }

    private fun downloadCanvasPixels(context: Context) {
        val jsonObjRequest: StringRequest = object : StringRequest(
            Method.GET,
            Utils.baseUrlApi + "/api/v1/canvas/pixels",
            Response.Listener { response ->
                SessionSettings.instance.arrJsonStr = response

                doneLoadingPixels = true
                downloadFinished()
            },
            Response.ErrorListener { error ->
                showConnectionErrorMessage()
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

        jsonObjRequest.retryPolicy = DefaultRetryPolicy(30000, 1, 1.0f)

        jsonObjRequest.tag = "download"
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
                    Utils.baseUrlApi + "/api/v1/devices/register",
                    paramsJson,
                    { response ->
                        SessionSettings.instance.dropsAmt = response.getInt("paint_qty")
                        SessionSettings.instance.sentUniqueId = true

                        doneSendingDeviceId = true
                        downloadFinished()
                    },
                    { error ->
                        showConnectionErrorMessage()
                    })

                request.tag = "download"
                requestQueue.add(request)
            }
        }
    }

    private fun getDeviceInfo() {
        context?.apply {
            val uniqueId = SessionSettings.instance.uniqueId

            uniqueId?.apply {
                val request = JsonObjectRequest(
                    Request.Method.GET,
                    Utils.baseUrlApi + "/api/v1/devices/$uniqueId/info",
                    null,
                    { response ->
                        SessionSettings.instance.dropsAmt = response.getInt("paint_qty")
                        SessionSettings.instance.xp = response.getInt("xp")

                        StatTracker.instance.numPixelsPaintedWorld = response.getInt("wt")
                        StatTracker.instance.numPixelsPaintedSingle = response.getInt("st")
                        StatTracker.instance.totalPaintAccrued = response.getInt("tp")
                        StatTracker.instance.numPixelOverwritesIn = response.getInt("oi")
                        StatTracker.instance.numPixelOverwritesOut = response.getInt("oo")

                        doneLoadingPaintQty = true
                        downloadFinished()
                    },
                    { error ->
                        showConnectionErrorMessage()
                    })

                request.tag = "download"
                requestQueue.add(request)
            }
        }
    }

    private fun getTopContributors() {
        context?.apply {
            val request = JsonObjectRequest(
                Request.Method.GET,
                Utils.baseUrlApi + "/api/v1/top/contributors",
                null,
                { response ->
                    val topContributors = response.getJSONArray("data")

                    val topContributorNameViews1 = listOf(top_contributor_name_1, top_contributor_name_2,
                        top_contributor_name_3, top_contributor_name_4, top_contributor_name_5)

                    val topContributorAmtViews1 = listOf(top_contributor_amt_1, top_contributor_amt_2,
                        top_contributor_amt_3, top_contributor_amt_4, top_contributor_amt_5)

                    val topContributorNameViews2 = listOf(top_contributor_name_6, top_contributor_name_7,
                        top_contributor_name_8, top_contributor_name_9, top_contributor_name_10)

                    val topContributorAmtViews2 = listOf(top_contributor_amt_6, top_contributor_amt_7,
                        top_contributor_amt_8, top_contributor_amt_9, top_contributor_amt_10)

                    for (i in 0 until topContributors.length()) {
                        val topContributor = topContributors.getJSONObject(i)

                        val name = topContributor.getString("name")
                        val amt = topContributor.getString("amt")

                        if (i < 5) {
                            topContributorNameViews1[i].text = name
                            topContributorAmtViews1[i].text = amt.toString()

                            topContributorNameViews1[i].alpha = 0F
                            topContributorAmtViews1[i].alpha = 0F

                            topContributorNameViews1[i].animate().setDuration(500).alphaBy(1F)
                            topContributorAmtViews1[i].animate().setDuration(500).alphaBy(1F)
                        }
                        else if (i < 10) {
                            topContributorNameViews2[i - 5].text = name
                            topContributorAmtViews2[i - 5].text = amt.toString()

                            topContributorNameViews2[i - 5].alpha = 0F
                            topContributorAmtViews2[i - 5].alpha = 0F

                            topContributorNameViews2[i - 5].animate().setDuration(500).alphaBy(1F)
                            topContributorAmtViews2[i - 5].animate().setDuration(500).alphaBy(1F)
                        }
                    }

                    doneLoadingTopContributors = true
                    downloadFinished()
                },
                { error ->

                })

            requestQueue.add(request)
        }
    }

    private fun showConnectionErrorMessage() {
        (context as Activity).runOnUiThread {
            requestQueue.cancelAll("download")

            var errorType = 0
            var message = "Oops, could not find world pixel data. Please try again"

            context?.apply {
                if (!Utils.isNetworkAvailable(this)) {
                    errorType = 1
                    message = "No network connectivity"
                }
            }

            AlertDialog.Builder(context)
                .setMessage(message)
                // The dialog is automatically dismissed when a dialog button is clicked.
                .setPositiveButton(
                    android.R.string.ok,
                    object : DialogInterface.OnClickListener {
                        override fun onClick(dialog: DialogInterface?, id: Int) {
                            dialog?.dismiss()
                            dataLoadingCallback?.onConnectionError(errorType)
                        }
                    })
                .setOnDismissListener {
                    dataLoadingCallback?.onConnectionError(errorType)
                }
                .show()
        }
    }

    private fun downloadFinished() {
        updateNumLoaded()
        if (loadingDone()) {
            dataLoadingCallback?.onDataLoaded(world)
        }
    }

    private fun updateNumLoaded() {
        status_text.text = "Loading ${getNumLoaded()} / 3"
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

        if (doneLoadingTopContributors) {
            num++
        }

        return num
    }
}