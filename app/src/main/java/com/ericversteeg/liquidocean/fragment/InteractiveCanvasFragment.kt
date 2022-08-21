package com.ericversteeg.liquidocean.fragment

import android.app.Activity
import android.app.AlertDialog
import android.content.*
import android.content.res.Configuration
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.fragment.app.Fragment
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.bumptech.glide.signature.ObjectKey
import com.ericversteeg.liquidocean.activity.InteractiveCanvasActivity
import com.ericversteeg.liquidocean.R
import com.ericversteeg.liquidocean.colorpicker.HSBPalette
import com.ericversteeg.liquidocean.helper.Animator
import com.ericversteeg.liquidocean.helper.PanelThemeConfig
import com.ericversteeg.liquidocean.helper.Utils
import com.ericversteeg.liquidocean.listener.*
import com.ericversteeg.liquidocean.model.*
import com.ericversteeg.liquidocean.service.CanvasService
import com.ericversteeg.liquidocean.view.ActionButtonView
import com.ericversteeg.liquidocean.view.ButtonFrame
import com.ericversteeg.liquidocean.view.PaintColorIndicator
import com.ericversteeg.liquidocean.view.RecentColorView
import com.google.android.material.snackbar.Snackbar
import com.plattysoft.leonids.ParticleSystem
import com.plattysoft.leonids.modifiers.AlphaModifier
import io.reactivex.rxjava3.core.Observable
import kotlinx.android.synthetic.main.fragment_art_export.*
import kotlinx.android.synthetic.main.fragment_interactive_canvas.*
import kotlinx.android.synthetic.main.fragment_interactive_canvas.menu_container
import kotlinx.android.synthetic.main.fragment_interactive_canvas.pixel_history_fragment_container
import kotlinx.android.synthetic.main.fragment_interactive_canvas.surface_view
import kotlinx.android.synthetic.main.fragment_interactive_canvas.view.*
import kotlinx.android.synthetic.main.fragment_loading_screen.*
import kotlinx.android.synthetic.main.palette_adapter_view.*
import org.json.JSONArray
import top.defaults.colorpicker.ColorObserver
import java.lang.Exception
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.max


class InteractiveCanvasFragment : Fragment(), InteractiveCanvasListener, PaintQtyListener,
    RecentColorsListener, PaintBarActionListener, PixelHistoryListener,
    InteractiveCanvasGestureListener, ArtExportListener, ArtExportFragmentListener, ObjectSelectionListener,
    PalettesFragmentListener, DrawFrameConfigFragmentListener, CanvasEdgeTouchListener, DeviceCanvasViewportResetListener,
    SelectedObjectMoveView, SelectedObjectView, MenuCardListener, SocketConnectCallback {

    var initalColor = 0

    var topLeftParticleSystem: ParticleSystem? = null
    var topRightParticleSystem: ParticleSystem? = null
    var bottomLeftParticleSystem: ParticleSystem? = null
    var bottomRightParticleSystem: ParticleSystem? = null

    lateinit var server: Server
    var world = false
    var realmId = 0

    var interactiveCanvasFragmentListener: InteractiveCanvasFragmentListener? = null

    var paintEventTimer: Timer? = null

    val firstInfoTapFixYOffset = 0
    var firstInfoTap = true

    var toolboxOpen = false

    lateinit var panelThemeConfig: PanelThemeConfig

    var paintTextMode = -1

    val paintTextModeTime = 0
    val paintTextModeAmt = 1
    var paintTextModeHide = -1

    var animatingTools = false

    var palettesFragment: PalettesFragment? = null

    var recentlyRemovedColor = 0
    var recentlyRemovedColorIndex = 0

    var menuFragment: MenuFragment? = null

    var terminalFragment: TerminalFragment? = null

    lateinit var visibleActionViews: Array<ButtonFrame>

    lateinit var canvasService: CanvasService

    var paused = false
    var pauseTime = 0L
    private val maxBgTime = 60 * 60

    private var lastCanvasSummaryImageTime = 0L

    private var wifiStateReceiver: BroadcastReceiver? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_interactive_canvas, container, false)

        // setup views here

        return view
    }

    override fun notifyPixelsReady() {
        requireActivity().runOnUiThread {
            paint_panel_button.visibility = View.VISIBLE
            open_tools_button.visibility = View.VISIBLE
            recent_colors_button.visibility = View.VISIBLE
            menu_button.visibility = View.VISIBLE

            togglePaintPanel(SessionSettings.instance.paintPanelOpen)
            toggleTools(SessionSettings.instance.toolboxOpen)

            progress_circular.visibility = View.GONE
        }
    }

    // setup views
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (savedInstanceState != null) {
            //SessionSettings.instance.load(requireContext())
            (requireActivity() as InteractiveCanvasActivity).showMenuFragment()
            return
        }

        SessionSettings.instance.canvasOpen = true

        InteractiveCanvasSocket.instance.socketConnectCallback = this

        context?.apply {
            SessionSettings.instance.tablet = Utils.isTablet(this)
        }

        server = SessionSettings.instance.lastVisitedServer!!

        SessionSettings.instance.addPaintInterval = server.pixelInterval / 60
        canvasService = CanvasService(server)

        // must call before darkIcons
        if (surface_view == null) {
            (requireActivity() as InteractiveCanvasActivity).showMenuFragment()
            return
        }

        surface_view.interactiveCanvas.server = server
        surface_view.interactiveCanvas.realmId = realmId
        surface_view.interactiveCanvas.world = world

        SessionSettings.instance.darkIcons = (SessionSettings.instance.backgroundColorsIndex == 1 || SessionSettings.instance.backgroundColorsIndex == 3)

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

                //setupPaintEventTimer()
            }
            else {
                SessionSettings.instance.paintColor = SessionSettings.instance.getSharedPrefs(this).getInt(
                    "last_single_paint_color",
                    surface_view.interactiveCanvas.getGridLineColor()
                )
            }
        }

        visibleActionViews = arrayOf(menu_button, paint_panel_button, open_tools_button, recent_colors_button,
            export_button, background_button, grid_lines_button, canvas_summary_button)

        panelThemeConfig = PanelThemeConfig.buildConfig(SessionSettings.instance.panelResIds[SessionSettings.instance.panelBackgroundResIndex])

        // listeners
        surface_view.pixelHistoryListener = this
        surface_view.gestureListener = this
        surface_view.objectSelectionListener = this
        surface_view.selectedObjectMoveView = this
        surface_view.selectedObjectView = this
        surface_view.canvasEdgeTouchListener = this

        surface_view.interactiveCanvas.interactiveCanvasListener = this
        surface_view.interactiveCanvas.recentColorsListener = this
        surface_view.interactiveCanvas.artExportListener = this
        surface_view.interactiveCanvas.deviceCanvasViewportResetListener = this

        paint_qty_bar.actionListener = this
        paint_qty_circle.actionListener = this

        // palette
        palette_name_text.setOnClickListener {
            showPalettesFragmentPopover()
        }

        palette_name_text.text = SessionSettings.instance.palette.name

        palette_add_color_action.type = ActionButtonView.Type.ADD
        palette_add_color_button.actionBtnView = palette_add_color_action

        palette_add_color_button.setOnClickListener {
            if (SessionSettings.instance.palette.colors.size < Palette.maxColors) {
                SessionSettings.instance.palette.addColor(SessionSettings.instance.paintColor)
                syncPaletteAndColor()
            }
            else {
                Toast.makeText(context, "${SessionSettings.instance.palette.name} is full", Toast.LENGTH_SHORT).show()
            }
        }

        palette_remove_color_action.type = ActionButtonView.Type.REMOVE
        palette_remove_color_button.actionBtnView = palette_remove_color_action

        palette_remove_color_button.setOnClickListener {
            showPaletteColorRemovePrompt(SessionSettings.instance.paintColor)
        }

        syncPaletteAndColor()

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

        // paint_qty_bar.world = world

        pixel_history_fragment_container.x = 0F
        pixel_history_fragment_container.y = 0F

        // paint panel
        paint_amt_info.text = SessionSettings.instance.dropsAmt.toString()

        if (SessionSettings.instance.lockPaintPanel) {
            lock_paint_panel_action.type = ActionButtonView.Type.LOCK_CLOSE
        }
        else {
            lock_paint_panel_action.type = ActionButtonView.Type.LOCK_OPEN
        }
        lock_paint_panel.actionBtnView = lock_paint_panel_action

        paint_indicator_view_bottom_layer.panelThemeConfig = panelThemeConfig
        paint_indicator_view.topLayer = true

        if (SessionSettings.instance.selectedPaletteIndex == 0) {
            setupColorPalette(surface_view.interactiveCanvas.recentColorsList.toTypedArray())
        }

        recolorVisibleActionViews()

        if (SessionSettings.instance.closePaintBackButtonColor != -1) {
            close_paint_panel.color = SessionSettings.instance.closePaintBackButtonColor
        }
        else {
            close_paint_panel.color = panelThemeConfig.actionButtonColor
        }

        if (panelThemeConfig.actionButtonColor == Color.BLACK) {
            palette_name_text.setTextColor(Color.parseColor("#FF111111"))
            palette_name_text.setShadowLayer(3F, 2F, 2F, Color.parseColor("#7F333333"))

            paint_color_accept.color = Color.BLACK

            palette_add_color_action.colorMode = ActionButtonView.ColorMode.BLACK
            palette_remove_color_action.colorMode = ActionButtonView.ColorMode.BLACK

            paint_yes.color = Color.BLACK
            paint_no.color = Color.BLACK

            lock_paint_panel_action.colorMode = ActionButtonView.ColorMode.BLACK
        }
        else {
            palette_name_text.setTextColor(Color.WHITE)

            paint_color_accept.color = Color.WHITE

            palette_add_color_action.colorMode = ActionButtonView.ColorMode.WHITE
            palette_remove_color_action.colorMode = ActionButtonView.ColorMode.WHITE

            paint_yes.color = Color.WHITE
            paint_no.color = Color.WHITE

            lock_paint_panel_action.colorMode = ActionButtonView.ColorMode.WHITE
        }

        if (panelThemeConfig.actionButtonColor == ActionButtonView.blackPaint.color) {
            paint_color_accept.color = Color.BLACK
        }

        if (panelThemeConfig.inversePaintEventInfo) {
            paint_time_info_container.setBackgroundResource(R.drawable.timer_text_background_inverse)
            paint_time_info.setTextColor(ActionButtonView.blackPaint.color)
            paint_amt_info.setTextColor(ActionButtonView.blackPaint.color)
        }

        paint_qty_bar.panelThemeConfig = panelThemeConfig
        paint_qty_circle.panelThemeConfig = panelThemeConfig
        paint_indicator_view.panelThemeConfig = panelThemeConfig

        // color picker view
        //color_picker_view.setSelectorColor(Color.WHITE)

        default_black_color_action.type = ActionButtonView.Type.BLACK_COLOR_DEFAULT
        default_black_color_button.actionBtnView = default_black_color_action

        default_black_color_button.setOnClickListener {
            hsb_palette.init(ActionButtonView.blackPaint.color)
        }

        default_white_color_action.type = ActionButtonView.Type.WHITE_COLOR_DEFAULT
        default_white_color_button.actionBtnView = default_white_color_action

        default_white_color_button.setOnClickListener {
            hsb_palette.init(ActionButtonView.whitePaint.color)
        }

        val textChangeListener = object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                try {
                    val color = Color.parseColor("#$s")
                    hsb_palette.init(color)

                    hideKeyboard()
                }
                catch (exception: Exception) {

                }
            }

            override fun afterTextChanged(s: Editable?) {

            }
        }

        color_hex_string_input.addTextChangedListener(textChangeListener)

        color_hex_string_input.setOnEditorActionListener { textView, actionId, keyEvent ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard()
            }
            true
        }

        hsb_palette.listen(object: HSBPalette.ColorListener {
            override fun onColor(color: Int) {
                Log.i("Color", color.toString())

                paint_indicator_view_bottom_layer.setPaintColor(color)

                if (PaintColorIndicator.isColorLight(color) && panelThemeConfig.actionButtonColor == Color.WHITE) {
                    paint_color_accept.color = Color.BLACK
                }
                else if (panelThemeConfig.actionButtonColor == Color.WHITE) {
                    paint_color_accept.color = Color.WHITE
                }
                else if (PaintColorIndicator.isColorDark(color) && panelThemeConfig.actionButtonColor == Color.BLACK) {
                    paint_color_accept.color = Color.WHITE
                }
                else if (panelThemeConfig.actionButtonColor == Color.BLACK) {
                    paint_color_accept.color = Color.BLACK
                }

                color_hex_string_input.removeTextChangedListener(textChangeListener)

                val hexColor = java.lang.String.format("%06X", 0xFFFFFF and color)
                color_hex_string_input.setText(hexColor)

                color_hex_string_input.addTextChangedListener(textChangeListener)

                // palette color actions
                syncPaletteAndColor()
            }
        })

        // button clicks
        paint_panel.setOnClickListener {
            closePopoverFragment()
        }

        // paint buttons
        paint_panel_button.setOnClickListener {
            togglePaintPanel(true)
        }

        paint_yes.setOnClickListener {
            if (world && !InteractiveCanvasSocket.instance.isConnected()) return@setOnClickListener

            surface_view.endPainting(true)

            paint_yes_container.visibility = View.GONE
            paint_no_container.visibility = View.GONE
            close_paint_panel_container.visibility = View.VISIBLE

            paint_yes.invalidate()

            surface_view.startPainting()
        }

        paint_no.setOnClickListener {
            if (color_picker_frame.visibility == View.VISIBLE) {
                paint_indicator_view_bottom_layer.setPaintColor(initalColor)
                syncPaletteAndColor()

                color_picker_frame.visibility = View.GONE

                if (SessionSettings.instance.canvasLockBorder) {
                    paint_warning_frame.visibility = View.VISIBLE
                }

                paint_yes.visibility = View.VISIBLE

                paint_color_accept.visibility = View.GONE

                //recent_colors_button.visibility = View.VISIBLE
                //recent_colors_container.visibility = View.GONE

                surface_view.endPaintSelection()

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

                //recent_colors_button.visibility = View.VISIBLE
                //recent_colors_container.visibility = View.GONE

                surface_view.startPainting()
            }
        }

        close_paint_panel.setOnClickListener {
            togglePaintPanel(false)

            recent_colors_container.visibility = View.GONE
            recent_colors_action.visibility = View.VISIBLE

            closePopoverFragment()
        }

        lock_paint_panel.setOnClickListener {
            SessionSettings.instance.lockPaintPanel = !SessionSettings.instance.lockPaintPanel

            if (SessionSettings.instance.lockPaintPanel) {
                lock_paint_panel_action.type = ActionButtonView.Type.LOCK_CLOSE
            }
            else {
                lock_paint_panel_action.type = ActionButtonView.Type.LOCK_OPEN
            }
        }

        paint_indicator_view.setOnClickListener {
            // start color selection mode
            if (color_picker_frame.visibility != View.VISIBLE) {
                color_picker_frame.visibility = View.VISIBLE
                initalColor = SessionSettings.instance.paintColor
                hsb_palette.init(initalColor)

                paint_warning_frame.visibility = View.GONE

                paint_color_accept.visibility = View.VISIBLE

                paint_yes_container.visibility = View.GONE
                close_paint_panel_container.visibility = View.GONE
                paint_no_container.visibility = View.VISIBLE

                //recent_colors_button.visibility = View.GONE
                //recent_colors_container.visibility = View.GONE

                surface_view.startPaintSelection()

                stopEmittingParticles()
            }
        }

        paint_color_accept.setOnClickListener {
            color_picker_frame.visibility = View.GONE

            if (SessionSettings.instance.canvasLockBorder) {
                paint_warning_frame.visibility = View.VISIBLE
            }

            paint_yes.visibility = View.VISIBLE

            paint_color_accept.visibility = View.GONE

            surface_view.endPaintSelection()

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

            SessionSettings.instance.saveColor(requireContext())
        }

        // to stop click-through to the canvas behind
        color_picker_frame.setOnClickListener {

        }

        // recent colors
        recent_colors_button.setOnClickListener {
            if (recent_colors_container.visibility != View.VISIBLE) {
                recent_colors_container.visibility = View.VISIBLE
                recent_colors_action.visibility = View.INVISIBLE

                if (paint_panel.visibility != View.VISIBLE) {
                    togglePaintPanel(true)
                }

                if (canvas_summary_view.visibility == View.VISIBLE) {
                    canvas_summary_container.visibility = View.INVISIBLE
                }
            }
            else {
                recent_colors_container.visibility = View.GONE
                recent_colors_action.visibility = View.VISIBLE
            }
        }

        // menu button
        if (!SessionSettings.instance.selectedHand) {
            toggleMenu(true)
        }

        menu_button.setOnClickListener {
            if (surface_view.isExporting()) {
                export_fragment_container.visibility = View.INVISIBLE
                surface_view.endExport()

                toggleExportBorder(false)

                // export_button.background = ResourcesCompat.getDrawable(resources, R.drawable.ic_share, null)
            }
            else if (surface_view.isObjectMoveSelection()) {
                surface_view.interactiveCanvas.cancelMoveSelectedObject()
                toggleExportBorder(false, double = true)

                surface_view.startExport()
                toggleExportBorder(true)
            }
            else if (surface_view.isObjectMoving()) {
                surface_view.interactiveCanvas.cancelMoveSelectedObject()
                toggleExportBorder(false)
            }
            else if (terminal_container.visibility == View.VISIBLE) {
                toggleTerminal(false)
            }
            else {
                (requireActivity() as InteractiveCanvasActivity).showOptionsFragment(this)
                //toggleMenu(menu_container.visibility != View.VISIBLE)
            }
        }

//        activity?.apply {
//            menu_button.setLongPressActionListener(this, object: LongPressListener {
//                override fun onLongPress() {
//                    //toggleTerminal(true)
//                }
//            })
//        }

        // export button
        export_button.setOnClickListener {
            if (export_button.toggleState == ButtonFrame.ToggleState.NONE) {
                surface_view.startExport()
                export_button.toggleState = ButtonFrame.ToggleState.SINGLE
                toggleExportBorder(true)
            }
            else if (export_button.toggleState == ButtonFrame.ToggleState.SINGLE) {
                surface_view.endExport()
                if (world) {
                    toggleExportBorder(false)
                }
                else {
                    surface_view.startObjectMove()
                    export_button.toggleState = ButtonFrame.ToggleState.DOUBLE
                    toggleExportBorder(true, double = true)
                }
            }
            else if (export_button.toggleState == ButtonFrame.ToggleState.DOUBLE) {
                surface_view.interactiveCanvas.cancelMoveSelectedObject()
                toggleExportBorder(false)
            }
        }

        Log.i("Panel size", SessionSettings.instance.panelResIds.size.toString())

        // background button
        background_button.setOnClickListener {
            if (SessionSettings.instance.backgroundColorsIndex == surface_view.interactiveCanvas.numBackgrounds - 1) {
                SessionSettings.instance.backgroundColorsIndex = 0
            }
            else {
                SessionSettings.instance.backgroundColorsIndex += 1
            }

            SessionSettings.instance.darkIcons = (SessionSettings.instance.backgroundColorsIndex == 1 || SessionSettings.instance.backgroundColorsIndex == 3)
            recolorVisibleActionViews()

            if (SessionSettings.instance.backgroundColorsIndex == surface_view.interactiveCanvas.numBackgrounds - 1
                && (SessionSettings.instance.canvasBackgroundPrimaryColor == 0 || SessionSettings.instance.canvasBackgroundSecondaryColor == 0)) {
                SessionSettings.instance.backgroundColorsIndex = 0
            }

            invalidateButtons()

            if (canvas_summary_container.visibility == View.VISIBLE) {
                canvas_summary_view.invalidate()
            }

            surface_view.interactiveCanvas.interactiveCanvasDrawer?.notifyRedraw()

            SessionSettings.instance.saveBackground(requireContext())
        }

        // grid lines toggle button
        grid_lines_button.setOnClickListener {
            SessionSettings.instance.gridLineMode += 1

            if (SessionSettings.instance.gridLineMode > 1) {
                SessionSettings.instance.gridLineMode = 0
            }

            surface_view.interactiveCanvas.interactiveCanvasDrawer?.notifyRedraw()
        }

        // canvas summary toggle button
        canvas_summary_button.setOnClickListener {
            toggleCanvasSummary()
        }

        // open tools button
        open_tools_button.setOnClickListener {
            if (toolboxOpen) {
                toggleTools(false)
            }
            else {
                toggleTools(true)
            }
        }

        // recent colors background
        recent_colors_container.setOnClickListener {

        }

        // tablet & righty
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

                    color_hex_string_input.textSize = 28F
                    var linearLayoutParams = LinearLayout.LayoutParams(Utils.dpToPx(context, 120), LinearLayout.LayoutParams.MATCH_PARENT)
                    linearLayoutParams.rightMargin = Utils.dpToPx(context, 10)
                    linearLayoutParams.gravity = Gravity.BOTTOM

                    color_hex_string_input.layoutParams = linearLayoutParams

                    color_hex_string_input.gravity = Gravity.BOTTOM

                    // default color buttons size
                    var frameLayoutParams = (default_black_color_action.layoutParams as FrameLayout.LayoutParams)
                    frameLayoutParams.width = (color_picker_frame.layoutParams.width * 0.16).toInt()
                    frameLayoutParams.height = frameLayoutParams.width

                    default_black_color_action.layoutParams = frameLayoutParams

                    frameLayoutParams = (default_white_color_action.layoutParams as FrameLayout.LayoutParams)
                    frameLayoutParams.width = (color_picker_frame.layoutParams.width * 0.16).toInt()
                    frameLayoutParams.height = frameLayoutParams.width

                    default_white_color_action.layoutParams = frameLayoutParams

                    linearLayoutParams = (default_white_color_button.layoutParams as LinearLayout.LayoutParams)

                    if (default_white_color_action.layoutParams.width <= Utils.dpToPx(context, 40)) {
                        linearLayoutParams.marginStart = Utils.dpToPx(context, 10)
                    }
                    else {
                        linearLayoutParams.marginStart = Utils.dpToPx(context, 20)
                    }

                    default_white_color_button.layoutParams = linearLayoutParams

                    // paint panel
                    layoutParams = ConstraintLayout.LayoutParams(
                        ((150 / 1000F) * view.width).toInt(),
                        ConstraintLayout.LayoutParams.MATCH_PARENT
                    )
                    layoutParams.rightToRight = ConstraintSet.PARENT_ID

                    paint_panel.layoutParams = layoutParams

                    linearLayoutParams = (paint_yes_container.layoutParams as LinearLayout.LayoutParams)
                    if (paint_panel.layoutParams.width < 288) {
                        linearLayoutParams.rightMargin = Utils.dpToPx(context, 5)
                    }
                    else {
                        linearLayoutParams.rightMargin = Utils.dpToPx(context, 30)
                    }
                    paint_yes_container.layoutParams = linearLayoutParams

                    // paint indicator size
                    val frameWidth = ((150 / 1000F) * view.width).toInt()
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
                        /*layoutParams = ConstraintLayout.LayoutParams(
                            (paint_qty_bar.width * 1.25).toInt(),
                            (paint_qty_bar.height * 1.25).toInt()
                        )
                        layoutParams.topToTop = ConstraintSet.PARENT_ID
                        layoutParams.leftToLeft = ConstraintSet.PARENT_ID
                        layoutParams.rightToRight = ConstraintSet.PARENT_ID

                        layoutParams.topMargin = Utils.dpToPx(context, 15)

                        paint_qty_bar.layoutParams = layoutParams*/
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

                    //layoutParams = ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, Utils.dpToPx(context, 40))
                    palette_name_text.textSize = 28F

                    var actionButtonLayoutParams = FrameLayout.LayoutParams(Utils.dpToPx(context, 30), Utils.dpToPx(context, 30))
                    actionButtonLayoutParams.gravity = Gravity.CENTER
                    palette_add_color_action.layoutParams = actionButtonLayoutParams

                    actionButtonLayoutParams = FrameLayout.LayoutParams(Utils.dpToPx(context, 30), Utils.dpToPx(context, 4))
                    actionButtonLayoutParams.gravity = Gravity.CENTER
                    palette_remove_color_action.layoutParams = actionButtonLayoutParams

                    actionButtonLayoutParams = FrameLayout.LayoutParams(Utils.dpToPx(context, 27), Utils.dpToPx(context, 30))
                    actionButtonLayoutParams.gravity = Gravity.CENTER
                    lock_paint_panel_action.layoutParams = actionButtonLayoutParams

                    layoutParams = ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.WRAP_CONTENT)
                    layoutParams.startToStart = ConstraintSet.PARENT_ID
                    layoutParams.endToEnd = ConstraintSet.PARENT_ID
                    layoutParams.topToBottom = palette_name_text.id
                    layoutParams.topMargin = Utils.dpToPx(context, 10)

                    color_action_button_menu.layoutParams = layoutParams

                    /*layoutParams = ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, Utils.dpToPx(context, 40))
                    layoutParams.topMargin = Utils.dpToPx(context, 20)
                    layoutParams.bottomMargin = Utils.dpToPx(context, 50)
                    layoutParams.topToTop = ConstraintSet.PARENT_ID

                    palette_name_text.layoutParams = layoutParams*/

                    /*layoutParams = ConstraintLayout.LayoutParams(Utils.dpToPx(context, 40), Utils.dpToPx(context, 40))
                    layoutParams.topMargin = Utils.dpToPx(context, 20)
                    layoutParams.startToStart = ConstraintSet.PARENT_ID
                    layoutParams.endToEnd = ConstraintSet.PARENT_ID
                    layoutParams.topToBottom = palette_name_text.id

                    palette_add_color_button.layoutParams = layoutParams

                    layoutParams = ConstraintLayout.LayoutParams(Utils.dpToPx(context, 40), Utils.dpToPx(context, 40))
                    layoutParams.topMargin = Utils.dpToPx(context, 20)
                    layoutParams.startToStart = ConstraintSet.PARENT_ID
                    layoutParams.endToEnd = ConstraintSet.PARENT_ID
                    layoutParams.topToBottom = palette_name_text.id

                    palette_remove_color_button.layoutParams = layoutParams

                    frameLayoutParams = FrameLayout.LayoutParams(Utils.dpToPx(context, 30), Utils.dpToPx(context, 30))
                    frameLayoutParams.topMargin = Utils.dpToPx(context, 5)
                    frameLayoutParams.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL

                    palette_add_color_action.layoutParams = frameLayoutParams

                    frameLayoutParams = FrameLayout.LayoutParams(Utils.dpToPx(context, 30), Utils.dpToPx(context, 6))
                    frameLayoutParams.topMargin = Utils.dpToPx(context, 17)
                    frameLayoutParams.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL

                    palette_remove_color_action.layoutParams = frameLayoutParams*/
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
                setPanelBackground()

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
                    val toolboxButtons = arrayOf(export_button, background_button, grid_lines_button, canvas_summary_button)

                    for (button in toolboxButtons) {
                        layoutParams = button.layoutParams as ConstraintLayout.LayoutParams
                        layoutParams.rightToRight = -1
                        layoutParams.leftToLeft = ConstraintSet.PARENT_ID
                        layoutParams.leftMargin = Utils.dpToPx(context, 6)
                        button.layoutParams = layoutParams
                    }

                    // recent colors button
                    layoutParams = ConstraintLayout.LayoutParams(Utils.dpToPx(context, 80), Utils.dpToPx(context, 80))

                    layoutParams.bottomToBottom = ConstraintSet.PARENT_ID
                    layoutParams.rightToLeft = color_picker_frame.id

                    recent_colors_button.layoutParams = layoutParams

                    // recent colors action
                    layoutParams3 = recent_colors_action.layoutParams as FrameLayout.LayoutParams
                    layoutParams3.gravity = Gravity.END or Gravity.BOTTOM
                    recent_colors_action.layoutParams = layoutParams3

                    // recent colors container
                    layoutParams = recent_colors_container.layoutParams as ConstraintLayout.LayoutParams

                    layoutParams.leftToRight = -1
                    layoutParams.rightToLeft = color_picker_frame.id
                    recent_colors_container.layoutParams = layoutParams

                    layoutParams = canvas_summary_container.layoutParams as ConstraintLayout.LayoutParams

                    layoutParams.startToStart = -1
                    layoutParams.endToEnd = ConstraintSet.PARENT_ID
                    canvas_summary_container.layoutParams = layoutParams

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
                else {
                    // close paint panel button
                    close_paint_panel_action.rotation = 180F
                }
                // small action buttons
                if (SessionSettings.instance.smallActionButtons) {
                    // paint yes
                    var layoutParams = paint_yes_image.layoutParams as FrameLayout.LayoutParams

                    layoutParams.width = (layoutParams.width * 0.833).toInt()
                    layoutParams.height = (layoutParams.height * 0.833).toInt()
                    paint_yes_image.layoutParams = layoutParams
                    paint_yes_image.layoutParams = layoutParams

                    // paint no
                    layoutParams = paint_no_image.layoutParams as FrameLayout.LayoutParams

                    layoutParams.width = (layoutParams.width * 0.833).toInt()
                    layoutParams.height = (layoutParams.height * 0.833).toInt()
                    paint_no_image.layoutParams = layoutParams
                    paint_no_image.layoutParams = layoutParams

                    // paint select accept
                    layoutParams = paint_color_accept_image.layoutParams as FrameLayout.LayoutParams

                    layoutParams.width = (layoutParams.width * 0.833).toInt()
                    layoutParams.height = (layoutParams.height * 0.833).toInt()
                    paint_color_accept_image.layoutParams = layoutParams
                    paint_color_accept_image.layoutParams = layoutParams

                    // close paint panel
                    layoutParams = close_paint_panel_action.layoutParams as FrameLayout.LayoutParams

                    layoutParams.width = (layoutParams.width * 0.833).toInt()
                    layoutParams.height = (layoutParams.height * 0.833).toInt()
                    close_paint_panel_action.layoutParams = layoutParams
                }

                surface_view.setInitialPositionAndScale()
            }
        })
    }

    override fun onPause() {
        super.onPause()

        stopEmittingParticles()

        // unregister listeners
        SessionSettings.instance.paintQtyListeners.remove(this)

        context?.apply {
            surface_view.interactiveCanvas.saveUnits(this)
            surface_view.interactiveCanvas.interactiveCanvasListener = null

            SessionSettings.instance.saveLastPaintColor(this, world)
        }

        paintEventTimer?.cancel()

        if (world) {
            //InteractiveCanvasSocket.instance.socket?.disconnect()
        }
        else {
            context?.apply {
                val deviceViewport = surface_view.interactiveCanvas.deviceViewport!!

                SessionSettings.instance.restoreDeviceViewportCenterX = deviceViewport.centerX()
                SessionSettings.instance.restoreDeviceViewportCenterY = deviceViewport.centerY()

                SessionSettings.instance.restoreCanvasScaleFactor = surface_view.interactiveCanvas.lastScaleFactor

                SessionSettings.instance.save(this)
                StatTracker.instance.save(this)
            }
        }

        pauseCanvas()
    }

    private fun pauseCanvas() {
        Log.i("Interactive Canvas", "Canvas Pause")
        InteractiveCanvasSocket.instance.disconnect()

        paused = true
        pauseTime = System.currentTimeMillis() / 1000
    }

    override fun onResume() {
        super.onResume()

        Log.i("On Resume", "Canvas resumed.")

        surface_view.interactiveCanvas.interactiveCanvasListener = this

        if (paused) {
            reload_transparent.visibility = View.VISIBLE
            reload_transparent.setOnClickListener { }
            resumeCanvas()

//            canvasService.logIp(SessionSettings.instance.uniqueId!!) { response ->
//                if (response == null || !response.get("success").asBoolean) {
//                    activity?.runOnUiThread {
//                        (activity as? InteractiveCanvasActivity)?.showMenuFragment()
//                    }
//                    return@logIp
//                }
//            }

            paused = false
        }
    }

    private fun resumeCanvas() {
        val time = System.currentTimeMillis() / 1000

        if (time - pauseTime > maxBgTime) {
            (requireActivity() as InteractiveCanvasActivity).showMenuFragment()
        }
        else {
            InteractiveCanvasSocket.instance.socketConnectCallback = this
            reconnectToSocket()
        }
    }

    // screen rotation
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        if (!SessionSettings.instance.tablet) {
            surface_view.interactiveCanvas.interactiveCanvasDrawer?.notifyRedraw()
            return
        }

        paint_panel.background = null

        Utils.setViewLayoutListener(requireView(), object : Utils.ViewLayoutListener {
            override fun onViewLayout(view: View) {
                // interactive canvas
                surface_view.interactiveCanvas.deviceViewport?.apply {
                    surface_view.interactiveCanvas.updateDeviceViewport(this@InteractiveCanvasFragment.requireContext())
                }

                // color picker frame width
                var layoutParams = ConstraintLayout.LayoutParams(
                    (view.width * 0.35).toInt(),
                    ConstraintLayout.LayoutParams.MATCH_PARENT
                )

                layoutParams.leftToLeft = (color_picker_frame.layoutParams as ConstraintLayout.LayoutParams).leftToLeft
                layoutParams.rightToRight = (color_picker_frame.layoutParams as ConstraintLayout.LayoutParams).rightToRight

                color_picker_frame.layoutParams = layoutParams

                // color picker default color buttons
                var frameLayoutParams = (default_black_color_action.layoutParams as FrameLayout.LayoutParams)
                frameLayoutParams.width = (color_picker_frame.layoutParams.width * 0.16).toInt()
                frameLayoutParams.height = frameLayoutParams.width

                default_black_color_action.layoutParams = frameLayoutParams

                frameLayoutParams = (default_white_color_action.layoutParams as FrameLayout.LayoutParams)
                frameLayoutParams.width = (color_picker_frame.layoutParams.width * 0.16).toInt()
                frameLayoutParams.height = frameLayoutParams.width

                default_white_color_action.layoutParams = frameLayoutParams

                var linearLayoutParams = (default_white_color_button.layoutParams as LinearLayout.LayoutParams)
                if (default_white_color_action.layoutParams.width <= Utils.dpToPx(context, 40)) {
                    linearLayoutParams.marginStart = Utils.dpToPx(context, 10)
                }
                else {
                    linearLayoutParams.marginStart = Utils.dpToPx(context, 20)
                }
                default_white_color_button.layoutParams = linearLayoutParams

                val paintPanelWidth = if (Utils.isTablet(requireContext())) {
                    ((150 / 1000F) * view.width).toInt()
                }
                else {
                    ((250 / 1000F) * view.width).toInt()
                }

                // paint panel
                layoutParams = ConstraintLayout.LayoutParams(
                    paintPanelWidth,
                    ConstraintLayout.LayoutParams.MATCH_PARENT
                )
                layoutParams.leftToLeft = (paint_panel.layoutParams as ConstraintLayout.LayoutParams).leftToLeft
                layoutParams.rightToRight = (paint_panel.layoutParams as ConstraintLayout.LayoutParams).rightToRight

                paint_panel.layoutParams = layoutParams

                linearLayoutParams = (paint_yes_container.layoutParams as LinearLayout.LayoutParams)
                if (paint_panel.layoutParams.width < 288) {
                    linearLayoutParams.rightMargin = Utils.dpToPx(context, 5)
                }
                else {
                    linearLayoutParams.rightMargin = Utils.dpToPx(context, 30)
                }
                paint_yes_container.layoutParams = linearLayoutParams

                // paint indicator size
                val frameWidth = ((150 / 1000F) * view.width).toInt()
                val indicatorMargin = (frameWidth * 0.15).toInt()
                val indicatorWidth = frameWidth - indicatorMargin

                layoutParams = ConstraintLayout.LayoutParams(indicatorWidth, indicatorWidth)
                layoutParams.topToTop = (paint_indicator_view.layoutParams as ConstraintLayout.LayoutParams).topToTop
                layoutParams.bottomToBottom = (paint_indicator_view.layoutParams as ConstraintLayout.LayoutParams).bottomToBottom
                layoutParams.leftToLeft = (paint_indicator_view.layoutParams as ConstraintLayout.LayoutParams).leftToLeft
                layoutParams.rightToRight = (paint_indicator_view.layoutParams as ConstraintLayout.LayoutParams).rightToRight

                paint_indicator_view_bottom_layer.layoutParams = layoutParams
                paint_indicator_view.layoutParams = layoutParams

                device_canvas_viewport_view.updateDeviceViewport()

                setPanelBackground()
            }
        })
    }

    // view helper
    private fun setPanelBackground() {
        context?.apply {
            val backgroundDrawable = ContextCompat.getDrawable(this, SessionSettings.instance.panelResIds[SessionSettings.instance.panelBackgroundResIndex]) as BitmapDrawable

            if (SessionSettings.instance.tablet) {
                paint_panel.clipChildren = false

                val scale = requireView().height / backgroundDrawable.bitmap.height.toFloat()

                val newWidth = (backgroundDrawable.bitmap.width * scale).toInt()
                val newHeight = (backgroundDrawable.bitmap.height * scale).toInt()
                val newBitmap = Bitmap.createScaledBitmap(backgroundDrawable.bitmap, newWidth,
                    newHeight, false)
                val scaledBitmapDrawable = BitmapDrawable(resources, newBitmap)

                val resizedBitmap = Bitmap.createBitmap(scaledBitmapDrawable.bitmap, max(0, scaledBitmapDrawable.bitmap.width / 2 - paint_panel.layoutParams.width / 2), 0, paint_panel.layoutParams.width, scaledBitmapDrawable.bitmap.height)
                val resizedBitmapDrawable = BitmapDrawable(resizedBitmap)

                scaledBitmapDrawable.gravity = Gravity.CENTER

                paint_panel.setBackgroundDrawable(resizedBitmapDrawable)
            }
            else {
                paint_panel.setBackgroundDrawable(backgroundDrawable)
            }
        }
    }

    private fun hideKeyboard() {
        context?.apply {
            val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(view?.windowToken, 0)
        }
    }

    private fun recolorVisibleActionViews() {
        for (buttonFrame in visibleActionViews) {
            buttonFrame.isLight = SessionSettings.instance.backgroundColorsIndex != 1 && SessionSettings.instance.backgroundColorsIndex != 3
        }
    }

    private fun setupColorPalette(colors: Array<Int>?) {
        if (colors != null) {
            var i = 0
            for (v in recent_colors_container.children) {
                if (i < colors.size) {
                    (v as RecentColorView).color = colors[colors.size - 1 - i]
                    v.visibility = View.VISIBLE
                }
                else {
                    v.visibility = View.GONE
                }

                v.setOnClickListener {
                    (v as RecentColorView).color?.apply {
                        SessionSettings.instance.paintColor = this
                        notifyPaintColorUpdate(SessionSettings.instance.paintColor)

                        //recent_colors_container.visibility = View.GONE
                        //recent_colors_action.visibility = View.VISIBLE
                    }
                }

                val layoutParams = v.layoutParams

                layoutParams.width = Utils.dpToPx(context, SessionSettings.instance.colorPaletteSize * 10)
                layoutParams.height = Utils.dpToPx(context, SessionSettings.instance.colorPaletteSize * 10)

                v.layoutParams = layoutParams

                i++
            }

            // fixes issue where with no colors on the color palette the paint panel won't open
            if (colors.isEmpty()) {
                recent1.visibility = View.VISIBLE
            }
        }
        else {
            for (v in recent_colors_container.children) {
                (v as ActionButtonView).type = ActionButtonView.Type.RECENT_COLOR
            }
        }
    }

    private fun invalidateButtons() {
        menu_action.invalidate()
        paint_panel_action_view.invalidate()
        export_action.invalidate()
        background_action.invalidate()
        grid_lines_action.invalidate()
        canvas_summary_action.invalidate()
        recent_colors_action.invalidate()
        open_tools_action.invalidate()
        object_move_up_action.invalidate()
        object_move_down_action.invalidate()
        object_move_left_action.invalidate()
        object_move_right_action.invalidate()
    }

    // view toggles
    private fun togglePaintPanel(show: Boolean, softHide: Boolean = false) {
        if (show) {
            paint_panel.visibility = View.VISIBLE
            paint_panel_button.visibility = View.GONE

            export_button.visibility = View.INVISIBLE
            background_button.visibility = View.INVISIBLE

            open_tools_button.visibility = View.INVISIBLE

            toggleTools(false)

            if (pixel_history_fragment_container.visibility == View.VISIBLE) {
                pixel_history_fragment_container.visibility = View.GONE
            }

            if (canvas_summary_view.visibility == View.VISIBLE) {
                canvas_summary_container.visibility = View.INVISIBLE
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

            menu_button.visibility = View.GONE
            menu_container.visibility = View.GONE

            SessionSettings.instance.paintPanelOpen = true
        }
        else if (softHide) {
            paint_panel.visibility = View.GONE

            toggleTools(false)
        }
        else {
            surface_view.endPainting(false)

            paint_panel.visibility = View.GONE
            paint_warning_frame.visibility = View.GONE

            paint_panel_button.visibility = View.VISIBLE

            //recent_colors_action.visibility = View.VISIBLE
            //recent_colors_container.visibility = View.GONE

            if (toolboxOpen) {
                export_button.visibility = View.VISIBLE
                background_button.visibility = View.VISIBLE
                grid_lines_button.visibility = View.VISIBLE
            }

            menu_button.visibility = View.VISIBLE

            open_tools_button.visibility = View.VISIBLE

            toggleExportBorder(false)

            stopEmittingParticles()

            SessionSettings.instance.paintPanelOpen = false
        }
    }

    private fun toggleTools(show: Boolean) {
        if (!animatingTools) {
            if (show && !toolboxOpen) {
                animatingTools = true

                export_button.visibility = View.VISIBLE
                background_button.visibility = View.VISIBLE
                grid_lines_button.visibility = View.VISIBLE
                canvas_summary_button.visibility = View.VISIBLE

                Animator.animateMenuItems(
                    listOf(
                        listOf(export_button), listOf(background_button), listOf(
                            grid_lines_button
                        ), listOf(canvas_summary_button)
                    ), cascade = false, out = false, inverse = SessionSettings.instance.rightHanded,
                    completion = object: Animator.CompletionHandler {
                        override fun onCompletion() {
                            animatingTools = false
                            toolboxOpen = true
                        }
                    }
                )

                toolboxOpen = true
                SessionSettings.instance.toolboxOpen = true
            }
            else if (!show && toolboxOpen) {
                animatingTools = true

                Animator.animateMenuItems(
                    listOf(
                        listOf(export_button), listOf(background_button), listOf(
                            grid_lines_button
                        ), listOf(canvas_summary_button)
                    ), cascade = false, out = true, inverse = SessionSettings.instance.rightHanded,
                    completion = object: Animator.CompletionHandler {
                        override fun onCompletion() {
                            animatingTools = false
                            toolboxOpen = false
                        }
                    }
                )

                toolboxOpen = false
                SessionSettings.instance.toolboxOpen = false
            }
        }
    }

    private fun toggleExportBorder(show: Boolean, double: Boolean = false) {
        var color = ActionButtonView.lightYellowSemiPaint.color
        if (double) {
            color = ActionButtonView.lightGreenPaint.color
        }
        if (show) {
            context?.apply {
                val drawable: GradientDrawable = export_border_view.background as GradientDrawable
                drawable.setStroke(
                    Utils.dpToPx(this, 2),
                    color
                ) // set stroke width and stroke color
            }
            export_border_view.visibility = View.VISIBLE
        }
        else {
            export_border_view.visibility = View.GONE

            if (double) {
                export_button.toggleState = ButtonFrame.ToggleState.SINGLE
            }
            else {
                export_button.toggleState = ButtonFrame.ToggleState.NONE
            }

            export_action.invalidate()
        }
    }

    private fun toggleMenu(open: Boolean) {
        if (menuFragment == null) {
            menuFragment = MenuFragment.createFromCanvas(this)

            menuFragment?.menuButtonListener = (activity as InteractiveCanvasActivity)
            menuFragment?.menuCardListener = this

            fragmentManager?.apply {
                beginTransaction().replace(menu_container.id, menuFragment!!).commit()

                menu_container.alpha = 0F
                menu_container.animate().alpha(1f).setDuration(250).withEndAction {

                }.start()
                //interactiveCanvasFragmentListener?.onInteractiveCanvasBack()
            }
        }
        else {
            menuFragment!!.clearMenuTextHighlights()
        }
        if (open) {
            menu_container.visibility = View.VISIBLE
        }
        else {
            menu_container.visibility = View.GONE
        }
    }

    private fun toggleTerminal(open: Boolean) {
        if (terminalFragment == null) {
            terminalFragment = TerminalFragment()

            terminalFragment?.interactiveCanvas = surface_view.interactiveCanvas

            fragmentManager?.apply {
                beginTransaction().replace(terminal_container.id, terminalFragment!!).commit()
            }
        }

        if (open) {
            terminal_container.visibility = View.VISIBLE
        }
        else {
            terminal_container.visibility = View.GONE
        }
    }

    // "Window" fragments
    // pixel history listener
    override fun showPixelHistoryFragmentPopover(screenPoint: Point) {
        fragmentManager?.apply {
            if (surface_view.interactiveCanvas.isSelectedPixelBackground()) return

            surface_view.interactiveCanvas.getPixelHistory(surface_view.interactiveCanvas.pixelIdForUnitPoint(
                surface_view.interactiveCanvas.lastSelectedUnitPoint
            ), object : PixelHistoryCallback {
                override fun onHistoryJsonResponse(historyJson: JSONArray) {
                    // set bottom-left of view to screenPoint

                    pixel_history_fragment_container?.apply {
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
                            if (pixel_history_fragment_container.x < Utils.dpToPx(context, 20).toFloat()) {
                                pixel_history_fragment_container.x = Utils.dpToPx(context, 20).toFloat()
                            } else if (pixel_history_fragment_container.x + pixel_history_fragment_container.width > width - Utils.dpToPx(context, 20).toFloat()) {
                                pixel_history_fragment_container.x =
                                    width - pixel_history_fragment_container.width.toFloat() - Utils.dpToPx(
                                        context,
                                        20
                                    ).toFloat()
                            }

                            if (pixel_history_fragment_container.y < Utils.dpToPx(context, 20).toFloat()) {
                                pixel_history_fragment_container.y = Utils.dpToPx(context, 20).toFloat()
                            } else if (pixel_history_fragment_container.y + pixel_history_fragment_container.height > height - Utils.dpToPx(context, 20).toFloat()) {
                                pixel_history_fragment_container.y =
                                    height - pixel_history_fragment_container.height.toFloat() - Utils.dpToPx(
                                        context,
                                        20
                                    ).toFloat()
                            }

                            val fragment = PixelHistoryFragment.create(server)
                            fragment.pixelHistoryJson = historyJson

                            beginTransaction().replace(
                                R.id.pixel_history_fragment_container,
                                fragment
                            ).commit()

                            pixel_history_fragment_container.visibility = View.VISIBLE
                        }
                    }
                }
            })
        }
    }

    // Palette and Canvas Frame fragments also use pixel_history_fragment_container
    override fun showDrawFrameConfigFragmentPopover(screenPoint: Point) {
        if (pixel_history_fragment_container.visibility == View.VISIBLE) {
            closePopoverFragment()
            return
        }

        if (canvas_summary_view.visibility == View.VISIBLE) {
            canvas_summary_container.visibility = View.INVISIBLE
        }

        fragmentManager?.apply {
            pixel_history_fragment_container?.apply {
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
                    if (pixel_history_fragment_container.x < Utils.dpToPx(context, 20).toFloat()) {
                        pixel_history_fragment_container.x = Utils.dpToPx(context, 20).toFloat()
                    } else if (pixel_history_fragment_container.x + pixel_history_fragment_container.width > width - Utils.dpToPx(context, 20).toFloat()) {
                        pixel_history_fragment_container.x =
                            width - pixel_history_fragment_container.width.toFloat() - Utils.dpToPx(
                                context,
                                20
                            ).toFloat()
                    }

                    if (pixel_history_fragment_container.y < Utils.dpToPx(context, 20).toFloat()) {
                        pixel_history_fragment_container.y = Utils.dpToPx(context, 20).toFloat()
                    } else if (pixel_history_fragment_container.y + pixel_history_fragment_container.height > height - Utils.dpToPx(context, 20).toFloat()) {
                        pixel_history_fragment_container.y =
                            height - pixel_history_fragment_container.height.toFloat() - Utils.dpToPx(
                                context,
                                20
                            ).toFloat()
                    }

                    val fragment = DrawFrameConfigFragment()
                    fragment.drawFrameConfigFragmentListener = this@InteractiveCanvasFragment

                    fragment.panelThemeConfig = panelThemeConfig

                    fragment.centerX = surface_view.interactiveCanvas.lastSelectedUnitPoint.x
                    fragment.centerY = surface_view.interactiveCanvas.lastSelectedUnitPoint.y

                    beginTransaction().replace(
                        R.id.pixel_history_fragment_container,
                        fragment
                    ).commit()

                    pixel_history_fragment_container.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun showPalettesFragmentPopover() {
        var screenPoint = Point(surface_view.width, 0)
        if (SessionSettings.instance.rightHanded) {
            screenPoint = Point(0, 0)
        }

        fragmentManager?.apply {
            // set bottom-left of view to screenPoint

            pixel_history_fragment_container?.apply {
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
                    if (pixel_history_fragment_container.x < Utils.dpToPx(context, 20).toFloat()) {
                        pixel_history_fragment_container.x = Utils.dpToPx(context, 20).toFloat()
                    } else if (pixel_history_fragment_container.x + pixel_history_fragment_container.width > width - Utils.dpToPx(context, 20).toFloat()) {
                        pixel_history_fragment_container.x =
                            width - pixel_history_fragment_container.width.toFloat() - Utils.dpToPx(
                                context,
                                20
                            ).toFloat()
                    }

                    if (pixel_history_fragment_container.y < Utils.dpToPx(context, 20).toFloat()) {
                        pixel_history_fragment_container.y = Utils.dpToPx(context, 20).toFloat()
                    } else if (pixel_history_fragment_container.y + pixel_history_fragment_container.height > height - Utils.dpToPx(context, 20).toFloat()) {
                        pixel_history_fragment_container.y =
                            height - pixel_history_fragment_container.height.toFloat() - Utils.dpToPx(
                                context,
                                20
                            ).toFloat()
                    }

                    val fragment = PalettesFragment()

                    palettesFragment = fragment
                    palettesFragment?.palettesFragmentListener = this@InteractiveCanvasFragment
                    palettesFragment?.panelThemeConfig = panelThemeConfig

                    beginTransaction().replace(
                        R.id.pixel_history_fragment_container,
                        fragment
                    ).commit()

                    pixel_history_fragment_container.visibility = View.VISIBLE
                }
            }
        }
    }

    // particle emitters
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

    // prompts
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

    private fun showPaletteColorRemovePrompt(color: Int) {
        AlertDialog.Builder(context)
            .setMessage(resources.getString(R.string.alert_message_palette_remove_color, SessionSettings.instance.palette.name))
            .setPositiveButton(
                android.R.string.yes
            ) { dialog, _ ->
                recentlyRemovedColorIndex = SessionSettings.instance.palette.colors.indexOf(color)
                recentlyRemovedColor = color

                SessionSettings.instance.palette.removeColor(color)
                syncPaletteAndColor()

                showPaletteColorUndoSnackbar(SessionSettings.instance.palette)

                dialog?.dismiss()
            }
            .setNegativeButton(
                android.R.string.no
            ) { dialog, _ -> dialog?.dismiss() }
            .show()
    }

    // interactive canvas listener
    override fun notifyPaintColorUpdate(color: Int) {
        hsb_palette.setColor(color)
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

    override fun notifyPaintActionStarted() {
        //recent_colors_action.visibility = View.VISIBLE
        //recent_colors_container.visibility = View.GONE

        if (!SessionSettings.instance.lockPaintPanel) {
            togglePaintPanel(show = false, softHide = true)
        }
    }

    override fun notifyClosePaletteFragment() {
        closePopoverFragment()
    }

    override fun isPaletteFragmentOpen(): Boolean {
        return pixel_history_fragment_container.visibility == View.VISIBLE
    }

    override fun notifyDeviceViewportUpdate() {
        if (device_canvas_viewport_view.visibility == View.VISIBLE) {
            device_canvas_viewport_view.updateDeviceViewport(surface_view.interactiveCanvas)
        }
    }

    override fun notifyUpdateCanvasSummary() {
        if (canvas_summary_container.visibility == View.VISIBLE) {
            canvas_summary_view.invalidate()
        }
    }

    override fun onDeviceViewportUpdate() {
        val canvasBounds = surface_view.interactiveCanvas.canvasScreenBounds()
        //Log.v("canvas bounds", canvasBounds.toString())

        for (buttonFrame in visibleActionViews) {
            val lastColorMode = buttonFrame.isLight

            val location = IntArray(2)
            buttonFrame.getLocationOnScreen(location)

            val x = location[0]
            val y = location[1]

            var inBounds = true

            // left
            if (x + buttonFrame.width / 2 < canvasBounds.left) {
                inBounds = false
            }
            // top
            else if (y + buttonFrame.height / 2 < canvasBounds.top) {
                inBounds = false
            }
            // right
            else if (x + buttonFrame.width / 2 > canvasBounds.right) {
                inBounds = false
            }
            // bottom
            else if (y + buttonFrame.height / 2 > canvasBounds.bottom) {
                inBounds = false
            }

            if (!inBounds) {
                buttonFrame.isLight = true
            }
            else {
                buttonFrame.isLight = !SessionSettings.instance.darkIcons
            }

            if (buttonFrame.isLight != lastColorMode) {
                buttonFrame.invalidate()
            }
        }
    }

    // interactive canvas gesture listener
    override fun onInteractiveCanvasPan() {
        pixel_history_fragment_container.visibility = View.GONE

        /*if (device_canvas_viewport_view.visibility == View.VISIBLE) {
            device_canvas_viewport_view.updateDeviceViewport(surface_view.interactiveCanvas)
        }*/
    }

    override fun onInteractiveCanvasScale() {
        pixel_history_fragment_container.visibility = View.GONE

        /*if (device_canvas_viewport_view.visibility == View.VISIBLE) {
            device_canvas_viewport_view.updateDeviceViewport(surface_view.interactiveCanvas)
        }*/
    }

    // paint qty listener
    override fun paintQtyChanged(qty: Int) {
        //drops_amt_text.text = qty.toString()
        activity?.runOnUiThread {
            paint_amt_info.text = qty.toString()
        }
    }

    // recent colors listener
    override fun onNewRecentColors(colors: Array<Int>) {
        if (SessionSettings.instance.selectedPaletteIndex == 0) {
            setupColorPalette(colors)
        }
    }

    // paint bar action listener
    override fun onPaintBarDoubleTapped() {
//        if (world) {
//            paintTextMode += 1
//            if (paintTextMode == 1) {
//                paintTextMode = -1
//            }
//
//            if (paintTextMode == paintTextModeTime) {
//                paint_time_info.visibility = View.VISIBLE
//                paint_time_info_container.visibility = View.VISIBLE
//
//                val layoutParams = paint_time_info_container.layoutParams as ConstraintLayout.LayoutParams
//                layoutParams.width = max((paint_time_info.paint.measureText(paint_time_info.text.toString()) + Utils.dpToPx(context, 10)).toInt(), Utils.dpToPx(context, 30))
//
//                paint_time_info_container.layoutParams = layoutParams
//
//                paint_amt_info.visibility = View.INVISIBLE
//            }
//            else if (paintTextMode == paintTextModeAmt) {
//                paint_amt_info.visibility = View.VISIBLE
//                paint_time_info_container.visibility = View.VISIBLE
//
//                val layoutParams = paint_time_info_container.layoutParams as ConstraintLayout.LayoutParams
//                layoutParams.width = max((paint_amt_info.paint.measureText(paint_amt_info.text.toString()) + Utils.dpToPx(context, 20)).toInt(), Utils.dpToPx(context, 30))
//
//                paint_time_info_container.layoutParams = layoutParams
//
//                paint_time_info.visibility = View.INVISIBLE
//            }
//            else if (paintTextMode == paintTextModeHide) {
//                paint_time_info.visibility = View.INVISIBLE
//                paint_time_info_container.visibility = View.INVISIBLE
//                paint_amt_info.visibility = View.INVISIBLE
//            }
//        }
    }

    // object selection listener
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

    // art export listener
    override fun onArtExported(pixelPositions: List<InteractiveCanvas.RestorePoint>) {
        toggleExportBorder(false)

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

    // art export fragment listener
    override fun onArtExportBack() {
        fragmentManager?.popBackStack()

        export_fragment_container.visibility = View.GONE
        surface_view.endExport()
    }

    // palettes fragment listener
    override fun onPaletteSelected(palette: Palette, index: Int) {
        palette_name_text.text = palette.name

        pixel_history_fragment_container.visibility = View.GONE

        syncPaletteAndColor()
    }

    override fun onPaletteDeleted(palette: Palette) {
        showPaletteUndoSnackbar(palette)
        if (palette.name == palette_name_text.text) {
            if (SessionSettings.instance.palettes.size > 0) {
                palette_name_text.text = SessionSettings.instance.palettes[0].name
            }
        }
    }

    // palette fragment helper
    private fun showPaletteUndoSnackbar(palette: Palette) {
        palettesFragment?.apply {
            val snackbar = Snackbar.make(requireView(), "Deleted ${palette.name} palette", Snackbar.LENGTH_LONG)
            snackbar.setAction("Undo") {
                undoDelete()
                this@InteractiveCanvasFragment.palette_name_text.text = SessionSettings.instance.palette.name
            }
            snackbar.show()
        }
    }

    private fun showPaletteColorUndoSnackbar(palette: Palette) {
        val snackbar = Snackbar.make(requireView(), "Removed color from ${palette.name} palette", Snackbar.LENGTH_LONG)
        snackbar.setAction("Undo") {
            undoPaletteColorRemove()
        }
        snackbar.show()
    }

    private fun undoPaletteColorRemove() {
        SessionSettings.instance.palette.colors.add(recentlyRemovedColorIndex, recentlyRemovedColor)
        syncPaletteAndColor()
    }

    fun syncPaletteAndColor() {
        if (SessionSettings.instance.selectedPaletteIndex == 0) {
            palette_add_color_button.visibility = View.GONE
            palette_remove_color_button.visibility = View.GONE

            setupColorPalette(surface_view.interactiveCanvas.recentColorsList.toTypedArray())
        }
        else {
            if (SessionSettings.instance.palette.colors.contains(SessionSettings.instance.paintColor)) {
                palette_add_color_button.visibility = View.GONE
                palette_remove_color_button.visibility = View.VISIBLE

            }
            else {
                palette_add_color_button.visibility = View.VISIBLE
                palette_remove_color_button.visibility = View.GONE
            }

            setupColorPalette(SessionSettings.instance.palette.colors.toTypedArray())
        }
    }

    // draw frame config listener
    override fun createDrawFrame(centerX: Int, centerY: Int, width: Int, height: Int, color: Int) {
        surface_view.createDrawFrame(centerX, centerY, width, height, color)

        closePopoverFragment()
    }

    private fun toggleCanvasSummary() {
        if (canvas_summary_container.visibility != View.VISIBLE) {
            if (System.currentTimeMillis() - lastCanvasSummaryImageTime > 1000 * 60 * 10) {
                Log.i("Canvas Summary", "Reloading canvas image")
                Glide.with(this)
                    .load("${server.serviceAltBaseUrl()}/canvas")
                    .signature(ObjectKey(System.currentTimeMillis().toString()))
                    .into(canvas_summary_view)

                lastCanvasSummaryImageTime = System.currentTimeMillis()
            }

            device_canvas_viewport_view.updateDeviceViewport(surface_view.interactiveCanvas)

            canvas_summary_container.visibility = View.VISIBLE
        }
        else {
            canvas_summary_container.visibility = View.INVISIBLE
        }
    }

    // canvas edge touch listener
    override fun onTouchCanvasEdge() {
        togglePaintPanel(true)
    }

    // device canvas viewport reset listener
    override fun resetDeviceCanvasViewport() {
        context?.apply {
            surface_view.interactiveCanvas.lastScaleFactor = surface_view.interactiveCanvas.startScaleFactor
            surface_view.scaleFactor = surface_view.interactiveCanvas.lastScaleFactor
            surface_view.interactiveCanvas.ppu = (surface_view.interactiveCanvas.basePpu * surface_view.scaleFactor ).toInt()

            surface_view.interactiveCanvas.updateDeviceViewport(
                this,
                surface_view.interactiveCanvas.rows / 2F, surface_view.interactiveCanvas.cols / 2F
            )
        }
    }

    private fun closePopoverFragment() {
        if (pixel_history_fragment_container.visibility == View.VISIBLE) {
            pixel_history_fragment_container.visibility = View.GONE
        }
    }

    // selected object view
    override fun showSelectedObjectYesAndNoButtons(screenPoint: Point) {
        selected_object_yes_button.actionBtnView = selected_object_yes_action
        selected_object_no_button.actionBtnView = selected_object_no_action

        selected_object_yes_action.type = ActionButtonView.Type.YES
        selected_object_yes_action.colorMode = ActionButtonView.ColorMode.COLOR

        selected_object_no_action.type = ActionButtonView.Type.NO
        selected_object_no_action.colorMode = ActionButtonView.ColorMode.COLOR

        selected_object_yes_no_container.x = (screenPoint.x - selected_object_yes_button.layoutParams.width - Utils.dpToPx(context, 5)).toFloat()
        selected_object_yes_no_container.y = (screenPoint.y - selected_object_yes_button.layoutParams.height / 2 - Utils.dpToPx(context, 5)).toFloat()

        selected_object_yes_button.setOnClickListener {
            surface_view.interactiveCanvas.endMoveSelection(true)
        }

        selected_object_no_button.setOnClickListener {
            surface_view.interactiveCanvas.endMoveSelection(false)
        }

        selected_object_yes_no_container.visibility = View.VISIBLE
    }

    override fun hideSelectedObjectYesAndNoButtons() {
        selected_object_yes_no_container.visibility = View.GONE
    }

    override fun selectedObjectEnded() {

    }

    // selected object move view
    override fun showSelectedObjectMoveButtons(bounds: Rect) {
        object_move_up_button.actionBtnView = object_move_up_action
        object_move_up_action.type = ActionButtonView.Type.SOLID
        object_move_up_button.visibility = View.VISIBLE

        object_move_down_button.actionBtnView = object_move_down_action
        object_move_down_action.type = ActionButtonView.Type.SOLID
        object_move_down_button.visibility = View.VISIBLE

        object_move_left_button.actionBtnView = object_move_left_action
        object_move_left_action.type = ActionButtonView.Type.SOLID
        object_move_left_button.visibility = View.VISIBLE

        object_move_right_button.actionBtnView = object_move_right_action
        object_move_right_action.type = ActionButtonView.Type.SOLID
        object_move_right_button.visibility = View.VISIBLE

        object_move_up_button.setOnClickListener {
            surface_view.interactiveCanvas.moveSelection(InteractiveCanvas.Direction.UP)
        }
        object_move_down_button.setOnClickListener {
            surface_view.interactiveCanvas.moveSelection(InteractiveCanvas.Direction.DOWN)
        }
        object_move_left_button.setOnClickListener {
            surface_view.interactiveCanvas.moveSelection(InteractiveCanvas.Direction.LEFT)
        }
        object_move_right_button.setOnClickListener {
            surface_view.interactiveCanvas.moveSelection(InteractiveCanvas.Direction.RIGHT)
        }

        val cX = (bounds.right + bounds.left) / 2
        val cY = (bounds.bottom + bounds.top) / 2

        object_move_up_button.x = (cX - object_move_up_button.layoutParams.width / 2).toFloat()
        object_move_up_button.y = (bounds.top - object_move_up_button.layoutParams.height - Utils.dpToPx(context, 20)).toFloat()

        object_move_down_button.x = (cX - object_move_down_button.layoutParams.width / 2).toFloat()
        object_move_down_button.y = (bounds.bottom + Utils.dpToPx(context, 20)).toFloat()

        object_move_left_button.x = (bounds.left - Utils.dpToPx(context, 20) - object_move_left_button.layoutParams.width).toFloat()
        object_move_left_button.y = (cY - object_move_left_button.layoutParams.height / 2).toFloat()

        object_move_right_button.x = (bounds.right + Utils.dpToPx(context, 20)).toFloat()
        object_move_right_button.y = (cY - object_move_left_button.layoutParams.height / 2).toFloat()
    }

    override fun updateSelectedObjectMoveButtons(bounds: Rect) {
        val cX = (bounds.right + bounds.left) / 2
        val cY = (bounds.bottom + bounds.top) / 2

        object_move_up_button.x = (cX - object_move_up_button.layoutParams.width / 2).toFloat()
        object_move_up_button.y = (bounds.top - object_move_up_button.layoutParams.height - Utils.dpToPx(context, 20)).toFloat()

        object_move_down_button.x = (cX - object_move_down_button.layoutParams.width / 2).toFloat()
        object_move_down_button.y = (bounds.bottom + Utils.dpToPx(context, 20)).toFloat()

        object_move_left_button.x = (bounds.left - Utils.dpToPx(context, 20) - object_move_left_button.layoutParams.width).toFloat()
        object_move_left_button.y = (cY - object_move_left_button.layoutParams.height / 2).toFloat()

        object_move_right_button.x = (bounds.right + Utils.dpToPx(context, 20)).toFloat()
        object_move_right_button.y = (cY - object_move_left_button.layoutParams.height / 2).toFloat()
    }

    override fun hideSelectedObjectMoveButtons() {
        object_move_up_button.visibility = View.GONE
        object_move_down_button.visibility = View.GONE
        object_move_left_button.visibility = View.GONE
        object_move_right_button.visibility = View.GONE

        selected_object_yes_no_container.visibility = View.GONE
    }

    override fun selectedObjectMoveEnded() {
        toggleExportBorder(false)
    }

    // menu card listener
    override fun moveMenuCardBy(x: Float, y: Float) {
        menu_container.x += x
        menu_container.y += y

        view?.apply {
            if (menu_container.x + menu_container.width > width) {
                menu_container.x = (width - menu_container.width).toFloat()
            }
            if (menu_container.x < 0) {
                menu_container.x = 0F
            }
            if (menu_container.y + menu_container.height > height) {
                menu_container.y = (height - menu_container.height).toFloat()
            }
            if (menu_container.y < 0) {
                menu_container.y = 0F
            }
        }
    }

    override fun closeMenu() {
        menu_container.visibility = View.GONE
    }

    // world API
    private fun sendApiStatusCheck() {
        val requestQueue = Volley.newRequestQueue(context)
        val request = object: JsonObjectRequest(
            Request.Method.GET,
            "api/v1/status",
            null,
            { response ->
                (context as Activity?)?.runOnUiThread {
                    //sendSocketStatusCheck()
                }
            },
            { error ->
                (context as Activity?)?.runOnUiThread {
                    showDisconnectedMessage(1)
                }
            }) {

            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = "application/json; charset=utf-8"
                headers["key1"] = Utils.key1
                return headers
            }
        }

        request.retryPolicy = DefaultRetryPolicy(20000, 1, 1.5f)
        requestQueue.add(request)
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

    private fun setupPaintEventTimer() {
        paintEventTimer = Timer()
        paintEventTimer?.schedule(object : TimerTask() {
            override fun run() {
                activity?.runOnUiThread {
//                    if (System.currentTimeMillis() > SessionSettings.instance.nextPaintTime) {
//                        SessionSettings.instance.nextPaintTime =
//                            System.currentTimeMillis() + 300 * 1000
//                    }

                    if (SessionSettings.instance.nextPaintTime == 0L) {
                        paint_time_info.text = String.format("< %d min", SessionSettings.instance.addPaintInterval)
                        return@runOnUiThread
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

                            if (paint_time_info.visibility == View.VISIBLE) {
                                val layoutParams = paint_time_info_container.layoutParams as ConstraintLayout.LayoutParams
                                layoutParams.width = (paint_time_info.paint.measureText(paint_time_info.text.toString()) + Utils.dpToPx(context, 10)).toInt()

                                paint_time_info_container.layoutParams = layoutParams
                            }

                        } catch (ex: IllegalStateException) {

                        }
                    }
                }
            }
        }, 0, 1000)
    }

    // socket callback
    override fun onSocketConnect() {
        Log.i("Canvas Socket", "Socket connected!")
        canvasService.getRecentPixels(pauseTime) { pixels ->
            pixels?.also {
                for (element in it) {
                    val pixelInfo = element.asString
                    surface_view?.interactiveCanvas?.receivePixel(pixelInfo)
                }
            }
        }

        SessionSettings.instance.uniqueId?.also { uuid ->
            canvasService.getPaintQty(uuid) { paintQtyInfo ->
                paintQtyInfo?.also {
                    val paintQty = it.get("paint_qty").asInt
                    Log.i("Canvas Service", "Paint qty = $paintQty")
                    SessionSettings.instance.dropsAmt = paintQty
                    reload_transparent?.visibility = View.GONE
                }
            }
        }

        surface_view.interactiveCanvas
            .registerForSocketEvents(InteractiveCanvasSocket.instance.requireSocket())

        showNoSocket(false)
    }

    override fun onSocketDisconnect(error: Boolean) {
        Log.i("Canvas Socket", "Socket disconnect.")
        showNoSocket()

        if (error) {
            activity?.runOnUiThread {
                (activity as? InteractiveCanvasActivity)?.showMenuFragment()
            }
        }
    }

    private fun reconnectToSocket() {
        InteractiveCanvasSocket.instance.startSocket(server)
    }

    private fun scheduleReconnect() {
        val rSec = (Math.random() * 25 + 5).toLong()
        Log.i("Canvas Socket", "Scheduling reconnect in $rSec seconds")
        Observable.timer(rSec, TimeUnit.SECONDS).subscribe()
    }

    private fun applyOptions() {
        // panel background
        setPanelBackground()

        // panel theme config
        panelThemeConfig = PanelThemeConfig.buildConfig(SessionSettings.instance.panelResIds[SessionSettings.instance.panelBackgroundResIndex])

        if (SessionSettings.instance.closePaintBackButtonColor != -1) {
            close_paint_panel.color = SessionSettings.instance.closePaintBackButtonColor
        }
        else {
            close_paint_panel.color = panelThemeConfig.actionButtonColor
        }

        if (panelThemeConfig.actionButtonColor == Color.BLACK) {
            palette_name_text.setTextColor(Color.parseColor("#FF111111"))
            palette_name_text.setShadowLayer(3F, 2F, 2F, Color.parseColor("#7F333333"))

            paint_color_accept.color = Color.BLACK

            palette_add_color_action.colorMode = ActionButtonView.ColorMode.BLACK
            palette_remove_color_action.colorMode = ActionButtonView.ColorMode.BLACK

            paint_yes.color = Color.BLACK
            paint_no.color = Color.BLACK

            lock_paint_panel_action.colorMode = ActionButtonView.ColorMode.BLACK
        }
        else {
            palette_name_text.setTextColor(Color.WHITE)

            paint_color_accept.color = Color.WHITE

            palette_add_color_action.colorMode = ActionButtonView.ColorMode.WHITE
            palette_remove_color_action.colorMode = ActionButtonView.ColorMode.WHITE

            paint_yes.color = Color.WHITE
            paint_no.color = Color.WHITE

            lock_paint_panel_action.colorMode = ActionButtonView.ColorMode.WHITE
        }

        if (panelThemeConfig.inversePaintEventInfo) {
            paint_time_info_container.setBackgroundResource(R.drawable.timer_text_background_inverse)
            paint_time_info.setTextColor(ActionButtonView.blackPaint.color)
            paint_amt_info.setTextColor(ActionButtonView.blackPaint.color)
        }

        surface_view.interactiveCanvas.interactiveCanvasDrawer?.notifyRedraw()

        if (SessionSettings.instance.showPaintBar) {
            surface_view.paintActionListener = paint_qty_bar
            SessionSettings.instance.paintQtyListeners.add(paint_qty_bar)

            paint_qty_circle.visibility = View.GONE
        }
        else if (SessionSettings.instance.showPaintCircle) {
            surface_view.paintActionListener = paint_qty_circle
            SessionSettings.instance.paintQtyListeners.add(paint_qty_circle)

            paint_qty_circle.visibility = View.VISIBLE
            paint_qty_bar.visibility = View.GONE
        }
        else {
            paint_qty_bar.visibility = View.VISIBLE
            paint_qty_circle.visibility = View.GONE
        }

        if (!world) {
            paint_qty_bar.visibility = View.GONE
            paint_qty_circle.visibility = View.GONE
        }

        paint_qty_bar.invalidate()
        paint_qty_circle.invalidate()
    }

    fun closeOptions(fragment: OptionsFragment) {
        applyOptions()

        childFragmentManager
            .beginTransaction()
            .remove(fragment)
            .commit()

        //onViewCreated(requireView(), null)
    }

    private fun showNoSocket(show: Boolean = true) {
        activity?.runOnUiThread {
            if (show) {
                image_no_socket?.visibility = View.VISIBLE
            }
            else {
                image_no_socket?.visibility = View.GONE
            }
        }
    }
}