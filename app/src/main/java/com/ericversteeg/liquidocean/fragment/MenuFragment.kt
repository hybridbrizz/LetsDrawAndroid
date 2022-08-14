package com.ericversteeg.liquidocean.fragment

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.ericversteeg.liquidocean.R
import com.ericversteeg.liquidocean.activity.InteractiveCanvasActivity
import com.ericversteeg.liquidocean.adapter.ServersRecyclerAdapter
import com.ericversteeg.liquidocean.helper.Animator
import com.ericversteeg.liquidocean.helper.Utils
import com.ericversteeg.liquidocean.listener.MenuButtonListener
import com.ericversteeg.liquidocean.listener.MenuCardListener
import com.ericversteeg.liquidocean.model.InteractiveCanvas
import com.ericversteeg.liquidocean.model.SessionSettings
import com.ericversteeg.liquidocean.service.ServerService
import com.ericversteeg.liquidocean.view.ActionButtonView
import kotlinx.android.synthetic.main.fragment_menu.*
import java.util.*

class MenuFragment: Fragment() {

    var canvasFragment: InteractiveCanvasFragment? = null

    var menuButtonListener: MenuButtonListener? = null

    var backCount = 0

    var showcaseTimer = Timer()

    var route = -1

    var animatingMenu = false

    var menuButtonContainerWidth = 0

    var menuCardListener: MenuCardListener? = null

    var touchEventCount = 1
    var touchEventPollInterval = 5

    var touchTotalX = 0F
    var touchTotalY = 0F

    private val service = ServerService()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_menu, container, false)

        // setup views here

        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        (requireActivity() as InteractiveCanvasActivity).canvasFragment = canvasFragment
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        SessionSettings.instance.canvasOpen = false

        view.setBackgroundColor(Color.BLACK)

        view.setBackgroundResource(SessionSettings.instance.menuBackgroundResId)

        back_button.setOnClickListener {
            if (recycler_view_servers.visibility == View.VISIBLE) {
                showMenuOptions()
            }
            else if (connect_input_container.visibility == View.VISIBLE) {
                if (SessionSettings.instance.servers.isEmpty()) {
                    showMenuOptions()
                }
                else {
                    showServerList()
                }
            }

            if (backCount == 1) {
                resetMenu()
                animateMenuButtons(0)
            }
            else if (backCount == 2) {
                resetToPlayMode()
                animateMenuButtons(1)
            }

            backCount--
        }

        back_button.visibility = View.GONE

        stats_button.type = ActionButtonView.Type.STATS
        stats_button.topLayer = true
        stats_button_bottom_layer.type = ActionButtonView.Type.STATS

        single_button.type = ActionButtonView.Type.SINGLE
        single_button.topLayer = true
        single_button_bottom_layer.type = ActionButtonView.Type.SINGLE

        world_button.type = ActionButtonView.Type.WORLD
        world_button.topLayer = true
        world_button_bottom_layer.type = ActionButtonView.Type.WORLD

        //dev_button.type = ActionButtonView.Type.DEV
        //dev_button.topLayer = true
        //dev_button_bottom_layer.type = ActionButtonView.Type.DEV

        /*val artShowcase = SessionSettings.instance.artShowcase
        if (artShowcase != null && artShowcase.size > 0) {
            art_showcase.showBackground = false
            art_showcase.art = artShowcase[0]
        }*/

        /*draw_menu_text.setOnClickListener {
            /*// menuButtonListener?.onMenuButtonSelected(playMenuIndex)
            play_button_container.visibility = View.GONE

            options_button_container.visibility = View.GONE

            stats_button_container.visibility = View.GONE

            howto_button_container.visibility = View.GONE

            single_button_container.visibility = View.VISIBLE

            world_button_container.visibility = View.INVISIBLE

            dev_button_container.visibility = View.VISIBLE

            empty_button_2_container.visibility = View.VISIBLE

            back_button.visibility = View.VISIBLE
            backCount++

            animateMenuButtons(1)*/

            if (!SessionSettings.instance.selectedHand) {
                draw_button_container.visibility = View.GONE
                options_button_container.visibility = View.GONE
                stats_button_container.visibility = View.GONE
                howto_button_container.visibility = View.GONE

                lefty_button_container.visibility = View.VISIBLE
                righty_button_container.visibility = View.VISIBLE

                //empty_button_1_container.visibility = View.VISIBLE
                //empty_button_2_container.visibility = View.VISIBLE

                route = singleMenuIndex

                backCount++
                animateMenuButtons(2)
            }
            else {
                menuButtonListener?.onMenuButtonSelected(singleMenuIndex)
                menuCardListener?.closeMenu()
            }
        }*/

        menu_button.setOnClickListener {
            showServerList()
        }

        connect_button.setOnClickListener {
            //menuButtonListener?.onMenuButtonSelected(worldMenuIndex)
            if (SessionSettings.instance.servers.isEmpty()) {
                showConnectInput()
            }
            else {
                SessionSettings.instance.lastVisitedServer?.also {
                    menuButtonListener?.onServerSelected(it)
                } ?: showServerList()
            }
        }

        options_button.setOnClickListener {
            menuButtonListener?.onMenuButtonSelected(optionsMenuIndex)
            menuCardListener?.closeMenu()
            clearMenuTextHighlights()
        }

        stats_button.setOnClickListener {
            menuButtonListener?.onMenuButtonSelected(statsMenuIndex)
        }

        howto_button.setOnClickListener {
            menuButtonListener?.onMenuButtonSelected(howtoMenuIndex)
            menuCardListener?.closeMenu()
            clearMenuTextHighlights()
        }

        single_button.setOnClickListener {

        }

        world_button.setOnClickListener {
            if (!SessionSettings.instance.selectedHand) {
                single_button_container.visibility = View.GONE
                world_button_container.visibility = View.GONE
                dev_button_container.visibility = View.GONE

                lefty_button.visibility = View.VISIBLE
                righty_button.visibility = View.VISIBLE

                empty_button_1_container.visibility = View.VISIBLE

                route = worldMenuIndex

                backCount++
                animateMenuButtons(2)
            }
            else {
                clearMenuTextHighlights()
                menuButtonListener?.onMenuButtonSelected(worldMenuIndex)
            }
        }

        dev_button.setOnClickListener {
            if (!SessionSettings.instance.selectedHand) {
                single_button_container.visibility = View.GONE
                world_button_container.visibility = View.GONE
                dev_button_container.visibility = View.GONE

                lefty_button.visibility = View.VISIBLE
                righty_button.visibility = View.VISIBLE

                empty_button_1_container.visibility = View.VISIBLE

                route = devMenuIndex

                backCount++
                animateMenuButtons(2)
            }
            else {
                menuButtonListener?.onMenuButtonSelected(devMenuIndex)
            }
        }

        lefty_menu_text.setOnClickListener {
            menuButtonListener?.onMenuButtonSelected(leftyMenuIndex, singleMenuIndex)
            showMenuOptions()
        }

        righty_menu_text.setOnClickListener {
            menuButtonListener?.onMenuButtonSelected(rightyMenuIndex, singleMenuIndex)
            showMenuOptions()
        }

        menu_button_container.setOnClickListener {

        }

        if (SessionSettings.instance.selectedHand) {
            connect_button.visibility = View.VISIBLE
            options_button.visibility = View.VISIBLE
            howto_button.visibility = View.VISIBLE

            if (canvasFragment != null) {
                connect_button.visibility = View.GONE
            }

            animateMenuButtons(0)
        }

        view.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (view == null) return

                view.viewTreeObserver.removeOnGlobalLayoutListener(this)

                if (menu_button_container == null) return

                if (view.height > Utils.dpToPx(context, 500)) {
                    SessionSettings.instance.tablet = true
                }

                val safeViews: MutableList<View> = ArrayList()

                if (art_showcase != null) {
                    safeViews.add(art_showcase)
                    safeViews.add(menu_button_container_horizontal_spacer)

                    Animator.animatePixelColorEffect(pixel_view_1, view, safeViews.toList())
                    Animator.animatePixelColorEffect(pixel_view_2, view, safeViews.toList())
                    Animator.animatePixelColorEffect(pixel_view_3, view, safeViews.toList())
                    Animator.animatePixelColorEffect(pixel_view_4, view, safeViews.toList())
                    Animator.animatePixelColorEffect(pixel_view_5, view, safeViews.toList())
                    Animator.animatePixelColorEffect(pixel_view_6, view, safeViews.toList())
                    Animator.animatePixelColorEffect(pixel_view_7, view, safeViews.toList())
                }
                view.viewTreeObserver.removeOnGlobalLayoutListener(this)

                menuButtonContainerWidth = menu_button_container.width
            }
        })

        view.setOnTouchListener(object: View.OnTouchListener {
            override fun onTouch(vw: View?, ev: MotionEvent?): Boolean {
                mPanDetector.onTouchEvent(ev)

                return true
            }
        })

        if (!SessionSettings.instance.selectedHand) {
            selectHand()
        }
    }

    fun clearMenuTextHighlights() {

    }

    private fun selectHand() {
        if (!SessionSettings.instance.selectedHand) {
            lefty_button.visibility = View.VISIBLE
            righty_button.visibility = View.VISIBLE

            animateMenuButtons(2)
        }
    }

    private fun showMenuOptions() {
        resetMenu()

        menu_button.visibility = View.VISIBLE

        connect_button.visibility = View.VISIBLE
        options_button.visibility = View.VISIBLE
        howto_button.visibility = View.VISIBLE

        animateMenuButtons(0)
    }

    private fun showConnectInput() {
        resetMenu()

        back_button.visibility = View.VISIBLE
        menu_button.visibility = View.GONE

        button_add_server.setOnClickListener {
            val accessKey = input_access_key.text.toString().trim().uppercase()
            if (accessKey.length == 5 || accessKey.length == 8) {
                it.isEnabled = false

                if (SessionSettings.instance.hasServer(accessKey)) {
                    it.isEnabled = true
                    return@setOnClickListener
                }

                service.getServer(accessKey) { _, server ->
                    it.isEnabled = true

                    if (server == null) {
                        Toast.makeText(requireContext(), "Can't find server", Toast.LENGTH_LONG).show()
                        return@getServer
                    }
                    SessionSettings.instance.addServer(requireContext(), server)
                    showServerList()
                }
            }
        }

        connect_input_container.visibility = View.VISIBLE

        animateMenuButtons(3)
    }

    private fun showServerList() {
        menu_button.visibility = View.GONE
        back_button.visibility = View.VISIBLE
        add_button.visibility = View.VISIBLE

        add_button.setOnClickListener {
            showConnectInput()
        }

        connect_button.visibility = View.GONE
        options_button.visibility = View.GONE
        howto_button.visibility = View.GONE

        connect_input_container.visibility = View.GONE

        recycler_view_servers.visibility = View.VISIBLE

        with (recycler_view_servers) {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            adapter = ServersRecyclerAdapter(requireContext(), SessionSettings.instance.servers) { server ->
                menuButtonListener?.onServerSelected(server)
            }
        }
    }

    // panning

    private val mGestureListener = object : GestureDetector.SimpleOnGestureListener() {

        override fun onScroll(
            e1: MotionEvent,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            touchTotalX += distanceX
            touchTotalY += distanceY
            if (touchEventCount == touchEventPollInterval) {
                menuCardListener?.moveMenuCardBy(-touchTotalX, -touchTotalY)
                touchEventCount = 1
            }
            else {
                touchEventCount++
            }

            return true
        }
    }

    private val mPanDetector = GestureDetector(context, mGestureListener)

    private fun fadeInAllView(list: List<View>) {
        for (vw in list) {
            Animator.fadeInView(vw, 1000)
        }
    }

    private fun animateMenuButtons(layer: Int, out: Boolean = false) {
        if (!animatingMenu) {
            animatingMenu = true
            if (layer == 0) {
                Animator.animateMenuItems(listOf(
                    listOf(connect_menu_text), listOf(options_menu_text),
                    listOf(stats_button_bottom_layer, stats_button), listOf(how_to_menu_text)), cascade = true, out = false, inverse = false,
                    completion = object: Animator.CompletionHandler {
                        override fun onCompletion() {
                            animatingMenu = false
                        }
                    }
                )
            }
            else if (layer == 1) {
                Animator.animateMenuItems(listOf(listOf(single_button_bottom_layer, single_button), listOf(world_button_bottom_layer, world_button),
                    listOf(dev_button_bottom_layer, dev_button)), cascade = true, out = false, inverse = false,
                    completion = object: Animator.CompletionHandler {
                        override fun onCompletion() {
                            animatingMenu = false
                        }
                    }
                )
            }
            else if (layer == 2) {
                Animator.animateMenuItems(listOf(listOf(lefty_menu_text), listOf(righty_menu_text)),
                    cascade = true, out = false, inverse = false,
                    completion = object: Animator.CompletionHandler {
                        override fun onCompletion() {
                            animatingMenu = false
                        }
                    }
                )
            }
            else if (layer == 3) {
                Animator.animateMenuItems(listOf(listOf(connect_input_container)),
                    cascade = true, out = false, inverse = false,
                    completion = object: Animator.CompletionHandler {
                        override fun onCompletion() {
                            animatingMenu = false
                        }
                    }
                )
            }
            else if (layer == 4) {
                Animator.animateMenuItems(listOf(listOf(connect_input_container)),
                    cascade = true, out = false, inverse = false,
                    completion = object: Animator.CompletionHandler {
                        override fun onCompletion() {
                            animatingMenu = false
                        }
                    }
                )
            }
            else {
                animatingMenu = false
            }
        }
    }

    fun getNextArtShowcase(): List<InteractiveCanvas.RestorePoint>? {
        SessionSettings.instance.artShowcase?.apply {
            if (SessionSettings.instance.showcaseIndex == size) {
                SessionSettings.instance.showcaseIndex = 0
            }

            if (size > 0) {
                val nextArt = this[SessionSettings.instance.showcaseIndex]
                SessionSettings.instance.showcaseIndex += 1

                return nextArt
            }
        }

        return null
    }

    override fun onPause() {
        super.onPause()

        showcaseTimer.cancel()
        Animator.context = null
    }

    override fun onResume() {
        super.onResume()

        Animator.context = context

        showcaseTimer = Timer()
        showcaseTimer.schedule(object: TimerTask() {
            override fun run() {
                activity?.runOnUiThread {
                    SessionSettings.instance.artShowcase?.apply {
                        if (art_showcase != null) {
                            art_showcase.alpha = 0F

                            art_showcase.showBackground = false
                            art_showcase.art = getNextArtShowcase()

                            art_showcase.animate().alpha(1F).setDuration(2500).withEndAction {
                                Timer().schedule(object: TimerTask() {
                                    override fun run() {
                                        activity?.runOnUiThread {
                                            if (art_showcase != null) {
                                                art_showcase.animate().alpha(0F).setDuration(1500).start()
                                            }
                                        }
                                    }

                                }, 3000)
                            }.start()
                        }
                    }
                }
            }

        }, 0, 7000)
    }

    private fun resetMenu() {
        //draw_button_container.visibility = View.VISIBLE

        connect_button.visibility = View.GONE

        options_button.visibility = View.GONE

        stats_button_container.visibility = View.GONE

        howto_button.visibility = View.GONE

        single_button_container.visibility = View.GONE

        world_button_container.visibility = View.GONE

        dev_button_container.visibility = View.GONE

        lefty_button.visibility = View.GONE

        righty_button.visibility = View.GONE

        empty_button_2_container.visibility = View.GONE

        connect_input_container.visibility = View.GONE

        recycler_view_servers.visibility = View.GONE

        back_button.visibility = View.GONE
        add_button.visibility = View.GONE
    }

    private fun resetToPlayMode() {
        single_button_container.visibility = View.VISIBLE

        world_button_container.visibility = View.VISIBLE

        dev_button_container.visibility = View.VISIBLE

        empty_button_2_container.visibility = View.VISIBLE

        lefty_button.visibility = View.GONE

        righty_button.visibility = View.GONE

        empty_button_1_container.visibility = View.GONE

        back_button.visibility = View.VISIBLE
    }

    companion object {
        val playMenuIndex = 0
        val optionsMenuIndex = 1
        val statsMenuIndex = 2
        val howtoMenuIndex = 3
        val singleMenuIndex = 4
        val worldMenuIndex = 5
        val devMenuIndex = 6
        val leftyMenuIndex = 7
        val rightyMenuIndex = 8

        fun createFromCanvas(canvasFragment: InteractiveCanvasFragment): MenuFragment {
            val fragment = MenuFragment()
            fragment.canvasFragment = canvasFragment
            return fragment
        }
    }
}