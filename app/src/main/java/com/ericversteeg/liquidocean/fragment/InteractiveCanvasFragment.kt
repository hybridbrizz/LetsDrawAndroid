package com.ericversteeg.liquidocean.fragment

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.ericversteeg.liquidocean.R
import com.ericversteeg.liquidocean.helper.Animator
import com.ericversteeg.liquidocean.helper.PanelThemeConfig
import com.ericversteeg.liquidocean.helper.Utils
import com.ericversteeg.liquidocean.listener.*
import com.ericversteeg.liquidocean.model.InteractiveCanvas
import com.ericversteeg.liquidocean.model.InteractiveCanvasSocket
import com.ericversteeg.liquidocean.model.SessionSettings
import com.ericversteeg.liquidocean.view.ActionButtonView
import com.ericversteeg.liquidocean.view.PaintQuantityBar
import com.ericversteeg.liquidocean.view.PaintQuantityCircle
import com.plattysoft.leonids.ParticleSystem
import com.plattysoft.leonids.modifiers.AlphaModifier
import kotlinx.android.synthetic.main.fragment_art_export.*
import kotlinx.android.synthetic.main.fragment_interactive_canvas.*
import org.json.JSONArray
import top.defaults.colorpicker.ColorObserver
import java.util.*
import kotlin.math.ceil
import kotlin.math.floor


class InteractiveCanvasFragment : Fragment(), InteractiveCanvasDrawerCallback, PaintQtyListener,
    RecentColorsListener, SocketStatusCallback, PaintBarActionListener, PixelHistoryListener,
    InteractiveCanvasGestureListener, ArtExportListener, ArtExportFragmentListener, ObjectSelectionListener {

    var scaleFactor = 1f

    var initalColor = 0

    var topLeftParticleSystem: ParticleSystem? = null
    var topRightParticleSystem: ParticleSystem? = null
    var bottomLeftParticleSystem: ParticleSystem? = null
    var bottomRightParticleSystem: ParticleSystem? = null

    var world = false
    var realmId = 0

    var interactiveCanvasFragmentListener: InteractiveCanvasFragmentListener? = null

    var paintEventTimer: Timer? = null

    val firstInfoTapFixYOffset = 0
    var firstInfoTap = true

    var toolboxOpen = false

    val paint = Paint()
    val altPaint = Paint()
    val gridLinePaint = Paint()
    val gridLinePaintAlt = Paint()

    lateinit var panelThemeConfig: PanelThemeConfig

    var paintTextMode = 2

    val paintTextModeTime = 0
    val paintTextModeAmt = 1
    var paintTextModeHide = 2

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

        if (world) {
            InteractiveCanvasSocket.instance.socket.disconnect()
        }
    }

    override fun onResume() {
        super.onResume()

        if (world) {
            Timer().schedule(object : TimerTask() {
                override fun run() {
                    context?.apply {
                        var connected = Utils.isNetworkAvailable(this)
                        if (!connected) {
                            (context as Activity?)?.runOnUiThread {
                                showDisconnectedMessage(0)
                            }
                        } else {
                            sendApiStatusCheck()
                        }
                    }
                }
            }, 1000 * 60, 1000 * 60)

            getPaintTimerInfo()
        }

        surface_view.interactiveCanvas.drawCallbackListener = this

        if (world) {
            InteractiveCanvasSocket.instance.socket.apply {
                if (!connected()) {
                    connect()
                }
            }
        }
    }

    private fun sendApiStatusCheck() {
        val requestQueue = Volley.newRequestQueue(context)
        context?.apply {
            val request = JsonObjectRequest(
                Request.Method.GET,
                Utils.baseUrlApi + "/api/v1/status",
                null,
                { response ->
                    (context as Activity?)?.runOnUiThread {
                        sendSocketStatusCheck()
                    }
                },
                { error ->
                    (context as Activity?)?.runOnUiThread {
                        showDisconnectedMessage(1)
                    }
                })

            requestQueue.add(request)
        }
    }

    private fun sendSocketStatusCheck() {
        InteractiveCanvasSocket.instance.checkSocketStatus()
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
                        val timeUntil = response.getInt("s").toLong()

                        if (timeUntil < 0) {
                            paint_time_info.text = "???"
                        } else {
                            SessionSettings.instance.timeSync = timeUntil
                            setupPaintEventTimer()
                        }
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
        paintEventTimer?.schedule(object : TimerTask() {
            override fun run() {
                activity?.runOnUiThread {
                    if (System.currentTimeMillis() > SessionSettings.instance.nextPaintTime) {
                        SessionSettings.instance.nextPaintTime =
                            System.currentTimeMillis() + 300 * 1000
                    }

                    val m =
                        (SessionSettings.instance.nextPaintTime - System.currentTimeMillis()) / 1000 / 60
                    val s =
                        ((SessionSettings.instance.nextPaintTime - System.currentTimeMillis()) / 1000) % 60

                    if (m == 0L) {
                        try {
                            paint_time_info.text = s.toString()
                        } catch (ex: IllegalStateException) {

                        }
                    } else {
                        try {
                            paint_time_info.text = String.format("%02d:%02d", m, s)
                        } catch (ex: IllegalStateException) {

                        }
                    }
                }
            }
        }, 0, 1000)
    }

    // pixel history listener
    override fun showPixelHistoryFragmentPopover(screenPoint: Point) {
        fragmentManager?.apply {
            surface_view.interactiveCanvas.getPixelHistory(surface_view.interactiveCanvas.pixelIdForUnitPoint(
                surface_view.interactiveCanvas.lastSelectedUnitPoint
            ), object : PixelHistoryCallback {
                override fun onHistoryJsonResponse(historyJson: JSONArray) {
                    // set bottom-left of view to screenPoint

                    val dX = (screenPoint.x + Utils.dpToPx(context, 10)).toFloat()
                    val dY = (screenPoint.y - Utils.dpToPx(context, 120) - Utils.dpToPx(
                        context,
                        10
                    )).toFloat()

                    pixel_history_fragment_container.x = dX
                    pixel_history_fragment_container.y = dY

                    if (firstInfoTap) {
                        pixel_history_fragment_container.y -= Utils.dpToPx(
                            context,
                            firstInfoTapFixYOffset
                        )
                        firstInfoTap = false
                    }

                    view?.apply {
                        if (pixel_history_fragment_container.x < 0) {
                            pixel_history_fragment_container.x = Utils.dpToPx(context, 20).toFloat()
                        } else if (pixel_history_fragment_container.x + pixel_history_fragment_container.width > width) {
                            pixel_history_fragment_container.x =
                                width - pixel_history_fragment_container.width.toFloat() - Utils.dpToPx(
                                    context,
                                    20
                                ).toFloat()
                        }

                        if (pixel_history_fragment_container.y < 0) {
                            pixel_history_fragment_container.y = Utils.dpToPx(context, 20).toFloat()
                        } else if (pixel_history_fragment_container.y + pixel_history_fragment_container.height > height) {
                            pixel_history_fragment_container.y =
                                height - pixel_history_fragment_container.height.toFloat() - Utils.dpToPx(
                                    context,
                                    20
                                ).toFloat()
                        }

                        val fragment = PixelHistoryFragment()
                        fragment.pixelHistoryJson = historyJson

                        beginTransaction().replace(
                            R.id.pixel_history_fragment_container,
                            fragment
                        ).commit()

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
            export_fragment_container.setOnClickListener {

            }
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
                drawable.setStroke(
                    Utils.dpToPx(this, 2),
                    ActionButtonView.lightYellowSemiPaint.color
                ) // set stroke width and stroke color
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
        surface_view.interactiveCanvas.realmId = realmId
        surface_view.interactiveCanvas.world = world

        SessionSettings.instance.darkIcons = (surface_view.interactiveCanvas.getGridLineColor() == Color.BLACK)

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

            surface_view.interactiveCanvas.updateDeviceViewport(
                this,
                surface_view.interactiveCanvas.rows / 2F, surface_view.interactiveCanvas.cols / 2F
            )
        }

        panelThemeConfig = PanelThemeConfig.buildConfig(SessionSettings.instance.panelBackgroundResId)

        surface_view.pixelHistoryListener = this
        surface_view.gestureListener = this
        surface_view.objectSelectionListener = this

        surface_view.interactiveCanvas.recentColorsListener = this
        surface_view.interactiveCanvas.artExportListener = this

        if (SessionSettings.instance.showPaintBar) {
            surface_view.paintActionListener = paint_qty_bar
            SessionSettings.instance.paintQtyListeners.add(paint_qty_bar)

            paint_qty_circle.visibility = View.GONE
        }
        else if (SessionSettings.instance.showPaintCircle) {
            surface_view.paintActionListener = paint_qty_circle
            SessionSettings.instance.paintQtyListeners.add(paint_qty_circle)

            paint_qty_bar.visibility = View.GONE
        }
        else {
            paint_qty_bar.visibility = View.GONE
            paint_qty_circle.visibility = View.GONE
        }

        if (!world) {
            paint_qty_bar.visibility = View.GONE
            paint_qty_circle.visibility = View.GONE
        }

        InteractiveCanvasSocket.instance.socketStatusCallback = this

        // paint_qty_bar.world = world

        color_picker_view.setSelectorColor(Color.WHITE)

        pixel_history_fragment_container.x = 0F
        pixel_history_fragment_container.y = 0F

        paint_qty_bar.actionListener = this
        paint_qty_circle.actionListener = this

        back_button.actionBtnView = back_action
        back_action.type = ActionButtonView.Type.BACK

        paint_panel_button.actionBtnView = paint_panel_action_view
        paint_panel_action_view.type = ActionButtonView.Type.PAINT

        paint_yes_bottom_layer.static = true
        paint_yes_bottom_layer.type = ActionButtonView.Type.YES
        paint_yes_bottom_layer.colorMode = ActionButtonView.ColorMode.COLOR

        paint_yes.type = ActionButtonView.Type.YES
        paint_yes.colorMode = ActionButtonView.ColorMode.COLOR
        paint_yes.topLayer = true

        paint_no_bottom_layer.static = true
        paint_no_bottom_layer.type = ActionButtonView.Type.NO
        paint_no_bottom_layer.colorMode = ActionButtonView.ColorMode.COLOR

        paint_no.type = ActionButtonView.Type.NO
        paint_no.colorMode = ActionButtonView.ColorMode.COLOR
        paint_no.topLayer = true

        close_paint_panel_bottom_layer.static = true
        close_paint_panel_bottom_layer.type = ActionButtonView.Type.PAINT_CLOSE

        close_paint_panel.type = ActionButtonView.Type.PAINT_CLOSE
        close_paint_panel.topLayer = true

        if (panelThemeConfig.actionButtonColor == Color.BLACK) {
            close_paint_panel.colorMode = ActionButtonView.ColorMode.BLACK
        }
        else {
            close_paint_panel.colorMode = ActionButtonView.ColorMode.WHITE
        }

        paint_color_accept_image.type = ActionButtonView.Type.YES

        if (panelThemeConfig.actionButtonColor == Color.BLACK) {
            paint_color_accept_image_bottom_layer.colorMode = ActionButtonView.ColorMode.BLACK
            paint_color_accept_image.colorMode = ActionButtonView.ColorMode.BLACK
        }
        else {
            paint_color_accept_image_bottom_layer.colorMode = ActionButtonView.ColorMode.WHITE
            paint_color_accept_image.colorMode = ActionButtonView.ColorMode.WHITE
        }

        recent_colors.type = ActionButtonView.Type.DOT
        recent_colors_button.actionBtnView = recent_colors

        export_action.type = ActionButtonView.Type.EXPORT
        export_button.actionBtnView = export_action

        background_action.type = ActionButtonView.Type.CHANGE_BACKGROUND
        background_button.actionBtnView = background_action

        grid_lines_action.type = ActionButtonView.Type.GRID_LINES
        grid_lines_button.actionBtnView = grid_lines_action

        open_tools_action.type = ActionButtonView.Type.DOT
        open_tools_button.actionBtnView = open_tools_action

        setupRecentColors(surface_view.interactiveCanvas.recentColorsList.toTypedArray())

        color_picker_view.setEnabledAlpha(false)

        color_picker_view.subscribe(object : ColorObserver {
            override fun onColor(color: Int, fromUser: Boolean, shouldPropagate: Boolean) {
                paint_indicator_view_bottom_layer.setPaintColor(color)

                /* if (Utils.isColorDark(color)) {
                    paint_yes.setImageDrawable(resources.getDrawable(R.drawable.ic_done_white_border))
                }
                else {
                    paint_yes.setImageDrawable(resources.getDrawable(R.drawable.ic_done_white))
                    DrawableCompat.setTint(paint_yes.drawable, color)
                } */
            }
        })

        paint_indicator_view_bottom_layer.panelThemeConfig = panelThemeConfig
        paint_indicator_view.topLayer = true

        paint_color_accept_frame.actionBtnView = paint_color_accept_image

        paint_color_accept_image_bottom_layer.type = paint_color_accept_image.type
        if (panelThemeConfig.actionButtonColor == ActionButtonView.blackPaint.color) {
            paint_color_accept_image_bottom_layer.colorMode = ActionButtonView.ColorMode.BLACK
            paint_color_accept_image.colorMode = ActionButtonView.ColorMode.BLACK
        }

        paint_color_accept_image_bottom_layer.static = true

        paint_color_accept_image.topLayer = true
        paint_color_accept_image.touchStateListener = paint_indicator_view
        paint_color_accept_image.hideOnTouchEnd = true

        if (panelThemeConfig.inversePaintEventInfo) {
            paint_time_info_container.setBackgroundResource(R.drawable.timer_text_background_inverse)
            paint_time_info.setTextColor(ActionButtonView.darkGrayPaint.color)
            paint_amt_info.setTextColor(ActionButtonView.darkGrayPaint.color)
        }

        paint_amt_info.text = SessionSettings.instance.dropsAmt.toString()

        paint_panel.setOnClickListener {

        }

        paint_panel_button.setOnClickListener {
            paint_panel.visibility = View.VISIBLE
            paint_panel_button.visibility = View.GONE

            export_button.visibility = View.INVISIBLE
            background_button.visibility = View.INVISIBLE

            open_tools_button.visibility = View.INVISIBLE

            if (pixel_history_fragment_container.visibility == View.VISIBLE) {
                pixel_history_fragment_container.visibility = View.GONE
            }

            var startLoc = paint_panel.width.toFloat() * 0.99F
            if (SessionSettings.instance.rightHanded) {
                startLoc = -startLoc
            }

            paint_panel.animate().translationX(startLoc).setDuration(0).withEndAction {
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
                        drawable.setStroke(
                            Utils.dpToPx(this, 4),
                            SessionSettings.instance.canvasLockBorderColor
                        ) // set stroke width and stroke color
                    }

                    paint_warning_frame.visibility = View.VISIBLE
                    paint_warning_frame.alpha = 0F
                    paint_warning_frame.animate().alpha(1F).setDuration(50).start()
                }
            }.start()

            surface_view.startPainting()

            if (pixel_history_fragment_container.visibility == View.VISIBLE) {
                pixel_history_fragment_container.visibility = View.GONE
            }

            recent_colors_button.visibility = View.VISIBLE
            recent_colors_container.visibility = View.GONE

            back_button.visibility = View.GONE
        }

        paint_yes.setOnClickListener {
            surface_view.endPainting(true)

            paint_yes_container.visibility = View.GONE
            paint_no_container.visibility = View.GONE
            close_paint_panel_container.visibility = View.VISIBLE

            paint_yes.touchState = ActionButtonView.TouchState.INACTIVE
            paint_yes.invalidate()

            surface_view.startPainting()
        }

        paint_no.setOnClickListener {
            if (color_picker_frame.visibility == View.VISIBLE) {
                paint_indicator_view_bottom_layer.setPaintColor(initalColor)
                color_picker_frame.visibility = View.GONE

                if (SessionSettings.instance.canvasLockBorder) {
                    paint_warning_frame.visibility = View.VISIBLE
                }

                paint_yes.visibility = View.VISIBLE

                paint_color_accept_frame.visibility = View.GONE

                recent_colors_button.visibility = View.VISIBLE
                recent_colors_container.visibility = View.GONE

                surface_view.endPaintSelection()

                paint_no_bottom_layer.colorMode = ActionButtonView.ColorMode.COLOR
                paint_no.colorMode = ActionButtonView.ColorMode.COLOR

                if (surface_view.interactiveCanvas.restorePoints.size == 0) {
                    paint_yes_container.visibility = View.GONE
                    paint_no_container.visibility = View.GONE
                    close_paint_panel_container.visibility = View.VISIBLE
                }
                else {
                    paint_yes_container.visibility = View.VISIBLE
                    paint_no_container.visibility = View.VISIBLE
                    close_paint_panel_container.visibility = View.GONE
                }

                startParticleEmitters()
            }
            else {
                surface_view.endPainting(false)

                paint_yes_container.visibility = View.GONE
                paint_no_container.visibility = View.GONE
                close_paint_panel_container.visibility = View.VISIBLE

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
                grid_lines_button.visibility = View.VISIBLE
            }

            back_button.visibility = View.VISIBLE

            open_tools_button.visibility = View.VISIBLE

            stopEmittingParticles()
        }

        paint_qty_bar.panelThemeConfig = panelThemeConfig
        paint_qty_circle.panelThemeConfig = panelThemeConfig
        paint_indicator_view.panelThemeConfig = panelThemeConfig

        paint_indicator_view.setOnClickListener {
            // start color selection mode
            if (color_picker_frame.visibility != View.VISIBLE) {
                color_picker_frame.visibility = View.VISIBLE
                initalColor = SessionSettings.instance.paintColor
                color_picker_view.setInitialColor(initalColor)

                paint_warning_frame.visibility = View.GONE

                paint_color_accept_frame.visibility = View.VISIBLE

                paint_yes_container.visibility = View.GONE
                close_paint_panel_container.visibility = View.GONE
                paint_no_container.visibility = View.VISIBLE

                if (panelThemeConfig.actionButtonColor == Color.BLACK) {
                    paint_no_bottom_layer.colorMode = ActionButtonView.ColorMode.BLACK
                    paint_no.colorMode = ActionButtonView.ColorMode.BLACK
                }
                else {
                    paint_no_bottom_layer.colorMode = ActionButtonView.ColorMode.WHITE
                    paint_no.colorMode = ActionButtonView.ColorMode.WHITE
                }

                recent_colors_button.visibility = View.GONE
                recent_colors_container.visibility = View.GONE

                surface_view.startPaintSelection()

                stopEmittingParticles()
            }
        }

        paint_color_accept_frame.setOnClickListener {
            color_picker_frame.visibility = View.GONE

            if (SessionSettings.instance.canvasLockBorder) {
                paint_warning_frame.visibility = View.VISIBLE
            }

            paint_yes.visibility = View.VISIBLE

            paint_color_accept_frame.visibility = View.GONE

            surface_view.endPaintSelection()

            paint_no_bottom_layer.colorMode = ActionButtonView.ColorMode.COLOR
            paint_no.colorMode = ActionButtonView.ColorMode.COLOR

            recent_colors_container.visibility = View.GONE
            recent_colors_button.visibility = View.VISIBLE

            if (surface_view.interactiveCanvas.restorePoints.size == 0) {
                paint_yes_container.visibility = View.GONE
                paint_no_container.visibility = View.GONE
                close_paint_panel_container.visibility = View.VISIBLE
            }
            else {
                paint_yes_container.visibility = View.VISIBLE
                paint_no_container.visibility = View.VISIBLE
                close_paint_panel_container.visibility = View.GONE
            }

            startParticleEmitters()
        }

        // to stop click-through to the canvas behind
        color_picker_frame.setOnClickListener {
            
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

        // grid lines toggle button
        grid_lines_button.setOnClickListener {
            SessionSettings.instance.gridLineMode += 1

            if (SessionSettings.instance.gridLineMode > 1) {
                SessionSettings.instance.gridLineMode = 0
            }

            surface_view.interactiveCanvas.drawCallbackListener?.notifyRedraw()
        }

        // open tools button
        open_tools_button.setOnClickListener {
            if (!toolboxOpen) {
                export_button.visibility = View.VISIBLE
                background_button.visibility = View.VISIBLE
                grid_lines_button.visibility = View.VISIBLE

                Animator.animateMenuItems(
                    listOf(
                        listOf(export_button), listOf(background_button), listOf(
                            grid_lines_button
                        )
                    ), cascade = false, out = false, inverse = SessionSettings.instance.rightHanded
                )

                toolboxOpen = true
            }
            else {
                Animator.animateMenuItems(
                    listOf(
                        listOf(export_button), listOf(background_button), listOf(
                            grid_lines_button
                        )
                    ), cascade = false, out = true, inverse = SessionSettings.instance.rightHanded
                )

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

        Utils.setViewLayoutListener(view, object : Utils.ViewLayoutListener {
            override fun onViewLayout(view: View) {
                // tablet
                if (SessionSettings.instance.tablet) {
                    // color picker frame width
                    var layoutParams = ConstraintLayout.LayoutParams(
                        (view.width * 0.35).toInt(),
                        ConstraintLayout.LayoutParams.MATCH_PARENT
                    )

                    layoutParams.leftToLeft = ConstraintSet.PARENT_ID

                    color_picker_frame.layoutParams = layoutParams

                    // paint panel width
                    layoutParams = ConstraintLayout.LayoutParams(
                        ((250 / 1000F) * view.height).toInt(),
                        ConstraintLayout.LayoutParams.MATCH_PARENT
                    )
                    layoutParams.rightToRight = ConstraintSet.PARENT_ID

                    paint_panel.layoutParams = layoutParams

                    // paint indicator size
                    val frameWidth = ((250 / 1000F) * view.height).toInt()
                    val indicatorMargin = (frameWidth * 0.15).toInt()
                    val indicatorWidth = frameWidth - indicatorMargin

                    layoutParams = ConstraintLayout.LayoutParams(indicatorWidth, indicatorWidth)
                    layoutParams.topToTop = ConstraintSet.PARENT_ID
                    layoutParams.bottomToBottom = ConstraintSet.PARENT_ID
                    layoutParams.leftToLeft = ConstraintSet.PARENT_ID
                    layoutParams.rightToRight = ConstraintSet.PARENT_ID

                    paint_indicator_view_bottom_layer.layoutParams = layoutParams
                    paint_indicator_view.layoutParams = layoutParams

                    if (SessionSettings.instance.showPaintBar) {
                        // paint quantity bar size
                        layoutParams = ConstraintLayout.LayoutParams(
                            (paint_qty_bar.width * 1.25).toInt(),
                            (paint_qty_bar.height * 1.25).toInt()
                        )
                        layoutParams.topToTop = ConstraintSet.PARENT_ID
                        layoutParams.leftToLeft = ConstraintSet.PARENT_ID
                        layoutParams.rightToRight = ConstraintSet.PARENT_ID

                        layoutParams.topMargin = Utils.dpToPx(context, 15)

                        paint_qty_bar.layoutParams = layoutParams
                    }
                    else if (SessionSettings.instance.showPaintCircle) {
                        // paint quantity circle size
                        layoutParams = ConstraintLayout.LayoutParams(
                            (paint_qty_circle.width * 1.25).toInt(),
                            (paint_qty_circle.height * 1.25).toInt()
                        )
                        layoutParams.topToTop = ConstraintSet.PARENT_ID
                        layoutParams.leftToLeft = ConstraintSet.PARENT_ID
                        layoutParams.rightToRight = ConstraintSet.PARENT_ID

                        layoutParams.topMargin = Utils.dpToPx(context, 15)

                        paint_qty_circle.layoutParams = layoutParams
                    }
                }

                // paint text info placement
                if (SessionSettings.instance.showPaintBar) {
                    val layoutParams = ConstraintLayout.LayoutParams(paint_time_info_container.width, paint_time_info_container.height)
                    layoutParams.topToBottom = paint_qty_bar.id
                    layoutParams.leftToLeft = ConstraintSet.PARENT_ID
                    layoutParams.rightToRight = ConstraintSet.PARENT_ID

                    layoutParams.topMargin = Utils.dpToPx(context, 0)

                    paint_time_info_container.layoutParams = layoutParams
                }
                else if (SessionSettings.instance.showPaintCircle) {
                    paint_time_info_container.setBackgroundColor(Color.TRANSPARENT)
                }

                // background texture scaling
                context?.apply {
                    val backgroundDrawable = resources.getDrawable(SessionSettings.instance.panelBackgroundResId) as BitmapDrawable

                    if (SessionSettings.instance.tablet) {
                        val scale = view.height / backgroundDrawable.bitmap.height.toFloat()

                        val newWidth = (backgroundDrawable.bitmap.width * scale).toInt()
                        val newHeight = (backgroundDrawable.bitmap.height * scale).toInt()
                        val newBitmap = Bitmap.createScaledBitmap(backgroundDrawable.bitmap, newWidth,
                            newHeight, false)
                        val scaledBitmapDrawable = BitmapDrawable(resources, newBitmap)

                        var layerDrawable: LayerDrawable = ContextCompat.getDrawable(
                            this,
                            R.drawable.panel_texture_background
                        ) as LayerDrawable

                        scaledBitmapDrawable.gravity = Gravity.TOP or Gravity.LEFT
                        layerDrawable.addLayer(scaledBitmapDrawable)

                        paint_panel.setBackgroundDrawable(layerDrawable)
                    }
                    else {
                        paint_panel.setBackgroundDrawable(backgroundDrawable)
                    }
                }

                // right-handed
                if (SessionSettings.instance.rightHanded) {
                    // paint panel
                    var layoutParams = paint_panel.layoutParams as ConstraintLayout.LayoutParams
                    layoutParams.rightToRight = -1
                    layoutParams.leftToLeft = ConstraintSet.PARENT_ID

                    paint_panel.layoutParams = layoutParams

                    paint_panel.invalidate()

                    // canvas lock border
                    layoutParams = paint_warning_frame.layoutParams as ConstraintLayout.LayoutParams

                    layoutParams.leftToLeft = -1
                    layoutParams.rightToRight = ConstraintSet.PARENT_ID
                    layoutParams.rightToLeft = -1
                    layoutParams.leftToRight = paint_panel.id
                    paint_warning_frame.layoutParams = layoutParams

                    // close paint panel button
                    close_paint_panel_bottom_layer.rotation = 180F
                    close_paint_panel.rotation = 180F

                    // color picker
                    layoutParams = color_picker_frame.layoutParams as ConstraintLayout.LayoutParams

                    layoutParams.leftToLeft = -1
                    layoutParams.rightToRight = ConstraintSet.PARENT_ID
                    color_picker_frame.layoutParams = layoutParams

                    // paint meter bar
                    paint_qty_bar.rotation = 180F

                    // toolbox
                    layoutParams = open_tools_button.layoutParams as ConstraintLayout.LayoutParams

                    layoutParams.rightToRight = -1
                    layoutParams.leftToLeft = ConstraintSet.PARENT_ID
                    open_tools_button.layoutParams = layoutParams

                    var layoutParams3 = open_tools_action.layoutParams as FrameLayout.LayoutParams
                    layoutParams3.gravity = Gravity.LEFT or Gravity.BOTTOM
                    open_tools_action.layoutParams = layoutParams3

                    // toolbox buttons
                    val toolboxButtons = arrayOf(export_button, background_button, grid_lines_button)

                    for (button in toolboxButtons) {
                        layoutParams = button.layoutParams as ConstraintLayout.LayoutParams
                        layoutParams.rightToRight = -1
                        layoutParams.leftToLeft = ConstraintSet.PARENT_ID
                        button.layoutParams = layoutParams
                    }

                    // recent colors
                    layoutParams = recent_colors_button.layoutParams as ConstraintLayout.LayoutParams

                    layoutParams.leftToLeft = -1
                    layoutParams.rightToRight = ConstraintSet.PARENT_ID
                    recent_colors_button.layoutParams = layoutParams

                    layoutParams3 = recent_colors.layoutParams as FrameLayout.LayoutParams
                    layoutParams3.gravity = Gravity.RIGHT or Gravity.BOTTOM
                    recent_colors.layoutParams = layoutParams3

                    // recent colors container
                    layoutParams = recent_colors_container.layoutParams as ConstraintLayout.LayoutParams

                    layoutParams.leftToLeft = -1
                    layoutParams.rightToRight = ConstraintSet.PARENT_ID
                    recent_colors_container.layoutParams = layoutParams

                    //open_tools_button.layoutParams = layoutParams

                    // paint yes
                    /*var layoutParams2 = paint_yes_container.layoutParams as LinearLayout.LayoutParams
                    layoutParams2.rightMargin = 0
                    paint_yes_container.layoutParams = layoutParams2

                    // paint no
                    layoutParams2 = paint_no_container.layoutParams as LinearLayout.LayoutParams
                    layoutParams2.rightMargin = Utils.dpToPx(context, 30)
                    paint_no_container.layoutParams = layoutParams2

                    paint_action_button_container.removeViewAt(0)
                    paint_action_button_container.addView(paint_yes_container)*/
                }
            }
        })
    }

    private fun setupRecentColors(recentColors: Array<Int>?) {
        if (recentColors != null) {
            var i = 0
            for (v in recent_colors_container.children) {
                (v as ActionButtonView).type = ActionButtonView.Type.RECENT_COLOR

                if (i < recentColors.size) {
                    v.representingColor = recentColors[recentColors.size - 1 - i]
                    v.visibility = View.VISIBLE
                }
                else {
                    v.visibility = View.GONE
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
        val gridLineMode = SessionSettings.instance.gridLineMode
        if (gridLineMode == InteractiveCanvas.GRID_LINE_MODE_ON && surface_view.interactiveCanvas.ppu >= surface_view.interactiveCanvas.autoCloseGridLineThreshold) {

            val gridLineColor = surface_view.interactiveCanvas.getGridLineColor()

            gridLinePaint.strokeWidth = 1f
            gridLinePaint.color = gridLineColor

            gridLinePaintAlt.strokeWidth = 1f
            gridLinePaintAlt.color = gridLineColor

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

                    val inGrid = unitX >= 0 && unitX < interactiveCanvas.cols && unitY >= 0 && unitY < interactiveCanvas.rows

                    if (inGrid) {
                        val color = interactiveCanvas.arr[unitY][unitX]
                        // val alpha = 0xFF and (color shr 24)

                        // background
                        if (color == 0) {
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

                    // transparency
                    /* if (inGrid) {
                        val color = interactiveCanvas.arr[unitY][unitX]
                        val alpha = 0xFF and (color shr 24)

                        if (color != 0 && alpha < 255) {
                            altPaint.color = color
                            canvas.drawRect(rect, altPaint)
                        }
                    } */
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
            .setNegativeButton(
                android.R.string.no
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
        close_paint_panel_container.visibility = View.GONE
        paint_yes_container.visibility = View.VISIBLE
        paint_no_container.visibility = View.VISIBLE
    }

    override fun notifyPaintingEnded() {
        close_paint_panel_container.visibility = View.VISIBLE
        paint_yes_container.visibility = View.GONE
        paint_no_container.visibility = View.GONE
    }

    override fun notifyCloseRecentColors() {
        recent_colors_button.visibility = View.VISIBLE
        recent_colors_container.visibility = View.GONE
    }

    // paint qty listener
    override fun paintQtyChanged(qty: Int) {
        //drops_amt_text.text = qty.toString()
        activity?.runOnUiThread {
            paint_amt_info.text = qty.toString()
        }
    }

    override fun onNewRecentColors(colors: Array<Int>) {
        setupRecentColors(colors)
    }

    override fun onPaintBarDoubleTapped() {
        if (world) {
            paintTextMode += 1
            if (paintTextMode == 3) {
                paintTextMode = 0
            }

            if (paintTextMode == paintTextModeTime) {
                paint_time_info.visibility = View.VISIBLE
                paint_time_info_container.visibility = View.VISIBLE

                paint_amt_info.visibility = View.INVISIBLE
            }
            else if (paintTextMode == paintTextModeAmt) {
                paint_amt_info.visibility = View.VISIBLE
                paint_time_info_container.visibility = View.VISIBLE

                paint_time_info.visibility = View.INVISIBLE
            }
            else if (paintTextMode == paintTextModeHide) {
                paint_time_info.visibility = View.INVISIBLE
                paint_time_info_container.visibility = View.INVISIBLE
                paint_amt_info.visibility = View.INVISIBLE
            }
        }
    }

    private fun invalidateButtons() {
        back_action.invalidate()
        paint_panel_action_view.invalidate()
        export_action.invalidate()
        background_action.invalidate()
        grid_lines_action.invalidate()
        recent_colors.invalidate()
        open_tools_action.invalidate()
    }

    override fun onObjectSelectionBoundsChanged(startPoint: PointF, endPoint: PointF) {
        object_selection_view.visibility = View.VISIBLE

        if (SessionSettings.instance.backgroundColorsIndex == 1 || SessionSettings.instance.backgroundColorsIndex == 3) {
            object_selection_view.setBackgroundResource(R.drawable.object_selection_background_darkgray)
        }
        else {
            object_selection_view.setBackgroundResource(R.drawable.object_selection_background_white)
        }

        object_selection_view.layoutParams = ConstraintLayout.LayoutParams((endPoint.x - startPoint.x).toInt(), (endPoint.y - startPoint.y).toInt())

        object_selection_view.x = startPoint.x
        object_selection_view.y = startPoint.y
    }

    override fun onObjectSelectionEnded() {
        object_selection_view.visibility = View.GONE
    }
}