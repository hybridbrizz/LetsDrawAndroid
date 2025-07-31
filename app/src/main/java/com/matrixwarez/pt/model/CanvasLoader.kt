package com.matrixwarez.pt.model

import android.app.Activity
import android.app.AlertDialog
import android.util.Log
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.matrixwarez.pt.R
import com.matrixwarez.pt.activity.InteractiveCanvasActivity
import com.matrixwarez.pt.helper.Utils
import com.matrixwarez.pt.listener.DataLoadingCallback
import com.matrixwarez.pt.listener.SocketConnectCallback
import com.matrixwarez.pt.service.CanvasService
import com.matrixwarez.pt.service.ServerService
import com.matrixwarez.pt.view.LoadingProgressBar
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID

class CanvasLoader(val activity: Activity, var server: Server, val progressBar: LoadingProgressBar,
                   val dataLoadingCallback: DataLoadingCallback? = null,
                   val socketListener: SocketConnectCallback): QueueSocket.SocketListener, SocketConnectCallback {

    private val serverService = ServerService()

    private val requestQueue = Volley.newRequestQueue(activity)
    private val dataRequestQueue = Volley.newRequestQueue(activity)

    var showingError = false

    var queuePos = -1

    private var abortOnPause = true
    private var aborted = false

    var doneLoadingPixels = false
    var doneLoadingPaintQty = false
    var doneSendingDeviceId = false
    var doneLoadingChunkCount = 0
    var doneCheckingIp = false

    var doneConnectingQueue = false
    var doneConnectingSocket = false

    private var canvasService: CanvasService? = null

    fun abort() {
        aborted = true

        InteractiveCanvasSocket.instance.socketConnectCallback = null
        InteractiveCanvasSocket.instance.disconnect()

        QueueSocket.instance.socketListener = null
        QueueSocket.instance.socket?.disconnect()

        serverService.abort()
        canvasService?.abort()
    }

    fun startLoading() {
        // start connect
        val accessKey = if (server.isAdmin) {
            server.adminKey
        }
        else {
            server.accessKey
        }

        serverService.getServer(accessKey) { code, server ->
            server?.let {
                canvasService = CanvasService(server)
            }

            val storeduuid = this.server.uuid
            val storedpublic = this.server.public

            if (!this.server.public) {
                SessionSettings.instance.removeServer(activity, this.server, false)
            }

            if (server == null && code >= 400 && code < 500) {
                showConnectionErrorMessage(authError = true)
                return@getServer
            } else if (server == null) {
                showConnectionErrorMessage(socket = false)
                SessionSettings.instance.addServer(activity, this.server)
                return@getServer
            }

            this.server = server.also {
                it.uuid = storeduuid
                it.public = storedpublic
                it.lastVisited = System.currentTimeMillis()
            }

            if (this.server.public) {
                SessionSettings.instance.serverLastVisitedTimes[this.server.id.toString()] =
                    this.server.lastVisited
            } else {
                SessionSettings.instance.addServer(activity, this.server)
            }

            canvasService = CanvasService(server)

            SessionSettings.instance.uniqueId = this.server.uuid
            SessionSettings.instance.lastVisitedServer = this.server
            SessionSettings.instance.saveLastVisitedIndex(activity)

            SessionSettings.instance.maxPaintAmt = server.maxPixels
            SessionSettings.instance.addPaintInterval = server.pixelInterval

            SessionSettings.instance.canvasSize = server.size
            SessionSettings.instance.maxSend = server.maxSend

            Log.d("Connection", "Got server info, starting queue socket connect.")
            QueueSocket.instance.socketListener = this
            QueueSocket.instance.startSocket(server)
        }
    }

    private fun getCanvas() {
        SessionSettings.instance.maxPaintAmt = server.maxPixels

        // register device or sync paint qty
        if (server.uuid == "") {
            sendDeviceId(server)
        }
        else {
            getDeviceInfo(server)
        }

        Observable.fromRunnable<Void> {
            downloadChunkPixels(1)
            downloadChunkPixels(2)
            downloadChunkPixels(3)
            downloadChunkPixels(4)
        }.subscribeOn(Schedulers.io()).subscribe()
    }

    private fun downloadChunkPixels(chunk: Int) {
        Log.d("Connection", "Downloading canvas chunk $chunk.")

        canvasService?.getChunkPixels(chunk) { response ->
            if (response == null) {
                showConnectionErrorMessage()
                return@getChunkPixels
            }

            Log.d("Connection", "Got canvas chunk $chunk.")

            when(chunk) {
                1 -> SessionSettings.instance.chunk1 = response
                2 -> SessionSettings.instance.chunk2 = response
                3 -> SessionSettings.instance.chunk3 = response
                4 -> SessionSettings.instance.chunk4 = response
            }

            doneLoadingChunkCount += 1
            downloadFinished()
        }
    }

    private fun sendDeviceId(server: Server) {
        val uniqueId = UUID.randomUUID().toString().uppercase()

        val requestParams = HashMap<String, String>()

        requestParams["uuid"] = uniqueId

        val paramsJson = JSONObject(requestParams as Map<String, String>)

        val request = object : JsonObjectRequest(
            Method.POST,
            server.serviceBaseUrl() + "api/v1/devices/register",
            paramsJson,
            { response ->
                Log.d("Connection", "Successfully sent device info.")

                server.uuid = uniqueId
                SessionSettings.instance.saveServers(activity)
                if (server.public) {
                    SessionSettings.instance.publicServerUniqueIds[server.id.toString()] = uniqueId
                    SessionSettings.instance.save(activity)
                }

                SessionSettings.instance.deviceId = response.getInt("id")
                SessionSettings.instance.dropsAmt = response.getInt("paint_qty")
                SessionSettings.instance.displayName = ""
                SessionSettings.instance.sentUniqueId = true

                doneSendingDeviceId = true
                downloadFinished()

                if (!server.isAdmin) {
                    canvasService?.logIp(uniqueId) { res ->
                        if (res == null) {
                            showConnectionErrorMessage(socket = false)
                            return@logIp
                        }
                        else if (!res.get("success").asBoolean) {
                            showConnectionErrorMessage(banError = true)
                            return@logIp
                        }

                        doneCheckingIp = true
                        downloadFinished()
                    }
                }
                else {
                    doneCheckingIp = true
                    downloadFinished()
                }
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
        Log.d("Connection", "Sending device info.")
    }

    private fun getDeviceInfo(server: Server) {
        val uniqueId = server.uuid

        val request = object: JsonObjectRequest(
            Method.GET,
            server.serviceBaseUrl() + "api/v1/devices/$uniqueId/info",
            null,
            { response ->
                Log.d("Connection", "Successfully got device info.")

                SessionSettings.instance.deviceId = response.getInt("id")
                SessionSettings.instance.dropsAmt = response.getInt("paint_qty")
                SessionSettings.instance.displayName = response.getString("name")
                SessionSettings.instance.xp = response.getInt("xp")

                StatTracker.instance.numPixelsPaintedWorld = response.getInt("wt")
                StatTracker.instance.numPixelsPaintedSingle = response.getInt("st")
                StatTracker.instance.totalPaintAccrued = response.getInt("tp")
                StatTracker.instance.numPixelOverwritesIn = response.getInt("oi")
                StatTracker.instance.numPixelOverwritesOut = response.getInt("oo")

                if (response.getInt("banned") != 0) {
                    showConnectionErrorMessage(banError = true)
                }
                else {
                    doneLoadingPaintQty = true
                    downloadFinished()
                }

                if (!server.isAdmin) {
                    CoroutineScope(Dispatchers.Main.immediate).launch {
                        canvasService?.logIp(uniqueId) { res ->
                            if (res == null) {
                                showConnectionErrorMessage(socket = false)
                                return@logIp
                            }
                            else if (!res.get("success").asBoolean) {
                                showConnectionErrorMessage(banError = true)
                                return@logIp
                            }

                            doneCheckingIp = true
                            downloadFinished()
                        }
                    }
                }
                else {
                    doneCheckingIp = true
                    downloadFinished()
                }
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
        Log.d("Connection", "Getting device info.")
    }

    private fun showConnectionErrorMessage(socket: Boolean = false, authError: Boolean = false, banError: Boolean = false, queue: Boolean = false) {
        if (aborted) return

        InteractiveCanvasSocket.instance.disconnect()

        if (!showingError) {
            showingError = true
            activity.runOnUiThread {
                requestQueue.cancelAll("download")

                val message = if (authError) {
                    "Access key has changed."
                }
                else if (banError) {
                    "You are banned."
                }
                else if (socket) {
                    "Socket error."
                }
                else if (queue) {
                    "Queue is not responding."
                }
                else {
                    "Server error."
                }

                AlertDialog.Builder(activity, R.style.AlertDialogTheme)
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
            abortOnPause = false
            dataLoadingCallback?.onDataLoaded(server)
        }
    }

    private fun updateNumLoaded() {
        if (activity == null) return

        activity.runOnUiThread {
            progressBar.progress = getNumLoaded() / 8f
        }
    }

    private fun loadingDone(): Boolean {
        Log.d("Check loading done", "doneLoadingPaintQty = $doneLoadingPaintQty, " +
                "doneSendingDeviceId = $doneSendingDeviceId, " +
                "doneLoadingCheckCount = $doneLoadingChunkCount, " +
                "doneConnectingQueue = $doneConnectingQueue, " +
                "doneConnectingSocket = $doneConnectingSocket, doneCheckingIp = $doneCheckingIp")
        return (doneLoadingPaintQty || doneSendingDeviceId) &&
                doneLoadingChunkCount == 4 &&
                doneConnectingQueue && doneConnectingSocket && doneCheckingIp
    }

    private fun getNumLoaded(): Int {
        var num = 0

        num += doneLoadingChunkCount

        if (doneLoadingPaintQty || doneSendingDeviceId) {
            num++
        }

        if (doneConnectingQueue) {
            num++
        }

        if (doneConnectingSocket) {
            num++
        }

        return num
    }

    // queue socket listener
    override fun onQueueConnect() {
        Log.d("Connection", "Connected to queue.")
        doneConnectingQueue = true
        updateNumLoaded()
    }

    override fun onQueueConnectError() {
        doneConnectingQueue = false
        showConnectionErrorMessage(queue = true)
    }

    override fun onCanvasSocketDownError() {
        doneConnectingQueue = false
        showConnectionErrorMessage(socket = true)
    }

    override fun onAddedToQueue(pos: Int) {
        Log.d("Connection", "Added to queue.")
        queuePos = pos
        updateQueuePos(true)
    }

    override fun onQueuePos(pos: Int) {
        Log.d("Connection", "Queue pos is $pos.")
        queuePos = pos
        updateQueuePos()
    }

    private fun updateQueuePos(start: Boolean = false) {}

    override fun onServiceReady() {
        Log.d("Connection", "Canvas socket ready, disconnecting from queue.")
        queuePos = 0
        updateQueuePos()

        QueueSocket.instance.socketListener = null
        QueueSocket.instance.socket?.disconnect()

        InteractiveCanvasSocket.instance.socketConnectCallback = this
        InteractiveCanvasSocket.instance.startSocket(server)
    }

    // canvas socket listener
    override fun onSocketConnect() {
        Log.d("Connection", "Connected to canvas socket.")
        Log.i("Canvas Socket", "Socket connected!")

        doneConnectingSocket = true
        updateNumLoaded()
        InteractiveCanvasSocket.instance.socketConnectCallback = socketListener

        getCanvas()

        socketListener.onSocketConnect()
    }

    override fun onSocketDisconnect(error: Boolean) {
        Log.i("Canvas Socket", "Socket disconnect.")

        doneConnectingSocket = false
        showConnectionErrorMessage(true)

        socketListener.onSocketDisconnect(error)
    }
}