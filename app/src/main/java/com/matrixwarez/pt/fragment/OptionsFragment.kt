package com.matrixwarez.pt.fragment

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.matrixwarez.pt.R
import com.matrixwarez.pt.helper.Utils
import com.matrixwarez.pt.listener.FragmentListener
import com.matrixwarez.pt.listener.OptionsListener
import com.matrixwarez.pt.model.SessionSettings
import kotlinx.android.synthetic.main.fragment_options.back_button
import kotlinx.android.synthetic.main.fragment_options.change_name_button
import kotlinx.android.synthetic.main.fragment_options.change_name_container
import kotlinx.android.synthetic.main.fragment_options.credits_container
import kotlinx.android.synthetic.main.fragment_options.fragment_container
import kotlinx.android.synthetic.main.fragment_options.input_name
import kotlinx.android.synthetic.main.fragment_options.option_canvas_background_primary_color_button
import kotlinx.android.synthetic.main.fragment_options.option_canvas_background_primary_color_reset_button
import kotlinx.android.synthetic.main.fragment_options.option_canvas_background_secondary_color_button
import kotlinx.android.synthetic.main.fragment_options.option_canvas_background_secondary_color_reset_button
import kotlinx.android.synthetic.main.fragment_options.option_grid_line_color_button
import kotlinx.android.synthetic.main.fragment_options.option_grid_line_color_reset_button
import org.json.JSONObject
import top.defaults.colorpicker.ColorPickerPopup
import top.defaults.colorpicker.ColorPickerPopup.ColorPickerObserver


class OptionsFragment: Fragment(), FragmentListener {

    var optionsListener: OptionsListener? = null

    private fun onBack() {
        if (credits_container.visibility == View.VISIBLE) {
            credits_container.visibility = View.GONE
        }
        else {
            context?.apply {
                SessionSettings.instance.save(this)
            }

            if (isFromInteractiveCanvas()) {
                (parentFragment as InteractiveCanvasFragment).closeOptions(this@OptionsFragment)
                (requireActivity() as? AppCompatActivity)?.supportActionBar?.show()
            }
            else {
                optionsListener?.onOptionsBack()
            }
        }
    }

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

        if (!isFromInteractiveCanvas()) {
            change_name_container.visibility = View.GONE
        }

        back_button.setOnClickListener {
            onBack()
        }

        requireActivity()
            .onBackPressedDispatcher
            .addCallback {
                onBack()
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

        context?.apply {
            view.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    } else {
                        view.viewTreeObserver.removeGlobalOnLayoutListener(this)
                    }
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
            showColorPicker(option_grid_line_color_button, SessionSettings.instance.canvasGridLineColor) { color ->
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
            showColorPicker(option_canvas_background_primary_color_button, SessionSettings.instance.canvasBackgroundPrimaryColor) { color ->
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
            showColorPicker(option_canvas_background_secondary_color_button, SessionSettings.instance.canvasBackgroundSecondaryColor) { color ->
                option_canvas_background_secondary_color_button.setBackgroundColor(color)
                SessionSettings.instance.canvasBackgroundSecondaryColor = color
            }
        }

//        if (!SessionSettings.instance.tablet) {
//            Animator.animateTitleFromTop(back_button)
//            Animator.animateHorizontalViewEnter(option_grid_line_color_container, true)
//            Animator.animateHorizontalViewEnter(option_canvas_background_primary_color_container, true)
//            Animator.animateHorizontalViewEnter(option_canvas_background_secondary_color_container, true)
//        }
    }

    override fun onResume() {
        super.onResume()

        input_name.setText(SessionSettings.instance.displayName)

        fragment_container.visibility = View.GONE
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

    private fun showColorPicker(colorView: View, initialColor: Int, onColorPicked: (color: Int) -> Unit) {
        val initialOrWhite = if (initialColor == Color.TRANSPARENT) {
            Color.WHITE
        }
        else {
            initialColor
        }

        ColorPickerPopup.Builder(activity)
            .initialColor(initialOrWhite) // Set initial color
            .enableBrightness(true) // Enable brightness slider or not
            .enableAlpha(false) // Enable alpha slider or not
            .okTitle("Choose")
            .cancelTitle("Cancel")
            .showIndicator(false)
            .showValue(false)
            .backgroundColor(Color.BLACK)
            .foregroundColor(Color.WHITE)
            .build()
            .show(colorView, object : ColorPickerObserver() {
                override fun onColorPicked(color: Int) {
                    colorView.setBackgroundColor(color)
                    onColorPicked.invoke(color)
                }

                override fun onColor(color: Int, fromUser: Boolean, shouldPropagate: Boolean) {
                    colorView.setBackgroundColor(color)
                }

                override fun onColorCancel() {
                    if (initialColor == Color.TRANSPARENT) {
                        colorView.setBackgroundColor(Color.WHITE)
                    }
                    else {
                        colorView.setBackgroundColor(initialColor)
                    }
                }
            })
    }
}