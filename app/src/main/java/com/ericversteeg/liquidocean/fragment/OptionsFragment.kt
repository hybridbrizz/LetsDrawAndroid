package com.ericversteeg.liquidocean.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.ericversteeg.liquidocean.R
import com.ericversteeg.liquidocean.SignInActivity
import com.ericversteeg.liquidocean.model.SessionSettings
import com.ericversteeg.liquidocean.listener.OptionsListener
import com.ericversteeg.liquidocean.view.ActionButtonView
import kotlinx.android.synthetic.main.fragment_interactive_canvas.*
import kotlinx.android.synthetic.main.fragment_options.*
import kotlinx.android.synthetic.main.fragment_options.back_action
import kotlinx.android.synthetic.main.fragment_options.back_button

class OptionsFragment: Fragment() {

    var optionsListener: OptionsListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_options, container, false)

        // setup views here

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        back_button.actionBtnView = back_action
        back_action.type = ActionButtonView.Type.BACK

        back_button.setOnClickListener {
            optionsListener?.onOptionsBack()
        }

        sign_in_button.setOnClickListener {
            val intent = Intent(context, SignInActivity::class.java)
            startActivity(intent)
        }

        context?.apply {
            if (!SessionSettings.instance.getSharedPrefs(this).contains("arr_single")) {
                reset_single_play.isEnabled = false
            }

            reset_single_play.setOnClickListener {
                resetSinglePlay()
            }
        }
    }

    private fun resetSinglePlay() {
        context?.apply {
            val ed = SessionSettings.instance.getSharedPrefs(this).edit()
            ed.remove("arr_single")
            ed.remove("grid_line_color")
            ed.apply()

            optionsListener?.onResetSinglePlay()
        }
    }
}