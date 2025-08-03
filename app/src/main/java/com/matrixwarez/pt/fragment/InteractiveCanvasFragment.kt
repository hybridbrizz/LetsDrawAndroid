package com.matrixwarez.pt.fragment

import android.app.Activity
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.material.snackbar.Snackbar
import com.matrixwarez.pt.R
import com.matrixwarez.pt.activity.InteractiveCanvasActivity
import com.matrixwarez.pt.colorpicker.ColorPickerFragment
import com.matrixwarez.pt.compose.CanvasMenuView
import com.matrixwarez.pt.compose.ClientCanvasLocationsView
import com.matrixwarez.pt.compose.ClientsInfoListView
import com.matrixwarez.pt.compose.HelpMessageListView
import com.matrixwarez.pt.helper.Animator
import com.matrixwarez.pt.helper.PanelThemeConfig
import com.matrixwarez.pt.helper.Utils
import com.matrixwarez.pt.listener.ArtExportFragmentListener
import com.matrixwarez.pt.listener.ArtExportListener
import com.matrixwarez.pt.listener.CanvasEdgeTouchListener
import com.matrixwarez.pt.listener.DataLoadingCallback
import com.matrixwarez.pt.listener.DeviceCanvasViewportResetListener
import com.matrixwarez.pt.listener.DrawFrameConfigFragmentListener
import com.matrixwarez.pt.listener.InteractiveCanvasFragmentListener
import com.matrixwarez.pt.listener.InteractiveCanvasGestureListener
import com.matrixwarez.pt.listener.InteractiveCanvasListener
import com.matrixwarez.pt.listener.InteractiveCanvasViewModeListener
import com.matrixwarez.pt.listener.MenuCardListener
import com.matrixwarez.pt.listener.ObjectSelectionListener
import com.matrixwarez.pt.listener.PaintBarActionListener
import com.matrixwarez.pt.listener.PaintQtyListener
import com.matrixwarez.pt.listener.PalettesFragmentListener
import com.matrixwarez.pt.listener.PixelHistoryCallback
import com.matrixwarez.pt.listener.PixelHistoryListener
import com.matrixwarez.pt.listener.RecentColorsListener
import com.matrixwarez.pt.listener.SelectedObjectMoveView
import com.matrixwarez.pt.listener.SelectedObjectView
import com.matrixwarez.pt.listener.SocketConnectCallback
import com.matrixwarez.pt.model.CanvasLoader
import com.matrixwarez.pt.model.ColorPanelIcon
import com.matrixwarez.pt.model.InteractiveCanvas
import com.matrixwarez.pt.model.InteractiveCanvasSocket
import com.matrixwarez.pt.model.Palette
import com.matrixwarez.pt.model.Server
import com.matrixwarez.pt.model.SessionSettings
import com.matrixwarez.pt.service.CanvasService
import com.matrixwarez.pt.view.ActionButtonView
import com.matrixwarez.pt.view.ButtonFrame
import com.matrixwarez.pt.view.ColorPaletteView
import com.matrixwarez.pt.view.InteractiveCanvasView
import io.reactivex.rxjava3.core.Observable
import kotlinx.android.synthetic.main.fragment_interactive_canvas.banner_icon
import kotlinx.android.synthetic.main.fragment_interactive_canvas.banner_text
import kotlinx.android.synthetic.main.fragment_interactive_canvas.canvas_menu
import kotlinx.android.synthetic.main.fragment_interactive_canvas.canvas_summary_container
import kotlinx.android.synthetic.main.fragment_interactive_canvas.canvas_summary_view
import kotlinx.android.synthetic.main.fragment_interactive_canvas.client_canvas_locations
import kotlinx.android.synthetic.main.fragment_interactive_canvas.color_palette_view
import kotlinx.android.synthetic.main.fragment_interactive_canvas.color_picker_frame
import kotlinx.android.synthetic.main.fragment_interactive_canvas.device_canvas_viewport_view
import kotlinx.android.synthetic.main.fragment_interactive_canvas.done_button
import kotlinx.android.synthetic.main.fragment_interactive_canvas.drawer_layout
import kotlinx.android.synthetic.main.fragment_interactive_canvas.erase_action_view
import kotlinx.android.synthetic.main.fragment_interactive_canvas.erase_button_background
import kotlinx.android.synthetic.main.fragment_interactive_canvas.erase_button_background_outer
import kotlinx.android.synthetic.main.fragment_interactive_canvas.export_action
import kotlinx.android.synthetic.main.fragment_interactive_canvas.export_button
import kotlinx.android.synthetic.main.fragment_interactive_canvas.export_fragment_container
import kotlinx.android.synthetic.main.fragment_interactive_canvas.help_messages
import kotlinx.android.synthetic.main.fragment_interactive_canvas.image_no_socket
import kotlinx.android.synthetic.main.fragment_interactive_canvas.ll_latency_container
import kotlinx.android.synthetic.main.fragment_interactive_canvas.loading_progress_bar
import kotlinx.android.synthetic.main.fragment_interactive_canvas.lock_paint_panel
import kotlinx.android.synthetic.main.fragment_interactive_canvas.lock_paint_panel_action
import kotlinx.android.synthetic.main.fragment_interactive_canvas.menu_action
import kotlinx.android.synthetic.main.fragment_interactive_canvas.menu_button
import kotlinx.android.synthetic.main.fragment_interactive_canvas.menu_container
import kotlinx.android.synthetic.main.fragment_interactive_canvas.nav_view
import kotlinx.android.synthetic.main.fragment_interactive_canvas.object_move_down_action
import kotlinx.android.synthetic.main.fragment_interactive_canvas.object_move_down_button
import kotlinx.android.synthetic.main.fragment_interactive_canvas.object_move_left_action
import kotlinx.android.synthetic.main.fragment_interactive_canvas.object_move_left_button
import kotlinx.android.synthetic.main.fragment_interactive_canvas.object_move_right_action
import kotlinx.android.synthetic.main.fragment_interactive_canvas.object_move_right_button
import kotlinx.android.synthetic.main.fragment_interactive_canvas.object_move_up_action
import kotlinx.android.synthetic.main.fragment_interactive_canvas.object_move_up_button
import kotlinx.android.synthetic.main.fragment_interactive_canvas.object_selection_view
import kotlinx.android.synthetic.main.fragment_interactive_canvas.paint_amt_info
import kotlinx.android.synthetic.main.fragment_interactive_canvas.paint_button_background
import kotlinx.android.synthetic.main.fragment_interactive_canvas.paint_button_background_outer
import kotlinx.android.synthetic.main.fragment_interactive_canvas.paint_button_container_2
import kotlinx.android.synthetic.main.fragment_interactive_canvas.paint_control_layout
import kotlinx.android.synthetic.main.fragment_interactive_canvas.paint_indicator_view_bottom_layer
import kotlinx.android.synthetic.main.fragment_interactive_canvas.paint_panel
import kotlinx.android.synthetic.main.fragment_interactive_canvas.paint_panel_action_view
import kotlinx.android.synthetic.main.fragment_interactive_canvas.paint_qty_bar
import kotlinx.android.synthetic.main.fragment_interactive_canvas.paint_qty_circle
import kotlinx.android.synthetic.main.fragment_interactive_canvas.paint_time_info
import kotlinx.android.synthetic.main.fragment_interactive_canvas.paint_time_info_container
import kotlinx.android.synthetic.main.fragment_interactive_canvas.palette_add_color_action
import kotlinx.android.synthetic.main.fragment_interactive_canvas.palette_add_color_button
import kotlinx.android.synthetic.main.fragment_interactive_canvas.palette_name_text
import kotlinx.android.synthetic.main.fragment_interactive_canvas.palette_remove_color_action
import kotlinx.android.synthetic.main.fragment_interactive_canvas.palette_remove_color_button
import kotlinx.android.synthetic.main.fragment_interactive_canvas.pixel_history_fragment_container
import kotlinx.android.synthetic.main.fragment_interactive_canvas.recent_color_palette_view
import kotlinx.android.synthetic.main.fragment_interactive_canvas.recent_colors_container
import kotlinx.android.synthetic.main.fragment_interactive_canvas.selected_object_no_action
import kotlinx.android.synthetic.main.fragment_interactive_canvas.selected_object_no_button
import kotlinx.android.synthetic.main.fragment_interactive_canvas.selected_object_yes_action
import kotlinx.android.synthetic.main.fragment_interactive_canvas.selected_object_yes_button
import kotlinx.android.synthetic.main.fragment_interactive_canvas.selected_object_yes_no_container
import kotlinx.android.synthetic.main.fragment_interactive_canvas.server_list
import kotlinx.android.synthetic.main.fragment_interactive_canvas.stream_banner
import kotlinx.android.synthetic.main.fragment_interactive_canvas.surface_view
import kotlinx.android.synthetic.main.fragment_interactive_canvas.terminal_container
import kotlinx.android.synthetic.main.fragment_interactive_canvas.text_bottom_display
import kotlinx.android.synthetic.main.fragment_interactive_canvas.text_coords
import kotlinx.android.synthetic.main.fragment_interactive_canvas.text_latency
import kotlinx.android.synthetic.main.fragment_interactive_canvas.toolbar
import kotlinx.android.synthetic.main.fragment_interactive_canvas.view.menu_container
import kotlinx.android.synthetic.main.fragment_interactive_canvas.view.pixel_history_fragment_container
import kotlinx.android.synthetic.main.fragment_interactive_canvas.view.surface_view
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.TimeUnit
import kotlin.math.max


class InteractiveCanvasFragment : Fragment(), InteractiveCanvasListener, PaintQtyListener,
    RecentColorsListener, PaintBarActionListener, PixelHistoryListener,
    InteractiveCanvasGestureListener, ArtExportListener, ArtExportFragmentListener, ObjectSelectionListener,
    PalettesFragmentListener, DrawFrameConfigFragmentListener, CanvasEdgeTouchListener, DeviceCanvasViewportResetListener,
    SelectedObjectMoveView, SelectedObjectView, MenuCardListener, SocketConnectCallback, ColorPaletteView.Listener,
    InteractiveCanvasViewModeListener, DataLoadingCallback {

    var server: Server? = null
    var tempServer: Server? = null

    var world = false
    var realmId = 0

    var interactiveCanvasFragmentListener: InteractiveCanvasFragmentListener? = null

    var paintEventTimer: Timer? = null

    val firstInfoTapFixYOffset = 0
    var firstInfoTap = true

    var toolboxOpen = false

    lateinit var panelThemeConfig: PanelThemeConfig

    var animatingTools = false

    var palettesFragment: PalettesFragment? = null

    var recentlyRemovedColor = 0
    var recentlyRemovedColorIndex = 0

    var menuFragment: MenuFragment? = null

    var terminalFragment: TerminalFragment? = null

    lateinit var visibleActionViews: Array<ButtonFrame>

    var canvasService: CanvasService? = null

    var paused = false
    var pauseTime = System.currentTimeMillis()
    private val maxBgTime = 60 * 5

    private var lastCanvasSummaryImageTime = 0L

    private var saveViewportTimer: Timer? = null

    private var clientsInfoState = mutableStateOf<List<Triple<String, Int, Int>>?>(null)

    private val lineColorDarkState = mutableStateOf(false)
    private val showServerListState = mutableStateOf(false)
    private val mapMarkerIndexState = mutableIntStateOf(0)
    private val showMenuState = mutableStateOf(false)

    private var leave = false

    private var colorPanelIcons = mutableSetOf<ColorPanelIcon>()

    private var colorPickerFragment: ColorPickerFragment? = null

    private var menuLatencyText: TextView? = null
    private var menuSocketStatusImage: ImageView? = null

    private var canvasLoader: CanvasLoader? = null

    private var doneLoading = false

    private fun onServer() {
        loading_progress_bar.visibility = View.GONE

        val headerView = nav_view.getHeaderView(0)
        val serverNameText = headerView.findViewById<TextView>(R.id.text_server_name)
        serverNameText.text = server?.name ?: ""

        surface_view.interactiveCanvas.server = server
        SessionSettings.instance.addPaintInterval = server!!.pixelInterval / 60
        canvasService = CanvasService(server!!)

        if (server != null && server!!.isAdmin) {
            export_button.visibility = View.VISIBLE
        }

        requireActivity().title = "${server!!.name} (${SessionSettings.instance.displayNameOrId()})"

        surface_view.interactiveCanvas.realmId = realmId
        surface_view.interactiveCanvas.world = world

        setupStreamBanner()

        paint_button_container_2.visibility = View.VISIBLE

        help_messages.setContent {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(120.dp))
                server?.let {
                    HelpMessageListView(server = it) {
                        SessionSettings.instance.showHelpMessages = false
                        SessionSettings.instance.save(requireContext())
                        help_messages.visibility = View.GONE
                    }
                }
            }
        }

        surface_view.interactiveCanvas.startLatencyJob()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("Test Option Selection", "Has Options Menu")
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_interactive_canvas, container, false)

        // setup views here

        return view
    }

    private var pixelsReadyCount = 0

    override fun notifyPixelsReady() {
        menu_button.visibility = View.VISIBLE

        togglePaintPanel(SessionSettings.instance.paintPanelOpen)
        toggleTools(SessionSettings.instance.toolboxOpen)

        pixelsReadyCount += 1

        if (pixelsReadyCount >= 2) {
            doneLoading = true
        }
    }

    private lateinit var drawerToggle: ActionBarDrawerToggle

    private fun startPainting() {
        surface_view.startPainting()

        paint_button_container_2.visibility = View.GONE

        requireActivity().title = SessionSettings.instance.dropsAmt.toString()

        paint_control_layout.visibility = View.VISIBLE

        color_palette_view.visibility = View.VISIBLE

        recent_color_palette_view.visibility = View.VISIBLE

        (requireActivity() as? InteractiveCanvasActivity)?.colorActionBar(Color.parseColor("#202020"))

        addBackToEndPainting()

        pixel_history_fragment_container.visibility = View.GONE
    }

    private fun endPainting() {
        surface_view.endPainting()

        paint_button_container_2.visibility = View.VISIBLE

        requireActivity().title = "${server!!.name} (${SessionSettings.instance.displayNameOrId()})"

        paint_control_layout.visibility = View.GONE

        color_palette_view.visibility = View.GONE

        recent_color_palette_view.visibility = View.GONE

        (requireActivity() as? InteractiveCanvasActivity)?.colorActionBar(Color.BLACK)

        addBackToMenuOnBackPressed()
    }

    private fun setupToolbarWithHamburger() {
        val headerView = nav_view.getHeaderView(0)
        val serverNameText = headerView.findViewById<TextView>(R.id.text_server_name)
        serverNameText.text = server?.name ?: ""

        menuLatencyText = headerView.findViewById(R.id.text_latency)
        menuSocketStatusImage = headerView.findViewById(R.id.image_socket_status)

        (activity as AppCompatActivity).setSupportActionBar(toolbar)

        // Create the toggle
        drawerToggle = ActionBarDrawerToggle(
            requireActivity(),
            drawer_layout,
            toolbar,
            R.string.open_menu,
            R.string.close_menu
        )

        // Set the toggle as the DrawerListener
        drawer_layout.addDrawerListener(drawerToggle)

        // Enable the hamburger icon
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        drawerToggle.syncState()

        toolbar.setNavigationOnClickListener {
            when (surface_view.mode == InteractiveCanvasView.Mode.PAINTING
                    || surface_view.mode == InteractiveCanvasView.Mode.ERASING) {
                true -> {
                    Log.d("Test Option Selection", "Painting")

                    endPainting()

                    true
                }
                false -> {
                    Log.d("Test Option Selection", "Not Painting")
                    toggleDrawer()
                }
            }
        }

        nav_view.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.canvas_item_server_list -> {
                    toggleDrawer()
                    showServerList()
                    true
                }
                R.id.canvas_item_reset_camera -> {
                    toggleDrawer()
                    surface_view.setInitialPositionAndScale()
                    true
                }
                R.id.canvas_item_community -> {
                    toggleDrawer()
                    if (server!!.iconLink.isNotBlank()) {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(server!!.iconLink))
                            startActivity(intent)
                        }
                        catch (e: Exception) {}
                    }
                    true
                }
                R.id.canvas_item_options -> {
                    toggleDrawer()
                    (requireActivity() as? AppCompatActivity)?.supportActionBar?.hide()
                    (requireActivity() as InteractiveCanvasActivity).showOptionsFragment(this@InteractiveCanvasFragment)
                    true
                }
                R.id.canvas_item_yank -> {
                    toggleDrawer()
                    (requireActivity() as? AppCompatActivity)?.supportActionBar?.hide()
                    val fragment = ArtExportFragment()
                    fragment.interactiveCanvas = surface_view.interactiveCanvas
                    fragment.listener = this@InteractiveCanvasFragment

                    childFragmentManager.apply {
                        // export_button.background = ResourcesCompat.getDrawable(resources, R.drawable.ic_share, null)

                        beginTransaction().replace(R.id.export_fragment_container, fragment).addToBackStack("Export").commit()

                        export_fragment_container.visibility = View.VISIBLE
                        export_fragment_container.setOnClickListener {

                        }
                    }
                    true
                }
                R.id.canvas_item_help -> {
                    toggleDrawer()
                    showHelpMessages()
                    true
                }
                R.id.canvas_item_leave -> {
                    toggleDrawer()
                    leave()
                    true
                }
                R.id.canvas_item_grid_lines -> {
                    toggleDrawer()

                    SessionSettings.instance.gridLineMode += 1

                    if (SessionSettings.instance.gridLineMode > 1) {
                        SessionSettings.instance.gridLineMode = 0
                    }

                    surface_view.interactiveCanvas.interactiveCanvasDrawer?.notifyRedraw()

                    true
                }
                R.id.canvas_item_background -> {
                    toggleDrawer()
                    changeBackground()
                    true
                }
                R.id.canvas_item_map -> {
                    toggleDrawer()
                    toggleCanvasSummary()
                    true
                }
                else -> false
            }
        }
    }

    private fun toggleDrawer() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START, false)
        } else {
            drawer_layout.openDrawer(GravityCompat.START)
        }
    }

    private fun leave() {
        leave = true
        lastCanvasSummaryImageTime = 0L
        when (doneLoading) {
            true -> {
                InteractiveCanvasSocket.instance.disconnect()
            }
            false -> {
                canvasLoader?.abort()
                onSocketDisconnect(false)
            }
        }
    }

    private fun addBackToMenuOnBackPressed() {
        requireActivity()
            .onBackPressedDispatcher
            .addCallback {
                if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
                    drawer_layout.closeDrawer(GravityCompat.START)
                }
                else {
                    leave()
                }
            }
    }

    private fun addBackToEndPainting() {
        requireActivity()
            .onBackPressedDispatcher
            .addCallback {
                endPainting()
            }
    }

    private fun addBackToColorSelection() {
        requireActivity()
            .onBackPressedDispatcher
            .addCallback {
                onPaintIndicatorClick()
            }
    }

    // setup views
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        paint_button_container_2.visibility = View.GONE

        surface_view.interactiveCanvas.server = tempServer

        // call abort in leave()
        canvasLoader = CanvasLoader(
            activity = requireActivity(),
            server = tempServer!!,
            progressBar = loading_progress_bar,
            dataLoadingCallback = this,
            socketListener = this
        )
        canvasLoader?.startLoading()

        setupToolbarWithHamburger()

        if (savedInstanceState != null) {
            //SessionSettings.instance.load(requireContext())
            (requireActivity() as InteractiveCanvasActivity).showMenuFragment()
            return
        }

        addBackToMenuOnBackPressed()

        Log.d("Test test", "Adding color picker fragment now")
        colorPickerFragment = ColorPickerFragment()

        Log.d("Color Picker Frame Test", color_picker_frame.width.toString())
        Log.d("Color Picker Frame Test", color_picker_frame.height.toString())
        parentFragmentManager.beginTransaction()
            .replace(R.id.color_picker_frame, colorPickerFragment!!)
            .commit()

        SessionSettings.instance.canvasOpen = true

        InteractiveCanvasSocket.instance.socketConnectCallback = this

        context?.apply {
            SessionSettings.instance.tablet = Utils.isTablet(this)
        }

        surface_view.interactiveCanvas.world = world

        // must call before darkIcons
        if (surface_view == null) {
            (requireActivity() as InteractiveCanvasActivity).showMenuFragment()
            return
        }

        SessionSettings.instance.darkIcons = (SessionSettings.instance.backgroundColorsIndex == 1 || SessionSettings.instance.backgroundColorsIndex == 3)
        lineColorDarkState.value = SessionSettings.instance.darkIcons

        SessionSettings.instance.paintQtyListeners.add(this)

        if (SessionSettings.instance.backgroundColorsIndex == 1 || SessionSettings.instance.backgroundColorsIndex == 3) {
            SessionSettings.instance.darkIcons = true

            invalidateButtons()
        }

        setupColorPalette()

        lineColorDarkState.value = SessionSettings.instance.darkIcons

        help_messages.setContent {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(120.dp))
                server?.let {
                    HelpMessageListView(server = it) {
                        SessionSettings.instance.showHelpMessages = false
                        SessionSettings.instance.save(requireContext())
                        help_messages.visibility = View.GONE
                    }
                }
            }
        }

        client_canvas_locations.setContent {
            ClientCanvasLocationsView(
                clientsInfoState = clientsInfoState,
                interactiveCanvas = surface_view.interactiveCanvas,
                redrawCountState = surface_view.redrawCountState,
                lineColorIsDarkState = lineColorDarkState,
                mapMarkerIndexState = mapMarkerIndexState
            )
        }

        server_list.setContent {
            var showServerList by showServerListState
            val clientsInfo by clientsInfoState

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        closeServerList()
                    }
            ) {
                AnimatedVisibility(
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 120.dp),
                    visible = showServerList && clientsInfo != null,
                    enter = fadeIn(
                        tween(200)
                    ),
                    exit = fadeOut(
                        tween(200)
                    )
                ) {
                    ClientsInfoListView(
                        interactiveCanvas = surface_view.interactiveCanvas,
                        clientsInfo = clientsInfo!!,
                        mapMarkerIndexState = mapMarkerIndexState
                    )
                }
            }
        }

        canvas_menu.setContent {
            var showMenu by showMenuState

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        closeCanvasMenu()
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(80.dp))
                AnimatedVisibility(
                    visible = showMenu,
                    enter = fadeIn(
                        tween(200)
                    ),
                    exit = fadeOut(
                        tween(200)
                    )
                ) {
                    CanvasMenuView(
                        server = server!!,
                        latencyTextState = surface_view.interactiveCanvas.latencyTextState,
                        connectedState = surface_view.interactiveCanvas.connectedState,
                        onServerList = {
                            closeCanvasMenu()
                            showServerList()
                        },
                        onCommunity = {
                            if (server!!.iconLink.isNotBlank()) {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(server!!.iconLink))
                                    startActivity(intent)
                                }
                                catch (e: Exception) {}
                            }
                            closeCanvasMenu()
                        },
                        onStyles = {
                            (requireActivity() as InteractiveCanvasActivity).showOptionsFragment(this@InteractiveCanvasFragment)
                            closeCanvasMenu()
                        },
                        onGrabImage = {
                            val fragment = ArtExportFragment()
                            fragment.interactiveCanvas = surface_view.interactiveCanvas
                            fragment.listener = this@InteractiveCanvasFragment

                            fragmentManager?.apply {
                                // export_button.background = ResourcesCompat.getDrawable(resources, R.drawable.ic_share, null)

                                beginTransaction().replace(R.id.export_fragment_container, fragment).addToBackStack("Export").commit()

                                export_fragment_container.visibility = View.VISIBLE
                                export_fragment_container.setOnClickListener {

                                }
                            }
                            closeCanvasMenu()
                        },
                        onHelp = {
                            showHelpMessages()
                            closeCanvasMenu()
                        },
                        onLeave = {
                            InteractiveCanvasSocket.instance.disconnect()
                            lastCanvasSummaryImageTime = 0L
                            leave = true
                        },
                        onGridLines = {
                            SessionSettings.instance.gridLineMode += 1

                            if (SessionSettings.instance.gridLineMode > 1) {
                                SessionSettings.instance.gridLineMode = 0
                            }

                            surface_view.interactiveCanvas.interactiveCanvasDrawer?.notifyRedraw()

                            closeCanvasMenu()
                        },
                        onBackground = {
                            changeBackground()
                        },
                        onSummary = {
                            toggleCanvasSummary()
                            closeCanvasMenu()
                        }
                    )
                }
            }
        }

        ll_latency_container.setOnClickListener {
            showMenuState.value = false
            showServerListState.value = !showServerListState.value
        }

        setupStreamBanner()

        visibleActionViews = arrayOf(menu_button,
            export_button)

        panelThemeConfig = PanelThemeConfig.buildConfig(SessionSettings.instance.panelResIds[SessionSettings.instance.panelBackgroundResIndex])

        // listeners
        surface_view.pixelHistoryListener = this
        surface_view.gestureListener = this
        surface_view.modeListener = this
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

        // color panel icons
        colorPanelIcons.add(
            ColorPanelIcon(
                context = requireContext(),
                name = "Edit Canvas",
                iconViews = listOf(
                    paint_panel_action_view,
                    text_bottom_display
                ),
                bgView = paint_button_background,
                outerBgView = paint_button_background_outer,
                isSelected = {
                    surface_view.mode == InteractiveCanvasView.Mode.PAINTING ||
                            surface_view.mode == InteractiveCanvasView.Mode.PAINT_SELECTION_PAINTING
                },
                onPress = {
                    startPainting()
                }
            )
        )

        colorPanelIcons.add(
            ColorPanelIcon(
                context = requireContext(),
                name = "Erase",
                iconViews = listOf(
                    erase_action_view
                ),
                bgView = erase_button_background,
                outerBgView = erase_button_background_outer,
                isSelected = {
                    surface_view.mode == InteractiveCanvasView.Mode.ERASING
                },
                onPress = {
                    if (surface_view.mode == InteractiveCanvasView.Mode.ERASING) {
                        surface_view.endErasing()
                    }
                    else {
                        surface_view.startErasing()
                    }
                }
            )
        )

        done_button.setOnClickListener {
            endPainting()
        }

        paint_indicator_view_bottom_layer.setOnClickListener {
            onPaintIndicatorClick()
        }

        // palette
        palette_name_text.setOnClickListener {
            showPalettesFragmentPopover()
        }

        updateSelectedColor(SessionSettings.instance.paintColor)

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

        paint_panel.visibility = View.GONE

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

        requireActivity().title = ""
        text_bottom_display.text = SessionSettings.instance.dropsAmt.toString()

        recolorVisibleActionViews()

        if (panelThemeConfig.actionButtonColor == Color.BLACK) {
            palette_name_text.setTextColor(Color.parseColor("#FF111111"))
            palette_name_text.setShadowLayer(3F, 2F, 2F, Color.parseColor("#7F333333"))

            //paint_color_accept.color = Color.BLACK

            palette_add_color_action.colorMode = ActionButtonView.ColorMode.BLACK
            palette_remove_color_action.colorMode = ActionButtonView.ColorMode.BLACK

            lock_paint_panel_action.colorMode = ActionButtonView.ColorMode.BLACK
        }
        else {
            palette_name_text.setTextColor(Color.WHITE)

            //paint_color_accept.color = Color.WHITE

            palette_add_color_action.colorMode = ActionButtonView.ColorMode.WHITE
            palette_remove_color_action.colorMode = ActionButtonView.ColorMode.WHITE

            lock_paint_panel_action.colorMode = ActionButtonView.ColorMode.WHITE
        }

        if (panelThemeConfig.actionButtonColor == ActionButtonView.blackPaint.color) {
            //paint_color_accept.color = Color.BLACK
        }

        if (panelThemeConfig.inversePaintEventInfo) {
            paint_time_info_container.setBackgroundResource(R.drawable.timer_text_background_inverse)
            paint_time_info.setTextColor(ActionButtonView.blackPaint.color)
            paint_amt_info.setTextColor(ActionButtonView.blackPaint.color)
        }

        paint_qty_bar.panelThemeConfig = panelThemeConfig
        paint_qty_circle.panelThemeConfig = panelThemeConfig
        paint_indicator_view_bottom_layer.panelThemeConfig = panelThemeConfig

        // color picker view
        //color_picker_view.setSelectorColor(Color.WHITE)

        colorPickerFragment?.listen(object: ColorPickerFragment.ColorListener {
            override fun onColor(color: Int) {
                updateSelectedColor(color)
            }

            override fun requestClose() {
                onPaintIndicatorClick()
            }

            override fun requestPickCanvas() {
                color_picker_frame.visibility = View.INVISIBLE
            }

            override fun pickCanvasColor(color: Int) {
                colorPickerFragment?.setColor(color, false)
                color_picker_frame.visibility = View.VISIBLE
            }
        })

        // button clicks
        paint_panel.setOnClickListener {
            closePopoverFragment()
        }

        togglePaintPanel(true)

//        // paint buttons
//        paint_panel_button.setOnClickListener {
//            togglePaintPanel(true)
//        }

//        paint_yes.setOnClickListener {
//            if (world && !InteractiveCanvasSocket.instance.isConnected()) return@setOnClickListener
//
//            //surface_view.endPainting(true)
//
//            paint_yes_container.visibility = View.GONE
//            paint_no_container.visibility = View.GONE
//
//            paint_yes.invalidate()
//
//            //surface_view.startPainting()
//        }

//        paint_no.setOnClickListener {
//            if (color_picker_frame.visibility == View.VISIBLE) {
//                paint_indicator_view_bottom_layer.setPaintColor(initalColor)
//                syncPaletteAndColor()
//
//                color_picker_frame.visibility = View.GONE
//                recent_colors_button.visibility = View.VISIBLE
//
//                if (SessionSettings.instance.canvasLockBorder) {
//                    paint_warning_frame.visibility = View.VISIBLE
//                }
//
//                paint_yes.visibility = View.VISIBLE
//
//                //recent_colors_button.visibility = View.VISIBLE
//                //recent_colors_container.visibility = View.GONE
//
//                surface_view.endPaintSelection()
//
//                if (surface_view.interactiveCanvas.restorePoints.size == 0) {
//                    paint_yes_container.visibility = View.GONE
//                    paint_no_container.visibility = View.GONE
//                }
//                else {
//                    paint_yes_container.visibility = View.VISIBLE
//                    paint_no_container.visibility = View.VISIBLE
//                }
//            }
//            else {
//                //surface_view.endPainting(false)
//
//                paint_yes_container.visibility = View.GONE
//                paint_no_container.visibility = View.GONE
//
//                //recent_colors_button.visibility = View.VISIBLE
//                //recent_colors_container.visibility = View.GONE
//
//                //surface_view.startPainting()
//            }
//        }

//        close_paint_panel.setOnClickListener {
//            togglePaintPanel(false)
//
//            recent_colors_container.visibility = View.GONE
//            recent_colors_action.visibility = View.VISIBLE
//
//            closePopoverFragment()
//        }

        lock_paint_panel.setOnClickListener {
            SessionSettings.instance.lockPaintPanel = !SessionSettings.instance.lockPaintPanel

            if (SessionSettings.instance.lockPaintPanel) {
                lock_paint_panel_action.type = ActionButtonView.Type.LOCK_CLOSE
            }
            else {
                lock_paint_panel_action.type = ActionButtonView.Type.LOCK_OPEN
            }
        }

//        paint_color_accept.setOnClickListener {
//            color_picker_frame.visibility = View.GONE
//            recent_colors_button.visibility = View.VISIBLE
//
//            if (SessionSettings.instance.canvasLockBorder) {
//                paint_warning_frame.visibility = View.VISIBLE
//            }
//
//            paint_yes.visibility = View.VISIBLE
//
//            paint_color_accept.visibility = View.GONE
//
//            surface_view.endPaintSelection()
//
//            if (surface_view.interactiveCanvas.restorePoints.size == 0) {
//                paint_yes_container.visibility = View.GONE
//                paint_no_container.visibility = View.GONE
//            }
//            else {
//                paint_yes_container.visibility = View.VISIBLE
//                paint_no_container.visibility = View.VISIBLE
//            }
//
//            SessionSettings.instance.saveColor(requireContext())
//        }

        // to stop click-through to the canvas behind
        color_picker_frame.setOnClickListener {

        }

        // menu button
        if (!SessionSettings.instance.selectedHand) {
            toggleMenu(true)
        }

        menu_button.setOnClickListener {

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
                    export_button.toggleState = ButtonFrame.ToggleState.NONE
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

        // open tools button
//        open_tools_button.setOnClickListener {
//            if (toolboxOpen) {
//                toggleTools(false)
//            }
//            else {
//                toggleTools(true)
//            }
//        }

        // recent colors background
        recent_colors_container.setOnClickListener {

        }

        // tablet & righty
        Utils.setViewLayoutListener(view, object : Utils.ViewLayoutListener {
            override fun onViewLayout(view: View) {
                // tablet
//                if (SessionSettings.instance.tablet) {
//                    // color picker frame width
//                    var layoutParams = ConstraintLayout.LayoutParams(
//                        (view.width * 0.35).toInt(),
//                        ConstraintLayout.LayoutParams.MATCH_PARENT
//                    )
//
//                    layoutParams.leftToLeft = ConstraintSet.PARENT_ID
//
//                    color_picker_frame.layoutParams = layoutParams
//
//                    //color_hex_string_input.textSize = 28F
//                    var linearLayoutParams = LinearLayout.LayoutParams(Utils.dpToPx(context, 120), LinearLayout.LayoutParams.MATCH_PARENT)
//                    linearLayoutParams.rightMargin = Utils.dpToPx(context, 10)
//                    linearLayoutParams.gravity = Gravity.BOTTOM
//
//                    //color_hex_string_input.layoutParams = linearLayoutParams
//
//                    //color_hex_string_input.gravity = Gravity.BOTTOM
//
//                    // default color buttons size
//                    var frameLayoutParams = (default_black_color_action.layoutParams as FrameLayout.LayoutParams)
//                    frameLayoutParams.width = (color_picker_frame.layoutParams.width * 0.16).toInt()
//                    frameLayoutParams.height = frameLayoutParams.width
//
//                    default_black_color_action.layoutParams = frameLayoutParams
//
//                    frameLayoutParams = (default_white_color_action.layoutParams as FrameLayout.LayoutParams)
//                    frameLayoutParams.width = (color_picker_frame.layoutParams.width * 0.16).toInt()
//                    frameLayoutParams.height = frameLayoutParams.width
//
//                    default_white_color_action.layoutParams = frameLayoutParams
//
//                    linearLayoutParams = (default_white_color_button.layoutParams as LinearLayout.LayoutParams)
//
//                    if (default_white_color_action.layoutParams.width <= Utils.dpToPx(context, 40)) {
//                        linearLayoutParams.marginStart = Utils.dpToPx(context, 10)
//                    }
//                    else {
//                        linearLayoutParams.marginStart = Utils.dpToPx(context, 20)
//                    }
//
//                    default_white_color_button.layoutParams = linearLayoutParams
//
//                    // paint panel
//                    layoutParams = ConstraintLayout.LayoutParams(
//                        ((150 / 1000F) * view.width).toInt(),
//                        ConstraintLayout.LayoutParams.MATCH_PARENT
//                    )
//                    layoutParams.rightToRight = ConstraintSet.PARENT_ID
//
//                    paint_panel.layoutParams = layoutParams
//
//                    // paint indicator size
//                    val frameWidth = ((150 / 1000F) * view.width).toInt()
//                    val indicatorMargin = (frameWidth * 0.15).toInt()
//                    val indicatorWidth = frameWidth - indicatorMargin
//
//                    layoutParams = ConstraintLayout.LayoutParams(indicatorWidth, indicatorWidth)
//                    layoutParams.topToTop = ConstraintSet.PARENT_ID
//                    layoutParams.bottomToBottom = ConstraintSet.PARENT_ID
//                    layoutParams.leftToLeft = ConstraintSet.PARENT_ID
//                    layoutParams.rightToRight = ConstraintSet.PARENT_ID
//
//                    paint_indicator_view_bottom_layer.layoutParams = layoutParams
//                    paint_indicator_view.layoutParams = layoutParams
//
//                    if (SessionSettings.instance.showPaintBar) {
//                        // paint quantity bar size
//                        /*layoutParams = ConstraintLayout.LayoutParams(
//                            (paint_qty_bar.width * 1.25).toInt(),
//                            (paint_qty_bar.height * 1.25).toInt()
//                        )
//                        layoutParams.topToTop = ConstraintSet.PARENT_ID
//                        layoutParams.leftToLeft = ConstraintSet.PARENT_ID
//                        layoutParams.rightToRight = ConstraintSet.PARENT_ID
//
//                        layoutParams.topMargin = Utils.dpToPx(context, 15)
//
//                        paint_qty_bar.layoutParams = layoutParams*/
//                    }
//                    else if (SessionSettings.instance.showPaintCircle) {
//                        // paint quantity circle size
//                        layoutParams = ConstraintLayout.LayoutParams(
//                            (paint_qty_circle.width * 1.25).toInt(),
//                            (paint_qty_circle.height * 1.25).toInt()
//                        )
//                        layoutParams.topToTop = ConstraintSet.PARENT_ID
//                        layoutParams.leftToLeft = ConstraintSet.PARENT_ID
//                        layoutParams.rightToRight = ConstraintSet.PARENT_ID
//
//                        layoutParams.topMargin = Utils.dpToPx(context, 15)
//
//                        paint_qty_circle.layoutParams = layoutParams
//                    }
//
//                    //layoutParams = ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, Utils.dpToPx(context, 40))
//                    palette_name_text.textSize = 28F
//
//                    var actionButtonLayoutParams = FrameLayout.LayoutParams(Utils.dpToPx(context, 30), Utils.dpToPx(context, 30))
//                    actionButtonLayoutParams.gravity = Gravity.CENTER
//                    palette_add_color_action.layoutParams = actionButtonLayoutParams
//
//                    actionButtonLayoutParams = FrameLayout.LayoutParams(Utils.dpToPx(context, 30), Utils.dpToPx(context, 4))
//                    actionButtonLayoutParams.gravity = Gravity.CENTER
//                    palette_remove_color_action.layoutParams = actionButtonLayoutParams
//
//                    actionButtonLayoutParams = FrameLayout.LayoutParams(Utils.dpToPx(context, 27), Utils.dpToPx(context, 30))
//                    actionButtonLayoutParams.gravity = Gravity.CENTER
//                    lock_paint_panel_action.layoutParams = actionButtonLayoutParams
//
//                    layoutParams = ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.WRAP_CONTENT)
//                    layoutParams.startToStart = ConstraintSet.PARENT_ID
//                    layoutParams.endToEnd = ConstraintSet.PARENT_ID
//                    layoutParams.topToBottom = palette_name_text.id
//                    layoutParams.topMargin = Utils.dpToPx(context, 10)
//
//                    color_action_button_menu.layoutParams = layoutParams
//
//                    /*layoutParams = ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, Utils.dpToPx(context, 40))
//                    layoutParams.topMargin = Utils.dpToPx(context, 20)
//                    layoutParams.bottomMargin = Utils.dpToPx(context, 50)
//                    layoutParams.topToTop = ConstraintSet.PARENT_ID
//
//                    palette_name_text.layoutParams = layoutParams*/
//
//                    /*layoutParams = ConstraintLayout.LayoutParams(Utils.dpToPx(context, 40), Utils.dpToPx(context, 40))
//                    layoutParams.topMargin = Utils.dpToPx(context, 20)
//                    layoutParams.startToStart = ConstraintSet.PARENT_ID
//                    layoutParams.endToEnd = ConstraintSet.PARENT_ID
//                    layoutParams.topToBottom = palette_name_text.id
//
//                    palette_add_color_button.layoutParams = layoutParams
//
//                    layoutParams = ConstraintLayout.LayoutParams(Utils.dpToPx(context, 40), Utils.dpToPx(context, 40))
//                    layoutParams.topMargin = Utils.dpToPx(context, 20)
//                    layoutParams.startToStart = ConstraintSet.PARENT_ID
//                    layoutParams.endToEnd = ConstraintSet.PARENT_ID
//                    layoutParams.topToBottom = palette_name_text.id
//
//                    palette_remove_color_button.layoutParams = layoutParams
//
//                    frameLayoutParams = FrameLayout.LayoutParams(Utils.dpToPx(context, 30), Utils.dpToPx(context, 30))
//                    frameLayoutParams.topMargin = Utils.dpToPx(context, 5)
//                    frameLayoutParams.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
//
//                    palette_add_color_action.layoutParams = frameLayoutParams
//
//                    frameLayoutParams = FrameLayout.LayoutParams(Utils.dpToPx(context, 30), Utils.dpToPx(context, 6))
//                    frameLayoutParams.topMargin = Utils.dpToPx(context, 17)
//                    frameLayoutParams.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
//
//                    palette_remove_color_action.layoutParams = frameLayoutParams*/
//                }
//
//                // paint text info placement
//                if (SessionSettings.instance.showPaintBar) {
//                    val layoutParams = ConstraintLayout.LayoutParams(paint_time_info_container.width, paint_time_info_container.height)
//                    layoutParams.topToBottom = paint_qty_bar.id
//                    layoutParams.leftToLeft = ConstraintSet.PARENT_ID
//                    layoutParams.rightToRight = ConstraintSet.PARENT_ID
//
//                    layoutParams.topMargin = Utils.dpToPx(context, 0)
//
//                    paint_time_info_container.layoutParams = layoutParams
//                }
//                else if (SessionSettings.instance.showPaintCircle) {
//                    paint_time_info_container.setBackgroundColor(Color.TRANSPARENT)
//                }
//
//                // background texture scaling
//                setPanelBackground()
//
//                // right-handed
//                if (SessionSettings.instance.rightHanded) {
//                    // paint panel
//                    var layoutParams = paint_panel.layoutParams as ConstraintLayout.LayoutParams
//                    layoutParams.rightToRight = -1
//                    layoutParams.leftToLeft = ConstraintSet.PARENT_ID
//
//                    paint_panel.layoutParams = layoutParams
//
//                    paint_panel.invalidate()
//
//                    // canvas lock border
//                    layoutParams = paint_warning_frame.layoutParams as ConstraintLayout.LayoutParams
//
//                    layoutParams.leftToLeft = -1
//                    layoutParams.rightToRight = ConstraintSet.PARENT_ID
//                    layoutParams.rightToLeft = -1
//                    layoutParams.leftToRight = paint_panel.id
//                    paint_warning_frame.layoutParams = layoutParams
//
//                    // color picker
//                    layoutParams = color_picker_frame.layoutParams as ConstraintLayout.LayoutParams
//
//                    layoutParams.leftToLeft = -1
//                    layoutParams.rightToRight = ConstraintSet.PARENT_ID
//                    color_picker_frame.layoutParams = layoutParams
//
//                    // paint meter bar
//                    paint_qty_bar.rotation = 180F
//
////                    // toolbox
////                    layoutParams = open_tools_button.layoutParams as ConstraintLayout.LayoutParams
////
////                    layoutParams.rightToRight = -1
////                    layoutParams.leftToLeft = ConstraintSet.PARENT_ID
////                    open_tools_button.layoutParams = layoutParams
////
////                    var layoutParams3 = open_tools_action.layoutParams as FrameLayout.LayoutParams
////                    layoutParams3.gravity = Gravity.LEFT or Gravity.BOTTOM
////                    open_tools_action.layoutParams = layoutParams3
//
//                    // toolbox buttons
////                    val toolboxButtons = arrayOf(export_button, background_button, grid_lines_button, canvas_summary_button)
////
////                    for (button in toolboxButtons) {
////                        layoutParams = button.layoutParams as ConstraintLayout.LayoutParams
////                        layoutParams.rightToRight = -1
////                        layoutParams.leftToLeft = ConstraintSet.PARENT_ID
////                        //layoutParams.leftMargin = Utils.dpToPx(context, 6)
////                        button.layoutParams = layoutParams
////                    }
//
////                    val toolboxImages = arrayOf(export_action, background_action, grid_lines_action, canvas_summary_action)
////
////                    for (image in toolboxImages) {
////                        image.layoutParams = (image.layoutParams as FrameLayout.LayoutParams).also {
////                            it.gravity = Gravity.START or Gravity.TOP
////                        }
////                    }
//
//                    // recent colors button
////                    layoutParams = ConstraintLayout.LayoutParams(Utils.dpToPx(context, 80), Utils.dpToPx(context, 80))
////
////                    layoutParams.bottomToBottom = ConstraintSet.PARENT_ID
////                    layoutParams.rightToLeft = color_picker_frame.id
////
////                    recent_colors_button.layoutParams = layoutParams
////
////                    // recent colors action
////                    val layoutParams3 = recent_colors_action.layoutParams as FrameLayout.LayoutParams
////                    layoutParams3.gravity = Gravity.END or Gravity.BOTTOM
////                    recent_colors_action.layoutParams = layoutParams3
//
//                    // recent colors container
//                    layoutParams = recent_colors_container.layoutParams as ConstraintLayout.LayoutParams
//
//                    layoutParams.leftToRight = -1
//                    layoutParams.rightToLeft = color_picker_frame.id
//                    recent_colors_container.layoutParams = layoutParams
//
//                    layoutParams = canvas_summary_container.layoutParams as ConstraintLayout.LayoutParams
//
//                    layoutParams.startToStart = -1
//                    layoutParams.endToEnd = ConstraintSet.PARENT_ID
//                    canvas_summary_container.layoutParams = layoutParams
//
//                    //open_tools_button.layoutParams = layoutParams
//
//                    // paint yes
//                    /*var layoutParams2 = paint_yes_container.layoutParams as LinearLayout.LayoutParams
//                    layoutParams2.rightMargin = 0
//                    paint_yes_container.layoutParams = layoutParams2
//
//                    // paint no
//                    layoutParams2 = paint_no_container.layoutParams as LinearLayout.LayoutParams
//                    layoutParams2.rightMargin = Utils.dpToPx(context, 30)
//                    paint_no_container.layoutParams = layoutParams2
//
//                    paint_action_button_container.removeViewAt(0)
//                    paint_action_button_container.addView(paint_yes_container)*/
//                }
//                else {
//                    // close paint panel button
//                }

                surface_view.setInitialPositionAndScale()

                if (SessionSettings.instance.showHelpMessages) {
                    showHelpMessages()
                }
            }
        })
    }

    override fun onPause() {
        super.onPause()

        // unregister listeners
        SessionSettings.instance.paintQtyListeners.remove(this)

        context?.apply {
            //surface_view.interactiveCanvas.saveUnits(this)
            surface_view.interactiveCanvas.interactiveCanvasListener = null

            SessionSettings.instance.save(requireContext())
        }

        paintEventTimer?.cancel()
        saveViewportTimer?.cancel()

        context?.apply {

            //StatTracker.instance.save(this)
        }

        pauseCanvas()
    }

    private fun pauseCanvas() {
        Log.i("Interactive Canvas", "Canvas Pause")
        surface_view.interactiveCanvas.cancelLatencyJob()
        InteractiveCanvasSocket.instance.socketConnectCallback = null
        InteractiveCanvasSocket.instance.disconnect()

        paused = true
        pauseTime = System.currentTimeMillis() / 1000

        cancelRecentPixels()
    }

//    private fun saveDeviceViewport() {
//        val deviceViewport = surface_view.interactiveCanvas.deviceViewport!!
//
//        SessionSettings.instance.restoreDeviceViewportCenterX = deviceViewport.centerX()
//        SessionSettings.instance.restoreDeviceViewportCenterY = deviceViewport.centerY()
//
//        SessionSettings.instance.restoreCanvasScaleFactor = surface_view.interactiveCanvas.lastScaleFactor
//
//        SessionSettings.instance.saveViewportInfo(this@InteractiveCanvasFragment.requireContext())
//    }

    override fun onResume() {
        super.onResume()

        applyOptions()

//        saveViewportTimer = Timer()
//        saveViewportTimer?.schedule(object: TimerTask() {
//            override fun run() {
//                saveDeviceViewport()
//            }
//        }, 2000L, 2000L)

        Log.i("On Resume", "Canvas resumed.")

        surface_view.interactiveCanvas.interactiveCanvasListener = this

        if (paused) {
            //reload_transparent.visibility = View.VISIBLE
            //reload_transparent.setOnClickListener { }
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

    override fun onDestroyView() {
        super.onDestroyView()
        surface_view.interactiveCanvas.cancelLatencyJob()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        drawerToggle.onConfigurationChanged(newConfig)
    }

    // screen rotation
//    override fun onConfigurationChanged(newConfig: Configuration) {
//        super.onConfigurationChanged(newConfig)
//
//        if (!SessionSettings.instance.tablet) {
//            surface_view.interactiveCanvas.interactiveCanvasDrawer?.notifyRedraw()
//            return
//        }
//
//        paint_panel.background = null
//
//        Utils.setViewLayoutListener(requireView(), object : Utils.ViewLayoutListener {
//            override fun onViewLayout(view: View) {
//                // interactive canvas
//                surface_view.interactiveCanvas.deviceViewport?.apply {
//                    surface_view.interactiveCanvas.updateDeviceViewport(this@InteractiveCanvasFragment.requireContext())
//                }
//
//                // color picker frame width
//                var layoutParams = ConstraintLayout.LayoutParams(
//                    (view.width * 0.35).toInt(),
//                    ConstraintLayout.LayoutParams.MATCH_PARENT
//                )
//
//                layoutParams.leftToLeft = (color_picker_frame.layoutParams as ConstraintLayout.LayoutParams).leftToLeft
//                layoutParams.rightToRight = (color_picker_frame.layoutParams as ConstraintLayout.LayoutParams).rightToRight
//
//                color_picker_frame.layoutParams = layoutParams
//
//                // color picker default color buttons
//                var frameLayoutParams = (default_black_color_action.layoutParams as FrameLayout.LayoutParams)
//                frameLayoutParams.width = (color_picker_frame.layoutParams.width * 0.16).toInt()
//                frameLayoutParams.height = frameLayoutParams.width
//
//                default_black_color_action.layoutParams = frameLayoutParams
//
//                frameLayoutParams = (default_white_color_action.layoutParams as FrameLayout.LayoutParams)
//                frameLayoutParams.width = (color_picker_frame.layoutParams.width * 0.16).toInt()
//                frameLayoutParams.height = frameLayoutParams.width
//
//                default_white_color_action.layoutParams = frameLayoutParams
//
//                var linearLayoutParams = (default_white_color_button.layoutParams as LinearLayout.LayoutParams)
//                if (default_white_color_action.layoutParams.width <= Utils.dpToPx(context, 40)) {
//                    linearLayoutParams.marginStart = Utils.dpToPx(context, 10)
//                }
//                else {
//                    linearLayoutParams.marginStart = Utils.dpToPx(context, 20)
//                }
//                default_white_color_button.layoutParams = linearLayoutParams
//
//                val paintPanelWidth = if (Utils.isTablet(requireContext())) {
//                    ((150 / 1000F) * view.width).toInt()
//                }
//                else {
//                    ((250 / 1000F) * view.width).toInt()
//                }
//
//                // paint panel
//                layoutParams = ConstraintLayout.LayoutParams(
//                    paintPanelWidth,
//                    ConstraintLayout.LayoutParams.MATCH_PARENT
//                )
//                layoutParams.leftToLeft = (paint_panel.layoutParams as ConstraintLayout.LayoutParams).leftToLeft
//                layoutParams.rightToRight = (paint_panel.layoutParams as ConstraintLayout.LayoutParams).rightToRight
//
//                paint_panel.layoutParams = layoutParams
//
//                // paint indicator size
//                val frameWidth = ((150 / 1000F) * view.width).toInt()
//                val indicatorMargin = (frameWidth * 0.15).toInt()
//                val indicatorWidth = frameWidth - indicatorMargin
//
//                layoutParams = ConstraintLayout.LayoutParams(indicatorWidth, indicatorWidth)
//                layoutParams.topToTop = (paint_indicator_view.layoutParams as ConstraintLayout.LayoutParams).topToTop
//                layoutParams.bottomToBottom = (paint_indicator_view.layoutParams as ConstraintLayout.LayoutParams).bottomToBottom
//                layoutParams.leftToLeft = (paint_indicator_view.layoutParams as ConstraintLayout.LayoutParams).leftToLeft
//                layoutParams.rightToRight = (paint_indicator_view.layoutParams as ConstraintLayout.LayoutParams).rightToRight
//
//                paint_indicator_view_bottom_layer.layoutParams = layoutParams
//                paint_indicator_view.layoutParams = layoutParams
//
//                device_canvas_viewport_view.updateDeviceViewport()
//
//                setPanelBackground()
//            }
//        })
//    }

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
        val isLight = SessionSettings.instance.backgroundColorsIndex != 1 && SessionSettings.instance.backgroundColorsIndex != 3
        for (buttonFrame in visibleActionViews) {
            buttonFrame.isLight = isLight

            when (isLight) {
                true -> {
                    text_latency.setTextColor(Color.WHITE)
                    text_latency.setShadowLayer(2f, 1f, 1f, Color.BLACK)
                }
                false -> {
                    text_latency.setTextColor(Color.BLACK)
                    text_latency.setShadowLayer(2f, 1f, 1f, Color.WHITE)
                }
            }
        }
    }

    private fun updateSelectedColor(color: Int) {
        Log.i("Selected Color", color.toString())

        paint_indicator_view_bottom_layer.setPaintColor(color)

        // color panel icons
        colorPanelIcons.forEach {
            it.updateAppearance(color)
        }

        // palette color actions
        syncPaletteAndColor()
    }

    private fun setupColorPalette() {
        if (Utils.isTablet(requireContext())) {
            val layoutParams = color_palette_view.layoutParams as ConstraintLayout.LayoutParams
            layoutParams.dimensionRatio = "16:1"
            color_palette_view.layoutParams = layoutParams

            color_palette_view.rows = 1
            color_palette_view.cols = 16
        }

        color_palette_view.listener = this
        color_palette_view.colors = SessionSettings.instance.colorPaletteColors
            ?.toMutableList() ?: mutableListOf()

        recent_color_palette_view.listener = this
        recent_color_palette_view.colors = SessionSettings.instance.recentColorPaletteColors
            ?.toMutableList() ?: mutableListOf()
    }

    private fun invalidateButtons() {
        menu_action.invalidate()
        export_action.invalidate()
        object_move_up_action.invalidate()
        object_move_down_action.invalidate()
        object_move_left_action.invalidate()
        object_move_right_action.invalidate()
    }

    // view toggles
    private fun togglePaintPanel(show: Boolean, softHide: Boolean = false) {

    }

    private fun showCanvasMenu() {
        if (surface_view.isExporting()) {
            export_fragment_container.visibility = View.INVISIBLE
            surface_view.endExport()

            toggleExportBorder(false)
            export_button.toggleState = ButtonFrame.ToggleState.NONE

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
        else if (showServerListState.value) {
            showServerListState.value = false
        }
        else {
            canvas_menu.visibility = View.VISIBLE
            showMenuState.value = true
            //(requireActivity() as InteractiveCanvasActivity).showOptionsFragment(this)
            //toggleMenu(menu_container.visibility != View.VISIBLE)
        }
    }

    private fun closeCanvasMenu() {
        showMenuState.value = false
        canvas_menu.visibility = View.GONE
    }

    private fun showServerList() {
        server_list.visibility = View.VISIBLE
        showServerListState.value = true
    }

    private fun closeServerList() {
        showServerListState.value = false

        server_list.visibility = View.GONE
    }

    private fun onPaintIndicatorClick() {
        // start color selection mode
        if (color_picker_frame.visibility != View.VISIBLE) {
            color_picker_frame.visibility = View.VISIBLE

            recent_colors_container.visibility = View.GONE

            colorPickerFragment?.setColor(SessionSettings.instance.paintColor, true)
            surface_view.startPaintSelection()
            addBackToColorSelection()
        }
        else {
            color_picker_frame.visibility = View.GONE
            surface_view.endPaintSelection()
            addBackToEndPainting()
        }
    }

    private fun toggleTools(show: Boolean) {
        if (!animatingTools) {
            if (show && !toolboxOpen) {
                animatingTools = true

                Animator.animateMenuItems(
                    listOf(
                        listOf(export_button)
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
                        listOf(export_button)
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
//        var color = ActionButtonView.lightYellowSemiPaint.color
//        if (double) {
//            color = ActionButtonView.lightGreenPaint.color
//        }
//        if (show) {
//            context?.apply {
//                val drawable: GradientDrawable = export_border_view.background as GradientDrawable
//                drawable.setStroke(
//                    Utils.dpToPx(this, 2),
//                    color
//                ) // set stroke width and stroke color
//            }
//            export_border_view.visibility = View.VISIBLE
//        }
//        else {
//            export_border_view.visibility = View.GONE
//
//            if (double) {
//                export_button.toggleState = ButtonFrame.ToggleState.SINGLE
//            }
//            else {
//                export_button.toggleState = ButtonFrame.ToggleState.NONE
//            }
//
//            export_action.invalidate()
//        }
        export_button.select(show)
        export_button.invalidate()
    }

    private fun changeBackground() {
        if (SessionSettings.instance.backgroundColorsIndex >= surface_view.interactiveCanvas.numBackgrounds - 1) {
            SessionSettings.instance.backgroundColorsIndex = 0
        }
        else {
            SessionSettings.instance.backgroundColorsIndex += 1
        }

        SessionSettings.instance.darkIcons = (SessionSettings.instance.backgroundColorsIndex == 1 || SessionSettings.instance.backgroundColorsIndex == 3)
        lineColorDarkState.value = SessionSettings.instance.darkIcons
        recolorVisibleActionViews()

        invalidateButtons()

        if (canvas_summary_container.visibility == View.VISIBLE) {
            canvas_summary_view.invalidate()
        }

        surface_view.interactiveCanvas.interactiveCanvasDrawer?.notifyRedraw()

        SessionSettings.instance.saveBackground(requireContext())
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

            // set bottom-left of view to screenPoint

            val fragment = pixel_history_fragment_container?.run {
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

                val fragment = view?.run {
                    if (pixel_history_fragment_container.x < Utils.dpToPx(context, 20).toFloat()) {
                        pixel_history_fragment_container.x = Utils.dpToPx(context, 20).toFloat()
                    } else if (pixel_history_fragment_container.x + pixel_history_fragment_container.width > width - Utils.dpToPx(context, 20).toFloat()) {
                        pixel_history_fragment_container.x =
                            width - pixel_history_fragment_container.width.toFloat() - Utils.dpToPx(
                                context,
                                20
                            ).toFloat()
                    }

                    if (pixel_history_fragment_container.y < Utils.dpToPx(context, 40).toFloat()) {
                        pixel_history_fragment_container.y = Utils.dpToPx(context, 40).toFloat()
                    } else if (pixel_history_fragment_container.y + pixel_history_fragment_container.height > height - Utils.dpToPx(context, 20).toFloat()) {
                        pixel_history_fragment_container.y =
                            height - pixel_history_fragment_container.height.toFloat() - Utils.dpToPx(
                                context,
                                20
                            ).toFloat()
                    }

                    val fragment = PixelHistoryFragment.create(server!!)

                    fragment.setPixelHistoryJson(null)

                    beginTransaction().replace(
                        R.id.pixel_history_fragment_container,
                        fragment
                    ).commit()

                    pixel_history_fragment_container.visibility = View.VISIBLE

                    fragment
                }

                fragment
            }

            surface_view.interactiveCanvas.getPixelHistory(surface_view.interactiveCanvas.pixelIdForUnitPoint(
                surface_view.interactiveCanvas.lastSelectedUnitPoint
            ), object : PixelHistoryCallback {
                override fun onHistoryJsonResponse(historyJson: JSONArray) {
                    fragment?.setPixelHistoryJson(historyJson)
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
    override fun notifyCoords(x: Int, y: Int, color: Int) {
        //text_coords.setTextColor(color)
        text_coords.text = "($x, $y) - ${String.format("%.2f", surface_view.interactiveCanvas.lastScaleFactor * 2)}"
    }

    override fun notifyPaintColorUpdate(color: Int) {

    }

    override fun notifyPickCanvasColor(color: Int) {
        colorPickerFragment?.listeners?.forEach { it.pickCanvasColor(color) }
    }

    override fun notifyPaintingStarted() {

    }

    override fun notifyPaintingEnded() {

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

    private var msValue = 0L
    private var count = 0

    override fun notifySocketLatency(ms: String, msValue: Long) {
        var str = ms
        if (count > 1) {
            str = "($count) $ms"
        }
        text_latency.text = str

        this.msValue = msValue
    }

    override fun notifySocketLatencyText(text: String) {
        menuLatencyText?.text = text
    }

    override fun notifyConnectionCount(count: Int) {
        this.count = count
        if (count < 2) return

        Log.d("Connection Count", count.toString())
        text_latency.text = "($count) ${msValue} ms"
    }

    override fun notifyClientsInfo(clientsInfo: List<Triple<String, Int, Int>>) {
        Log.d("Clients Info", clientsInfo.joinToString("|") { "${it.first}, ${it.second}" })
        clientsInfoState.value = clientsInfo
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

    override fun onInteractiveCanvasDoubleTap() {

    }

    // paint qty listener
    override fun paintQtyChanged(qty: Int) {
        //drops_amt_text.text = qty.toString()
        lifecycleScope.launch {
            activity?.runOnUiThread {
                paint_amt_info.text = qty.toString()
            }
            text_bottom_display.text = qty.toString()

            if (surface_view.mode == InteractiveCanvasView.Mode.PAINTING) {
                requireActivity().title = qty.toString()
            }
        }
    }

    // recent colors listener
    override fun onNewRecentColors(colors: List<Int>) {
        setupColorPalette()
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
        toggleExportBorder(false)
        export_button.toggleState = ButtonFrame.ToggleState.NONE
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
        childFragmentManager.popBackStack()

        export_fragment_container.visibility = View.GONE
        surface_view.endExport()

        (requireActivity() as? AppCompatActivity)?.supportActionBar?.show()

        addBackToMenuOnBackPressed()
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

            setupColorPalette()
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

            setupColorPalette()
        }
    }

    // draw frame config listener
    override fun createDrawFrame(centerX: Int, centerY: Int, width: Int, height: Int, color: Int) {
        surface_view.createDrawFrame(centerX, centerY, width, height, color)

        closePopoverFragment()
    }

    private var summaryUpdateJob: Job? = null

    private fun toggleCanvasSummary() {
        if (canvas_summary_container.visibility != View.VISIBLE) {
            summaryUpdateJob?.cancel()
            summaryUpdateJob = requireActivity().lifecycleScope.launch {
                while (true) {
                    surface_view.interactiveCanvas.bitmap?.let {
                        canvas_summary_view.setImageBitmap(it)
                    }
                    withContext(Dispatchers.Default) {
                        delay(1000 * 7)
                    }
                }
            }

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

    private val coroutineScope = CoroutineScope(Dispatchers.Main.immediate)
    private var recentPixelsJob: Job? = null

    private fun startRecentPixels() {
        if (server == null) return
        if (recentPixelsJob != null) return

        recentPixelsJob = coroutineScope.launch {
            while (true) {
                canvasService?.getRecentPixels(pauseTime) { pixels ->
                    Log.i("Recent Pixels", "Got recent pixels")
                    pixels?.also {
                        for (element in it) {
                            val pixelInfo = element.asString
                            surface_view?.interactiveCanvas?.receivePixels(pixelInfo)
                        }
                    }
                }
                pauseTime = System.currentTimeMillis()
                withContext(Dispatchers.Default) {
                    delay(85 * 1000)
                }
            }
        }
    }

    private fun cancelRecentPixels() {
        recentPixelsJob?.cancel()
        recentPixelsJob = null
    }

    // socket callback
    override fun onSocketConnect() {
        Log.i("Canvas Socket", "Socket connected!")

        SessionSettings.instance.paintQtyListeners.add(this)

        startRecentPixels()

        SessionSettings.instance.uniqueId?.also { uuid ->
            canvasService?.getPaintQty(uuid) { paintQtyInfo ->
                paintQtyInfo?.also {
                    val paintQty = it.get("paint_qty").asInt
                    Log.i("Canvas Service", "Paint qty = $paintQty")
                    SessionSettings.instance.dropsAmt = paintQty
                    //reload_transparent?.visibility = View.GONE
                }
            }
        }

        surface_view.interactiveCanvas
            .registerForSocketEvents(InteractiveCanvasSocket.instance.requireSocket())

        if (canvasLoader?.loadingDone() == true) {
            surface_view.interactiveCanvas.startLatencyJob()
        }

        updateSocketStatus(true)
    }

    override fun onSocketDisconnect(error: Boolean) {
        Log.i("Canvas Socket", "Socket disconnect ($error).")

        updateSocketStatus(false)

        if (error || leave) {
            activity?.runOnUiThread {
                InteractiveCanvasSocket.instance.socketConnectCallback = null
                (activity as? InteractiveCanvasActivity)?.showMenuFragment()
            }
        }
    }

    private fun reconnectToSocket() {
        InteractiveCanvasSocket.instance.startSocket(server!!)
        updateSocketStatus(true)
    }

    private fun scheduleReconnect() {
        val rSec = (Math.random() * 25 + 5).toLong()
        Log.i("Canvas Socket", "Scheduling reconnect in $rSec seconds")
        Observable.timer(rSec, TimeUnit.SECONDS).subscribe()
    }

    private fun applyOptions() {
        // panel background
        //setPanelBackground()

        if (server != null) {
            requireActivity().title = "${server!!.name} (${SessionSettings.instance.displayNameOrId()})"
        }

        // panel theme config
        panelThemeConfig = PanelThemeConfig.buildConfig(SessionSettings.instance.panelResIds[SessionSettings.instance.panelBackgroundResIndex])

        if (panelThemeConfig.actionButtonColor == Color.BLACK) {
            palette_name_text.setTextColor(Color.parseColor("#FF111111"))
            palette_name_text.setShadowLayer(3F, 2F, 2F, Color.parseColor("#7F333333"))

            palette_add_color_action.colorMode = ActionButtonView.ColorMode.BLACK
            palette_remove_color_action.colorMode = ActionButtonView.ColorMode.BLACK

            lock_paint_panel_action.colorMode = ActionButtonView.ColorMode.BLACK
        }
        else {
            palette_name_text.setTextColor(Color.WHITE)

            palette_add_color_action.colorMode = ActionButtonView.ColorMode.WHITE
            palette_remove_color_action.colorMode = ActionButtonView.ColorMode.WHITE

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

        addBackToMenuOnBackPressed()
    }

    private fun updateSocketStatus(connected: Boolean) {
        activity?.runOnUiThread {
            if (connected) {
                image_no_socket.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.green_circle))
                menuSocketStatusImage?.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.green_circle))
            }
            else {
                image_no_socket.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.red_circle))
                menuSocketStatusImage?.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.red_circle))
            }
        }

        surface_view.interactiveCanvas.connectedState.value = connected
    }

    private fun setupStreamBanner() {
        if (server == null || !server!!.showBanner) return

        stream_banner.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(server!!.iconLink))
            startActivity(intent)
        }

        banner_text.text = server!!.bannerText

        Glide.with(this)
            .load(server!!.iconUrl)
            .listener(object: RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    stream_banner.animate().setDuration(200).alpha(1F).start()
                    return false
                }
            })
            .circleCrop()
            .into(banner_icon)

        Timer().schedule(object: TimerTask() {
            override fun run() {
                activity?.runOnUiThread {
                    stream_banner.animate().setDuration(500).alpha(0F).withEndAction {
                        stream_banner.visibility = View.GONE
                    }.start()
                }
            }
        }, 3000)
    }

    // Recent Colors View Listener
    override fun onSelectColor(color: Int) {
        updateSelectedColor(color)
    }

    override fun onRequestLoadColor(index: Int) {}

    private fun showHelpMessages() {
        help_messages.visibility = View.VISIBLE
    }

    // Interactive Canvas View Mode Listener
    override fun onModeChanged(mode: InteractiveCanvasView.Mode) {
        updateSelectedColor(SessionSettings.instance.paintColor)
    }

    // Data Loading Callback
    override fun onDataLoaded(server: Server) {
        this.server = server
        onServer()
    }

    override fun onDataLoaded(world: Boolean, realmId: Int) {

    }

    override fun onConnectionError() {

    }
}