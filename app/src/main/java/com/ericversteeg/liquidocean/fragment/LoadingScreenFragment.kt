package com.ericversteeg.liquidocean.fragment

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.ericversteeg.liquidocean.R
import com.ericversteeg.liquidocean.helper.Animator
import com.ericversteeg.liquidocean.helper.Utils
import com.ericversteeg.liquidocean.listener.DataLoadingCallback
import com.ericversteeg.liquidocean.listener.SocketConnectCallback
import com.ericversteeg.liquidocean.model.InteractiveCanvas
import com.ericversteeg.liquidocean.model.InteractiveCanvasSocket
import com.ericversteeg.liquidocean.model.SessionSettings
import com.ericversteeg.liquidocean.model.StatTracker
import com.ericversteeg.liquidocean.view.ActionButtonView
import kotlinx.android.synthetic.main.fragment_loading_screen.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import kotlin.collections.HashMap

class LoadingScreenFragment : Fragment(), SocketConnectCallback {

    var doneLoadingPixels = false
    var doneLoadingPaintQty = false
    var doneSendingDeviceId = false
    var doneLoadingChunk1 = false
    var doneLoadingChunk2 = false
    var doneLoadingChunk3 = false
    var doneLoadingChunk4 = false
    var doneLoadingTopContributors = false

    var doneConnectingSocket = false

    var dataLoadingCallback: DataLoadingCallback? = null

    private lateinit var requestQueue: RequestQueue
    private lateinit var dataRequestQueue: RequestQueue

    var world = false
    var realmId = 0

    var lastDotsStr = ""

    var timer = Timer()

    val gameTips = arrayOf(
        "You can turn several features on / off in the Options menu.",
        "All drawings can be exported to a PNG files. Simply choose the object selector in the toolbox, tap an object, then select the share or save feature.",
        "Anything you create on the world canvas is automatically saved and shared with others.",
        "Like your level, paint, and other stats? Back your account up and sync across multiple devices with Google.",
        "Tap on any pixel on the world canvas to view a history of edits for that position.",
        "No violence, racism, profanity, or nudity of any kind is allowed on the world canvas.",
        "Anyone can get started painting on the world canvas in 5 minutes or less. Simply wait for the next Paint Cycle.",
        "Tap the bottom-left of the screen while drawing to bring up many recently used colors."
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
        dataRequestQueue = Volley.newRequestQueue(context)

        updateNumLoaded()

        if (realmId == 2) {
            connecting_title.text = "Connecting to dev server"
        }

        val rIndex = (Math.random() * gameTips.size).toInt()
        game_tip_text.text = "Tip: ${gameTips[rIndex]}"

        Timer().schedule(object : TimerTask() {
            override fun run() {
                activity?.runOnUiThread {
                    if (game_tip_text != null) {
                        Animator.fadeInView(game_tip_text)
                    }
                }
            }

        }, 3000)

        context?.apply {
            // sync paint qty or register device
            if (SessionSettings.instance.sentUniqueId) {
                getDeviceInfo()
            }
            else {
                sendDeviceId()
            }

            if (realmId == 2) {
                downloadCanvasPixels(this)
            }
            else if (world) {
                downloadChunkPixels(this, 1)
                downloadChunkPixels(this, 2)
                downloadChunkPixels(this, 3)
                downloadChunkPixels(this, 4)
            }

            getTopContributors()

            InteractiveCanvasSocket.instance.startSocket()
            InteractiveCanvasSocket.instance.socketConnectCallback = this@LoadingScreenFragment

            SessionSettings.instance.updateShortTermPixels()

            if (realmId == 2) {
                realm_art.jsonResId = R.raw.mc_tool_json
            }
            else {
                realm_art.jsonResId = R.raw.globe_json
            }
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

        Utils.setViewLayoutListener(view, object: Utils.ViewLayoutListener {
            override fun onViewLayout(view: View) {
                if (SessionSettings.instance.tablet) {
                    // contributors 1
                    var layoutParams = ConstraintLayout.LayoutParams(top_contributors_container_1.width, top_contributors_container_1.height)
                    layoutParams.rightToLeft = realm_art.id
                    layoutParams.topToTop = ConstraintSet.PARENT_ID
                    layoutParams.bottomToBottom = ConstraintSet.PARENT_ID

                    layoutParams.setMargins(Utils.dpToPx(context, 20), 0, Utils.dpToPx(context, 40), 0)

                    top_contributors_container_1.layoutParams = layoutParams

                    // contributors 2
                    layoutParams = ConstraintLayout.LayoutParams(top_contributors_container_2.width, top_contributors_container_2.height)
                    layoutParams.leftToRight = realm_art.id
                    layoutParams.topToTop = ConstraintSet.PARENT_ID
                    layoutParams.bottomToBottom = ConstraintSet.PARENT_ID

                    layoutParams.setMargins(Utils.dpToPx(context, 40), 0, Utils.dpToPx(context, 20), 0)

                    top_contributors_container_2.layoutParams = layoutParams
                }
            }
        })
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

    private fun downloadChunkPixels(context: Context, chunk: Int) {
        val jsonObjRequest: StringRequest = object : StringRequest(
            Method.GET,
            Utils.baseUrlApi + "/api/v1/canvas/${realmId}/pixels/${chunk}",
            Response.Listener { response ->
                lateinit var arr: Array<IntArray>
                if (chunk == 1) {
                    SessionSettings.instance.chunk1 = Array(256) { IntArray(1024) }
                    arr = SessionSettings.instance.chunk1
                    doneLoadingChunk1 = true
                } else if (chunk == 2) {
                    SessionSettings.instance.chunk2 = Array(256) { IntArray(1024) }
                    arr = SessionSettings.instance.chunk2
                    doneLoadingChunk2 = true
                } else if (chunk == 3) {
                    SessionSettings.instance.chunk3 = Array(256) { IntArray(1024) }
                    arr = SessionSettings.instance.chunk3
                    doneLoadingChunk3 = true
                } else if (chunk == 4) {
                    SessionSettings.instance.chunk4 = Array(256) { IntArray(1024) }
                    arr = SessionSettings.instance.chunk4
                    doneLoadingChunk4 = true
                }

                val chunkJsonArr = JSONArray(response)
                for (i in 0 until chunkJsonArr.length()) {
                    val chunkInnerJsonArr = chunkJsonArr.getJSONArray(i)
                    for (j in 0 until chunkInnerJsonArr.length()) {
                        arr[i][j] = chunkInnerJsonArr.getInt(j)
                    }
                }

                downloadFinished()
            },
            Response.ErrorListener { error ->
                showConnectionErrorMessage()
                error.message?.apply {
                    Log.i("Error", this)
                }
            }) {

            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = "application/json; charset=utf-8"
                return headers
            }
        }

        jsonObjRequest.retryPolicy = DefaultRetryPolicy(30000, 2, 1.0f)

        jsonObjRequest.tag = "download"
        dataRequestQueue.add(jsonObjRequest)
    }

    private fun downloadCanvasPixels(context: Context) {
        val jsonObjRequest: StringRequest = object : StringRequest(
            Method.GET,
            Utils.baseUrlApi + "/api/v1/canvas/${realmId}/pixels",
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

            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = "application/json; charset=utf-8"
                return headers
            }
        }

        jsonObjRequest.retryPolicy = DefaultRetryPolicy(60000, 1, 1.0f)

        jsonObjRequest.tag = "download"
        dataRequestQueue.add(jsonObjRequest)
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

                    val topContributorNameViews1 = listOf(
                        top_contributor_name_1, top_contributor_name_2,
                        top_contributor_name_3, top_contributor_name_4, top_contributor_name_5
                    )

                    val topContributorAmtViews1 = listOf(
                        top_contributor_amt_1, top_contributor_amt_2,
                        top_contributor_amt_3, top_contributor_amt_4, top_contributor_amt_5
                    )

                    val topContributorNameViews2 = listOf(
                        top_contributor_name_6, top_contributor_name_7,
                        top_contributor_name_8, top_contributor_name_9, top_contributor_name_10
                    )

                    val topContributorAmtViews2 = listOf(
                        top_contributor_amt_6, top_contributor_amt_7,
                        top_contributor_amt_8, top_contributor_amt_9, top_contributor_amt_10
                    )

                    for (i in 0 until topContributors.length()) {
                        val topContributor = topContributors.getJSONObject(i)

                        val name = topContributor.getString("name")
                        val amt = topContributor.getString("amt")

                        if (i < 5) {
                            if (i == 0) {
                                SessionSettings.instance.firstContributorName = name
                                topContributorNameViews1[i].setTextColor(ActionButtonView.yellowPaint.color)
                            } else if (i == 1) {
                                SessionSettings.instance.secondContributorName = name
                                topContributorNameViews1[i].setTextColor(Color.parseColor("#AFB3B1"))
                            } else if (i == 2) {
                                SessionSettings.instance.thirdContributorName = name
                                topContributorNameViews1[i].setTextColor(Color.parseColor("#BD927B"))
                            }

                            topContributorNameViews1[i].text = name
                            topContributorAmtViews1[i].text = amt.toString()

                            topContributorNameViews1[i].alpha = 0F
                            topContributorAmtViews1[i].alpha = 0F

                            topContributorNameViews1[i].animate().setDuration(500).alphaBy(1F)
                            topContributorAmtViews1[i].animate().setDuration(500).alphaBy(1F)
                        } else if (i < 10) {
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
                else if (!doneConnectingSocket) {
                    errorType = 2
                    message = "Socket connection error"
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
            dataLoadingCallback?.onDataLoaded(world, realmId)
        }
    }

    private fun updateNumLoaded() {
        if (realmId == 2) {
            status_text.text = "Loading ${getNumLoaded()} / 4"
        }
        else if (realmId == 1) {
            status_text.text = "Loading ${getNumLoaded()} / 7"
        }
    }

    private fun loadingDone(): Boolean {
        if (realmId == 2) {
            return (doneLoadingPaintQty || doneSendingDeviceId) && doneLoadingTopContributors &&
                    doneLoadingPixels && doneConnectingSocket
        }
        else if (world) {
            return (doneLoadingPaintQty || doneSendingDeviceId) && doneLoadingTopContributors &&
                    doneLoadingChunk1 && doneLoadingChunk2 && doneLoadingChunk3 && doneLoadingChunk4 &&
                    doneConnectingSocket
        }
        return false
    }

    private fun getNumLoaded(): Int {
        var num = 0
        if (realmId == 2) {
            if (doneLoadingPixels) {
                num++
            }

            if (doneLoadingPaintQty || doneSendingDeviceId) {
                num++
            }

            if (doneLoadingTopContributors) {
                num++
            }

            if (doneConnectingSocket) {
                num++
            }
        }
        else if (realmId == 1) {
            if (doneLoadingChunk1) {
                num++
            }

            if (doneLoadingChunk2) {
                num++
            }

            if (doneLoadingChunk3) {
                num++
            }

            if (doneLoadingChunk4) {
                num++
            }

            if (doneLoadingPaintQty || doneSendingDeviceId) {
                num++
            }

            if (doneLoadingTopContributors) {
                num++
            }

            if (doneConnectingSocket) {
                num++
            }
        }

        return num
    }

    override fun onSocketConnect() {
        doneConnectingSocket = true
        InteractiveCanvasSocket.instance.socketConnectCallback = null
    }

    override fun onSocketConnectError() {
        showConnectionErrorMessage()
    }
}