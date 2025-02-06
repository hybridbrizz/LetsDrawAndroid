package com.ericversteeg.liquidocean.fragment

import android.graphics.Color
import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.fragment.app.Fragment
import com.ericversteeg.liquidocean.databinding.FragmentMenuBinding
import com.ericversteeg.liquidocean.helper.Animator
import com.ericversteeg.liquidocean.helper.Utils
import com.ericversteeg.liquidocean.listener.MenuButtonListener
import com.ericversteeg.liquidocean.listener.MenuCardListener
import com.ericversteeg.liquidocean.model.InteractiveCanvas
import com.ericversteeg.liquidocean.model.SessionSettings
import com.ericversteeg.liquidocean.view.ActionButtonView
import java.util.Timer
import java.util.TimerTask

class MenuFragment: Fragment() {

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
    
    private var _binding: FragmentMenuBinding? = null
    val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        SessionSettings.instance.canvasOpen = false

        view.setBackgroundColor(Color.BLACK)

        val allViews = listOf<View>(binding.backButton, binding.backAction,
            binding.optionsMenuText, binding.howToMenuText, binding.menuButtonContainer)

        //fadeInAllView(allViews)

        binding.backButton.actionBtnView = binding.backAction
        binding.backAction.type = ActionButtonView.Type.BACK_SOLID

        view.setBackgroundResource(SessionSettings.instance.menuBackgroundResId)

        binding.backButton.setOnClickListener {
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

        binding.backButton.visibility = View.GONE

        binding.statsButton.type = ActionButtonView.Type.STATS
        binding.statsButton.topLayer = true
        binding.statsButtonBottomLayer.type = ActionButtonView.Type.STATS

        binding.singleButton.type = ActionButtonView.Type.SINGLE
        binding.singleButton.topLayer = true
        binding.singleButtonBottomLayer.type = ActionButtonView.Type.SINGLE

        binding.worldButton.type = ActionButtonView.Type.WORLD
        binding.worldButton.topLayer = true
        binding.worldButtonBottomLayer.type = ActionButtonView.Type.WORLD

        //binding.devButton.type = ActionButtonView.Type.DEV
        //binding.devButton.topLayer = true
        //binding.devButton_bottom_layer.type = ActionButtonView.Type.DEV

        /*val artShowcase = SessionSettings.instance.artShowcase
        if (artShowcase != null && artShowcase.size > 0) {
            binding.artShowcase.showBackground = false
            binding.artShowcase.art = artShowcase[0]
        }*/

        val menuTextViews = listOf(binding.optionsMenuText, binding.howToMenuText, binding.leftyMenuText, binding.rightyMenuText)
        for (textView in menuTextViews) {
            textView.setOnTouchListener(object: View.OnTouchListener {
                override fun onTouch(view: View?, ev: MotionEvent): Boolean {
                    if (ev.action == MotionEvent.ACTION_DOWN) {
                        Utils.colorizeTextView(textView, ActionButtonView.yellowPaint.color, ActionButtonView.lightYellowPaint.color)
                        textView.invalidate()
                    }
                    else if (ev.action == MotionEvent.ACTION_CANCEL) {
                        Utils.colorizeTextView(textView, "#CCCCCC", "#DDDDDD")
                        textView.invalidate()
                    }

                    return false
                }
            })
        }

        /*draw_menu_text.setOnClickListener {
            /*// menuButtonListener?.onMenuButtonSelected(playMenuIndex)
            play_button_container.visibility = View.GONE

            binding.optionsButtonContainer.visibility = View.GONE

            binding.statsButton_container.visibility = View.GONE

            binding.howtoButtonContainer.visibility = View.GONE

            binding.singleButtonContainer.visibility = View.VISIBLE

            binding.worldButtonContainer.visibility = View.INVISIBLE

            binding.devButtonContainer.visibility = View.VISIBLE

            binding.emptyButton2Container.visibility = View.VISIBLE

            binding.backButton.visibility = View.VISIBLE
            backCount++

            animateMenuButtons(1)*/

            if (!SessionSettings.instance.selectedHand) {
                draw_button_container.visibility = View.GONE
                binding.optionsButtonContainer.visibility = View.GONE
                binding.statsButton_container.visibility = View.GONE
                binding.howtoButtonContainer.visibility = View.GONE

                binding.leftyButtonContainer.visibility = View.VISIBLE
                binding.rightyButtonContainer.visibility = View.VISIBLE

                //binding.emptyButton1Container.visibility = View.VISIBLE
                //binding.emptyButton2Container.visibility = View.VISIBLE

                route = singleMenuIndex

                backCount++
                animateMenuButtons(2)
            }
            else {
                menuButtonListener?.onMenuButtonSelected(singleMenuIndex)
                menuCardListener?.closeMenu()
            }
        }*/

        binding.optionsMenuText.setOnClickListener {
            menuButtonListener?.onMenuButtonSelected(optionsMenuIndex)
            menuCardListener?.closeMenu()
        }

        binding.statsButton.setOnClickListener {
            menuButtonListener?.onMenuButtonSelected(statsMenuIndex)
        }

        binding.howToMenuText.setOnClickListener {
            menuButtonListener?.onMenuButtonSelected(howtoMenuIndex)
            menuCardListener?.closeMenu()
        }

        binding.singleButton.setOnClickListener {

        }

        binding.worldButton.setOnClickListener {
            if (!SessionSettings.instance.selectedHand) {
                binding.singleButtonContainer.visibility = View.GONE
                binding.worldButtonContainer.visibility = View.GONE
                binding.devButtonContainer.visibility = View.GONE

                binding.leftyButtonContainer.visibility = View.VISIBLE
                binding.rightyButtonContainer.visibility = View.VISIBLE

                binding.emptyButton1Container.visibility = View.VISIBLE

                route = worldMenuIndex

                backCount++
                animateMenuButtons(2)
            }
            else {
                menuButtonListener?.onMenuButtonSelected(worldMenuIndex)
            }
        }

        binding.devButton.setOnClickListener {
            if (!SessionSettings.instance.selectedHand) {
                binding.singleButtonContainer.visibility = View.GONE
                binding.worldButtonContainer.visibility = View.GONE
                binding.devButtonContainer.visibility = View.GONE

                binding.leftyButtonContainer.visibility = View.VISIBLE
                binding.rightyButtonContainer.visibility = View.VISIBLE

                binding.emptyButton1Container.visibility = View.VISIBLE

                route = devMenuIndex

                backCount++
                animateMenuButtons(2)
            }
            else {
                menuButtonListener?.onMenuButtonSelected(devMenuIndex)
            }
        }

        binding.leftyMenuText.setOnClickListener {
            menuButtonListener?.onMenuButtonSelected(leftyMenuIndex, singleMenuIndex)
        }

        binding.rightyMenuText.setOnClickListener {
            menuButtonListener?.onMenuButtonSelected(rightyMenuIndex, singleMenuIndex)
        }

        binding.menuButtonContainer.setOnClickListener {

        }

        if (SessionSettings.instance.selectedHand) {
            binding.optionsButtonContainer.visibility = View.VISIBLE
            binding.howtoButtonContainer.visibility = View.VISIBLE

            animateMenuButtons(0)
        }

        view.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (view.height > Utils.dpToPx(context, 500)) {
                    SessionSettings.instance.tablet = true
                }

                val safeViews: MutableList<View> = ArrayList()

                if (binding.artShowcase != null) {
                    safeViews.add(binding.artShowcase)
                    safeViews.add(binding.menuButtonContainerHorizontalSpacer)

                    Animator.animatePixelColorEffect(binding.pixelView1, view, safeViews.toList())
                    Animator.animatePixelColorEffect(binding.pixelView2, view, safeViews.toList())
                    Animator.animatePixelColorEffect(binding.pixelView3, view, safeViews.toList())
                    Animator.animatePixelColorEffect(binding.pixelView4, view, safeViews.toList())
                    Animator.animatePixelColorEffect(binding.pixelView5, view, safeViews.toList())
                    Animator.animatePixelColorEffect(binding.pixelView6, view, safeViews.toList())
                    Animator.animatePixelColorEffect(binding.pixelView7, view, safeViews.toList())
                }
                view.viewTreeObserver.removeOnGlobalLayoutListener(this)

                menuButtonContainerWidth = binding.menuButtonContainer.width
            }
        })

        view.setOnTouchListener(object: View.OnTouchListener {
            override fun onTouch(vw: View?, ev: MotionEvent?): Boolean {
                mPanDetector.onTouchEvent(ev)

                return true
            }
        })

        Utils.colorizeTextView(binding.optionsMenuText, "#CCCCCC", "#DDDDDD")
        Utils.colorizeTextView(binding.howToMenuText, "#CCCCCC", "#DDDDDD")
        Utils.colorizeTextView(binding.leftyMenuText, "#CCCCCC", "#DDDDDD")
        Utils.colorizeTextView(binding.rightyMenuText, "#CCCCCC", "#DDDDDD")

        if (!SessionSettings.instance.selectedHand) {
            selectHand()
        }
    }

    fun clearMenuTextHighlights() {
        Utils.colorizeTextView(binding.optionsMenuText, "#CCCCCC", "#DDDDDD")
        Utils.colorizeTextView(binding.howToMenuText, "#CCCCCC", "#DDDDDD")
    }

    private fun selectHand() {
        if (!SessionSettings.instance.selectedHand) {
            binding.leftyButtonContainer.visibility = View.VISIBLE
            binding.rightyButtonContainer.visibility = View.VISIBLE

            animateMenuButtons(2)
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
                Animator.animateMenuItems(listOf(listOf(binding.optionsMenuText),
                    listOf(binding.statsButtonBottomLayer, binding.statsButton), listOf(binding.howToMenuText)), cascade = true, out = false, inverse = false,
                    completion = object: Animator.CompletionHandler {
                        override fun onCompletion() {
                            animatingMenu = false
                        }
                    }
                )
            }
            else if (layer == 1) {
                Animator.animateMenuItems(listOf(listOf(binding.singleButtonBottomLayer, binding.singleButton), listOf(binding.worldButtonBottomLayer, binding.worldButton),
                    listOf(binding.devButtonBottomLayer, binding.devButton)), cascade = true, out = false, inverse = false,
                    completion = object: Animator.CompletionHandler {
                        override fun onCompletion() {
                            animatingMenu = false
                        }
                    }
                )
            }
            else if (layer == 2) {
                Animator.animateMenuItems(listOf(listOf(binding.leftyMenuText), listOf(binding.rightyMenuText)),
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
                        if (binding.artShowcase != null) {
                            binding.artShowcase.alpha = 0F

                            binding.artShowcase.showBackground = false
                            binding.artShowcase.art = getNextArtShowcase()

                            binding.artShowcase.animate().alpha(1F).setDuration(2500).withEndAction {
                                Timer().schedule(object: TimerTask() {
                                    override fun run() {
                                        activity?.runOnUiThread {
                                            binding.artShowcase.animate().alpha(0F).setDuration(1500).start()
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

        binding.optionsButtonContainer.visibility = View.VISIBLE

        binding.statsButtonContainer.visibility = View.GONE

        binding.howtoButtonContainer.visibility = View.VISIBLE

        binding.singleButtonContainer.visibility = View.GONE

        binding.worldButtonContainer.visibility = View.GONE

        binding.devButtonContainer.visibility = View.GONE

        binding.leftyButtonContainer.visibility = View.GONE

        binding.rightyButtonContainer.visibility = View.GONE

        binding.emptyButton2Container.visibility = View.GONE

        binding.backButton.visibility = View.GONE
    }

    private fun resetToPlayMode() {
        binding.singleButtonContainer.visibility = View.VISIBLE

        binding.worldButtonContainer.visibility = View.VISIBLE

        binding.devButtonContainer.visibility = View.VISIBLE

        binding.emptyButton2Container.visibility = View.VISIBLE

        binding.leftyButtonContainer.visibility = View.GONE

        binding.rightyButtonContainer.visibility = View.GONE

        binding.emptyButton1Container.visibility = View.GONE

        binding.backButton.visibility = View.VISIBLE
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