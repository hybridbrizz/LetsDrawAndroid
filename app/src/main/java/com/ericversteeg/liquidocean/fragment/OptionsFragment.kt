package com.ericversteeg.liquidocean.fragment

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.ericversteeg.liquidocean.R
import com.ericversteeg.liquidocean.SignInActivity
import com.ericversteeg.liquidocean.adapter.PanelRecyclerViewAdapter
import com.ericversteeg.liquidocean.listener.OptionsListener
import com.ericversteeg.liquidocean.model.SessionSettings
import com.ericversteeg.liquidocean.view.ActionButtonView
import kotlinx.android.synthetic.main.fragment_options.*
import top.defaults.colorpicker.ColorPickerPopup
import top.defaults.colorpicker.ColorPickerPopup.ColorPickerObserver


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

        title_image.type = ActionButtonView.Type.OPTIONS

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
                showSinglePlayRestWarning()
            }

            // option paint panel background
            panel_recycler_view.layoutManager = LinearLayoutManager(
                this,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            panel_recycler_view.adapter = PanelRecyclerViewAdapter(
                this, intArrayOf(
                    R.drawable.wood_texture_light,
                    R.drawable.wood_texture,
                    R.drawable.marble_2,
                    R.drawable.fall_leaves,
                    R.drawable.water_texture,
                    R.drawable.space_texture
                ).toMutableList()
            )

            (panel_recycler_view.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        }

        // option canvas lock
        option_canvas_lock_switch.isChecked = SessionSettings.instance.canvasLockBorder
        option_canvas_lock_switch.setOnCheckedChangeListener { _, value ->
            SessionSettings.instance.canvasLockBorder = value
        }

        // option canvas lock color
        option_canvas_lock_color_button.setBackgroundColor(SessionSettings.instance.canvasLockBorderColor)
        option_canvas_lock_color_reset.setOnClickListener {
            SessionSettings.instance.resetCanvasLockBorderColor()
            option_canvas_lock_color_button.setBackgroundColor(SessionSettings.instance.canvasLockBorderColor)
        }

        // option canvas lock color
        option_canvas_lock_color_button.setOnClickListener {
            ColorPickerPopup.Builder(activity)
                .initialColor(SessionSettings.instance.canvasLockBorderColor) // Set initial color
                .enableBrightness(true) // Enable brightness slider or not
                .enableAlpha(true) // Enable alpha slider or not
                .okTitle("Choose")
                .cancelTitle("Cancel")
                .showIndicator(true)
                .showValue(true)
                .build()
                .show(it, object : ColorPickerObserver() {
                    override fun onColorPicked(color: Int) {
                        it.setBackgroundColor(color)
                        SessionSettings.instance.canvasLockBorderColor = color
                    }

                    fun onColor(color: Int, fromUser: Boolean) {}
                })
        }

        // option emitters
        option_emitters_switch.isChecked = SessionSettings.instance.emittersEnabled
        option_emitters_switch.setOnCheckedChangeListener { _, value ->
            SessionSettings.instance.emittersEnabled = value
        }

        // option prompt to exit
        option_prompt_to_exit_switch.isChecked = SessionSettings.instance.promptToExit
        option_prompt_to_exit_switch.setOnCheckedChangeListener { _, value ->
            SessionSettings.instance.promptToExit = value
        }
    }

    private fun showSinglePlayRestWarning() {
        val alert = AlertDialog.Builder(context)

        val editText = EditText(activity)
        alert.setMessage("Please understand that resetting your single play will permanently erase your current single play canvas, to proceed please type PROCEED in all caps.")

        alert.setView(editText)

        alert.setPositiveButton(
            "Proceed"
        ) { dialog, _ ->
            if (editText.text.toString() == "PROCEED") {
                resetSinglePlay()
                dialog?.dismiss()
            }
        }

        alert.setNegativeButton("Cancel") { dialog, _ ->
            dialog?.dismiss()
        }

        alert.show()
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