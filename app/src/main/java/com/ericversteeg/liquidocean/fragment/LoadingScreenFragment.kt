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
import com.bumptech.glide.Glide
import com.ericversteeg.liquidocean.R
import com.ericversteeg.liquidocean.helper.Animator
import com.ericversteeg.liquidocean.helper.Utils
import com.ericversteeg.liquidocean.listener.DataLoadingCallback
import com.ericversteeg.liquidocean.listener.SocketConnectCallback
import com.ericversteeg.liquidocean.model.*
import com.ericversteeg.liquidocean.service.ServerService
import com.ericversteeg.liquidocean.view.ActionButtonView
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_loading_screen.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import kotlin.collections.HashMap

class LoadingScreenFragment : Fragment(), QueueSocket.SocketListener, SocketConnectCallback {

    private var serverService = ServerService()

    var doneLoadingPixels = false
    var doneLoadingPaintQty = false
    var doneSendingDeviceId = false
    var doneLoadingChunkCount = 0
    var doneLoadingTopContributors = false

    var doneConnectingQueue = false
    var doneConnectingSocket = false

    var dataLoadingCallback: DataLoadingCallback? = null

    private lateinit var requestQueue: RequestQueue
    private lateinit var dataRequestQueue: RequestQueue

    var world = false
    var realmId = 0
    lateinit var server: Server

    var lastDotsStr = ""

    var timer = Timer()

    val gameTips = arrayOf(
        "You can turn several features on / off in the Options menu.",
        "All drawings can be exported to PNG files. Simply choose the object selector in the toolbox, tap an object, then select the share or save feature.",
        "Anything you create on the world canvas is automatically saved and shared with others.",
        "Like your level, paint, and other stats? Back your account up and sync across multiple devices with an access pincode.",
        "Tap on any pixel on the world canvas to view a history of edits for that position.",
        "No racism, harassment, or hate speech is allowed on the world canvas.",
        "Anyone can get started painting on the world canvas in 5 minutes or less. Simply wait for the next Paint Cycle.",
        "Tap the bottom corner of the screen while drawing to bring up many recently used colors."
    )

    var showingError = false

    var queuePos = -1

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

        InteractiveCanvasSocket.instance.socketConnectCallback = null

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
        connecting_title.text = String.format("Connecting to %s", server.name)

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

        // start connect
        val accessKey = if (server.isAdmin) {
            server.adminKey
        }
        else {
            server.accessKey
        }
        serverService.getServer(accessKey) { server ->
            if (server == null) {
                showConnectionErrorMessage(authError = true)
                return@getServer
            }

            QueueSocket.instance.socketListener = this
            QueueSocket.instance.startSocket(server)

            Glide.with(this).load("${server.serviceAltBaseUrl()}/canvas").into(realm_art)

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
    }

    private fun getCanvas() {
        SessionSettings.instance.maxPaintAmt = server.maxPixels

        getTopContributors()

        // sync paint qty or register device
        if (SessionSettings.instance.sentUniqueId) {
            getDeviceInfo()
        }
        else {
            sendDeviceId()
        }

        if (realmId == 2) {
            downloadCanvasPixels()
        }
        else if (world) {
            Observable.fromRunnable<Void> {
                downloadChunkPixels(1)
                downloadChunkPixels(2)
                downloadChunkPixels(3)
                downloadChunkPixels(4)
            }.subscribeOn(Schedulers.io()).subscribe()
        }
    }

    private fun downloadChunkPixels(chunk: Int) {
        val jsonObjRequest: StringRequest = object : StringRequest(
            Method.GET,
            server.serviceBaseUrl() + "api/v1/canvas/${server.id}/pixels/${chunk}",
            Response.Listener { response ->
                when(chunk) {
                    1 -> SessionSettings.instance.chunk1 = response
                    2 -> SessionSettings.instance.chunk2 = response
                    3 -> SessionSettings.instance.chunk3 = response
                    4 -> SessionSettings.instance.chunk4 = response
                }

                doneLoadingChunkCount += 1
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
                headers["Content-Type"] = "application/json"
                headers["key1"] = Utils.key1
                return headers
            }
        }

        //jsonObjRequest.retryPolicy = DefaultRetryPolicy(30000, 2, 1.0f)

        jsonObjRequest.tag = "download"
        dataRequestQueue.add(jsonObjRequest)
    }

//    private fun processChunk(chunk: Int) {
//        val response = when(chunk) {
//            1 -> SessionSettings.instance.chunk1
//            2 -> SessionSettings.instance.chunk2
//            3 -> SessionSettings.instance.chunk3
//            4 -> SessionSettings.instance.chunk4
//            else -> ""
//        }
//        lateinit var arr: Array<IntArray>
//        if (chunk == 1) {
//            SessionSettings.instance.chunk1 = Array(256) { IntArray(1024) }
//            arr = SessionSettings.instance.chunk1
//            doneLoadingChunk1 = true
//        } else if (chunk == 2) {
//            SessionSettings.instance.chunk2 = Array(256) { IntArray(1024) }
//            arr = SessionSettings.instance.chunk2
//            doneLoadingChunk2 = true
//        } else if (chunk == 3) {
//            SessionSettings.instance.chunk3 = Array(256) { IntArray(1024) }
//            arr = SessionSettings.instance.chunk3
//            doneLoadingChunk3 = true
//        } else if (chunk == 4) {
//            SessionSettings.instance.chunk4 = Array(256) { IntArray(1024) }
//            arr = SessionSettings.instance.chunk4
//            doneLoadingChunk4 = true
//        }
//
//        val chunkJsonArr = JSONArray(response)
//        for (i in 0 until chunkJsonArr.length()) {
//            val chunkInnerJsonArr = chunkJsonArr.getJSONArray(i)
//            for (j in 0 until chunkInnerJsonArr.length()) {
//                arr[i][j] = chunkInnerJsonArr.getInt(j)
//            }
//        }
//    }

    private fun downloadCanvasPixels() {
        val jsonObjRequest: StringRequest = object : StringRequest(
            Method.GET,
            server.serviceBaseUrl() + "api/v1/canvas/${realmId}/pixels",
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
                headers["key1"] = Utils.key1
                return headers
            }
        }

        jsonObjRequest.retryPolicy = DefaultRetryPolicy(60000, 1, 1.0f)

        jsonObjRequest.tag = "download"
        dataRequestQueue.add(jsonObjRequest)
    }

    private fun sendDeviceId() {
        val uniqueId = SessionSettings.instance.uniqueId

        val requestParams = HashMap<String, String>()

        if (uniqueId == null) {
            return
        }

        requestParams["uuid"] = uniqueId

        val paramsJson = JSONObject(requestParams as Map<String, String>)

        val request = object : JsonObjectRequest(
            Request.Method.POST,
            server.serviceBaseUrl() + "api/v1/devices/register",
            paramsJson,
            { response ->
                SessionSettings.instance.deviceId = response.getInt("id")
                SessionSettings.instance.dropsAmt = response.getInt("paint_qty")
                SessionSettings.instance.sentUniqueId = true

                doneSendingDeviceId = true
                downloadFinished()
            },
            { error ->
                showConnectionErrorMessage()
            }) {

            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = "application/json; charset=utf-8"
                headers["key1"] = Utils.key1
                return headers
            }
        }

        request.tag = "download"
        requestQueue.add(request)
    }

    private fun getDeviceInfo() {
        val uniqueId = SessionSettings.instance.uniqueId

        val request = object: JsonObjectRequest(
            Request.Method.GET,
            server.serviceBaseUrl() + "api/v1/devices/$uniqueId/info",
            null,
            { response ->
                SessionSettings.instance.deviceId = response.getInt("id")
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
            }) {

            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = "application/json"
                headers["key1"] = Utils.key1
                return headers
            }
        }

        request.tag = "download"
        requestQueue.add(request)
    }

    private fun getTopContributors() {
        val request = object: JsonObjectRequest(
            Request.Method.GET,
            server.serviceBaseUrl() + "api/v1/top/contributors",
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

                    var name = topContributor.getString("name")

                    if (name.length > 10) {
                        name = "${name.substring(0 until 7)}..."
                    }

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

            }) {

            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = "application/json; charset=utf-8"
                headers["key1"] = Utils.key1
                return headers
            }
        }

        requestQueue.add(request)
    }

    private fun showConnectionErrorMessage(socket: Boolean = false, authError: Boolean = false) {
        InteractiveCanvasSocket.instance.disconnect()

        if (authError) {
            SessionSettings.instance.removeServer(requireContext(), server)
        }

        if (!showingError) {
            showingError = true
            (context as Activity?)?.runOnUiThread {
                requestQueue.cancelAll("download")

                val message = if (authError) {
                    "Access key has changed"
                }
                else if (socket) {
                    "Pixel socket isn't responding"
                }
                else {
                    "No connection to server can be established"
                }

                AlertDialog.Builder(context)
                    .setMessage(message)
                    // The dialog is automatically dismissed when a dialog button is clicked.
                    .setPositiveButton(
                        "..."
                    ) { dialog, id ->
                        dialog?.dismiss()
                        dataLoadingCallback?.onConnectionError()
                        showingError = false
                    }
                    .setOnDismissListener {
                        dataLoadingCallback?.onConnectionError()
                        showingError = false
                    }
                    .show()
            }
        }
    }

    private fun downloadFinished() {
        updateNumLoaded()
        if (loadingDone()) {
            dataLoadingCallback?.onDataLoaded(server)
        }
    }

    private fun updateNumLoaded() {
        if (activity == null) return

        requireActivity().runOnUiThread {
            if (realmId == 2) {
                status_text?.text = "Loading ${getNumLoaded()} / 4"
            }
            else {
                status_text?.text = "Loading ${getNumLoaded()} / 8"
            }
        }
    }

    private fun loadingDone(): Boolean {
        if (realmId == 2) {
            return (doneLoadingPaintQty || doneSendingDeviceId) && doneLoadingTopContributors &&
                    doneLoadingPixels && doneConnectingSocket
        }
        else if (world) {
            return (doneLoadingPaintQty || doneSendingDeviceId) && doneLoadingTopContributors &&
                    doneLoadingChunkCount == 4 &&
                    doneConnectingQueue && doneConnectingSocket
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
        else {
            num += doneLoadingChunkCount

            if (doneLoadingPaintQty || doneSendingDeviceId) {
                num++
            }

            if (doneLoadingTopContributors) {
                num++
            }

            if (doneConnectingQueue) {
                num++
            }

            if (doneConnectingSocket) {
                num++
            }
        }

        return num
    }

    // queue socket listener
    override fun onQueueConnect() {
        doneConnectingQueue = true
        updateNumLoaded()
    }

    override fun onQueueConnectError() {
        doneConnectingQueue = false
        showConnectionErrorMessage(true)
    }

    override fun onAddedToQueue(pos: Int) {
        queuePos = pos
        updateQueuePos(true)

        timer.schedule(object: TimerTask() {
            override fun run() {
                requireActivity().runOnUiThread {
                    updateQueuePos()
                }
            }
        }, QueueSocket.interval * 1000L, QueueSocket.interval * 1000L)
    }

    private fun updateQueuePos(start: Boolean = false) {
        requireActivity().runOnUiThread {
            if (start && queuePos > 1) {
                text_queue_pos.animate().setDuration(500).alphaBy(1F).start()
            }
            text_queue_pos.text = String.format("~%d in queue", queuePos--)
        }
    }

    override fun onServiceReady() {
        QueueSocket.instance.socketListener = null
        QueueSocket.instance.socket?.disconnect()

        InteractiveCanvasSocket.instance.socketConnectCallback = this
        InteractiveCanvasSocket.instance.startSocket(server)
    }

    // canvas socket listener
    override fun onSocketConnect() {
        Log.i("Canvas Socket", "Socket connected!")

        doneConnectingSocket = true
        updateNumLoaded()
        InteractiveCanvasSocket.instance.socketConnectCallback = null

        getCanvas()
    }

    override fun onSocketDisconnect(error: Boolean) {
        Log.i("Canvas Socket", "Socket disconnect.")

        doneConnectingSocket = false
        showConnectionErrorMessage(true)
    }
}