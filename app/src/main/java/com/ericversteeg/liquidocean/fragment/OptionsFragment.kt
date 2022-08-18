package com.ericversteeg.liquidocean.fragment

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.ericversteeg.liquidocean.R
import com.ericversteeg.liquidocean.activity.SignInActivity
import com.ericversteeg.liquidocean.adapter.PanelRecyclerViewAdapter
import com.ericversteeg.liquidocean.colorpicker.HSBColorPicker
import com.ericversteeg.liquidocean.helper.Animator
import com.ericversteeg.liquidocean.helper.Utils
import com.ericversteeg.liquidocean.listener.FragmentListener
import com.ericversteeg.liquidocean.listener.OptionsListener
import com.ericversteeg.liquidocean.model.SessionSettings
import com.ericversteeg.liquidocean.view.ActionButtonView
import kotlinx.android.synthetic.main.fragment_options.*
import kotlinx.android.synthetic.main.fragment_options.back_button
import kotlinx.android.synthetic.main.fragment_signin.*
import org.json.JSONObject
import top.defaults.colorpicker.ColorPickerPopup
import top.defaults.colorpicker.ColorPickerPopup.ColorPickerObserver


class OptionsFragment: Fragment(), FragmentListener {

    var optionsListener: OptionsListener? = null

    var selectingCanvasLockColor = false
    var selectingGridLineColor = false
    var selectingPaintMeterColor = false

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

        if (!Utils.isTablet(requireContext())) {
            option_color_palette_size.visibility = View.GONE
        }

        if (isFromInteractiveCanvas()) {
            option_right_handed.visibility = View.GONE
            option_small_action_buttons_container.visibility = View.GONE
            option_bold_action_buttons.visibility = View.GONE
            option_num_recent_colors.visibility = View.GONE
            option_show_paint_bar_container.visibility = View.GONE
            option_show_paint_circle_container.visibility = View.GONE
        }
        else {
            change_name_container.visibility = View.GONE
        }

        back_button.setOnClickListener {
            if (credits_container.visibility == View.VISIBLE) {
                credits_container.visibility = View.GONE
            }
            else {
                context?.apply {
                    SessionSettings.instance.save(this)
                }

                if (isFromInteractiveCanvas()) {
                    (parentFragment as InteractiveCanvasFragment).closeOptions(this@OptionsFragment)
                }
                else {
                    optionsListener?.onOptionsBack()
                }
            }
        }

        input_name.setText(SessionSettings.instance.displayName)

        input_name.setOnEditorActionListener(object : TextView.OnEditorActionListener {
            override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    val input = input_name.text.toString().trim()
                    if (input.length > 20) {
                        input_name.setBackgroundDrawable(
                            ResourcesCompat.getDrawable(
                                resources,
                                R.drawable.input_display_name_red,
                                null
                            )
                        )
                        change_name_button.isEnabled = false
                    }
                    else {
                        sendNameCheck(input_name.text.toString().trim())
                    }

                    val inputMethodManager =
                        context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
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
            intent.putExtra("mode", SignInFragment.modeSignIn)
            startActivity(intent)
        }

        recovery_pincode_button.setOnClickListener {
            val intent = Intent(context, SignInActivity::class.java)
            if (SessionSettings.instance.pincodeSet) {
                intent.putExtra("mode", SignInFragment.modeChangePincode)
            }
            else {
                intent.putExtra("mode", SignInFragment.modeSetPincode)
            }

            startActivity(intent)
        }

        context?.apply {
            if (!SessionSettings.instance.getSharedPrefs(this).contains("arr_canvas")) {
                reset_single_play.isEnabled = false
            }

            reset_single_play.setOnClickListener {
                showSinglePlayRestWarning()
            }

            import_single_play.setOnClickListener {
                showCanvasImportFragment()
            }

            export_single_play.setOnClickListener {
                showCanvasExportFragment()
            }

            // option paint panel background
            panel_recycler_view.layoutManager = LinearLayoutManager(
                this,
                LinearLayoutManager.HORIZONTAL,
                false
            )

            view.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    } else {
                        view.viewTreeObserver.removeGlobalOnLayoutListener(this)
                    }

                    (panel_recycler_view.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                        SessionSettings.instance.panelBackgroundResIndex, (view.width * 0.15).toInt()
                    )
                }
            })

            panel_recycler_view.adapter = PanelRecyclerViewAdapter(
                this, SessionSettings.instance.panelResIds.toMutableList()
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
                .showIndicator(false)
                .showValue(false)
                .build()
                .show(it, object : ColorPickerObserver() {
                    override fun onColorPicked(color: Int) {
                        it.setBackgroundColor(color)
                        SessionSettings.instance.canvasLockBorderColor = color
                    }

                    override fun onColor(color: Int, fromUser: Boolean, shouldPropagate: Boolean) {
                        it.setBackgroundColor(color)
                    }

                    override fun onColorCancel() {
                        it.setBackgroundColor(SessionSettings.instance.canvasLockBorderColor)
                    }
                })
        }

        // option grid line color
        option_grid_line_color_button.setBackgroundColor(SessionSettings.instance.canvasGridLineColor)
        option_grid_line_color_reset_button.setOnClickListener {
            SessionSettings.instance.canvasGridLineColor = -1
            option_grid_line_color_button.setBackgroundColor(SessionSettings.instance.canvasGridLineColor)
        }

        option_grid_line_color_button.setOnClickListener {
            showColorPicker(SessionSettings.instance.canvasGridLineColor) { color ->
                option_grid_line_color_button.setBackgroundColor(color)
                SessionSettings.instance.canvasGridLineColor = color
            }
        }

        // option canvas background primary color
        if (SessionSettings.instance.canvasBackgroundPrimaryColor == 0) {
            option_canvas_background_primary_color_button.setBackgroundColor(Color.WHITE)
        }
        else {
            option_canvas_background_primary_color_button.setBackgroundColor(SessionSettings.instance.canvasBackgroundPrimaryColor)
        }

        option_canvas_background_primary_color_reset_button.setOnClickListener {
            SessionSettings.instance.canvasBackgroundSecondaryColor = 0
            option_canvas_background_primary_color_button.setBackgroundColor(Color.WHITE)
        }

        option_canvas_background_primary_color_button.setOnClickListener {
            showColorPicker(SessionSettings.instance.canvasBackgroundPrimaryColor) { color ->
                option_canvas_background_primary_color_button.setBackgroundColor(color)
                SessionSettings.instance.canvasBackgroundPrimaryColor = color
            }
        }

        // option canvas background secondary color
        if (SessionSettings.instance.canvasBackgroundSecondaryColor == 0) {
            option_canvas_background_secondary_color_button.setBackgroundColor(Color.WHITE)
        }
        else {
            option_canvas_background_secondary_color_button.setBackgroundColor(SessionSettings.instance.canvasBackgroundSecondaryColor)
        }

        option_canvas_background_secondary_color_reset_button.setOnClickListener {
            SessionSettings.instance.canvasBackgroundSecondaryColor = 0
            option_canvas_background_secondary_color_button.setBackgroundColor(Color.WHITE)
        }

        option_canvas_background_secondary_color_button.setOnClickListener {
            showColorPicker(SessionSettings.instance.canvasBackgroundSecondaryColor) { color ->
                option_canvas_background_secondary_color_button.setBackgroundColor(color)
                SessionSettings.instance.canvasBackgroundSecondaryColor = color
            }
        }

        // option frame color
        option_frame_color_button.setBackgroundColor(SessionSettings.instance.frameColor)
        option_frame_color_reset_button.setOnClickListener {
            SessionSettings.instance.frameColor = Color.GRAY
            option_frame_color_button.setBackgroundColor(SessionSettings.instance.frameColor)
        }

        // option frame color
        option_frame_color_button.setOnClickListener {
            ColorPickerPopup.Builder(activity)
                .initialColor(SessionSettings.instance.frameColor) // Set initial color
                .enableBrightness(true) // Enable brightness slider or not
                .enableAlpha(true) // Enable alpha slider or not
                .okTitle("Choose")
                .cancelTitle("Cancel")
                .showIndicator(false)
                .showValue(false)
                .build()
                .show(it, object : ColorPickerObserver() {
                    override fun onColorPicked(color: Int) {
                        it.setBackgroundColor(color)
                        SessionSettings.instance.frameColor = color
                    }

                    override fun onColor(color: Int, fromUser: Boolean, shouldPropagate: Boolean) {
                        it.setBackgroundColor(color)
                    }

                    override fun onColorCancel() {
                        it.setBackgroundColor(SessionSettings.instance.frameColor)
                    }
                })
        }

        // option emitters
        option_emitters_container.visibility = View.GONE

        option_emitters_switch.isChecked = SessionSettings.instance.emittersEnabled
        option_emitters_switch.setOnCheckedChangeListener { _, value ->
            SessionSettings.instance.emittersEnabled = value
        }

        // option bold action buttons
        option_bold_action_buttons_switch.isChecked = SessionSettings.instance.boldActionButtons
        option_bold_action_buttons_switch.setOnCheckedChangeListener { _, value ->
            SessionSettings.instance.boldActionButtons = value
        }

        // option paint indicator width
        option_paint_indicator_width_value.text = SessionSettings.instance.colorIndicatorWidth.toString()

        option_paint_indicator_width_action_minus.type = ActionButtonView.Type.DOT
        option_paint_indicator_width_action_plus.type = ActionButtonView.Type.DOT

        option_paint_indicator_width_button_minus.actionBtnView = option_paint_indicator_width_action_minus
        option_paint_indicator_width_button_plus.actionBtnView = option_paint_indicator_width_action_plus

        option_paint_indicator_width_button_minus.setOnClickListener {
            var value = option_paint_indicator_width_value.text.toString().toInt() - 1
            if (value == 0) value = 1

            option_paint_indicator_width_value.text = value.toString()
            SessionSettings.instance.colorIndicatorWidth = value
        }

        option_paint_indicator_width_button_plus.setOnClickListener {
            var value = option_paint_indicator_width_value.text.toString().toInt() + 1
            if (value == 6) value = 5

            option_paint_indicator_width_value.text = value.toString()
            SessionSettings.instance.colorIndicatorWidth = value
        }

        // option color palette size
        option_color_palette_size_value.text = SessionSettings.instance.colorPaletteSize.toString()

        option_color_palette_size_action_minus.type = ActionButtonView.Type.DOT
        option_color_palette_size_action_plus.type = ActionButtonView.Type.DOT

        option_color_palette_size_button_minus.actionBtnView = option_color_palette_size_action_minus
        option_color_palette_size_button_plus.actionBtnView = option_color_palette_size_action_plus

        option_color_palette_size_button_minus.setOnClickListener {
            var value = option_color_palette_size_value.text.toString().toInt() - 1
            if (value <= 0) value = 1

            option_color_palette_size_value.text = value.toString()
            SessionSettings.instance.colorPaletteSize = value
        }

        option_color_palette_size_button_plus.setOnClickListener {
            var value = option_color_palette_size_value.text.toString().toInt() + 1
            if (value >= 15) value = 14

            option_color_palette_size_value.text = value.toString()
            SessionSettings.instance.colorPaletteSize = value
        }

        // option paint indicator fill circle
        option_paint_indicator_fill_circle_switch.isChecked = SessionSettings.instance.colorIndicatorFill

        option_paint_indicator_fill_circle_switch.setOnCheckedChangeListener { button, _ ->
            SessionSettings.instance.colorIndicatorFill = button.isChecked
            if (button.isChecked && SessionSettings.instance.colorIndicatorSquare) {
                option_paint_indicator_square_switch.isChecked = false
            }
        }

        // option paint indicator square
        option_paint_indicator_square_switch.isChecked = SessionSettings.instance.colorIndicatorSquare

        option_paint_indicator_square_switch.setOnCheckedChangeListener { button, _ ->
            SessionSettings.instance.colorIndicatorSquare = button.isChecked
            if (button.isChecked && SessionSettings.instance.colorIndicatorFill) {
                option_paint_indicator_fill_circle_switch.isChecked = false
            }
        }

        // option paint indicator outline
        option_paint_indicator_outline_switch.isChecked = SessionSettings.instance.colorIndicatorOutline

        option_paint_indicator_outline_switch.setOnCheckedChangeListener { button, _ ->
            SessionSettings.instance.colorIndicatorOutline = button.isChecked
        }

        // option close paint panel button color
        option_close_paint_panel_color_button.setBackgroundColor(SessionSettings.instance.closePaintBackButtonColor)
        option_close_paint_panel_color_reset_button.setOnClickListener {
            SessionSettings.instance.closePaintBackButtonColor = -1
            option_close_paint_panel_color_button.setBackgroundColor(SessionSettings.instance.closePaintBackButtonColor)
        }

        option_close_paint_panel_color_button.setOnClickListener {
            showColorPicker(SessionSettings.instance.closePaintBackButtonColor) { color ->
                option_close_paint_panel_color_button.setBackgroundColor(color)
                SessionSettings.instance.closePaintBackButtonColor = color
            }
        }

        // option show paint bar
        option_show_paint_bar_switch.isChecked = SessionSettings.instance.showPaintBar

        option_show_paint_bar_switch.setOnCheckedChangeListener { button, _ ->
            SessionSettings.instance.showPaintBar = button.isChecked
            if (button.isChecked && option_show_paint_circle_switch.isChecked) {
                option_show_paint_circle_switch.isChecked = false
                SessionSettings.instance.showPaintCircle = false
            }
            else if (!button.isChecked) {
                option_show_paint_circle_switch.isChecked = true
                SessionSettings.instance.showPaintBar = false
                SessionSettings.instance.showPaintCircle = true
            }
        }

        // option show paint circle
        option_show_paint_circle_switch.isChecked = SessionSettings.instance.showPaintCircle

        option_show_paint_circle_switch.setOnCheckedChangeListener { button, _ ->
            SessionSettings.instance.showPaintCircle = button.isChecked
            if (button.isChecked && option_show_paint_bar_switch.isChecked) {
                option_show_paint_bar_switch.isChecked = false
                SessionSettings.instance.showPaintBar = false
            }
            else if (!button.isChecked) {
                option_show_paint_bar_switch.isChecked = true
                SessionSettings.instance.showPaintCircle = false
                SessionSettings.instance.showPaintBar = true
            }
        }

        // option paint bar color
        option_paint_bar_color_button.setBackgroundColor(SessionSettings.instance.paintBarColor)
        option_paint_bar_color_reset_button.setOnClickListener {
            SessionSettings.instance.paintBarColor = ContextCompat.getColor(requireContext(), R.color.default_paint_qty_bar_color)
            option_paint_bar_color_button.setBackgroundColor(SessionSettings.instance.paintBarColor)
        }

        // option grid line color
        option_paint_bar_color_button.setOnClickListener {
            showColorPicker(SessionSettings.instance.paintBarColor) { color ->
                option_paint_bar_color_button.setBackgroundColor(color)
                SessionSettings.instance.paintBarColor = color
            }
        }

        // option right handed
        option_right_handed_switch.isChecked = SessionSettings.instance.rightHanded

        option_right_handed_switch.setOnCheckedChangeListener { button, _ ->
            SessionSettings.instance.rightHanded = button.isChecked
        }

        // option small action buttons
        option_small_action_buttons_switch.isChecked = SessionSettings.instance.smallActionButtons

        option_small_action_buttons_switch.setOnCheckedChangeListener { button, _ ->
            SessionSettings.instance.smallActionButtons = button.isChecked
        }

        option_paint_panel_texture_title.setOnClickListener {
            credits_container.visibility = View.VISIBLE
        }

        setupNumRecentColorsChoices()

        if (!SessionSettings.instance.tablet) {
            Animator.animateTitleFromTop(options_title_text)
            Animator.animateTitleFromTop(back_button)
            Animator.animateHorizontalViewEnter(option_paint_panel_texture_title, false)
            Animator.animateHorizontalViewEnter(panel_recycler_view, true)
            Animator.animateHorizontalViewEnter(option_grid_line_color_container, true)
            Animator.animateHorizontalViewEnter(option_paint_bar_color_container, true)
            Animator.animateHorizontalViewEnter(option_canvas_background_primary_color_container, true)
            Animator.animateHorizontalViewEnter(option_canvas_background_secondary_color_container, true)
        }
    }

    override fun onResume() {
        super.onResume()

        input_name.setText(SessionSettings.instance.displayName)

        if (SessionSettings.instance.pincodeSet) {
            sign_in_button.text = "Signed in"
            sign_in_button.isEnabled = false

            recovery_pincode_button.text = "Change access pincode"
        }

        fragment_container.visibility = View.GONE
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
        alert.setMessage(getString(R.string.reset_single_play_alert_message))

        alert.setView(editText)

        alert.setPositiveButton(
            "Erase"
        ) { dialog, _ ->
            if (editText.text.toString() == getString(R.string.reset_single_play_confirm_string)) {
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
            ed.remove("arr_canvas")
            ed.apply()

            SessionSettings.instance.restoreDeviceViewportLeft = 0F
            SessionSettings.instance.restoreDeviceViewportTop = 0F
            SessionSettings.instance.restoreDeviceViewportRight = 0F
            SessionSettings.instance.restoreDeviceViewportBottom = 0F

            SessionSettings.instance.restoreCanvasScaleFactor = 0F

            optionsListener?.onResetSinglePlay()
        }
    }

    private fun showCanvasImportFragment() {
        fragmentManager?.apply {
            val canvasImportFragment = CanvasImportFragment()
            canvasImportFragment.fragmentListener = this@OptionsFragment

            beginTransaction().replace(R.id.fragment_container, canvasImportFragment).commit()

            fragment_container.visibility = View.VISIBLE
        }
    }

    private fun showCanvasExportFragment() {
        fragmentManager?.apply {
            val canvasExportFragment = CanvasExportFragment()
            canvasExportFragment.fragmentListener = this@OptionsFragment

            beginTransaction().replace(R.id.fragment_container, canvasExportFragment).commit()

            fragment_container.visibility = View.VISIBLE
        }
    }

    fun sendNameCheck(name: String) {
        if (name.length > 20 || SessionSettings.instance.lastVisitedServer == null) {
            return
        }

        val requestQueue = Volley.newRequestQueue(context)

        val request = object: JsonObjectRequest(
            Request.Method.GET,
            SessionSettings.instance.lastVisitedServer!!.serviceBaseUrl() + "api/v1/devices/checkname/" + name,
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
            }) {

            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = "application/json; charset=utf-8"
                headers["key1"] = Utils.key1
                return headers
            }
        }

        request.tag = "download"
        requestQueue.add(request)
    }

    private fun updateDisplayName(name: String) {
        if (SessionSettings.instance.lastVisitedServer == null) return

        val requestQueue = Volley.newRequestQueue(context)

        val requestParams = HashMap<String, String>()

        requestParams["name"] = name

        val paramsJson = JSONObject(requestParams as Map<String, String>)

        val request = object: JsonObjectRequest(
            Method.POST,
            SessionSettings.instance.lastVisitedServer!!.serviceBaseUrl() + "api/v1/devices/${SessionSettings.instance.lastVisitedServer!!.uuid}",
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

    override fun onFragmentRemoved() {
        fragment_container.visibility = View.GONE
    }

    private fun isFromInteractiveCanvas(): Boolean {
        return parentFragment != null && parentFragment is InteractiveCanvasFragment
    }

    private fun showColorPicker(initialColor: Int, onColorPicked: (color: Int) -> Unit) {
        HSBColorPicker.showDialog(requireContext(), initialColor) { color ->
            onColorPicked.invoke(color)
        }
    }
}