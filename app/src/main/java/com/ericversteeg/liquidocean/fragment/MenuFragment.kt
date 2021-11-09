package com.ericversteeg.liquidocean.fragment

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.children
import androidx.fragment.app.Fragment
import com.ericversteeg.liquidocean.R
import com.ericversteeg.liquidocean.helper.Animator
import com.ericversteeg.liquidocean.helper.Utils
import com.ericversteeg.liquidocean.model.SessionSettings
import com.ericversteeg.liquidocean.listener.MenuButtonListener
import com.ericversteeg.liquidocean.model.InteractiveCanvas
import com.ericversteeg.liquidocean.view.ActionButtonView
import kotlinx.android.synthetic.main.fragment_art_export.*
import kotlinx.android.synthetic.main.fragment_menu.*
import kotlinx.android.synthetic.main.fragment_menu.back_action
import kotlinx.android.synthetic.main.fragment_menu.back_button
import kotlinx.android.synthetic.main.fragment_options.*
import java.util.*
import kotlin.collections.ArrayList

class MenuFragment: Fragment() {

    var menuButtonListener: MenuButtonListener? = null

    var backCount = 0

    var showcaseTimer = Timer()

    var route = -1

    var animatingMenu = false

    var menuButtonContainerWidth = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_menu, container, false)

        // setup views here

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        SessionSettings.instance.canvasOpen = false

        view.setBackgroundColor(Color.BLACK)

        val allViews = listOf<View>(back_button, back_action, draw_menu_text,
            options_menu_text, how_to_menu_text, menu_button_container)

        fadeInAllView(allViews)

        back_button.actionBtnView = back_action
        back_action.type = ActionButtonView.Type.BACK_SOLID

        view.setBackgroundResource(SessionSettings.instance.menuBackgroundResId)

        back_button.setOnClickListener {
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

        // backgrounds
        background_option_black.type = ActionButtonView.Type.BACKGROUND_BLACK
        background_option_white.type = ActionButtonView.Type.BACKGROUND_WHITE
        background_option_gray_thirds.type = ActionButtonView.Type.BACKGROUND_GRAY_THIRDS
        background_option_photoshop.type = ActionButtonView.Type.BACKGROUND_PHOTOSHOP
        background_option_classic.type = ActionButtonView.Type.BACKGROUND_CLASSIC
        background_option_chess.type = ActionButtonView.Type.BACKGROUND_CHESS

        val menuTextViews = listOf(draw_menu_text, options_menu_text, how_to_menu_text)
        for (textView in menuTextViews) {
            textView.setOnTouchListener(object: View.OnTouchListener {
                override fun onTouch(view: View?, ev: MotionEvent): Boolean {
                    if (ev.action == MotionEvent.ACTION_DOWN) {
                        textView.setTextColor(ActionButtonView.altGreenPaint.color)
                    }
                    else if (ev.action == MotionEvent.ACTION_CANCEL) {
                        textView.setTextColor(Color.parseColor("#DDFFFFFF"))
                    }

                    return false
                }
            })
        }

        draw_menu_text.setOnClickListener {
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
            }
        }

        options_menu_text.setOnClickListener {
            menuButtonListener?.onMenuButtonSelected(optionsMenuIndex)
        }

        stats_button.setOnClickListener {
            menuButtonListener?.onMenuButtonSelected(statsMenuIndex)
        }

        how_to_menu_text.setOnClickListener {
            menuButtonListener?.onMenuButtonSelected(howtoMenuIndex)
        }

        single_button.setOnClickListener {

        }

        world_button.setOnClickListener {
            if (!SessionSettings.instance.selectedHand) {
                single_button_container.visibility = View.GONE
                world_button_container.visibility = View.GONE
                dev_button_container.visibility = View.GONE

                lefty_button_container.visibility = View.VISIBLE
                righty_button_container.visibility = View.VISIBLE

                empty_button_1_container.visibility = View.VISIBLE

                route = worldMenuIndex

                backCount++
                animateMenuButtons(2)
            }
            else {
                menuButtonListener?.onMenuButtonSelected(worldMenuIndex)
            }
        }

        dev_button.setOnClickListener {
            if (!SessionSettings.instance.selectedHand) {
                single_button_container.visibility = View.GONE
                world_button_container.visibility = View.GONE
                dev_button_container.visibility = View.GONE

                lefty_button_container.visibility = View.VISIBLE
                righty_button_container.visibility = View.VISIBLE

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
            menuButtonListener?.onMenuButtonSelected(leftyMenuIndex, route)
        }

        righty_menu_text.setOnClickListener {
            menuButtonListener?.onMenuButtonSelected(rightyMenuIndex, route)
        }

        menu_button_container.setOnClickListener {

        }

        for (v1 in single_background_options.children) {
            val v = v1 as ViewGroup
            for (v2 in v.children) {
                v2.setOnClickListener {
                    val actionButton = it as ActionButtonView
                    menuButtonListener?.onSingleBackgroundOptionSelected(actionButton.type)
                }
            }
        }

        animateMenuButtons(0)

        view.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
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

        //view.alpha = 0F
        //view.animate().alphaBy(1F).setDuration(500)
    }

    private fun fadeInAllView(list: List<View>) {
        for (vw in list) {
            Animator.fadeInView(vw, 1000)
        }
    }

    private fun animateMenuButtons(layer: Int, out: Boolean = false) {
        if (!animatingMenu) {
            animatingMenu = true
            if (layer == 0) {
                Animator.animateMenuItems(listOf(listOf(draw_menu_text), listOf(options_menu_text),
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
                                            art_showcase.animate().alpha(0F).setDuration(1500).start()
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
        draw_button_container.visibility = View.VISIBLE

        options_button_container.visibility = View.VISIBLE

        stats_button_container.visibility = View.GONE

        howto_button_container.visibility = View.VISIBLE

        single_button_container.visibility = View.GONE

        world_button_container.visibility = View.GONE

        dev_button_container.visibility = View.GONE

        lefty_button_container.visibility = View.GONE

        righty_button_container.visibility = View.GONE

        empty_button_2_container.visibility = View.GONE

        single_background_options.visibility = View.GONE
        back_button.visibility = View.GONE
    }

    private fun resetToPlayMode() {
        single_button_container.visibility = View.VISIBLE

        world_button_container.visibility = View.VISIBLE

        dev_button_container.visibility = View.VISIBLE

        empty_button_2_container.visibility = View.VISIBLE

        lefty_button_container.visibility = View.GONE

        righty_button_container.visibility = View.GONE

        single_background_options.visibility = View.GONE

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
    }
}