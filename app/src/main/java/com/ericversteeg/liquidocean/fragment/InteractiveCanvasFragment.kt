package com.ericversteeg.liquidocean.fragment

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.ericversteeg.liquidocean.R
import com.ericversteeg.liquidocean.activity.InteractiveCanvasActivity
import com.ericversteeg.liquidocean.databinding.FragmentInteractiveCanvasBinding
import com.ericversteeg.liquidocean.helper.Animator
import com.ericversteeg.liquidocean.helper.PanelThemeConfig
import com.ericversteeg.liquidocean.helper.Utils
import com.ericversteeg.liquidocean.listener.ArtExportFragmentListener
import com.ericversteeg.liquidocean.listener.ArtExportListener
import com.ericversteeg.liquidocean.listener.CanvasEdgeTouchListener
import com.ericversteeg.liquidocean.listener.DeviceCanvasViewportResetListener
import com.ericversteeg.liquidocean.listener.DrawFrameConfigFragmentListener
import com.ericversteeg.liquidocean.listener.InteractiveCanvasFragmentListener
import com.ericversteeg.liquidocean.listener.InteractiveCanvasGestureListener
import com.ericversteeg.liquidocean.listener.InteractiveCanvasListener
import com.ericversteeg.liquidocean.listener.LongPressListener
import com.ericversteeg.liquidocean.listener.MenuCardListener
import com.ericversteeg.liquidocean.listener.ObjectSelectionListener
import com.ericversteeg.liquidocean.listener.PaintBarActionListener
import com.ericversteeg.liquidocean.listener.PaintQtyListener
import com.ericversteeg.liquidocean.listener.PalettesFragmentListener
import com.ericversteeg.liquidocean.listener.PixelHistoryCallback
import com.ericversteeg.liquidocean.listener.PixelHistoryListener
import com.ericversteeg.liquidocean.listener.RecentColorsListener
import com.ericversteeg.liquidocean.listener.SelectedObjectMoveView
import com.ericversteeg.liquidocean.listener.SelectedObjectView
import com.ericversteeg.liquidocean.listener.SocketStatusCallback
import com.ericversteeg.liquidocean.model.InteractiveCanvas
import com.ericversteeg.liquidocean.model.InteractiveCanvasSocket
import com.ericversteeg.liquidocean.model.Palette
import com.ericversteeg.liquidocean.model.SessionSettings
import com.ericversteeg.liquidocean.model.StatTracker
import com.ericversteeg.liquidocean.view.ActionButtonView
import com.ericversteeg.liquidocean.view.PaintColorIndicator
import com.google.android.material.snackbar.Snackbar
import org.json.JSONArray
import top.defaults.colorpicker.ColorObserver
import java.util.Timer
import java.util.TimerTask
import kotlin.math.max


class InteractiveCanvasFragment : Fragment(), InteractiveCanvasListener, PaintQtyListener,
    RecentColorsListener, SocketStatusCallback, PaintBarActionListener, PixelHistoryListener,
    InteractiveCanvasGestureListener, ArtExportListener, ArtExportFragmentListener, ObjectSelectionListener,
    PalettesFragmentListener, DrawFrameConfigFragmentListener, CanvasEdgeTouchListener, DeviceCanvasViewportResetListener,
    SelectedObjectMoveView, SelectedObjectView, MenuCardListener {

    var initalColor = 0

    var world = false
    var realmId = 0

    var interactiveCanvasFragmentListener: InteractiveCanvasFragmentListener? = null

    var paintEventTimer: Timer? = null

    val firstInfoTapFixYOffset = 0
    var firstInfoTap = true

    var toolboxOpen = false

    lateinit var panelThemeConfig: PanelThemeConfig

    var paintTextMode = 2

    val paintTextModeTime = 0
    val paintTextModeAmt = 1
    var paintTextModeHide = 2

    var animatingTools = false

    var palettesFragment: PalettesFragment? = null

    var recentlyRemovedColor = 0
    var recentlyRemovedColorIndex = 0

    var menuFragment: MenuFragment? = null

    var terminalFragment: TerminalFragment? = null

    lateinit var visibleActionViews: Array<ActionButtonView>
    
    private var _binding: FragmentInteractiveCanvasBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInteractiveCanvasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // setup views
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        SessionSettings.instance.canvasOpen = true

        context?.apply {
            SessionSettings.instance.tablet = Utils.isTablet(this)
        }

        // must call before darkIcons
        binding.surfaceView.interactiveCanvas.realmId = realmId
        binding.surfaceView.interactiveCanvas.world = world

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
                    binding.surfaceView.interactiveCanvas.getGridLineColor()
                )
            }
            else {
                SessionSettings.instance.paintColor = SessionSettings.instance.getSharedPrefs(this).getInt(
                    "last_single_paint_color",
                    binding.surfaceView.interactiveCanvas.getGridLineColor()
                )
            }
        }

        panelThemeConfig = PanelThemeConfig.buildConfig(SessionSettings.instance.panelResIds[SessionSettings.instance.panelBackgroundResIndex])

        // listeners
        binding.surfaceView.pixelHistoryListener = this
        binding.surfaceView.gestureListener = this
        binding.surfaceView.objectSelectionListener = this
        binding.surfaceView.selectedObjectMoveView = this
        binding.surfaceView.selectedObjectView = this
        binding.surfaceView.canvasEdgeTouchListener = this

        binding.surfaceView.interactiveCanvas.interactiveCanvasListener = this
        binding.surfaceView.interactiveCanvas.recentColorsListener = this
        binding.surfaceView.interactiveCanvas.artExportListener = this
        binding.surfaceView.interactiveCanvas.deviceCanvasViewportResetListener = this

        InteractiveCanvasSocket.instance.socketStatusCallback = this

        binding.paintQtyBar.actionListener = this
        binding.paintQtyCircle.actionListener = this

        // palette
        binding.paletteNameText.setOnClickListener {
            showPalettesFragmentPopover()
        }

        binding.paletteNameText.text = SessionSettings.instance.palette.name

        binding.paletteAddColorAction.type = ActionButtonView.Type.ADD
        binding.paletteAddColorButton.actionBtnView = binding.paletteAddColorAction

        binding.paletteAddColorButton.setOnClickListener {
            if (SessionSettings.instance.palette.colors.size < Palette.maxColors) {
                SessionSettings.instance.palette.addColor(SessionSettings.instance.paintColor)
                syncPaletteAndColor()
            }
            else {
                Toast.makeText(context, "${SessionSettings.instance.palette.name} is full", Toast.LENGTH_SHORT).show()
            }
        }

        binding.paletteRemoveColorAction.type = ActionButtonView.Type.REMOVE
        binding.paletteRemoveColorButton.actionBtnView = binding.paletteRemoveColorAction

        binding.paletteRemoveColorButton.setOnClickListener {
            showPaletteColorRemovePrompt(SessionSettings.instance.paintColor)
        }

        syncPaletteAndColor()

        if (SessionSettings.instance.showPaintBar) {
            binding.surfaceView.paintActionListener = binding.paintQtyBar
            SessionSettings.instance.paintQtyListeners.add(binding.paintQtyBar)

            binding.paintQtyCircle.visibility = View.GONE
        }
        else if (SessionSettings.instance.showPaintCircle) {
            binding.surfaceView.paintActionListener = binding.paintQtyCircle
            SessionSettings.instance.paintQtyListeners.add(binding.paintQtyCircle)

            binding.paintQtyBar.visibility = View.GONE
        }
        else {
            binding.paintQtyBar.visibility = View.GONE
            binding.paintQtyCircle.visibility = View.GONE
        }

        if (!world) {
            binding.paintQtyBar.visibility = View.GONE
            binding.paintQtyCircle.visibility = View.GONE
        }

        // binding.paintQtyBar.world = world

        binding.pixelHistoryFragmentContainer.x = 0F
        binding.pixelHistoryFragmentContainer.y = 0F

        binding.menuButton.actionBtnView = binding.menuAction
        binding.menuAction.type = ActionButtonView.Type.MENU

        // paint panel
        binding.paintAmtInfo.text = SessionSettings.instance.dropsAmt.toString()

        binding.paintPanelButton.actionBtnView = binding.paintPanelActionView
        binding.paintPanelActionView.type = ActionButtonView.Type.PAINT

        binding.paintYesBottomLayer.isStatic = true
        binding.paintYesBottomLayer.type = ActionButtonView.Type.YES
        binding.paintYesBottomLayer.colorMode = ActionButtonView.ColorMode.COLOR

        binding.paintYesTopLayer.type = ActionButtonView.Type.YES
        binding.paintYesTopLayer.colorMode = ActionButtonView.ColorMode.COLOR
        binding.paintYesTopLayer.topLayer = true

        binding.paintYes.actionBtnView = binding.paintYesTopLayer

        binding.paintNoBottomLayer.isStatic = true
        binding.paintNoBottomLayer.type = ActionButtonView.Type.NO
        binding.paintNoBottomLayer.colorMode = ActionButtonView.ColorMode.COLOR

        binding.paintNoTopLayer.type = ActionButtonView.Type.NO
        binding.paintNoTopLayer.colorMode = ActionButtonView.ColorMode.COLOR
        binding.paintNoTopLayer.topLayer = true

        binding.paintNo.actionBtnView = binding.paintNoTopLayer

        binding.closePaintPanelBottomLayer.isStatic = true
        binding.closePaintPanelBottomLayer.type = ActionButtonView.Type.PAINT_CLOSE

        binding.closePaintPanelTopLayer.type = ActionButtonView.Type.PAINT_CLOSE
        binding.closePaintPanelTopLayer.topLayer = true

        binding.closePaintPanel.actionBtnView = binding.closePaintPanelTopLayer

        if (SessionSettings.instance.lockPaintPanel) {
            binding.lockPaintPanelAction.type = ActionButtonView.Type.LOCK_CLOSE
        }
        else {
            binding.lockPaintPanelAction.type = ActionButtonView.Type.LOCK_OPEN
        }
        binding.lockPaintPanel.actionBtnView = binding.lockPaintPanelAction

        binding.paintIndicatorViewBottomLayer.panelThemeConfig = panelThemeConfig
        binding.paintIndicatorView.topLayer = true

        binding.paintColorAccept.actionBtnView = binding.paintColorAcceptImageTopLayer

        binding.paintColorAcceptImageBottomLayer.type = binding.paintColorAcceptImageTopLayer.type
        binding.paintColorAcceptImageBottomLayer.isStatic = true

        binding.paintColorAcceptImageTopLayer.topLayer = true
        binding.paintColorAcceptImageTopLayer.touchStateListener = binding.paintIndicatorView
        binding.paintColorAcceptImageTopLayer.hideOnTouchEnd = true

        togglePaintPanel(SessionSettings.instance.paintPanelOpen)

        // toolbox
        binding.exportAction.type = ActionButtonView.Type.EXPORT
        binding.exportButton.actionBtnView = binding.exportAction

        binding.backgroundAction.type = ActionButtonView.Type.CHANGE_BACKGROUND
        binding.backgroundButton.actionBtnView = binding.backgroundAction

        binding.gridLinesAction.type = ActionButtonView.Type.GRID_LINES
        binding.gridLinesButton.actionBtnView = binding.gridLinesAction

        binding.canvasSummaryAction.type = ActionButtonView.Type.CANVAS_SUMMARY
        binding.canvasSummaryButton.actionBtnView = binding.canvasSummaryAction

        binding.openToolsAction.type = ActionButtonView.Type.DOT
        binding.openToolsButton.actionBtnView = binding.openToolsAction

        // open toolbox
        toggleTools(SessionSettings.instance.toolboxOpen)

        // recent colors
        binding.recentColorsAction.type = ActionButtonView.Type.DOT
        binding.recentColorsButton.actionBtnView = binding.recentColorsAction

        if (SessionSettings.instance.selectedPaletteIndex == 0) {
            setupColorPalette(binding.surfaceView.interactiveCanvas.recentColorsList.toTypedArray())
        }

        // bold action buttons
        if (SessionSettings.instance.boldActionButtons) {
            binding.menuAction.toggleState = ActionButtonView.ToggleState.SINGLE
            binding.paintPanelActionView.toggleState = ActionButtonView.ToggleState.SINGLE
            binding.exportAction.exportBold = true
            binding.backgroundAction.toggleState = ActionButtonView.ToggleState.SINGLE
            binding.gridLinesAction.toggleState = ActionButtonView.ToggleState.SINGLE
            binding.canvasSummaryAction.toggleState = ActionButtonView.ToggleState.SINGLE
            binding.openToolsAction.toggleState = ActionButtonView.ToggleState.SINGLE
            binding.recentColorsAction.toggleState = ActionButtonView.ToggleState.SINGLE
        }

        // panel theme config
        visibleActionViews = arrayOf(binding.menuAction, binding.paintPanelActionView, binding.exportAction, binding.backgroundAction,
        binding.gridLinesAction, binding.canvasSummaryAction, binding.openToolsAction, binding.recentColorsAction)

        binding.menuAction.autoInvalidate = false
        binding.paintPanelActionView.autoInvalidate = false
        binding.exportAction.autoInvalidate = false
        binding.backgroundAction.autoInvalidate = false
        binding.gridLinesAction.autoInvalidate = false
        binding.canvasSummaryAction.autoInvalidate = false
        binding.openToolsAction.autoInvalidate = false
        binding.recentColorsAction.autoInvalidate = false

        recolorVisibleActionViews()

        if (SessionSettings.instance.closePaintBackButtonColor != -1) {
            binding.closePaintPanelBottomLayer.colorMode = ActionButtonView.ColorMode.COLOR
            binding.closePaintPanelTopLayer.colorMode = ActionButtonView.ColorMode.COLOR
        }
        else if (panelThemeConfig.actionButtonColor == Color.BLACK) {
            binding.closePaintPanelBottomLayer.colorMode = ActionButtonView.ColorMode.BLACK
            binding.closePaintPanelTopLayer.colorMode = ActionButtonView.ColorMode.BLACK
        }
        else {
            binding.closePaintPanelBottomLayer.colorMode = ActionButtonView.ColorMode.WHITE
            binding.closePaintPanelTopLayer.colorMode = ActionButtonView.ColorMode.WHITE
        }

        binding.paintColorAcceptImageTopLayer.type = ActionButtonView.Type.YES

        if (panelThemeConfig.actionButtonColor == Color.BLACK) {
            binding.paletteNameText.setTextColor(Color.parseColor("#FF111111"))
            binding.paletteNameText.setShadowLayer(3F, 2F, 2F, Color.parseColor("#7F333333"))

            binding.paintColorAcceptImageBottomLayer.colorMode = ActionButtonView.ColorMode.BLACK
            binding.paintColorAcceptImageTopLayer.colorMode = ActionButtonView.ColorMode.BLACK

            binding.paletteAddColorAction.colorMode = ActionButtonView.ColorMode.BLACK
            binding.paletteRemoveColorAction.colorMode = ActionButtonView.ColorMode.BLACK

            binding.lockPaintPanelAction.colorMode = ActionButtonView.ColorMode.BLACK
        }
        else {
            binding.paletteNameText.setTextColor(Color.WHITE)

            binding.paintColorAcceptImageBottomLayer.colorMode = ActionButtonView.ColorMode.WHITE
            binding.paintColorAcceptImageTopLayer.colorMode = ActionButtonView.ColorMode.WHITE

            binding.paletteAddColorAction.colorMode = ActionButtonView.ColorMode.WHITE
            binding.paletteRemoveColorAction.colorMode = ActionButtonView.ColorMode.WHITE

            binding.lockPaintPanelAction.colorMode = ActionButtonView.ColorMode.WHITE
        }

        if (panelThemeConfig.actionButtonColor == ActionButtonView.blackPaint.color) {
            binding.paintColorAcceptImageBottomLayer.colorMode = ActionButtonView.ColorMode.BLACK
            binding.paintColorAcceptImageTopLayer.colorMode = ActionButtonView.ColorMode.BLACK
        }

        if (panelThemeConfig.inversePaintEventInfo) {
            binding.paintTimeInfoContainer.setBackgroundResource(R.drawable.timer_text_background_inverse)
            binding.paintTimeInfo.setTextColor(ActionButtonView.blackPaint.color)
            binding.paintAmtInfo.setTextColor(ActionButtonView.blackPaint.color)
        }

        binding.paintQtyBar.panelThemeConfig = panelThemeConfig
        binding.paintQtyCircle.panelThemeConfig = panelThemeConfig
        binding.paintIndicatorView.panelThemeConfig = panelThemeConfig

        // color picker view
        binding.colorPickerView.setSelectorColor(Color.WHITE)

        binding.defaultBlackColorAction.type = ActionButtonView.Type.BLACK_COLOR_DEFAULT
        binding.defaultBlackColorButton.actionBtnView = binding.defaultBlackColorAction

        binding.defaultBlackColorButton.setOnClickListener {
            binding.colorPickerView.setInitialColor(ActionButtonView.blackPaint.color)
        }

        binding.defaultWhiteColorAction.type = ActionButtonView.Type.WHITE_COLOR_DEFAULT
        binding.defaultWhiteColorButton.actionBtnView = binding.defaultWhiteColorAction

        binding.defaultWhiteColorButton.setOnClickListener {
            binding.colorPickerView.setInitialColor(ActionButtonView.whitePaint.color)
        }

        val textChangeListener = object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                try {
                    val color = Color.parseColor("#$s")
                    binding.colorPickerView.setInitialColor(color)

                    hideKeyboard()
                }
                catch (exception: Exception) {

                }
            }

            override fun afterTextChanged(s: Editable?) {

            }
        }

        binding.colorHexStringInput.addTextChangedListener(textChangeListener)

        binding.colorHexStringInput.setOnEditorActionListener { textView, actionId, keyEvent ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard()
            }
            true
        }

        binding.colorPickerView.setEnabledAlpha(false)

        binding.colorPickerView.subscribe(object : ColorObserver {
            override fun onColor(color: Int, fromUser: Boolean, shouldPropagate: Boolean) {
                binding.paintIndicatorViewBottomLayer.setPaintColor(color)

                if (PaintColorIndicator.isColorLight(color) && panelThemeConfig.actionButtonColor == Color.WHITE) {
                    binding.paintColorAcceptImageTopLayer.colorMode = ActionButtonView.ColorMode.BLACK
                    binding.paintColorAcceptImageBottomLayer.colorMode = ActionButtonView.ColorMode.BLACK
                }
                else if (panelThemeConfig.actionButtonColor == Color.WHITE) {
                    binding.paintColorAcceptImageTopLayer.colorMode = ActionButtonView.ColorMode.WHITE
                    binding.paintColorAcceptImageBottomLayer.colorMode = ActionButtonView.ColorMode.WHITE
                }
                else if (PaintColorIndicator.isColorDark(color) && panelThemeConfig.actionButtonColor == Color.BLACK) {
                    binding.paintColorAcceptImageTopLayer.colorMode = ActionButtonView.ColorMode.WHITE
                    binding.paintColorAcceptImageBottomLayer.colorMode = ActionButtonView.ColorMode.WHITE
                }
                else if (panelThemeConfig.actionButtonColor == Color.BLACK) {
                    binding.paintColorAcceptImageTopLayer.colorMode = ActionButtonView.ColorMode.BLACK
                    binding.paintColorAcceptImageBottomLayer.colorMode = ActionButtonView.ColorMode.BLACK
                }

                binding.colorHexStringInput.removeTextChangedListener(textChangeListener)

                val hexColor = java.lang.String.format("%06X", 0xFFFFFF and color)
                binding.colorHexStringInput.setText(hexColor)

                binding.colorHexStringInput.addTextChangedListener(textChangeListener)

                // palette color actions
                syncPaletteAndColor()
            }
        })

        // button clicks
        binding.paintPanel.setOnClickListener {
            closePopoverFragment()
        }

        // paint buttons
        binding.paintPanelButton.setOnClickListener {
            togglePaintPanel(true)
        }

        binding.paintYes.setOnClickListener {
            binding.surfaceView.endPainting(true)

            binding.paintYesContainer.visibility = View.GONE
            binding.paintNoContainer.visibility = View.GONE
            binding.closePaintPanelContainer.visibility = View.VISIBLE

            binding.paintYesTopLayer.touchState = ActionButtonView.TouchState.INACTIVE
            binding.paintYes.invalidate()

            binding.surfaceView.startPainting()
        }

        binding.paintNo.setOnClickListener {
            if (binding.colorPickerFrame.visibility == View.VISIBLE) {
                binding.paintIndicatorViewBottomLayer.setPaintColor(initalColor)
                syncPaletteAndColor()

                binding.colorPickerFrame.visibility = View.GONE

                if (SessionSettings.instance.canvasLockBorder) {
                    binding.paintWarningFrame.visibility = View.VISIBLE
                }

                binding.paintYes.visibility = View.VISIBLE

                binding.paintColorAccept.visibility = View.GONE

                //binding.recentColorsButton.visibility = View.VISIBLE
                //binding.recentColorsContainer.visibility = View.GONE

                binding.surfaceView.endPaintSelection()

                binding.paintNoBottomLayer.colorMode = ActionButtonView.ColorMode.COLOR
                binding.paintNoTopLayer.colorMode = ActionButtonView.ColorMode.COLOR

                if (binding.surfaceView.interactiveCanvas.restorePoints.size == 0) {
                    binding.paintYesContainer.visibility = View.GONE
                    binding.paintNoContainer.visibility = View.GONE
                    binding.closePaintPanelContainer.visibility = View.VISIBLE
                }
                else {
                    binding.paintYesContainer.visibility = View.VISIBLE
                    binding.paintNoContainer.visibility = View.VISIBLE
                    binding.closePaintPanelContainer.visibility = View.GONE
                }
            }
            else {
                binding.surfaceView.endPainting(false)

                binding.paintYesContainer.visibility = View.GONE
                binding.paintNoContainer.visibility = View.GONE
                binding.closePaintPanelContainer.visibility = View.VISIBLE

                //binding.recentColorsButton.visibility = View.VISIBLE
                //binding.recentColorsContainer.visibility = View.GONE

                binding.surfaceView.startPainting()
            }
        }

        binding.closePaintPanel.setOnClickListener {
            togglePaintPanel(false)
            closePopoverFragment()
        }

        binding.lockPaintPanel.setOnClickListener {
            SessionSettings.instance.lockPaintPanel = !SessionSettings.instance.lockPaintPanel

            if (SessionSettings.instance.lockPaintPanel) {
                binding.lockPaintPanelAction.type = ActionButtonView.Type.LOCK_CLOSE
            }
            else {
                binding.lockPaintPanelAction.type = ActionButtonView.Type.LOCK_OPEN
            }
        }

        binding.paintIndicatorView.setOnClickListener {
            // start color selection mode
            if (binding.colorPickerFrame.visibility != View.VISIBLE) {
                binding.colorPickerFrame.visibility = View.VISIBLE
                initalColor = SessionSettings.instance.paintColor
                binding.colorPickerView.setInitialColor(initalColor)

                binding.paintWarningFrame.visibility = View.GONE

                binding.paintColorAccept.visibility = View.VISIBLE

                binding.paintYesContainer.visibility = View.GONE
                binding.closePaintPanelContainer.visibility = View.GONE
                binding.paintNoContainer.visibility = View.VISIBLE

                if (panelThemeConfig.actionButtonColor == Color.BLACK) {
                    binding.paintNoBottomLayer.colorMode = ActionButtonView.ColorMode.BLACK
                    binding.paintNoTopLayer.colorMode = ActionButtonView.ColorMode.BLACK
                }
                else {
                    binding.paintNoBottomLayer.colorMode = ActionButtonView.ColorMode.WHITE
                    binding.paintNoTopLayer.colorMode = ActionButtonView.ColorMode.WHITE
                }

                //binding.recentColorsButton.visibility = View.GONE
                //binding.recentColorsContainer.visibility = View.GONE

                binding.surfaceView.startPaintSelection()
            }
        }

        binding.paintColorAccept.setOnClickListener {
            binding.colorPickerFrame.visibility = View.GONE

            if (SessionSettings.instance.canvasLockBorder) {
                binding.paintWarningFrame.visibility = View.VISIBLE
            }

            binding.paintYes.visibility = View.VISIBLE

            binding.paintColorAccept.visibility = View.GONE

            binding.surfaceView.endPaintSelection()

            binding.paintNoBottomLayer.colorMode = ActionButtonView.ColorMode.COLOR
            binding.paintNoTopLayer.colorMode = ActionButtonView.ColorMode.COLOR

            if (binding.surfaceView.interactiveCanvas.restorePoints.size == 0) {
                binding.paintYesContainer.visibility = View.GONE
                binding.paintNoContainer.visibility = View.GONE
                binding.closePaintPanelContainer.visibility = View.VISIBLE
            }
            else {
                binding.paintYesContainer.visibility = View.VISIBLE
                binding.paintNoContainer.visibility = View.VISIBLE
                binding.closePaintPanelContainer.visibility = View.GONE
            }
        }

        // to stop click-through to the canvas behind
        binding.colorPickerFrame.setOnClickListener {
            
        }

        // recent colors
        binding.recentColorsButton.setOnClickListener {
            if (binding.recentColorsContainer.visibility != View.VISIBLE) {
                binding.recentColorsContainer.visibility = View.VISIBLE
                binding.recentColorsAction.visibility = View.INVISIBLE

                if (binding.paintPanel.visibility != View.VISIBLE) {
                    togglePaintPanel(true)
                }

                if (binding.canvasSummaryView.visibility == View.VISIBLE) {
                    binding.canvasSummaryContainer.visibility = View.INVISIBLE
                }
            }
            else {
                binding.recentColorsContainer.visibility = View.GONE
                binding.recentColorsAction.visibility = View.VISIBLE
            }
        }

        // menu button
        if (!SessionSettings.instance.selectedHand) {
            toggleMenu(true)
        }

        binding.menuButton.setOnClickListener {
            if (binding.surfaceView.isExporting()) {
                binding.exportFragmentContainer.visibility = View.INVISIBLE
                binding.surfaceView.endExport()

                toggleExportBorder(false)

                // binding.exportButton.background = ResourcesCompat.getDrawable(resources, R.drawable.ic_share, null)
            }
            else if (binding.surfaceView.isObjectMoveSelection()) {
                binding.surfaceView.interactiveCanvas.cancelMoveSelectedObject()
                toggleExportBorder(false, double = true)

                binding.surfaceView.startExport()
                toggleExportBorder(true)
            }
            else if (binding.surfaceView.isObjectMoving()) {
                binding.surfaceView.interactiveCanvas.cancelMoveSelectedObject()
                toggleExportBorder(false)
            }
            else if (binding.terminalContainer.visibility == View.VISIBLE) {
                toggleTerminal(false)
            }
            else {
                toggleMenu(binding.menuContainer.visibility != View.VISIBLE)
            }
        }

        activity?.apply {
            binding.menuButton.setLongPressActionListener(this, object: LongPressListener {
                override fun onLongPress() {
                    toggleTerminal(true)
                }
            })
        }

        // export button
        binding.exportButton.setOnClickListener {
            if (binding.exportAction.toggleState == ActionButtonView.ToggleState.NONE) {
                binding.surfaceView.startExport()
                binding.exportAction.toggleState = ActionButtonView.ToggleState.SINGLE
                toggleExportBorder(true)
            }
            else if (binding.exportAction.toggleState == ActionButtonView.ToggleState.SINGLE) {
                binding.surfaceView.endExport()
                binding.surfaceView.startObjectMove()
                binding.exportAction.toggleState = ActionButtonView.ToggleState.DOUBLE
                toggleExportBorder(true, double = true)
            }
            else if (binding.exportAction.toggleState == ActionButtonView.ToggleState.DOUBLE) {
                binding.surfaceView.interactiveCanvas.cancelMoveSelectedObject()
                toggleExportBorder(false)
            }
        }

        Log.i("Panel size", SessionSettings.instance.panelResIds.size.toString())

        // background button
        binding.backgroundButton.setOnClickListener {
            if (SessionSettings.instance.backgroundColorsIndex == binding.surfaceView.interactiveCanvas.numBackgrounds - 1) {
                SessionSettings.instance.backgroundColorsIndex = 0
            }
            else {
                SessionSettings.instance.backgroundColorsIndex += 1
            }

            SessionSettings.instance.darkIcons = (SessionSettings.instance.backgroundColorsIndex == 1 || SessionSettings.instance.backgroundColorsIndex == 3)
            recolorVisibleActionViews()

            if (SessionSettings.instance.backgroundColorsIndex == 6 && (SessionSettings.instance.canvasBackgroundPrimaryColor == 0 || SessionSettings.instance.canvasBackgroundSecondaryColor == 0)) {
                SessionSettings.instance.backgroundColorsIndex = 0
            }

            invalidateButtons()

            if (binding.canvasSummaryContainer.visibility == View.VISIBLE) {
                binding.canvasSummaryView.invalidate()
            }

            binding.surfaceView.interactiveCanvas.interactiveCanvasDrawer?.notifyRedraw()
        }

        // grid lines toggle button
        binding.gridLinesButton.setOnClickListener {
            SessionSettings.instance.gridLineMode += 1

            if (SessionSettings.instance.gridLineMode > 1) {
                SessionSettings.instance.gridLineMode = 0
            }

            binding.surfaceView.interactiveCanvas.interactiveCanvasDrawer?.notifyRedraw()
        }

        // canvas summary toggle button
        binding.canvasSummaryButton.setOnClickListener {
            toggleCanvasSummary()
        }

        // open tools button
        binding.openToolsButton.setOnClickListener {
            if (toolboxOpen) {
                toggleTools(false)
            }
            else {
                toggleTools(true)
            }
        }

        // recent colors background
        binding.recentColorsContainer.setOnClickListener {

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

                    binding.colorPickerFrame.layoutParams = layoutParams

                    binding.colorHexStringInput.textSize = 28F
                    var linearLayoutParams = LinearLayout.LayoutParams(Utils.dpToPx(context, 120), LinearLayout.LayoutParams.MATCH_PARENT)
                    linearLayoutParams.rightMargin = Utils.dpToPx(context, 10)
                    linearLayoutParams.gravity = Gravity.BOTTOM

                    binding.colorHexStringInput.layoutParams = linearLayoutParams

                    binding.colorHexStringInput.gravity = Gravity.BOTTOM

                    // default color buttons size
                    var frameLayoutParams = (binding.defaultBlackColorAction.layoutParams as FrameLayout.LayoutParams)
                    frameLayoutParams.width = (binding.colorPickerFrame.layoutParams.width * 0.16).toInt()
                    frameLayoutParams.height = frameLayoutParams.width

                    binding.defaultBlackColorAction.layoutParams = frameLayoutParams

                    frameLayoutParams = (binding.defaultWhiteColorAction.layoutParams as FrameLayout.LayoutParams)
                    frameLayoutParams.width = (binding.colorPickerFrame.layoutParams.width * 0.16).toInt()
                    frameLayoutParams.height = frameLayoutParams.width

                    binding.defaultWhiteColorAction.layoutParams = frameLayoutParams

                    linearLayoutParams = (binding.defaultWhiteColorButton.layoutParams as LinearLayout.LayoutParams)

                    if (binding.defaultWhiteColorAction.layoutParams.width <= Utils.dpToPx(context, 40)) {
                        linearLayoutParams.marginStart = Utils.dpToPx(context, 10)
                    }
                    else {
                        linearLayoutParams.marginStart = Utils.dpToPx(context, 20)
                    }

                    binding.defaultWhiteColorButton.layoutParams = linearLayoutParams

                    // paint panel
                    layoutParams = ConstraintLayout.LayoutParams(
                        ((150 / 1000F) * view.width).toInt(),
                        ConstraintLayout.LayoutParams.MATCH_PARENT
                    )
                    layoutParams.rightToRight = ConstraintSet.PARENT_ID

                    binding.paintPanel.layoutParams = layoutParams

                    linearLayoutParams = (binding.paintYesContainer.layoutParams as LinearLayout.LayoutParams)
                    if (binding.paintPanel.layoutParams.width < 288) {
                        linearLayoutParams.rightMargin = Utils.dpToPx(context, 5)
                    }
                    else {
                        linearLayoutParams.rightMargin = Utils.dpToPx(context, 30)
                    }
                    binding.paintYesContainer.layoutParams = linearLayoutParams

                    // paint indicator size
                    val frameWidth = ((150 / 1000F) * view.width).toInt()
                    val indicatorMargin = (frameWidth * 0.15).toInt()
                    val indicatorWidth = frameWidth - indicatorMargin

                    layoutParams = ConstraintLayout.LayoutParams(indicatorWidth, indicatorWidth)
                    layoutParams.topToTop = ConstraintSet.PARENT_ID
                    layoutParams.bottomToBottom = ConstraintSet.PARENT_ID
                    layoutParams.leftToLeft = ConstraintSet.PARENT_ID
                    layoutParams.rightToRight = ConstraintSet.PARENT_ID

                    binding.paintIndicatorViewBottomLayer.layoutParams = layoutParams
                    binding.paintIndicatorView.layoutParams = layoutParams

                    if (SessionSettings.instance.showPaintBar) {
                        // paint quantity bar size
                        /*layoutParams = ConstraintLayout.LayoutParams(
                            (binding.paintQtyBar.width * 1.25).toInt(),
                            (binding.paintQtyBar.height * 1.25).toInt()
                        )
                        layoutParams.topToTop = ConstraintSet.PARENT_ID
                        layoutParams.leftToLeft = ConstraintSet.PARENT_ID
                        layoutParams.rightToRight = ConstraintSet.PARENT_ID

                        layoutParams.topMargin = Utils.dpToPx(context, 15)

                        binding.paintQtyBar.layoutParams = layoutParams*/
                    }
                    else if (SessionSettings.instance.showPaintCircle) {
                        // paint quantity circle size
                        layoutParams = ConstraintLayout.LayoutParams(
                            (binding.paintQtyCircle.width * 1.25).toInt(),
                            (binding.paintQtyCircle.height * 1.25).toInt()
                        )
                        layoutParams.topToTop = ConstraintSet.PARENT_ID
                        layoutParams.leftToLeft = ConstraintSet.PARENT_ID
                        layoutParams.rightToRight = ConstraintSet.PARENT_ID

                        layoutParams.topMargin = Utils.dpToPx(context, 15)

                        binding.paintQtyCircle.layoutParams = layoutParams
                    }

                    //layoutParams = ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, Utils.dpToPx(context, 40))
                    binding.paletteNameText.textSize = 28F

                    var actionButtonLayoutParams = FrameLayout.LayoutParams(Utils.dpToPx(context, 30), Utils.dpToPx(context, 30))
                    actionButtonLayoutParams.gravity = Gravity.CENTER
                    binding.paletteAddColorAction.layoutParams = actionButtonLayoutParams

                    actionButtonLayoutParams = FrameLayout.LayoutParams(Utils.dpToPx(context, 30), Utils.dpToPx(context, 4))
                    actionButtonLayoutParams.gravity = Gravity.CENTER
                    binding.paletteRemoveColorAction.layoutParams = actionButtonLayoutParams

                    actionButtonLayoutParams = FrameLayout.LayoutParams(Utils.dpToPx(context, 27), Utils.dpToPx(context, 30))
                    actionButtonLayoutParams.gravity = Gravity.CENTER
                    binding.lockPaintPanelAction.layoutParams = actionButtonLayoutParams

                    layoutParams = ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.WRAP_CONTENT)
                    layoutParams.startToStart = ConstraintSet.PARENT_ID
                    layoutParams.endToEnd = ConstraintSet.PARENT_ID
                    layoutParams.topToBottom = binding.paletteNameText.id
                    layoutParams.topMargin = Utils.dpToPx(context, 10)

                    binding.colorActionButtonMenu.layoutParams = layoutParams

                    /*layoutParams = ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, Utils.dpToPx(context, 40))
                    layoutParams.topMargin = Utils.dpToPx(context, 20)
                    layoutParams.bottomMargin = Utils.dpToPx(context, 50)
                    layoutParams.topToTop = ConstraintSet.PARENT_ID

                    binding.paletteNameText.layoutParams = layoutParams*/

                    /*layoutParams = ConstraintLayout.LayoutParams(Utils.dpToPx(context, 40), Utils.dpToPx(context, 40))
                    layoutParams.topMargin = Utils.dpToPx(context, 20)
                    layoutParams.startToStart = ConstraintSet.PARENT_ID
                    layoutParams.endToEnd = ConstraintSet.PARENT_ID
                    layoutParams.topToBottom = binding.paletteNameText.id

                    binding.paletteAddColorButton.layoutParams = layoutParams

                    layoutParams = ConstraintLayout.LayoutParams(Utils.dpToPx(context, 40), Utils.dpToPx(context, 40))
                    layoutParams.topMargin = Utils.dpToPx(context, 20)
                    layoutParams.startToStart = ConstraintSet.PARENT_ID
                    layoutParams.endToEnd = ConstraintSet.PARENT_ID
                    layoutParams.topToBottom = binding.paletteNameText.id

                    binding.paletteRemoveColorButton.layoutParams = layoutParams

                    frameLayoutParams = FrameLayout.LayoutParams(Utils.dpToPx(context, 30), Utils.dpToPx(context, 30))
                    frameLayoutParams.topMargin = Utils.dpToPx(context, 5)
                    frameLayoutParams.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL

                    binding.paletteAddColorAction.layoutParams = frameLayoutParams

                    frameLayoutParams = FrameLayout.LayoutParams(Utils.dpToPx(context, 30), Utils.dpToPx(context, 6))
                    frameLayoutParams.topMargin = Utils.dpToPx(context, 17)
                    frameLayoutParams.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL

                    binding.paletteRemoveColorAction.layoutParams = frameLayoutParams*/
                }

                // paint text info placement
                if (SessionSettings.instance.showPaintBar) {
                    val layoutParams = ConstraintLayout.LayoutParams(binding.paintTimeInfoContainer.width, binding.paintTimeInfoContainer.height)
                    layoutParams.topToBottom = binding.paintQtyBar.id
                    layoutParams.leftToLeft = ConstraintSet.PARENT_ID
                    layoutParams.rightToRight = ConstraintSet.PARENT_ID

                    layoutParams.topMargin = Utils.dpToPx(context, 0)

                    binding.paintTimeInfoContainer.layoutParams = layoutParams
                }
                else if (SessionSettings.instance.showPaintCircle) {
                    binding.paintTimeInfoContainer.setBackgroundColor(Color.TRANSPARENT)
                }

                // background texture scaling
                setPanelBackground()

                // right-handed
                if (SessionSettings.instance.rightHanded) {
                    // paint panel
                    var layoutParams = binding.paintPanel.layoutParams as ConstraintLayout.LayoutParams
                    layoutParams.rightToRight = -1
                    layoutParams.leftToLeft = ConstraintSet.PARENT_ID

                    binding.paintPanel.layoutParams = layoutParams

                    binding.paintPanel.invalidate()

                    // canvas lock border
                    layoutParams = binding.paintWarningFrame.layoutParams as ConstraintLayout.LayoutParams

                    layoutParams.leftToLeft = -1
                    layoutParams.rightToRight = ConstraintSet.PARENT_ID
                    layoutParams.rightToLeft = -1
                    layoutParams.leftToRight = binding.paintPanel.id
                    binding.paintWarningFrame.layoutParams = layoutParams

                    // close paint panel button
                    binding.closePaintPanelBottomLayer.rotation = 180F
                    binding.closePaintPanelTopLayer.rotation = 180F

                    // color picker
                    layoutParams = binding.colorPickerFrame.layoutParams as ConstraintLayout.LayoutParams

                    layoutParams.leftToLeft = -1
                    layoutParams.rightToRight = ConstraintSet.PARENT_ID
                    binding.colorPickerFrame.layoutParams = layoutParams

                    // paint meter bar
                    binding.paintQtyBar.rotation = 180F

                    // toolbox
                    layoutParams = binding.openToolsButton.layoutParams as ConstraintLayout.LayoutParams

                    layoutParams.rightToRight = -1
                    layoutParams.leftToLeft = ConstraintSet.PARENT_ID
                    binding.openToolsButton.layoutParams = layoutParams

                    var layoutParams3 = binding.openToolsAction.layoutParams as FrameLayout.LayoutParams
                    layoutParams3.gravity = Gravity.LEFT or Gravity.BOTTOM
                    binding.openToolsAction.layoutParams = layoutParams3

                    // toolbox buttons
                    val toolboxButtons = arrayOf(binding.exportButton, binding.backgroundButton, binding.gridLinesButton, binding.canvasSummaryButton)

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
                    layoutParams.rightToLeft = binding.colorPickerFrame.id

                    binding.recentColorsButton.layoutParams = layoutParams

                    // recent colors action
                    layoutParams3 = binding.recentColorsAction.layoutParams as FrameLayout.LayoutParams
                    layoutParams3.gravity = Gravity.END or Gravity.BOTTOM
                    binding.recentColorsAction.layoutParams = layoutParams3

                    // recent colors container
                    layoutParams = binding.recentColorsContainer.layoutParams as ConstraintLayout.LayoutParams

                    layoutParams.leftToRight = -1
                    layoutParams.rightToLeft = binding.colorPickerFrame.id
                    binding.recentColorsContainer.layoutParams = layoutParams

                    layoutParams = binding.canvasSummaryContainer.layoutParams as ConstraintLayout.LayoutParams

                    layoutParams.startToStart = -1
                    layoutParams.endToEnd = ConstraintSet.PARENT_ID
                    binding.canvasSummaryContainer.layoutParams = layoutParams

                    //binding.openToolsButton.layoutParams = layoutParams

                    // paint yes
                    /*var layoutParams2 = binding.paintYes_container.layoutParams as LinearLayout.LayoutParams
                    layoutParams2.rightMargin = 0
                    binding.paintYes_container.layoutParams = layoutParams2

                    // paint no
                    layoutParams2 = binding.paintNo_container.layoutParams as LinearLayout.LayoutParams
                    layoutParams2.rightMargin = Utils.dpToPx(context, 30)
                    binding.paintNo_container.layoutParams = layoutParams2

                    paint_action_button_container.removeViewAt(0)
                    paint_action_button_container.addView(binding.paintYes_container)*/
                }

                // small action buttons
                if (SessionSettings.instance.smallActionButtons) {
                    // paint yes
                    var layoutParams = binding.paintYesBottomLayer.layoutParams as FrameLayout.LayoutParams

                    layoutParams.width = (layoutParams.width * 0.833).toInt()
                    layoutParams.height = (layoutParams.height * 0.833).toInt()
                    binding.paintYesBottomLayer.layoutParams = layoutParams
                    binding.paintYesTopLayer.layoutParams = layoutParams

                    // paint no
                    layoutParams = binding.paintNoBottomLayer.layoutParams as FrameLayout.LayoutParams

                    layoutParams.width = (layoutParams.width * 0.833).toInt()
                    layoutParams.height = (layoutParams.height * 0.833).toInt()
                    binding.paintNoBottomLayer.layoutParams = layoutParams
                    binding.paintNoTopLayer.layoutParams = layoutParams

                    // paint select accept
                    layoutParams = binding.paintColorAcceptImageBottomLayer.layoutParams as FrameLayout.LayoutParams

                    layoutParams.width = (layoutParams.width * 0.833).toInt()
                    layoutParams.height = (layoutParams.height * 0.833).toInt()
                    binding.paintColorAcceptImageBottomLayer.layoutParams = layoutParams
                    binding.paintColorAcceptImageTopLayer.layoutParams = layoutParams

                    // close paint panel
                    layoutParams = binding.closePaintPanelBottomLayer.layoutParams as FrameLayout.LayoutParams

                    layoutParams.width = (layoutParams.width * 0.833).toInt()
                    layoutParams.height = (layoutParams.height * 0.833).toInt()
                    binding.closePaintPanelBottomLayer.layoutParams = layoutParams
                    binding.closePaintPanelTopLayer.layoutParams = layoutParams
                }

                binding.surfaceView.setInitialPositionAndScale()
            }
        })
    }

    override fun onPause() {
        super.onPause()

        // unregister listeners
        SessionSettings.instance.paintQtyListeners.remove(this)

        context?.apply {
            binding.surfaceView.interactiveCanvas.saveUnits(this)
            binding.surfaceView.interactiveCanvas.interactiveCanvasListener = null

            SessionSettings.instance.saveLastPaintColor(this, world)
        }

        paintEventTimer?.cancel()

        if (world) {
            InteractiveCanvasSocket.instance.socket?.disconnect()
        }
        else {
            context?.apply {
                val deviceViewport = binding.surfaceView.interactiveCanvas.deviceViewport!!

                SessionSettings.instance.restoreDeviceViewportCenterX = deviceViewport.centerX()
                SessionSettings.instance.restoreDeviceViewportCenterY = deviceViewport.centerY()

                SessionSettings.instance.restoreCanvasScaleFactor = binding.surfaceView.interactiveCanvas.lastScaleFactor

                SessionSettings.instance.save(this)
                StatTracker.instance.save(this)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        /*if (world) {
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
        }*/

        binding.surfaceView.interactiveCanvas.interactiveCanvasListener = this

        if (world) {
            InteractiveCanvasSocket.instance.socket?.apply {
                if (!connected()) {
                    connect()
                }
            }
        }
    }

    // screen rotation
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        if (!SessionSettings.instance.tablet) {
            return
        }

        binding.paintPanel.background = null

        Utils.setViewLayoutListener(view!!, object : Utils.ViewLayoutListener {
            override fun onViewLayout(view: View) {
                // interactive canvas
                binding.surfaceView.interactiveCanvas.deviceViewport?.apply {
                    binding.surfaceView.interactiveCanvas.updateDeviceViewport(this@InteractiveCanvasFragment.context!!)
                }

                // color picker frame width
                var layoutParams = ConstraintLayout.LayoutParams(
                    (view.width * 0.35).toInt(),
                    ConstraintLayout.LayoutParams.MATCH_PARENT
                )

                layoutParams.leftToLeft = (binding.colorPickerFrame.layoutParams as ConstraintLayout.LayoutParams).leftToLeft
                layoutParams.rightToRight = (binding.colorPickerFrame.layoutParams as ConstraintLayout.LayoutParams).rightToRight

                binding.colorPickerFrame.layoutParams = layoutParams

                // color picker default color buttons
                var frameLayoutParams = (binding.defaultBlackColorAction.layoutParams as FrameLayout.LayoutParams)
                frameLayoutParams.width = (binding.colorPickerFrame.layoutParams.width * 0.16).toInt()
                frameLayoutParams.height = frameLayoutParams.width

                binding.defaultBlackColorAction.layoutParams = frameLayoutParams

                frameLayoutParams = (binding.defaultWhiteColorAction.layoutParams as FrameLayout.LayoutParams)
                frameLayoutParams.width = (binding.colorPickerFrame.layoutParams.width * 0.16).toInt()
                frameLayoutParams.height = frameLayoutParams.width

                binding.defaultWhiteColorAction.layoutParams = frameLayoutParams

                var linearLayoutParams = (binding.defaultWhiteColorButton.layoutParams as LinearLayout.LayoutParams)
                if (binding.defaultWhiteColorAction.layoutParams.width <= Utils.dpToPx(context, 40)) {
                    linearLayoutParams.marginStart = Utils.dpToPx(context, 10)
                }
                else {
                    linearLayoutParams.marginStart = Utils.dpToPx(context, 20)
                }
                binding.defaultWhiteColorButton.layoutParams = linearLayoutParams

                // paint panel
                layoutParams = ConstraintLayout.LayoutParams(
                    ((150 / 1000F) * view.width).toInt(),
                    ConstraintLayout.LayoutParams.MATCH_PARENT
                )
                layoutParams.leftToLeft = (binding.paintPanel.layoutParams as ConstraintLayout.LayoutParams).leftToLeft
                layoutParams.rightToRight = (binding.paintPanel.layoutParams as ConstraintLayout.LayoutParams).rightToRight

                binding.paintPanel.layoutParams = layoutParams

                linearLayoutParams = (binding.paintYesContainer.layoutParams as LinearLayout.LayoutParams)
                if (binding.paintPanel.layoutParams.width < 288) {
                    linearLayoutParams.rightMargin = Utils.dpToPx(context, 5)
                }
                else {
                    linearLayoutParams.rightMargin = Utils.dpToPx(context, 30)
                }
                binding.paintYesContainer.layoutParams = linearLayoutParams

                // paint indicator size
                val frameWidth = ((150 / 1000F) * view.width).toInt()
                val indicatorMargin = (frameWidth * 0.15).toInt()
                val indicatorWidth = frameWidth - indicatorMargin

                layoutParams = ConstraintLayout.LayoutParams(indicatorWidth, indicatorWidth)
                layoutParams.topToTop = (binding.paintIndicatorView.layoutParams as ConstraintLayout.LayoutParams).topToTop
                layoutParams.bottomToBottom = (binding.paintIndicatorView.layoutParams as ConstraintLayout.LayoutParams).bottomToBottom
                layoutParams.leftToLeft = (binding.paintIndicatorView.layoutParams as ConstraintLayout.LayoutParams).leftToLeft
                layoutParams.rightToRight = (binding.paintIndicatorView.layoutParams as ConstraintLayout.LayoutParams).rightToRight

                binding.paintIndicatorViewBottomLayer.layoutParams = layoutParams
                binding.paintIndicatorView.layoutParams = layoutParams

                binding.deviceCanvasViewportView.updateDeviceViewport()

                setPanelBackground()
            }
        })
    }

    // view helper
    private fun setPanelBackground() {
        context?.apply {
            val backgroundDrawable = ContextCompat.getDrawable(this, SessionSettings.instance.panelResIds[SessionSettings.instance.panelBackgroundResIndex]) as BitmapDrawable

            if (SessionSettings.instance.tablet) {
                binding.paintPanel.clipChildren = false

                val scale = view!!.height / backgroundDrawable.bitmap.height.toFloat()

                val newWidth = (backgroundDrawable.bitmap.width * scale).toInt()
                val newHeight = (backgroundDrawable.bitmap.height * scale).toInt()
                val newBitmap = Bitmap.createScaledBitmap(backgroundDrawable.bitmap, newWidth,
                    newHeight, false)
                val scaledBitmapDrawable = BitmapDrawable(resources, newBitmap)

                val resizedBitmap = Bitmap.createBitmap(scaledBitmapDrawable.bitmap, max(0, scaledBitmapDrawable.bitmap.width / 2 - binding.paintPanel.layoutParams.width / 2), 0, binding.paintPanel.layoutParams.width, scaledBitmapDrawable.bitmap.height)
                val resizedBitmapDrawable = BitmapDrawable(resizedBitmap)

                scaledBitmapDrawable.gravity = Gravity.CENTER

                binding.paintPanel.setBackgroundDrawable(resizedBitmapDrawable)
            }
            else {
                binding.paintPanel.setBackgroundDrawable(backgroundDrawable)
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
        for (actionView in visibleActionViews) {
            if (SessionSettings.instance.backgroundColorsIndex == 1 || SessionSettings.instance.backgroundColorsIndex == 3) {
                actionView.colorMode = ActionButtonView.ColorMode.BLACK
            }
            else {
                actionView.colorMode = ActionButtonView.ColorMode.WHITE
            }

            actionView.invalidate()
        }
    }

    private fun setupColorPalette(colors: Array<Int>?) {
        if (colors != null) {
            var i = 0
            for (v in binding.recentColorsContainer.children) {
                (v as ActionButtonView).type = ActionButtonView.Type.RECENT_COLOR

                if (i < colors.size) {
                    v.representingColor = colors[colors.size - 1 - i]
                    v.visibility = View.VISIBLE
                }
                else {
                    v.visibility = View.GONE
                }

                v.setOnClickListener {
                    v.representingColor?.apply {
                        SessionSettings.instance.paintColor = this
                        notifyPaintColorUpdate(SessionSettings.instance.paintColor)

                        //binding.recentColorsContainer.visibility = View.GONE
                        //binding.recentColorsAction.visibility = View.VISIBLE
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
                binding.recent1.visibility = View.VISIBLE
                binding.recent1.type = ActionButtonView.Type.NONE
            }
        }
        else {
            for (v in binding.recentColorsContainer.children) {
                (v as ActionButtonView).type = ActionButtonView.Type.RECENT_COLOR
            }
        }
    }

    private fun invalidateButtons() {
        binding.menuAction.invalidate()
        binding.paintPanelActionView.invalidate()
        binding.exportAction.invalidate()
        binding.backgroundAction.invalidate()
        binding.gridLinesAction.invalidate()
        binding.canvasSummaryAction.invalidate()
        binding.recentColorsAction.invalidate()
        binding.openToolsAction.invalidate()
        binding.objectMoveUpAction.invalidate()
        binding.objectMoveDownAction.invalidate()
        binding.objectMoveLeftAction.invalidate()
        binding.objectMoveRightAction.invalidate()
    }

    // view toggles
    private fun togglePaintPanel(show: Boolean, softHide: Boolean = false) {
        if (show) {
            binding.paintPanel.visibility = View.VISIBLE
            binding.paintPanelButton.visibility = View.GONE

            binding.exportButton.visibility = View.INVISIBLE
            binding.backgroundButton.visibility = View.INVISIBLE

            binding.openToolsButton.visibility = View.INVISIBLE

            toggleTools(false)

            if (binding.pixelHistoryFragmentContainer.visibility == View.VISIBLE) {
                binding.pixelHistoryFragmentContainer.visibility = View.GONE
            }

            if (binding.canvasSummaryView.visibility == View.VISIBLE) {
                binding.canvasSummaryContainer.visibility = View.INVISIBLE
            }

            var startLoc = binding.paintPanel.width.toFloat() * 0.99F
            if (SessionSettings.instance.rightHanded) {
                startLoc = -startLoc
            }

            binding.paintPanel.animate().translationX(startLoc).setDuration(0).withEndAction {
                binding.paintPanel.animate().translationX(0F).setDuration(50).setInterpolator(
                    AccelerateDecelerateInterpolator()
                ).withEndAction {

                    Log.i("ICF", "paint panel width is ${binding.paintPanel.width}")
                    Log.i("ICF", "paint panel height is ${binding.paintPanel.height}")

                }.start()

                if (SessionSettings.instance.canvasLockBorder) {
                    context?.apply {
                        val drawable: GradientDrawable = binding.paintWarningFrame.background as GradientDrawable
                        drawable.setStroke(
                            Utils.dpToPx(this, 4),
                            SessionSettings.instance.canvasLockBorderColor
                        ) // set stroke width and stroke color
                    }

                    binding.paintWarningFrame.visibility = View.VISIBLE
                    binding.paintWarningFrame.alpha = 0F
                    binding.paintWarningFrame.animate().alpha(1F).setDuration(50).start()
                }
            }.start()

            binding.surfaceView.startPainting()

            if (binding.pixelHistoryFragmentContainer.visibility == View.VISIBLE) {
                binding.pixelHistoryFragmentContainer.visibility = View.GONE
            }

            binding.menuButton.visibility = View.GONE
            binding.menuContainer.visibility = View.GONE

            SessionSettings.instance.paintPanelOpen = true
        }
        else if (softHide) {
            binding.paintPanel.visibility = View.GONE

            toggleTools(false)
        }
        else {
            binding.surfaceView.endPainting(false)

            binding.paintPanel.visibility = View.GONE
            binding.paintWarningFrame.visibility = View.GONE

            binding.paintPanelButton.visibility = View.VISIBLE

            //binding.recentColorsAction.visibility = View.VISIBLE
            //binding.recentColorsContainer.visibility = View.GONE

            if (toolboxOpen) {
                binding.exportButton.visibility = View.VISIBLE
                binding.backgroundButton.visibility = View.VISIBLE
                binding.gridLinesButton.visibility = View.VISIBLE
            }

            binding.menuButton.visibility = View.VISIBLE

            binding.openToolsButton.visibility = View.VISIBLE

            toggleExportBorder(false)

            SessionSettings.instance.paintPanelOpen = false
        }
    }

    private fun toggleTools(show: Boolean) {
        if (!animatingTools) {
            if (show && !toolboxOpen) {
                animatingTools = true

                binding.exportButton.visibility = View.VISIBLE
                binding.backgroundButton.visibility = View.VISIBLE
                binding.gridLinesButton.visibility = View.VISIBLE
                binding.canvasSummaryButton.visibility = View.VISIBLE

                Animator.animateMenuItems(
                    listOf(
                        listOf(binding.exportButton), listOf(binding.backgroundButton), listOf(
                            binding.gridLinesButton
                        ), listOf(binding.canvasSummaryButton)
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
                        listOf(binding.exportButton), listOf(binding.backgroundButton), listOf(
                            binding.gridLinesButton
                        ), listOf(binding.canvasSummaryButton)
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
                val drawable: GradientDrawable = binding.exportBorderView.background as GradientDrawable
                drawable.setStroke(
                    Utils.dpToPx(this, 2),
                    color
                ) // set stroke width and stroke color
            }
            binding.exportBorderView.visibility = View.VISIBLE
        }
        else {
            binding.exportBorderView.visibility = View.GONE

            if (double) {
                binding.exportAction.toggleState = ActionButtonView.ToggleState.SINGLE
            }
            else {
                binding.exportAction.toggleState = ActionButtonView.ToggleState.NONE
            }

            binding.exportAction.invalidate()
        }
    }

    private fun toggleMenu(open: Boolean) {
        if (menuFragment == null) {
            menuFragment = MenuFragment()

            menuFragment?.menuButtonListener = (activity as InteractiveCanvasActivity)
            menuFragment?.menuCardListener = this

            fragmentManager?.apply {
                beginTransaction().replace(binding.menuContainer.id, menuFragment!!).commit()

                binding.menuContainer.alpha = 0F
                binding.menuContainer.animate().alpha(1f).setDuration(250).withEndAction {

                }.start()
                //interactiveCanvasFragmentListener?.onInteractiveCanvasBack()
            }
        }
        else {
            menuFragment!!.clearMenuTextHighlights()
        }
        if (open) {
            binding.menuContainer.visibility = View.VISIBLE
        }
        else {
            binding.menuContainer.visibility = View.GONE
        }
    }

    private fun toggleTerminal(open: Boolean) {
        if (terminalFragment == null) {
            terminalFragment = TerminalFragment()

            terminalFragment?.interactiveCanvas = binding.surfaceView.interactiveCanvas

            fragmentManager?.apply {
                beginTransaction().replace(binding.terminalContainer.id, terminalFragment!!).commit()
            }
        }

        if (open) {
            binding.terminalContainer.visibility = View.VISIBLE
        }
        else {
            binding.terminalContainer.visibility = View.GONE
        }
    }

    // "Window" fragments
    // pixel history listener
    override fun showPixelHistoryFragmentPopover(screenPoint: Point) {
        fragmentManager?.apply {
            binding.surfaceView.interactiveCanvas.getPixelHistory(binding.surfaceView.interactiveCanvas.pixelIdForUnitPoint(
                binding.surfaceView.interactiveCanvas.lastSelectedUnitPoint
            ), object : PixelHistoryCallback {
                override fun onHistoryJsonResponse(historyJson: JSONArray) {
                    // set bottom-left of view to screenPoint

                    binding.pixelHistoryFragmentContainer?.apply {
                        val dX = (screenPoint.x + Utils.dpToPx(context, 10)).toFloat()
                        val dY = (screenPoint.y - Utils.dpToPx(context, 120) - Utils.dpToPx(
                            context,
                            10
                        )).toFloat()

                        binding.pixelHistoryFragmentContainer.x = dX
                        binding.pixelHistoryFragmentContainer.y = dY

                        if (firstInfoTap) {
                            binding.pixelHistoryFragmentContainer.y -= Utils.dpToPx(
                                context,
                                firstInfoTapFixYOffset
                            )
                            firstInfoTap = false
                        }

                        view?.apply {
                            if (binding.pixelHistoryFragmentContainer.x < Utils.dpToPx(context, 20).toFloat()) {
                                binding.pixelHistoryFragmentContainer.x = Utils.dpToPx(context, 20).toFloat()
                            } else if (binding.pixelHistoryFragmentContainer.x + binding.pixelHistoryFragmentContainer.width > width - Utils.dpToPx(context, 20).toFloat()) {
                                binding.pixelHistoryFragmentContainer.x =
                                    width - binding.pixelHistoryFragmentContainer.width.toFloat() - Utils.dpToPx(
                                        context,
                                        20
                                    ).toFloat()
                            }

                            if (binding.pixelHistoryFragmentContainer.y < Utils.dpToPx(context, 20).toFloat()) {
                                binding.pixelHistoryFragmentContainer.y = Utils.dpToPx(context, 20).toFloat()
                            } else if (binding.pixelHistoryFragmentContainer.y + binding.pixelHistoryFragmentContainer.height > height - Utils.dpToPx(context, 20).toFloat()) {
                                binding.pixelHistoryFragmentContainer.y =
                                    height - binding.pixelHistoryFragmentContainer.height.toFloat() - Utils.dpToPx(
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

                            binding.pixelHistoryFragmentContainer.visibility = View.VISIBLE
                        }
                    }
                }
            })
        }
    }

    // Palette and Canvas Frame fragments also use binding.pixelHistoryFragmentContainer
    override fun showDrawFrameConfigFragmentPopover(screenPoint: Point) {
        if (binding.pixelHistoryFragmentContainer.visibility == View.VISIBLE) {
            closePopoverFragment()
            return
        }

        if (binding.canvasSummaryView.visibility == View.VISIBLE) {
            binding.canvasSummaryContainer.visibility = View.INVISIBLE
        }

        fragmentManager?.apply {
            binding.pixelHistoryFragmentContainer?.apply {
                val dX = (screenPoint.x + Utils.dpToPx(context, 10)).toFloat()
                val dY = (screenPoint.y - Utils.dpToPx(context, 120) - Utils.dpToPx(
                    context,
                    10
                )).toFloat()

                binding.pixelHistoryFragmentContainer.x = dX
                binding.pixelHistoryFragmentContainer.y = dY

                if (firstInfoTap) {
                    binding.pixelHistoryFragmentContainer.y -= Utils.dpToPx(
                        context,
                        firstInfoTapFixYOffset
                    )
                    firstInfoTap = false
                }

                view?.apply {
                    if (binding.pixelHistoryFragmentContainer.x < Utils.dpToPx(context, 20).toFloat()) {
                        binding.pixelHistoryFragmentContainer.x = Utils.dpToPx(context, 20).toFloat()
                    } else if (binding.pixelHistoryFragmentContainer.x + binding.pixelHistoryFragmentContainer.width > width - Utils.dpToPx(context, 20).toFloat()) {
                        binding.pixelHistoryFragmentContainer.x =
                            width - binding.pixelHistoryFragmentContainer.width.toFloat() - Utils.dpToPx(
                                context,
                                20
                            ).toFloat()
                    }

                    if (binding.pixelHistoryFragmentContainer.y < Utils.dpToPx(context, 20).toFloat()) {
                        binding.pixelHistoryFragmentContainer.y = Utils.dpToPx(context, 20).toFloat()
                    } else if (binding.pixelHistoryFragmentContainer.y + binding.pixelHistoryFragmentContainer.height > height - Utils.dpToPx(context, 20).toFloat()) {
                        binding.pixelHistoryFragmentContainer.y =
                            height - binding.pixelHistoryFragmentContainer.height.toFloat() - Utils.dpToPx(
                                context,
                                20
                            ).toFloat()
                    }

                    val fragment = DrawFrameConfigFragment()
                    fragment.drawFrameConfigFragmentListener = this@InteractiveCanvasFragment

                    fragment.panelThemeConfig = panelThemeConfig

                    fragment.centerX = binding.surfaceView.interactiveCanvas.lastSelectedUnitPoint.x
                    fragment.centerY = binding.surfaceView.interactiveCanvas.lastSelectedUnitPoint.y

                    beginTransaction().replace(
                        R.id.pixel_history_fragment_container,
                        fragment
                    ).commit()

                    binding.pixelHistoryFragmentContainer.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun showPalettesFragmentPopover() {
        var screenPoint = Point(binding.surfaceView.width, 0)
        if (SessionSettings.instance.rightHanded) {
            screenPoint = Point(0, 0)
        }

        fragmentManager?.apply {
            // set bottom-left of view to screenPoint

            binding.pixelHistoryFragmentContainer?.apply {
                val dX = (screenPoint.x + Utils.dpToPx(context, 10)).toFloat()
                val dY = (screenPoint.y - Utils.dpToPx(context, 120) - Utils.dpToPx(
                    context,
                    10
                )).toFloat()

                binding.pixelHistoryFragmentContainer.x = dX
                binding.pixelHistoryFragmentContainer.y = dY

                if (firstInfoTap) {
                    binding.pixelHistoryFragmentContainer.y -= Utils.dpToPx(
                        context,
                        firstInfoTapFixYOffset
                    )
                    firstInfoTap = false
                }

                view?.apply {
                    if (binding.pixelHistoryFragmentContainer.x < Utils.dpToPx(context, 20).toFloat()) {
                        binding.pixelHistoryFragmentContainer.x = Utils.dpToPx(context, 20).toFloat()
                    } else if (binding.pixelHistoryFragmentContainer.x + binding.pixelHistoryFragmentContainer.width > width - Utils.dpToPx(context, 20).toFloat()) {
                        binding.pixelHistoryFragmentContainer.x =
                            width - binding.pixelHistoryFragmentContainer.width.toFloat() - Utils.dpToPx(
                                context,
                                20
                            ).toFloat()
                    }

                    if (binding.pixelHistoryFragmentContainer.y < Utils.dpToPx(context, 20).toFloat()) {
                        binding.pixelHistoryFragmentContainer.y = Utils.dpToPx(context, 20).toFloat()
                    } else if (binding.pixelHistoryFragmentContainer.y + binding.pixelHistoryFragmentContainer.height > height - Utils.dpToPx(context, 20).toFloat()) {
                        binding.pixelHistoryFragmentContainer.y =
                            height - binding.pixelHistoryFragmentContainer.height.toFloat() - Utils.dpToPx(
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

                    binding.pixelHistoryFragmentContainer.visibility = View.VISIBLE
                }
            }
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
        binding.colorPickerView.setInitialColor(color)
        binding.paintIndicatorView.setPaintColor(color)
    }

    override fun notifyPaintingStarted() {
        binding.closePaintPanelContainer.visibility = View.GONE
        binding.paintYesContainer.visibility = View.VISIBLE
        binding.paintNoContainer.visibility = View.VISIBLE
    }

    override fun notifyPaintingEnded() {
        binding.closePaintPanelContainer.visibility = View.VISIBLE
        binding.paintYesContainer.visibility = View.GONE
        binding.paintNoContainer.visibility = View.GONE
    }

    override fun notifyPaintActionStarted() {
        //binding.recentColorsAction.visibility = View.VISIBLE
        //binding.recentColorsContainer.visibility = View.GONE

        if (!SessionSettings.instance.lockPaintPanel) {
            togglePaintPanel(show = false, softHide = true)
        }
    }

    override fun notifyClosePaletteFragment() {
        closePopoverFragment()
    }

    override fun isPaletteFragmentOpen(): Boolean {
        return binding.pixelHistoryFragmentContainer.visibility == View.VISIBLE
    }

    override fun notifyDeviceViewportUpdate() {
        if (binding.deviceCanvasViewportView.visibility == View.VISIBLE) {
            binding.deviceCanvasViewportView.updateDeviceViewport(binding.surfaceView.interactiveCanvas)
        }
    }

    override fun notifyUpdateCanvasSummary() {
        if (binding.canvasSummaryContainer.visibility == View.VISIBLE) {
            binding.canvasSummaryView.invalidate()
        }
    }

    override fun onDeviceViewportUpdate() {
        val canvasBounds = binding.surfaceView.interactiveCanvas.canvasScreenBounds()
        //Log.v("canvas bounds", canvasBounds.toString())

        for (actionView in visibleActionViews) {
            val lastColorMode = actionView.colorMode

            val location = IntArray(2)
            actionView.getLocationOnScreen(location)

            val x = location[0]
            val y = location[1]

            var inBounds = true

            // left
            if (x + actionView.width / 2 < canvasBounds.left) {
                inBounds = false
            }
            // top
            else if (y + actionView.height / 2 < canvasBounds.top) {
                inBounds = false
            }
            // right
            else if (x + actionView.width / 2 > canvasBounds.right) {
                inBounds = false
            }
            // bottom
            else if (y + actionView.height / 2 > canvasBounds.bottom) {
                inBounds = false
            }

            if (!inBounds) {
                actionView.colorMode = ActionButtonView.ColorMode.WHITE
            }
            else {
                if (SessionSettings.instance.darkIcons) {
                    actionView.colorMode = ActionButtonView.ColorMode.BLACK
                } else {
                    actionView.colorMode = ActionButtonView.ColorMode.WHITE
                }
            }

            if (actionView.colorMode != lastColorMode) {
                actionView.invalidate()
            }
        }
    }

    // interactive canvas gesture listener
    override fun onInteractiveCanvasPan() {
        binding.pixelHistoryFragmentContainer.visibility = View.GONE

        /*if (binding.deviceCanvasViewportView.visibility == View.VISIBLE) {
            binding.deviceCanvasViewportView.updateDeviceViewport(binding.surfaceView.interactiveCanvas)
        }*/
    }

    override fun onInteractiveCanvasScale() {
        binding.pixelHistoryFragmentContainer.visibility = View.GONE

        /*if (binding.deviceCanvasViewportView.visibility == View.VISIBLE) {
            binding.deviceCanvasViewportView.updateDeviceViewport(binding.surfaceView.interactiveCanvas)
        }*/
    }

    // paint qty listener
    override fun paintQtyChanged(qty: Int) {
        //drops_amt_text.text = qty.toString()
        activity?.runOnUiThread {
            binding.paintAmtInfo.text = qty.toString()
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
        if (world) {
            paintTextMode += 1
            if (paintTextMode == 3) {
                paintTextMode = 0
            }

            if (paintTextMode == paintTextModeTime) {
                binding.paintTimeInfo.visibility = View.VISIBLE
                binding.paintTimeInfoContainer.visibility = View.VISIBLE

                val layoutParams = binding.paintTimeInfoContainer.layoutParams as ConstraintLayout.LayoutParams
                layoutParams.width = max((binding.paintTimeInfo.paint.measureText(binding.paintTimeInfo.text.toString()) + Utils.dpToPx(context, 10)).toInt(), Utils.dpToPx(context, 30))

                binding.paintTimeInfoContainer.layoutParams = layoutParams

                binding.paintAmtInfo.visibility = View.INVISIBLE
            }
            else if (paintTextMode == paintTextModeAmt) {
                binding.paintAmtInfo.visibility = View.VISIBLE
                binding.paintTimeInfoContainer.visibility = View.VISIBLE

                val layoutParams = binding.paintTimeInfoContainer.layoutParams as ConstraintLayout.LayoutParams
                layoutParams.width = max((binding.paintAmtInfo.paint.measureText(binding.paintAmtInfo.text.toString()) + Utils.dpToPx(context, 20)).toInt(), Utils.dpToPx(context, 30))

                binding.paintTimeInfoContainer.layoutParams = layoutParams

                binding.paintTimeInfo.visibility = View.INVISIBLE
            }
            else if (paintTextMode == paintTextModeHide) {
                binding.paintTimeInfo.visibility = View.INVISIBLE
                binding.paintTimeInfoContainer.visibility = View.INVISIBLE
                binding.paintAmtInfo.visibility = View.INVISIBLE
            }
        }
    }

    // object selection listener
    override fun onObjectSelectionBoundsChanged(startPoint: PointF, endPoint: PointF) {
        binding.objectSelectionView.visibility = View.VISIBLE

        if (SessionSettings.instance.backgroundColorsIndex == 1 || SessionSettings.instance.backgroundColorsIndex == 3) {
            binding.objectSelectionView.setBackgroundResource(R.drawable.object_selection_background_darkgray)
        }
        else {
            binding.objectSelectionView.setBackgroundResource(R.drawable.object_selection_background_white)
        }

        binding.objectSelectionView.layoutParams = ConstraintLayout.LayoutParams((endPoint.x - startPoint.x).toInt(), (endPoint.y - startPoint.y).toInt())

        binding.objectSelectionView.x = startPoint.x
        binding.objectSelectionView.y = startPoint.y
    }

    override fun onObjectSelectionEnded() {
        binding.objectSelectionView.visibility = View.GONE
    }

    // art export listener
    override fun onArtExported(pixelPositions: List<InteractiveCanvas.RestorePoint>) {
        toggleExportBorder(false)

        val fragment = ArtExportFragment()
        fragment.art = pixelPositions
        fragment.listener = this

        fragmentManager?.apply {
            // binding.exportButton.background = ResourcesCompat.getDrawable(resources, R.drawable.ic_share, null)

            beginTransaction().replace(R.id.export_fragment_container, fragment).addToBackStack("Export").commit()

            binding.exportFragmentContainer.visibility = View.VISIBLE
            binding.exportFragmentContainer.setOnClickListener {

            }
        }
    }

    // art export fragment listener
    override fun onArtExportBack() {
        fragmentManager?.popBackStack()

        binding.exportFragmentContainer.visibility = View.GONE
        binding.surfaceView.endExport()

        binding.exportAction.touchState = ActionButtonView.TouchState.INACTIVE
    }

    // palettes fragment listener
    override fun onPaletteSelected(palette: Palette, index: Int) {
        binding.paletteNameText.text = palette.name

        binding.pixelHistoryFragmentContainer.visibility = View.GONE

        syncPaletteAndColor()
    }

    override fun onPaletteDeleted(palette: Palette) {
        showPaletteUndoSnackbar(palette)
        if (palette.name == binding.paletteNameText.text) {
            if (SessionSettings.instance.palettes.size > 0) {
                binding.paletteNameText.text = SessionSettings.instance.palettes[0].name
            }
        }
    }

    // palette fragment helper
    private fun showPaletteUndoSnackbar(palette: Palette) {
        palettesFragment?.apply {
            val snackbar = Snackbar.make(view!!, "Deleted ${palette.name} palette", Snackbar.LENGTH_LONG)
            snackbar.setAction("Undo") {
                undoDelete()
                this@InteractiveCanvasFragment.binding.paletteNameText.text = SessionSettings.instance.palette.name
            }
            snackbar.show()
        }
    }

    private fun showPaletteColorUndoSnackbar(palette: Palette) {
        val snackbar = Snackbar.make(view!!, "Removed color from ${palette.name} palette", Snackbar.LENGTH_LONG)
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
            binding.paletteAddColorButton.visibility = View.GONE
            binding.paletteRemoveColorButton.visibility = View.GONE

            setupColorPalette(binding.surfaceView.interactiveCanvas.recentColorsList.toTypedArray())
        }
        else {
            if (SessionSettings.instance.palette.colors.contains(SessionSettings.instance.paintColor)) {
                binding.paletteAddColorButton.visibility = View.GONE
                binding.paletteRemoveColorButton.visibility = View.VISIBLE

            }
            else {
                binding.paletteAddColorButton.visibility = View.VISIBLE
                binding.paletteRemoveColorButton.visibility = View.GONE
            }

            setupColorPalette(SessionSettings.instance.palette.colors.toTypedArray())
        }
    }

    // draw frame config listener
    override fun createDrawFrame(centerX: Int, centerY: Int, width: Int, height: Int, color: Int) {
        binding.surfaceView.createDrawFrame(centerX, centerY, width, height, color)

        closePopoverFragment()
    }

    private fun toggleCanvasSummary() {
        if (binding.canvasSummaryContainer.visibility != View.VISIBLE) {
            binding.canvasSummaryView.drawBackground = false
            binding.canvasSummaryView.interactiveCanvas = binding.surfaceView.interactiveCanvas

            binding.deviceCanvasViewportView.updateDeviceViewport(binding.surfaceView.interactiveCanvas)

            binding.canvasSummaryContainer.visibility = View.VISIBLE
        }
        else {
            binding.canvasSummaryContainer.visibility = View.INVISIBLE
        }
    }

    // canvas edge touch listener
    override fun onTouchCanvasEdge() {
        togglePaintPanel(true)
    }

    // device canvas viewport reset listener
    override fun resetDeviceCanvasViewport() {
        context?.apply {
            binding.surfaceView.interactiveCanvas.lastScaleFactor = binding.surfaceView.interactiveCanvas.startScaleFactor
            binding.surfaceView.scaleFactor = binding.surfaceView.interactiveCanvas.lastScaleFactor
            binding.surfaceView.interactiveCanvas.ppu = (binding.surfaceView.interactiveCanvas.basePpu * binding.surfaceView.scaleFactor ).toInt()

            binding.surfaceView.interactiveCanvas.updateDeviceViewport(
                this,
                binding.surfaceView.interactiveCanvas.rows / 2F, binding.surfaceView.interactiveCanvas.cols / 2F
            )
        }
    }

    private fun closePopoverFragment() {
        if (binding.pixelHistoryFragmentContainer.visibility == View.VISIBLE) {
            binding.pixelHistoryFragmentContainer.visibility = View.GONE
        }
    }

    // selected object view
    override fun showSelectedObjectYesAndNoButtons(screenPoint: Point) {
        binding.selectedObjectYesButton.actionBtnView = binding.selectedObjectYesAction
        binding.selectedObjectNoButton.actionBtnView = binding.selectedObjectNoAction

        binding.selectedObjectYesAction.type = ActionButtonView.Type.YES
        binding.selectedObjectYesAction.colorMode = ActionButtonView.ColorMode.COLOR

        binding.selectedObjectNoAction.type = ActionButtonView.Type.NO
        binding.selectedObjectNoAction.colorMode = ActionButtonView.ColorMode.COLOR

        binding.selectedObjectYesNoContainer.x = (screenPoint.x - binding.selectedObjectYesButton.layoutParams.width - Utils.dpToPx(context, 5)).toFloat()
        binding.selectedObjectYesNoContainer.y = (screenPoint.y - binding.selectedObjectYesButton.layoutParams.height / 2 - Utils.dpToPx(context, 5)).toFloat()

        binding.selectedObjectYesButton.setOnClickListener {
            binding.surfaceView.interactiveCanvas.endMoveSelection(true)
        }

        binding.selectedObjectNoButton.setOnClickListener {
            binding.surfaceView.interactiveCanvas.endMoveSelection(false)
        }

        binding.selectedObjectYesNoContainer.visibility = View.VISIBLE
    }

    override fun hideSelectedObjectYesAndNoButtons() {
        binding.selectedObjectYesNoContainer.visibility = View.GONE
    }

    override fun selectedObjectEnded() {

    }

    // selected object move view
    override fun showSelectedObjectMoveButtons(bounds: Rect) {
        binding.objectMoveUpButton.actionBtnView = binding.objectMoveUpAction
        binding.objectMoveUpAction.type = ActionButtonView.Type.SOLID
        binding.objectMoveUpButton.visibility = View.VISIBLE

        binding.objectMoveDownButton.actionBtnView = binding.objectMoveDownAction
        binding.objectMoveDownAction.type = ActionButtonView.Type.SOLID
        binding.objectMoveDownButton.visibility = View.VISIBLE

        binding.objectMoveLeftButton.actionBtnView = binding.objectMoveLeftAction
        binding.objectMoveLeftAction.type = ActionButtonView.Type.SOLID
        binding.objectMoveLeftButton.visibility = View.VISIBLE

        binding.objectMoveRightButton.actionBtnView = binding.objectMoveRightAction
        binding.objectMoveRightAction.type = ActionButtonView.Type.SOLID
        binding.objectMoveRightButton.visibility = View.VISIBLE

        binding.objectMoveUpButton.setOnClickListener {
            binding.surfaceView.interactiveCanvas.moveSelection(InteractiveCanvas.Direction.UP)
        }
        binding.objectMoveDownButton.setOnClickListener {
            binding.surfaceView.interactiveCanvas.moveSelection(InteractiveCanvas.Direction.DOWN)
        }
        binding.objectMoveLeftButton.setOnClickListener {
            binding.surfaceView.interactiveCanvas.moveSelection(InteractiveCanvas.Direction.LEFT)
        }
        binding.objectMoveRightButton.setOnClickListener {
            binding.surfaceView.interactiveCanvas.moveSelection(InteractiveCanvas.Direction.RIGHT)
        }

        val cX = (bounds.right + bounds.left) / 2
        val cY = (bounds.bottom + bounds.top) / 2

        binding.objectMoveUpButton.x = (cX - binding.objectMoveUpButton.layoutParams.width / 2).toFloat()
        binding.objectMoveUpButton.y = (bounds.top - binding.objectMoveUpButton.layoutParams.height - Utils.dpToPx(context, 20)).toFloat()

        binding.objectMoveDownButton.x = (cX - binding.objectMoveDownButton.layoutParams.width / 2).toFloat()
        binding.objectMoveDownButton.y = (bounds.bottom + Utils.dpToPx(context, 20)).toFloat()

        binding.objectMoveLeftButton.x = (bounds.left - Utils.dpToPx(context, 20) - binding.objectMoveLeftButton.layoutParams.width).toFloat()
        binding.objectMoveLeftButton.y = (cY - binding.objectMoveLeftButton.layoutParams.height / 2).toFloat()

        binding.objectMoveRightButton.x = (bounds.right + Utils.dpToPx(context, 20)).toFloat()
        binding.objectMoveRightButton.y = (cY - binding.objectMoveLeftButton.layoutParams.height / 2).toFloat()
    }

    override fun updateSelectedObjectMoveButtons(bounds: Rect) {
        val cX = (bounds.right + bounds.left) / 2
        val cY = (bounds.bottom + bounds.top) / 2

        binding.objectMoveUpButton.x = (cX - binding.objectMoveUpButton.layoutParams.width / 2).toFloat()
        binding.objectMoveUpButton.y = (bounds.top - binding.objectMoveUpButton.layoutParams.height - Utils.dpToPx(context, 20)).toFloat()

        binding.objectMoveDownButton.x = (cX - binding.objectMoveDownButton.layoutParams.width / 2).toFloat()
        binding.objectMoveDownButton.y = (bounds.bottom + Utils.dpToPx(context, 20)).toFloat()

        binding.objectMoveLeftButton.x = (bounds.left - Utils.dpToPx(context, 20) - binding.objectMoveLeftButton.layoutParams.width).toFloat()
        binding.objectMoveLeftButton.y = (cY - binding.objectMoveLeftButton.layoutParams.height / 2).toFloat()

        binding.objectMoveRightButton.x = (bounds.right + Utils.dpToPx(context, 20)).toFloat()
        binding.objectMoveRightButton.y = (cY - binding.objectMoveLeftButton.layoutParams.height / 2).toFloat()
    }

    override fun hideSelectedObjectMoveButtons() {
        binding.objectMoveUpButton.visibility = View.GONE
        binding.objectMoveDownButton.visibility = View.GONE
        binding.objectMoveLeftButton.visibility = View.GONE
        binding.objectMoveRightButton.visibility = View.GONE

        binding.selectedObjectYesNoContainer.visibility = View.GONE
    }

    override fun selectedObjectMoveEnded() {
        toggleExportBorder(false)
    }

    // menu card listener
    override fun moveMenuCardBy(x: Float, y: Float) {
        binding.menuContainer.x += x
        binding.menuContainer.y += y

        view?.apply {
            if (binding.menuContainer.x + binding.menuContainer.width > width) {
                binding.menuContainer.x = (width - binding.menuContainer.width).toFloat()
            }
            if (binding.menuContainer.x < 0) {
                binding.menuContainer.x = 0F
            }
            if (binding.menuContainer.y + binding.menuContainer.height > height) {
                binding.menuContainer.y = (height - binding.menuContainer.height).toFloat()
            }
            if (binding.menuContainer.y < 0) {
                binding.menuContainer.y = 0F
            }
        }
    }

    override fun closeMenu() {
        binding.menuContainer.visibility = View.GONE
    }

    // world API
    private fun sendApiStatusCheck() {
        val requestQueue = Volley.newRequestQueue(context)
        val request = object: JsonObjectRequest(
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
        val request = object: JsonObjectRequest(
            Request.Method.GET,
            Utils.baseUrlApi + "/api/v1/paint/time/sync",
            null,
            { response ->
                (context as Activity?)?.runOnUiThread {
                    val timeUntil = response.getInt("s").toLong()

                    if (timeUntil < 0) {
                        binding.paintTimeInfo.text = "???"
                    } else {
                        SessionSettings.instance.timeSync = timeUntil
                        setupPaintEventTimer()
                    }
                }
            },
            { error ->
                (context as Activity?)?.runOnUiThread {

                }
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
                            binding.paintTimeInfo.text = s.toString()
                        } catch (ex: IllegalStateException) {

                        }
                    } else {
                        try {
                            binding.paintTimeInfo.text = String.format("%02d:%02d", m, s)

                            if (binding.paintTimeInfo.visibility == View.VISIBLE) {
                                val layoutParams = binding.paintTimeInfoContainer.layoutParams as ConstraintLayout.LayoutParams
                                layoutParams.width = (binding.paintTimeInfo.paint.measureText(binding.paintTimeInfo.text.toString()) + Utils.dpToPx(context, 10)).toInt()

                                binding.paintTimeInfoContainer.layoutParams = layoutParams
                            }

                        } catch (ex: IllegalStateException) {

                        }
                    }
                }
            }
        }, 0, 1000)
    }
}