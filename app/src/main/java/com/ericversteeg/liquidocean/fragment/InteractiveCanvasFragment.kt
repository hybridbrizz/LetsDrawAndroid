package com.ericversteeg.liquidocean.fragment

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.ericversteeg.liquidocean.R
import com.ericversteeg.liquidocean.helper.Utils
import com.ericversteeg.liquidocean.listener.*
import com.ericversteeg.liquidocean.model.InteractiveCanvas
import com.ericversteeg.liquidocean.model.SessionSettings
import com.ericversteeg.liquidocean.view.ActionButtonView
import com.plattysoft.leonids.ParticleSystem
import com.plattysoft.leonids.modifiers.AlphaModifier
import kotlinx.android.synthetic.main.fragment_art_export.*
import kotlinx.android.synthetic.main.fragment_interactive_canvas.*
import okhttp3.internal.Util
import org.json.JSONArray
import top.defaults.colorpicker.ColorObserver
import java.lang.IllegalStateException
import java.util.*
import kotlin.math.ceil
import kotlin.math.floor


class InteractiveCanvasFragment : Fragment(), InteractiveCanvasDrawerCallback, PaintQtyListener,
    RecentColorsListener, SocketStatusCallback, PaintBarActionListener, PixelHistoryListener,
    InteractiveCanvasGestureListener, ArtExportListener, ArtExportFragmentListener {

    var scaleFactor = 1f

    var initalColor = 0

    var topLeftParticleSystem: ParticleSystem? = null
    var topRightParticleSystem: ParticleSystem? = null
    var bottomLeftParticleSystem: ParticleSystem? = null
    var bottomRightParticleSystem: ParticleSystem? = null

    var world = false

    var interactiveCanvasFragmentListener: InteractiveCanvasFragmentListener? = null

    var paintEventTimer: Timer? = null

    val firstInfoTapFixYOffset = 0
    var firstInfoTap = true

    var toolboxOpen = false

    val paint = Paint()
    val gridLinePaint = Paint()
    val gridLinePaintAlt = Paint()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_interactive_canvas, container, false)

        // setup views here

        return view
    }

    override fun onPause() {
        super.onPause()

        stopEmittingParticles()

        // unregister listeners
        SessionSettings.instance.paintQtyListeners.remove(this)

        context?.apply {
            surface_view.interactiveCanvas.saveUnits(this)
            surface_view.interactiveCanvas.drawCallbackListener = null

            SessionSettings.instance.saveLastPaintColor(this, world)
        }

        paintEventTimer?.cancel()
    }

    override fun onResume() {
        super.onResume()

        if (world) {
            Timer().schedule(object : TimerTask() {
                override fun run() {
                    context?.apply {
                        var connected = Utils.isNetworkAvailable(this)
                        if (!connected) {
                            (context as Activity).runOnUiThread {
                                showDisconnectedMessage(0)
                            }
                        } else {
                            sendApiStatusCheck()
                        }
                    }
                }
            }, 0, 1000 * 60)

            getPaintTimerInfo()
        }

        surface_view.interactiveCanvas.drawCallbackListener = this
    }

    private fun sendApiStatusCheck() {
        val requestQueue = Volley.newRequestQueue(context)
        context?.apply {
            val request = JsonObjectRequest(
                Request.Method.GET,
                Utils.baseUrlApi + "/api/v1/status",
                null,
                { response ->
                    (context as Activity).runOnUiThread {
                        sendSocketStatusCheck()
                    }
                },
                { error ->
                    (context as Activity).runOnUiThread {
                        showDisconnectedMessage(1)
                    }
                })

            requestQueue.add(request)
        }
    }

    private fun sendSocketStatusCheck() {
        surface_view.interactiveCanvas.checkSocketStatus()
    }

    // socket check callback
    override fun onSocketStatusError() {
        (context as Activity?)?.runOnUiThread {
            showDisconnectedMessage(2)
        }
    }

    private fun showDisconnectedMessage(type: Int) {
        AlertDialog.Builder(context)
            .setMessage("Lost connection to world server (code=$type)")
            // The dialog is automatically dismissed when a dialog button is clicked.
            .setPositiveButton(
                android.R.string.ok,
                object : DialogInterface.OnClickListener {
                    override fun onClick(dialog: DialogInterface?, id: Int) {
                        interactiveCanvasFragmentListener?.onInteractiveCanvasBack()
                        dialog?.dismiss()
                    }
                })
            .setOnDismissListener {
                interactiveCanvasFragmentListener?.onInteractiveCanvasBack()
            }
            .show()
    }

    private fun getPaintTimerInfo() {
        val requestQueue = Volley.newRequestQueue(context)
        context?.apply {
            val request = JsonObjectRequest(
                Request.Method.GET,
                Utils.baseUrlApi + "/api/v1/paint/time/sync",
                null,
                { response ->
                    (context as Activity).runOnUiThread {
                        SessionSettings.instance.timeSync = response.getInt("s").toLong()
                        setupPaintEventTimer()
                    }
                },
                { error ->
                    (context as Activity).runOnUiThread {

                    }
                })

            requestQueue.add(request)
        }
    }

    private fun setupPaintEventTimer() {
        paintEventTimer = Timer()
        paintEventTimer?.schedule(object: TimerTask() {
            override fun run() {
                activity?.runOnUiThread {
                    if (System.currentTimeMillis() > SessionSettings.instance.nextPaintTime) {
                        SessionSettings.instance.nextPaintTime = SessionSettings.instance.nextPaintTime + 300 * 1000
                    }

                    var timeUntil = SessionSettings.instance.nextPaintTime

                    var m = (timeUntil - System.currentTimeMillis()) / 1000 / 60
                    var s = ((timeUntil - System.currentTimeMillis()) / 1000) % 60

                    if (m == 0L) {
                        try {
                            paint_time_info.text = "${s}s"
                        }
                        catch (ex: IllegalStateException) {
                            
                        }
                    }
                    else {
                        try {
                            paint_time_info.text = "${m}m ${s}s"
                        }
                        catch (ex: IllegalStateException) {

                        }
                    }
                }
            }
        }, 0, 1000)
    }

    // pixel history listener
    override fun showPixelHistoryFragmentPopover(screenPoint: Point) {
        fragmentManager?.apply {
            surface_view.interactiveCanvas.getPixelHistory(surface_view.interactiveCanvas.pixelIdForUnitPoint(surface_view.interactiveCanvas.lastSelectedUnitPoint), object: PixelHistoryCallback {
                override fun onHistoryJsonResponse(historyJson: JSONArray) {
                    // set bottom-left of view to screenPoint

                    val dX = (screenPoint.x + Utils.dpToPx(context, 10)).toFloat()
                    val dY = (screenPoint.y - Utils.dpToPx(context, 120) - Utils.dpToPx(context, 10)).toFloat()

                    pixel_history_fragment_container.x = dX
                    pixel_history_fragment_container.y = dY

                    if (firstInfoTap) {
                        pixel_history_fragment_container.y -= Utils.dpToPx(context, firstInfoTapFixYOffset)
                        firstInfoTap = false
                    }

                    view?.apply {
                        if (pixel_history_fragment_container.x < 0) {
                            pixel_history_fragment_container.x = Utils.dpToPx(context, 20).toFloat()
                        }
                        else if (pixel_history_fragment_container.x + pixel_history_fragment_container.width > width) {
                            pixel_history_fragment_container.x = width - pixel_history_fragment_container.width.toFloat() - Utils.dpToPx(context, 20).toFloat()
                        }

                        if (pixel_history_fragment_container.y < 0) {
                            pixel_history_fragment_container.y = Utils.dpToPx(context, 20).toFloat()
                        }
                        else if (pixel_history_fragment_container.y + pixel_history_fragment_container.height > height) {
                            pixel_history_fragment_container.y = height - pixel_history_fragment_container.height.toFloat() - Utils.dpToPx(context, 20).toFloat()
                        }

                        val fragment = PixelHistoryFragment()
                        fragment.pixelHistoryJson = historyJson

                        beginTransaction().replace(R.id.pixel_history_fragment_container, fragment).commit()

                        pixel_history_fragment_container.visibility = View.VISIBLE
                    }
                }
            })
        }
    }

    override fun onInteractiveCanvasPan() {
        pixel_history_fragment_container.visibility = View.GONE
    }

    override fun onInteractiveCanvasScale() {
        pixel_history_fragment_container.visibility = View.GONE
    }

    override fun onArtExported(pixelPositions: List<InteractiveCanvas.RestorePoint>) {
        showExportBorder(false)

        val fragment = ArtExportFragment()
        fragment.art = pixelPositions
        fragment.listener = this

        fragmentManager?.apply {
            // export_button.background = ResourcesCompat.getDrawable(resources, R.drawable.ic_share, null)

            beginTransaction().replace(R.id.export_fragment_container, fragment).addToBackStack("Export").commit()

            export_fragment_container.visibility = View.VISIBLE
        }
    }

    override fun onArtExportBack() {
        fragmentManager?.popBackStack()

        export_fragment_container.visibility = View.GONE
        surface_view.endExport()

        export_action.touchState = ActionButtonView.TouchState.INACTIVE
    }

    private fun showExportBorder(show: Boolean) {
        if (show) {
            context?.apply {
                val drawable: GradientDrawable = export_border_view.background as GradientDrawable
                drawable.setStroke(Utils.dpToPx(this, 2), ActionButtonView.lightYellowSemiPaint.color) // set stroke width and stroke color
            }
            export_border_view.visibility = View.VISIBLE
        }
        else {
            export_border_view.visibility = View.GONE
        }
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // must call before darkIcons
        surface_view.interactiveCanvas.world = world

        SessionSettings.instance.darkIcons = (surface_view.interactiveCanvas.getGridLineColor() == Color.BLACK)

        SessionSettings.instance.paintQtyListeners.add(paint_qty_bar)
        SessionSettings.instance.paintQtyListeners.add(this)

        if (SessionSettings.instance.backgroundColorsIndex == 1 || SessionSettings.instance.backgroundColorsIndex == 3) {
            SessionSettings.instance.darkIcons = true

            invalidateButtons()
        }

        context?.apply {
            if (world) {
                SessionSettings.instance.paintColor = SessionSettings.instance.getSharedPrefs(this).getInt(
                    "last_world_paint_color",
                    surface_view.interactiveCanvas.getGridLineColor()
                )
            }
            else {
                SessionSettings.instance.paintColor = SessionSettings.instance.getSharedPrefs(this).getInt(
                    "last_single_paint_color",
                    surface_view.interactiveCanvas.getGridLineColor()
                )
            }
        }

        surface_view.pixelHistoryListener = this
        surface_view.gestureListener = this

        surface_view.interactiveCanvas.recentColorsListener = this
        surface_view.interactiveCanvas.socketStatusCallback = this
        surface_view.interactiveCanvas.artExportListener = this
        surface_view.paintActionListener = paint_qty_bar

        paint_qty_bar.world = world

        color_picker_view.setSelectorColor(Color.WHITE)

        context?.apply {
            paint_panel.setBackgroundDrawable(resources.getDrawable(SessionSettings.instance.panelBackgroundResId))
        }

        pixel_history_fragment_container.x = 0F
        pixel_history_fragment_container.y = 0F

        paint_qty_bar.actionListener = this

        back_button.actionBtnView = back_action
        back_action.type = ActionButtonView.Type.BACK

        paint_panel_button.actionBtnView = paint_panel_action_view
        paint_panel_action_view.type = ActionButtonView.Type.PAINT

        paint_yes.type = ActionButtonView.Type.YES
        paint_yes.colorMode = ActionButtonView.ColorMode.COLOR

        paint_no.type = ActionButtonView.Type.NO
        paint_no.colorMode = ActionButtonView.ColorMode.COLOR

        close_paint_panel.type = ActionButtonView.Type.PAINT_CLOSE

        paint_color_accept_image.type = ActionButtonView.Type.YES
        paint_color_accept_image.colorMode = ActionButtonView.ColorMode.NONE

        recent_colors.type = ActionButtonView.Type.RECENT_COLORS
        recent_colors_button.actionBtnView = recent_colors

        export_action.type = ActionButtonView.Type.EXPORT
        export_button.actionBtnView = export_action

        background_action.type = ActionButtonView.Type.CHANGE_BACKGROUND
        background_button.actionBtnView = background_action

        open_tools_action.type = ActionButtonView.Type.NONE
        open_tools_button.actionBtnView = open_tools_action

        setupRecentColors(surface_view.interactiveCanvas.recentColorsList.toTypedArray())

        color_picker_view.subscribe(object : ColorObserver {
            override fun onColor(color: Int, fromUser: Boolean, shouldPropagate: Boolean) {
                paint_indicator_view.setPaintColor(color)

                /* if (Utils.isColorDark(color)) {
                    paint_yes.setImageDrawable(resources.getDrawable(R.drawable.ic_done_white_border))
                }
                else {
                    paint_yes.setImageDrawable(resources.getDrawable(R.drawable.ic_done_white))
                    DrawableCompat.setTint(paint_yes.drawable, color)
                } */
            }
        })

        paint_panel.setOnClickListener {

        }

        paint_panel_button.setOnClickListener {
            paint_panel.visibility = View.VISIBLE
            paint_panel_button.visibility = View.GONE

            export_button.visibility = View.INVISIBLE
            background_button.visibility = View.INVISIBLE

            paint_panel.animate().translationX(paint_panel.width.toFloat() * 0.99F).setDuration(0).withEndAction {
                paint_panel.animate().translationX(0F).setDuration(50).setInterpolator(
                    AccelerateDecelerateInterpolator()
                ).withEndAction {

                    startParticleEmitters()

                    Log.i("ICF", "paint panel width is ${paint_panel.width}")
                    Log.i("ICF", "paint panel height is ${paint_panel.height}")

                }.start()

                if (SessionSettings.instance.canvasLockBorder) {
                    context?.apply {
                        val drawable: GradientDrawable = paint_warning_frame.background as GradientDrawable
                        drawable.setStroke(Utils.dpToPx(this, 4), SessionSettings.instance.canvasLockBorderColor) // set stroke width and stroke color
                    }

                    paint_warning_frame.visibility = View.VISIBLE
                    paint_warning_frame.alpha = 0F
                    paint_warning_frame.animate().alpha(1F).setDuration(50).start()
                }
            }.start()

            surface_view.startPainting()

            recent_colors_button.visibility = View.VISIBLE
            recent_colors_container.visibility = View.GONE

            back_button.visibility = View.GONE
        }

        paint_yes.setOnClickListener {
            surface_view.endPainting(true)

            paint_yes.visibility = View.GONE
            paint_no.visibility = View.GONE
            close_paint_panel.visibility = View.VISIBLE

            surface_view.startPainting()
        }

        paint_no.setOnClickListener {
            if (color_picker_frame.visibility == View.VISIBLE) {
                paint_indicator_view.setPaintColor(initalColor)
                color_picker_frame.visibility = View.GONE

                if (SessionSettings.instance.canvasLockBorder) {
                    paint_warning_frame.visibility = View.VISIBLE
                }

                paint_yes.visibility = View.VISIBLE

                paint_color_accept_image.visibility = View.GONE

                recent_colors_button.visibility = View.VISIBLE
                recent_colors_container.visibility = View.GONE

                surface_view.endPaintSelection()

                paint_no.colorMode = ActionButtonView.ColorMode.COLOR

                if (surface_view.interactiveCanvas.restorePoints.size == 0) {
                    paint_yes.visibility = View.GONE
                    paint_no.visibility = View.GONE
                    close_paint_panel.visibility = View.VISIBLE
                }
                else {
                    paint_yes.visibility = View.VISIBLE
                    paint_no.visibility = View.VISIBLE
                    close_paint_panel.visibility = View.GONE
                }

                startParticleEmitters()
            }
            else {
                surface_view.endPainting(false)

                paint_yes.visibility = View.GONE
                paint_no.visibility = View.GONE
                close_paint_panel.visibility = View.VISIBLE

                recent_colors_button.visibility = View.VISIBLE
                recent_colors_container.visibility = View.GONE

                surface_view.startPainting()
            }
        }

        close_paint_panel.setOnClickListener {
            surface_view.endPainting(false)

            paint_panel.visibility = View.GONE
            paint_warning_frame.visibility = View.GONE

            paint_panel_button.visibility = View.VISIBLE

            recent_colors_button.visibility = View.GONE
            recent_colors_container.visibility = View.GONE

            if (toolboxOpen) {
                export_button.visibility = View.VISIBLE
                background_button.visibility = View.VISIBLE
            }

            back_button.visibility = View.VISIBLE

            stopEmittingParticles()
        }

        paint_indicator_view.setOnClickListener {
            // accept selected color
            if (color_picker_frame.visibility == View.VISIBLE) {
                color_picker_frame.visibility = View.GONE

                if (SessionSettings.instance.canvasLockBorder) {
                    paint_warning_frame.visibility = View.VISIBLE
                }

                paint_yes.visibility = View.VISIBLE

                paint_color_accept_image.visibility = View.GONE

                surface_view.endPaintSelection()

                paint_no.colorMode = ActionButtonView.ColorMode.COLOR

                recent_colors_container.visibility = View.GONE
                recent_colors_button.visibility = View.VISIBLE

                if (surface_view.interactiveCanvas.restorePoints.size == 0) {
                    paint_yes.visibility = View.GONE
                    paint_no.visibility = View.GONE
                    close_paint_panel.visibility = View.VISIBLE
                }
                else {
                    paint_yes.visibility = View.VISIBLE
                    paint_no.visibility = View.VISIBLE
                    close_paint_panel.visibility = View.GONE
                }

                startParticleEmitters()
            }
            // start color selection mode
            else {
                color_picker_frame.visibility = View.VISIBLE
                initalColor = SessionSettings.instance.paintColor
                color_picker_view.setInitialColor(initalColor)

                paint_warning_frame.visibility = View.GONE

                paint_color_accept_image.visibility = View.VISIBLE

                paint_yes.visibility = View.GONE
                close_paint_panel.visibility = View.GONE
                paint_no.visibility = View.VISIBLE

                paint_no.colorMode = ActionButtonView.ColorMode.NONE

                recent_colors_button.visibility = View.GONE
                recent_colors_container.visibility = View.GONE

                surface_view.startPaintSelection()

                stopEmittingParticles()
            }
        }

        // recent colors
        recent_colors_button.setOnClickListener {
            recent_colors_container.visibility = View.VISIBLE
            recent_colors_button.visibility = View.GONE
        }

        // back button
        back_button.setOnClickListener {
            if (surface_view.isExporting()) {
                export_fragment_container.visibility = View.INVISIBLE
                surface_view.endExport()

                showExportBorder(false)
                export_action.touchState = ActionButtonView.TouchState.INACTIVE

                // export_button.background = ResourcesCompat.getDrawable(resources, R.drawable.ic_share, null)
            }
            else {
                if (SessionSettings.instance.promptToExit) {
                    showExitPrompt()
                }
                else {
                    interactiveCanvasFragmentListener?.onInteractiveCanvasBack()
                }
            }
        }

        // export button
        export_button.setOnClickListener {
            surface_view.startExport()
            export_action.touchState = ActionButtonView.TouchState.ACTIVE

            showExportBorder(true)
        }

        // background button
        background_button.setOnClickListener {
            if (SessionSettings.instance.backgroundColorsIndex == surface_view.interactiveCanvas.numBackgrounds - 1) {
                SessionSettings.instance.backgroundColorsIndex = 0
            }
            else {
                SessionSettings.instance.backgroundColorsIndex += 1
            }

            SessionSettings.instance.darkIcons = (SessionSettings.instance.backgroundColorsIndex == 1 || SessionSettings.instance.backgroundColorsIndex == 3)

            invalidateButtons()

            surface_view.interactiveCanvas.drawCallbackListener?.notifyRedraw()
        }

        // open tools button
        open_tools_button.setOnClickListener {
            if (!toolboxOpen) {
                export_button.visibility = View.VISIBLE
                background_button.visibility = View.VISIBLE

                toolboxOpen = true
            }
            else {
                export_button.visibility = View.INVISIBLE
                background_button.visibility = View.INVISIBLE

                toolboxOpen = false
            }
        }

        context?.apply {
            // paint_panel.layoutParams = ConstraintLayout.LayoutParams(Utils.dpToPx(this, 200), ConstraintLayout.LayoutParams.MATCH_PARENT)
        }

        surface_view.interactiveCanvas.drawCallbackListener = this

        val holder = surface_view.holder
        holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder?) {
                if (holder != null) {
                    drawInteractiveCanvas(holder)
                }
            }

            override fun surfaceDestroyed(p0: SurfaceHolder?) {

            }

            override fun surfaceChanged(p0: SurfaceHolder?, p1: Int, p2: Int, p3: Int) {

            }
        })

        // gesture recognizer
        val scaleListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                scaleFactor *= detector.scaleFactor

                // Don't let the object get too small or too large.
                scaleFactor = Math.max(0.1f, Math.min(scaleFactor, 5.0f))

                return true
            }
        }
    }

    private fun setupRecentColors(recentColors: Array<Int>?) {
        if (recentColors != null) {
            var i = 0
            for (v in recent_colors_container.children) {
                (v as ActionButtonView).type = ActionButtonView.Type.RECENT_COLOR

                if (i < recentColors.size) {
                    v.representingColor = recentColors[recentColors.size - 1 - i]
                }

                v.setOnClickListener {
                    v.representingColor?.apply {
                        SessionSettings.instance.paintColor = this
                        notifyPaintColorUpdate(SessionSettings.instance.paintColor)

                        recent_colors_container.visibility = View.GONE
                        recent_colors_button.visibility = View.VISIBLE
                    }
                }

                i++
            }
        }
        else {
            for (v in recent_colors_container.children) {
                (v as ActionButtonView).type = ActionButtonView.Type.RECENT_COLOR
            }
        }
    }

    fun drawInteractiveCanvas(holder: SurfaceHolder) {
        paint.color = Color.parseColor("#FFFFFFFF")

        val canvas = holder.lockCanvas()

        val deviceViewport = surface_view.interactiveCanvas.deviceViewport!!
        val ppu = surface_view.interactiveCanvas.ppu

        canvas.drawARGB(255, 0, 0, 0)

        drawUnits(canvas, deviceViewport, ppu)
        drawGridLines(canvas, deviceViewport, ppu)

        holder.unlockCanvasAndPost(canvas)
    }

    private fun drawGridLines(canvas: Canvas, deviceViewport: RectF, ppu: Int) {
        if (surface_view.interactiveCanvas.ppu >= surface_view.interactiveCanvas.gridLineThreshold) {

            gridLinePaint.strokeWidth = 1f
            gridLinePaint.color = Color.WHITE

            gridLinePaintAlt.strokeWidth = 1f
            gridLinePaintAlt.color = Color.WHITE

            if (!world) {
                gridLinePaint.color = surface_view.interactiveCanvas.getGridLineColor()
                gridLinePaintAlt.color = surface_view.interactiveCanvas.getGridLineColor()
            }

            val unitsWide = canvas.width / surface_view.interactiveCanvas.ppu
            val unitsTall = canvas.height / surface_view.interactiveCanvas.ppu

            val gridXOffsetPx = (ceil(deviceViewport.left) - deviceViewport.left) * ppu
            val gridYOffsetPx = (ceil(deviceViewport.top) - deviceViewport.top) * ppu

            for (y in 0..unitsTall) {
                if (y % 2 == 0) {
                    canvas.drawLine(
                        0F,
                        y * ppu.toFloat() + gridYOffsetPx,
                        canvas.width.toFloat(),
                        y * ppu.toFloat() + gridYOffsetPx,
                        gridLinePaint
                    )
                }
                else {
                    canvas.drawLine(
                        0F,
                        y * ppu.toFloat() + gridYOffsetPx,
                        canvas.width.toFloat(),
                        y * ppu.toFloat() + gridYOffsetPx,
                        gridLinePaintAlt
                    )
                }
            }

            for (x in 0..unitsWide) {
                if (x % 2 == 0) {
                    canvas.drawLine(
                        x * ppu.toFloat() + gridXOffsetPx,
                        0F,
                        x * ppu.toFloat() + gridXOffsetPx,
                        canvas.height.toFloat(),
                        gridLinePaint
                    )
                }
                else {
                    canvas.drawLine(
                        x * ppu.toFloat() + gridXOffsetPx,
                        0F,
                        x * ppu.toFloat() + gridXOffsetPx,
                        canvas.height.toFloat(),
                        gridLinePaintAlt
                    )
                }
            }
        }

    }

    private fun drawUnits(canvas: Canvas, deviceViewport: RectF, ppu: Int) {
        val interactiveCanvas = surface_view.interactiveCanvas

        interactiveCanvas.deviceViewport?.apply {
            val startUnitIndexX = floor(left).toInt()
            val endUnitIndexX = ceil(right).toInt()
            val startUnitIndexY = floor(top).toInt()
            val endUnitIndexY = ceil(bottom).toInt()

            // val unitsWide = canvas.width / surface_view.interactiveCanvas.ppu

            val rangeX = endUnitIndexX - startUnitIndexX
            val rangeY = endUnitIndexY - startUnitIndexY

            paint.color = Color.BLACK

            val backgroundColors = interactiveCanvas.getBackgroundColors(SessionSettings.instance.backgroundColorsIndex)

            for (x in 0..rangeX) {
                for (y in 0..rangeY) {
                    val unitX = x + startUnitIndexX
                    val unitY = y + startUnitIndexY

                    if (unitX >= 0 && unitX < interactiveCanvas.cols && unitY >= 0 && unitY < interactiveCanvas.rows) {
                        // background
                        if (interactiveCanvas.arr[unitY][unitX] == 0) {
                            if ((unitX + unitY) % 2 == 0) {
                                paint.color = backgroundColors[0]
                            }
                            else {
                                paint.color = backgroundColors[1]
                            }
                        }
                        else {
                            paint.color = interactiveCanvas.arr[unitY][unitX]
                        }
                    }
                    else {
                        paint.color = Color.BLACK
                    }
                    val rect = interactiveCanvas.getScreenSpaceForUnit(
                        x + startUnitIndexX,
                        y + startUnitIndexY
                    )

                    canvas.drawRect(rect, paint)
                }
            }
        }
    }

    private fun startParticleEmitters() {
        if (SessionSettings.instance.emittersEnabled) {
            topLeftParticleSystem = ParticleSystem(activity, 80, R.drawable.particle_semi, 1000)
            topLeftParticleSystem?.setSpeedModuleAndAngleRange(0f, 0.1f, 345, 45)
            topLeftParticleSystem?.setRotationSpeed(144f)
            topLeftParticleSystem?.setAcceleration(0.00005f, 90)
            topLeftParticleSystem?.addModifier(AlphaModifier(0, 255, 0, 1000))
            topLeftParticleSystem?.emit(top_left_anchor, 16)

            topRightParticleSystem = ParticleSystem(activity, 80, R.drawable.particle_semi, 1000)
            topRightParticleSystem?.setSpeedModuleAndAngleRange(0f, 0.1f, 135, 195)
            topRightParticleSystem?.setRotationSpeed(144f)
            topRightParticleSystem?.setAcceleration(0.00005f, 90)
            topRightParticleSystem?.addModifier(AlphaModifier(0, 255, 0, 1000))
            topRightParticleSystem?.emit(top_right_anchor, 16)

            bottomLeftParticleSystem = ParticleSystem(activity, 80, R.drawable.particle_semi, 1000)
            bottomLeftParticleSystem?.setSpeedModuleAndAngleRange(0f, 0.1f, 315, 0)
            bottomLeftParticleSystem?.setRotationSpeed(144f)
            bottomLeftParticleSystem?.setAcceleration(0.00005f, 90)
            bottomLeftParticleSystem?.addModifier(AlphaModifier(0, 255, 0, 1000))
            bottomLeftParticleSystem?.emit(bottom_left_anchor, 16)

            bottomRightParticleSystem = ParticleSystem(activity, 80, R.drawable.particle_semi, 1000)
            bottomRightParticleSystem?.setSpeedModuleAndAngleRange(0f, 0.1f, 180, 225)
            bottomRightParticleSystem?.setRotationSpeed(144f)
            bottomRightParticleSystem?.setAcceleration(0.00005f, 90)
            bottomRightParticleSystem?.addModifier(AlphaModifier(0, 255, 0, 1000))
            bottomRightParticleSystem?.emit(bottom_right_anchor, 16)
        }
    }

    private fun stopEmittingParticles() {
        if (SessionSettings.instance.emittersEnabled) {
            topLeftParticleSystem?.stopEmitting()
            topRightParticleSystem?.stopEmitting()
            bottomLeftParticleSystem?.stopEmitting()
            bottomRightParticleSystem?.stopEmitting()
        }
    }

    private fun showExitPrompt() {
        AlertDialog.Builder(context)
            .setMessage(resources.getString(R.string.alert_message_exit_canvas))
            .setPositiveButton(
                android.R.string.yes
            ) { dialog, _ ->
                interactiveCanvasFragmentListener?.onInteractiveCanvasBack()
                dialog?.dismiss()
            }
            .setNegativeButton(android.R.string.no
            ) { dialog, _ -> dialog?.dismiss() }
            .show()
    }

    // interactive canvas drawer callback
    override fun notifyRedraw() {
        drawInteractiveCanvas(surface_view.holder)
    }

    override fun notifyPaintColorUpdate(color: Int) {
        color_picker_view.setInitialColor(color)
        paint_indicator_view.setPaintColor(color)
    }

    override fun notifyPaintingStarted() {
        close_paint_panel.visibility = View.GONE
        paint_yes.visibility = View.VISIBLE
        paint_no.visibility = View.VISIBLE
    }

    override fun notifyPaintingEnded() {
        close_paint_panel.visibility = View.VISIBLE
        paint_yes.visibility = View.GONE
        paint_no.visibility = View.GONE
    }

    override fun notifyCloseRecentColors() {
        recent_colors_button.visibility = View.VISIBLE
        recent_colors_container.visibility = View.GONE
    }

    // paint qty listener
    override fun paintQtyChanged(qty: Int) {
        drops_amt_text.text = qty.toString()
    }

    override fun onNewRecentColors(colors: Array<Int>) {
        setupRecentColors(colors)
    }

    override fun onPaintBarDoubleTapped() {
        if (paint_time_info.visibility == View.VISIBLE) {
            paint_time_info.visibility = View.INVISIBLE
        }
        else {
            paint_time_info.visibility = View.VISIBLE
        }
    }

    fun invalidateButtons() {
        back_action.invalidate()
        paint_panel_action_view.invalidate()
        export_action.invalidate()
        background_action.invalidate()
    }
}