package com.ericversteeg.liquidocean.fragment

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.drm.DrmStore
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.ericversteeg.liquidocean.R
import com.ericversteeg.liquidocean.SignInActivity
import com.ericversteeg.liquidocean.adapter.PanelRecyclerViewAdapter
import com.ericversteeg.liquidocean.helper.Animator
import com.ericversteeg.liquidocean.helper.Utils
import com.ericversteeg.liquidocean.listener.OptionsListener
import com.ericversteeg.liquidocean.model.SessionSettings
import com.ericversteeg.liquidocean.model.StatTracker
import com.ericversteeg.liquidocean.view.ActionButtonView
import kotlinx.android.synthetic.main.fragment_options.*
import org.json.JSONObject
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

        input_name.setText(SessionSettings.instance.displayName)

        input_name.setOnEditorActionListener(object : TextView.OnEditorActionListener {
            override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    sendNameCheck(input_name.text.toString().trim())

                    val inputMethodManager = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)

                    return true
                }
                return false
            }
        })

        change_name_button.setOnClickListener {
            updateDisplayName(input_name.text.toString())
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
                    R.drawable.space_texture,
                    R.drawable.metal_floor_1,
                    R.drawable.metal_floor_2,  // TODO: hard to see paint event countdown
                    R.drawable.foil,
                    R.drawable.rainbow_foil,   // TODO: hard to see paint event countdown
                    R.drawable.crystal_1,
                    R.drawable.crystal_2,      // TODO: hard to see paint event countdown
                    R.drawable.crystal_3,
                    R.drawable.crystal_4,
                    R.drawable.crystal_5,      // TODO: hard to see paint event countdown
                    R.drawable.crystal_6,
                    R.drawable.crystal_7,
                    R.drawable.crystal_8,      // TODO: hard to see paint event countdown
                    R.drawable.crystal_9,      // TODO: hard to see paint event countdown
                    R.drawable.crystal_10      // TODO: hard to see paint event countdown + dark icons

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

        // option paint indicator width
        option_paint_indicator_width_value.text = SessionSettings.instance.colorIndicatorWidth.toInt().toString()

        option_paint_indicator_width_action_minus.type = ActionButtonView.Type.DOT
        option_paint_indicator_width_action_plus.type = ActionButtonView.Type.DOT

        option_paint_indicator_width_button_minus.actionBtnView = option_paint_indicator_width_action_minus
        option_paint_indicator_width_button_plus.actionBtnView = option_paint_indicator_width_action_plus

        option_paint_indicator_width_button_minus.setOnClickListener {
            val value = option_paint_indicator_width_value.text.toString().toInt() - 1
            option_paint_indicator_width_value.text = value.toString()
            SessionSettings.instance.colorIndicatorWidth = value.toFloat()
        }

        option_paint_indicator_width_button_plus.setOnClickListener {
            val value = option_paint_indicator_width_value.text.toString().toInt() + 1
            option_paint_indicator_width_value.text = value.toString()
            SessionSettings.instance.colorIndicatorWidth = value.toFloat()
        }

        // option paint indicator fill circle
        option_paint_indicator_fill_circle_switch.isChecked = SessionSettings.instance.colorIndicatorFill

        option_paint_indicator_fill_circle_switch.setOnCheckedChangeListener { button, _ ->
            SessionSettings.instance.colorIndicatorFill = button.isChecked
        }

        // option paint indicator outline
        option_paint_indicator_outline_switch.isChecked = SessionSettings.instance.colorIndicatorOutline

        option_paint_indicator_outline_switch.setOnCheckedChangeListener { button, _ ->
            SessionSettings.instance.colorIndicatorOutline = button.isChecked
        }

        setupNumRecentColorsChoices()

        Animator.animateTitleFromTop(title_image)

        Animator.animateHorizontalViewEnter(option_paint_panel_texture_title, false)
        Animator.animateHorizontalViewEnter(panel_recycler_view, true)
        Animator.animateHorizontalViewEnter(option_canvas_lock_container, false)
        Animator.animateHorizontalViewEnter(option_canvas_lock_color_container, true)
    }


    private fun setupNumRecentColorsChoices() {
        // option num recent colors
        for (v in option_num_recent_colors_choice_layout.children) {
            val textView = v as TextView

            textView.setOnClickListener {
                SessionSettings.instance.numRecentColors = textView.text.toString().toInt()
                setupNumRecentColorsChoices()
            }

            if (SessionSettings.instance.numRecentColors.toString() == textView.text) {
                textView.setTextColor(ActionButtonView.altGreenPaint.color)
            }
            else {
                textView.setTextColor(ActionButtonView.whitePaint.color)
            }
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

    fun sendNameCheck(name: String) {
        val requestQueue = Volley.newRequestQueue(context)
        context?.apply {
            val uniqueId = SessionSettings.instance.uniqueId

            uniqueId?.apply {
                val request = JsonObjectRequest(
                    Request.Method.GET,
                    Utils.baseUrlApi + "/api/v1/devices/checkname/" + name,
                    null,
                    { response ->
                        activity?.runOnUiThread {
                            val taken = !response.getBoolean("a")
                            if (taken) {
                                input_name.setBackgroundDrawable(
                                    ResourcesCompat.getDrawable(
                                        resources,
                                        R.drawable.input_display_name_red,
                                        null
                                    )
                                )
                                change_name_button.isEnabled = false
                            } else {
                                input_name.setBackgroundDrawable(
                                    ResourcesCompat.getDrawable(
                                        resources,
                                        R.drawable.input_display_name_green,
                                        null
                                    )
                                )
                                change_name_button.isEnabled = true
                            }
                        }

                    },
                    { error ->
                        change_name_button.text = "Error"
                        change_name_button.isEnabled = false
                        input_name.isEnabled = false
                    })

                request.tag = "download"
                requestQueue.add(request)
            }
        }
    }

    private fun updateDisplayName(name: String) {
        val requestQueue = Volley.newRequestQueue(context)

        val requestParams = HashMap<String, String>()

        requestParams["name"] = name

        val paramsJson = JSONObject(requestParams as Map<String, String>)

        val request = JsonObjectRequest(
            Request.Method.POST,
            Utils.baseUrlApi + "/api/v1/devices/${SessionSettings.instance.uniqueId}",
            paramsJson,
            { response ->
                SessionSettings.instance.displayName = response.getString("name")
                change_name_button.text = "Updated"
                change_name_button.isEnabled = false
                input_name.isEnabled = false
            },
            { error ->
                change_name_button.text = "Error"
                change_name_button.isEnabled = false
                input_name.isEnabled = false
            })

        requestQueue.add(request)
    }
}