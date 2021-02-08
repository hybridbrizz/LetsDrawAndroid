package com.ericversteeg.liquidocean.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.fragment.app.Fragment
import com.ericversteeg.liquidocean.R
import com.ericversteeg.liquidocean.helper.SessionSettings
import com.ericversteeg.liquidocean.listener.MenuButtonListener
import com.ericversteeg.liquidocean.view.ActionButtonView
import kotlinx.android.synthetic.main.fragment_menu.*

class MenuFragment: Fragment() {

    var menuButtonListener: MenuButtonListener? = null

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

        view.setOnClickListener {
            resetMenu()
        }

        play_button.type = ActionButtonView.Type.PLAY
        options_button.type = ActionButtonView.Type.OPTIONS
        stats_button.type = ActionButtonView.Type.STATS
        exit_button.type = ActionButtonView.Type.EXIT
        single_button.type = ActionButtonView.Type.SINGLE
        world_button.type = ActionButtonView.Type.WORLD

        // backgrounds
        background_option_black.type = ActionButtonView.Type.BACKGROUND_BLACK
        background_option_white.type = ActionButtonView.Type.BACKGROUND_WHITE
        background_option_gray_thirds.type = ActionButtonView.Type.BACKGROUND_GRAY_THIRDS
        background_option_photoshop.type = ActionButtonView.Type.BACKGROUND_PHOTOSHOP
        background_option_classic.type = ActionButtonView.Type.BACKGROUND_CLASSIC
        background_option_chess.type = ActionButtonView.Type.BACKGROUND_CHESS

        play_button.setOnClickListener {
            // menuButtonListener?.onMenuButtonSelected(playMenuIndex)

            play_button.visibility = View.GONE
            options_button.visibility = View.GONE
            stats_button.visibility = View.GONE
            exit_button.visibility = View.GONE

            single_button.visibility = View.VISIBLE
            world_button.visibility = View.VISIBLE
            empty_button_1.visibility = View.VISIBLE
            empty_button_2.visibility = View.VISIBLE
        }

        options_button.setOnClickListener {
            menuButtonListener?.onMenuButtonSelected(optionsMenuIndex)
        }

        stats_button.setOnClickListener {
            menuButtonListener?.onMenuButtonSelected(statsMenuIndex)
        }

        exit_button.setOnClickListener {
            menuButtonListener?.onMenuButtonSelected(exitMenuIndex)
        }

        single_button.setOnClickListener {
            context?.apply {
                if (SessionSettings.instance.getSharedPrefs(this).contains("arr_single")) {
                    menuButtonListener?.onMenuButtonSelected(singleMenuIndex)
                }
                else {
                    showSingleBackgroundOptions()
                }
            }
        }

        world_button.setOnClickListener {
            menuButtonListener?.onMenuButtonSelected(worldMenuIndex)
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
    }

    private fun showSingleBackgroundOptions() {
        single_button.visibility = View.GONE
        world_button.visibility = View.GONE
        empty_button_1.visibility = View.GONE
        empty_button_2.visibility = View.GONE

        single_background_options.visibility = View.VISIBLE
    }

    private fun resetMenu() {
        play_button.visibility = View.VISIBLE
        options_button.visibility = View.VISIBLE
        stats_button.visibility = View.VISIBLE
        exit_button.visibility = View.VISIBLE

        single_button.visibility = View.GONE
        world_button.visibility = View.GONE
        empty_button_1.visibility = View.GONE
        empty_button_2.visibility = View.GONE

        single_background_options.visibility = View.GONE
    }

    companion object {
        val playMenuIndex = 0
        val optionsMenuIndex = 1
        val statsMenuIndex = 2
        val exitMenuIndex = 3
        val singleMenuIndex = 4
        val worldMenuIndex = 5
    }
}