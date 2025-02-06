package com.ericversteeg.liquidocean.fragment

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
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
import android.widget.EditText
import android.widget.TextView
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
import com.ericversteeg.liquidocean.databinding.FragmentOptionsBinding
import com.ericversteeg.liquidocean.helper.Animator
import com.ericversteeg.liquidocean.helper.Utils
import com.ericversteeg.liquidocean.listener.FragmentListener
import com.ericversteeg.liquidocean.listener.OptionsListener
import com.ericversteeg.liquidocean.model.SessionSettings
import com.ericversteeg.liquidocean.view.ActionButtonView
import org.json.JSONObject
import top.defaults.colorpicker.ColorPickerPopup
import top.defaults.colorpicker.ColorPickerPopup.ColorPickerObserver


class OptionsFragment: Fragment(), FragmentListener {

    var optionsListener: OptionsListener? = null

    var selectingCanvasLockColor = false
    var selectingGridLineColor = false
    var selectingPaintMeterColor = false
    
    private var _binding: FragmentOptionsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentOptionsBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.backButton.actionBtnView = binding.backAction
        binding.backAction.type = ActionButtonView.Type.BACK_SOLID

        binding.backButton.setOnClickListener {
            if (binding.creditsContainer.visibility == View.VISIBLE) {
                binding.creditsContainer.visibility = View.GONE
            }
            else {
                context?.apply {
                    SessionSettings.instance.save(this)
                }

                optionsListener?.onOptionsBack()
            }
        }

        binding.inputName.setText(SessionSettings.instance.displayName)

        binding.inputName.setOnEditorActionListener(object : TextView.OnEditorActionListener {
            override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    val input = binding.inputName.text.toString().trim()
                    if (input.length > 20) {
                        binding.inputName.setBackgroundDrawable(
                            ResourcesCompat.getDrawable(
                                resources,
                                R.drawable.input_display_name_red,
                                null
                            )
                        )
                        binding.changeNameButton.isEnabled = false
                    }
                    else {
                        sendNameCheck(binding.inputName.text.toString().trim())
                    }

                    val inputMethodManager =
                        context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)

                    return true
                }
                return false
            }
        })

        binding.changeNameButton.setOnClickListener {
            updateDisplayName(binding.inputName.text.toString())
        }

        binding.signInButton.setOnClickListener {
            val intent = Intent(context, SignInActivity::class.java)
            intent.putExtra("mode", SignInFragment.modeSignIn)
            startActivity(intent)
        }

        binding.recoveryPincodeButton.setOnClickListener {
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
                binding.resetSinglePlay.isEnabled = false
            }

            binding.resetSinglePlay.setOnClickListener {
                showSinglePlayRestWarning()
            }

            binding.importSinglePlay.setOnClickListener {
                showCanvasImportFragment()
            }

            binding.exportSinglePlay.setOnClickListener {
                showCanvasExportFragment()
            }

            // option paint panel background
            binding.panelRecyclerView.layoutManager = LinearLayoutManager(
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

                    (binding.panelRecyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                        SessionSettings.instance.panelBackgroundResIndex, (view.width * 0.15).toInt()
                    )
                }
            })

            binding.panelRecyclerView.adapter = PanelRecyclerViewAdapter(
                this, SessionSettings.instance.panelResIds.toMutableList()
            )

            (binding.panelRecyclerView.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        }

        // option canvas lock
        binding.optionCanvasLockSwitch.isChecked = SessionSettings.instance.canvasLockBorder
        binding.optionCanvasLockSwitch.setOnCheckedChangeListener { _, value ->
            SessionSettings.instance.canvasLockBorder = value
        }

        // option canvas lock color
        binding.optionCanvasLockColorButton.setBackgroundColor(SessionSettings.instance.canvasLockBorderColor)
        binding.optionCanvasLockColorReset.setOnClickListener {
            SessionSettings.instance.resetCanvasLockBorderColor()
            binding.optionCanvasLockColorButton.setBackgroundColor(SessionSettings.instance.canvasLockBorderColor)
        }

        // option canvas lock color
        binding.optionCanvasLockColorButton.setOnClickListener {
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
        binding.optionGridLineColorButton.setBackgroundColor(SessionSettings.instance.canvasGridLineColor)
        binding.optionGridLineColorResetButton.setOnClickListener {
            SessionSettings.instance.canvasGridLineColor = -1
            binding.optionGridLineColorButton.setBackgroundColor(SessionSettings.instance.canvasGridLineColor)
        }

        binding.optionGridLineColorButton.setOnClickListener {
            ColorPickerPopup.Builder(activity)
                .initialColor(SessionSettings.instance.canvasGridLineColor) // Set initial color
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
                        SessionSettings.instance.canvasGridLineColor = color
                    }

                    override fun onColor(color: Int, fromUser: Boolean, shouldPropagate: Boolean) {
                        it.setBackgroundColor(color)
                    }

                    override fun onColorCancel() {
                        it.setBackgroundColor(SessionSettings.instance.canvasGridLineColor)
                    }
                })
        }

        // option canvas background primary color
        if (SessionSettings.instance.canvasBackgroundPrimaryColor == 0) {
            binding.optionCanvasBackgroundPrimaryColorButton.setBackgroundColor(Color.WHITE)
        }
        else {
            binding.optionCanvasBackgroundPrimaryColorButton.setBackgroundColor(SessionSettings.instance.canvasBackgroundPrimaryColor)
        }

        binding.optionCanvasBackgroundPrimaryColorResetButton.setOnClickListener {
            SessionSettings.instance.canvasBackgroundSecondaryColor = 0
            binding.optionCanvasBackgroundPrimaryColorButton.setBackgroundColor(Color.WHITE)
        }

        binding.optionCanvasBackgroundPrimaryColorButton.setOnClickListener {
            ColorPickerPopup.Builder(activity)
                .initialColor(SessionSettings.instance.canvasBackgroundPrimaryColor) // Set initial color
                .enableBrightness(true) // Enable brightness slider or not
                .enableAlpha(false) // Enable alpha slider or not
                .okTitle("Choose")
                .cancelTitle("Cancel")
                .showIndicator(false)
                .showValue(false)
                .build()
                .show(it, object : ColorPickerObserver() {
                    override fun onColorPicked(color: Int) {
                        it.setBackgroundColor(color)
                        SessionSettings.instance.canvasBackgroundPrimaryColor = color
                    }

                    override fun onColor(color: Int, fromUser: Boolean, shouldPropagate: Boolean) {
                        it.setBackgroundColor(color)
                    }

                    override fun onColorCancel() {
                        if (SessionSettings.instance.canvasBackgroundPrimaryColor == 0) {
                            it.setBackgroundColor(Color.WHITE)
                        }
                        else {
                            it.setBackgroundColor(SessionSettings.instance.canvasBackgroundPrimaryColor)
                        }
                    }
                })
        }

        // option canvas background secondary color
        if (SessionSettings.instance.canvasBackgroundSecondaryColor == 0) {
            binding.optionCanvasBackgroundSecondaryColorButton.setBackgroundColor(Color.WHITE)
        }
        else {
            binding.optionCanvasBackgroundSecondaryColorButton.setBackgroundColor(SessionSettings.instance.canvasBackgroundSecondaryColor)
        }

        binding.optionCanvasBackgroundSecondaryColorResetButton.setOnClickListener {
            SessionSettings.instance.canvasBackgroundSecondaryColor = 0
            binding.optionCanvasBackgroundSecondaryColorButton.setBackgroundColor(Color.WHITE)
        }

        binding.optionCanvasBackgroundSecondaryColorButton.setOnClickListener {
            ColorPickerPopup.Builder(activity)
                .initialColor(SessionSettings.instance.canvasBackgroundSecondaryColor) // Set initial color
                .enableBrightness(true) // Enable brightness slider or not
                .enableAlpha(false) // Enable alpha slider or not
                .okTitle("Choose")
                .cancelTitle("Cancel")
                .showIndicator(false)
                .showValue(false)
                .build()
                .show(it, object : ColorPickerObserver() {
                    override fun onColorPicked(color: Int) {
                        it.setBackgroundColor(color)
                        SessionSettings.instance.canvasBackgroundSecondaryColor = color
                    }

                    override fun onColor(color: Int, fromUser: Boolean, shouldPropagate: Boolean) {
                        it.setBackgroundColor(color)
                    }

                    override fun onColorCancel() {
                        if (SessionSettings.instance.canvasBackgroundSecondaryColor == 0) {
                            it.setBackgroundColor(Color.WHITE)
                        }
                        else {
                            it.setBackgroundColor(SessionSettings.instance.canvasBackgroundSecondaryColor)
                        }
                    }
                })
        }

        // option frame color
        binding.optionFrameColorButton.setBackgroundColor(SessionSettings.instance.frameColor)
        binding.optionFrameColorResetButton.setOnClickListener {
            SessionSettings.instance.frameColor = Color.GRAY
            binding.optionFrameColorButton.setBackgroundColor(SessionSettings.instance.frameColor)
        }

        // option frame color
        binding.optionFrameColorButton.setOnClickListener {
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
        binding.optionEmittersContainer.visibility = View.GONE

        binding.optionEmittersSwitch.isChecked = SessionSettings.instance.emittersEnabled
        binding.optionEmittersSwitch.setOnCheckedChangeListener { _, value ->
            SessionSettings.instance.emittersEnabled = value
        }

        // option bold action buttons
        binding.optionBoldActionButtonsSwitch.isChecked = SessionSettings.instance.boldActionButtons
        binding.optionBoldActionButtonsSwitch.setOnCheckedChangeListener { _, value ->
            SessionSettings.instance.boldActionButtons = value
        }

        // option paint indicator width
        binding.optionPaintIndicatorWidthValue.text = SessionSettings.instance.colorIndicatorWidth.toString()

        binding.optionPaintIndicatorWidthActionMinus.type = ActionButtonView.Type.DOT
        binding.optionPaintIndicatorWidthActionPlus.type = ActionButtonView.Type.DOT

        binding.optionPaintIndicatorWidthButtonMinus.actionBtnView = binding.optionPaintIndicatorWidthActionMinus
        binding.optionPaintIndicatorWidthButtonPlus.actionBtnView = binding.optionPaintIndicatorWidthActionPlus

        binding.optionPaintIndicatorWidthButtonMinus.setOnClickListener {
            var value = binding.optionPaintIndicatorWidthValue.text.toString().toInt() - 1
            if (value == 0) value = 1

            binding.optionPaintIndicatorWidthValue.text = value.toString()
            SessionSettings.instance.colorIndicatorWidth = value
        }

        binding.optionPaintIndicatorWidthButtonPlus.setOnClickListener {
            var value = binding.optionPaintIndicatorWidthValue.text.toString().toInt() + 1
            if (value == 6) value = 5

            binding.optionPaintIndicatorWidthValue.text = value.toString()
            SessionSettings.instance.colorIndicatorWidth = value
        }

        // option color palette size
        binding.optionColorPaletteSizeValue.text = SessionSettings.instance.colorPaletteSize.toString()

        binding.optionColorPaletteSizeActionMinus.type = ActionButtonView.Type.DOT
        binding.optionColorPaletteSizeActionPlus.type = ActionButtonView.Type.DOT

        binding.optionColorPaletteSizeButtonMinus.actionBtnView = binding.optionColorPaletteSizeActionMinus
        binding.optionColorPaletteSizeButtonPlus.actionBtnView = binding.optionColorPaletteSizeActionPlus

        binding.optionColorPaletteSizeButtonMinus.setOnClickListener {
            var value = binding.optionColorPaletteSizeValue.text.toString().toInt() - 1
            if (value <= 0) value = 1

            binding.optionColorPaletteSizeValue.text = value.toString()
            SessionSettings.instance.colorPaletteSize = value
        }

        binding.optionColorPaletteSizeButtonPlus.setOnClickListener {
            var value = binding.optionColorPaletteSizeValue.text.toString().toInt() + 1
            if (value >= 15) value = 14

            binding.optionColorPaletteSizeValue.text = value.toString()
            SessionSettings.instance.colorPaletteSize = value
        }

        // option paint indicator fill circle
        binding.optionPaintIndicatorFillCircleSwitch.isChecked = SessionSettings.instance.colorIndicatorFill

        binding.optionPaintIndicatorFillCircleSwitch.setOnCheckedChangeListener { button, _ ->
            SessionSettings.instance.colorIndicatorFill = button.isChecked
            if (button.isChecked && SessionSettings.instance.colorIndicatorSquare) {
                binding.optionPaintIndicatorSquareSwitch.isChecked = false
            }
        }

        // option paint indicator square
        binding.optionPaintIndicatorSquareSwitch.isChecked = SessionSettings.instance.colorIndicatorSquare

        binding.optionPaintIndicatorSquareSwitch.setOnCheckedChangeListener { button, _ ->
            SessionSettings.instance.colorIndicatorSquare = button.isChecked
            if (button.isChecked && SessionSettings.instance.colorIndicatorFill) {
                binding.optionPaintIndicatorFillCircleSwitch.isChecked = false
            }
        }

        // option paint indicator outline
        binding.optionPaintIndicatorOutlineSwitch.isChecked = SessionSettings.instance.colorIndicatorOutline

        binding.optionPaintIndicatorOutlineSwitch.setOnCheckedChangeListener { button, _ ->
            SessionSettings.instance.colorIndicatorOutline = button.isChecked
        }

        // option close paint panel button color
        binding.optionClosePaintPanelColorButton.setBackgroundColor(SessionSettings.instance.closePaintBackButtonColor)
        binding.optionClosePaintPanelColorResetButton.setOnClickListener {
            SessionSettings.instance.closePaintBackButtonColor = -1
            binding.optionClosePaintPanelColorButton.setBackgroundColor(SessionSettings.instance.closePaintBackButtonColor)
        }

        binding.optionClosePaintPanelColorButton.setOnClickListener {
            ColorPickerPopup.Builder(activity)
                .initialColor(SessionSettings.instance.closePaintBackButtonColor) // Set initial color
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
                        SessionSettings.instance.closePaintBackButtonColor = color
                    }

                    override fun onColor(color: Int, fromUser: Boolean, shouldPropagate: Boolean) {
                        it.setBackgroundColor(color)
                    }

                    override fun onColorCancel() {
                        it.setBackgroundColor(SessionSettings.instance.closePaintBackButtonColor)
                    }
                })
        }

        // option show paint bar
        binding.optionShowPaintBarSwitch.isChecked = SessionSettings.instance.showPaintBar

        binding.optionShowPaintBarSwitch.setOnCheckedChangeListener { button, _ ->
            SessionSettings.instance.showPaintBar = button.isChecked
            if (button.isChecked && binding.optionShowPaintCircleSwitch.isChecked) {
                binding.optionShowPaintCircleSwitch.isChecked = false
                SessionSettings.instance.showPaintCircle = false
            }
        }

        // option show paint circle
        binding.optionShowPaintCircleSwitch.isChecked = SessionSettings.instance.showPaintCircle

        binding.optionShowPaintCircleSwitch.setOnCheckedChangeListener { button, _ ->
            SessionSettings.instance.showPaintCircle = button.isChecked
            if (button.isChecked && binding.optionShowPaintBarSwitch.isChecked) {
                binding.optionShowPaintBarSwitch.isChecked = false
                SessionSettings.instance.showPaintBar = false
            }
        }

        // option paint bar color
        binding.optionPaintBarColorButton.setBackgroundColor(SessionSettings.instance.paintBarColor)
        binding.optionPaintBarColorResetButton.setOnClickListener {
            SessionSettings.instance.paintBarColor = Color.parseColor("#FFAAAAAA")
            binding.optionPaintBarColorButton.setBackgroundColor(SessionSettings.instance.paintBarColor)
        }

        // option grid line color
        binding.optionPaintBarColorButton.setOnClickListener {
            ColorPickerPopup.Builder(activity)
                .initialColor(SessionSettings.instance.paintBarColor) // Set initial color
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
                        SessionSettings.instance.paintBarColor = color
                    }

                    override fun onColor(color: Int, fromUser: Boolean, shouldPropagate: Boolean) {
                        it.setBackgroundColor(color)
                    }

                    override fun onColorCancel() {
                        it.setBackgroundColor(SessionSettings.instance.paintBarColor)
                    }
                })
        }

        // option right handed
        binding.optionRightHandedSwitch.isChecked = SessionSettings.instance.rightHanded

        binding.optionRightHandedSwitch.setOnCheckedChangeListener { button, _ ->
            SessionSettings.instance.rightHanded = button.isChecked
        }

        // option small action buttons
        binding.optionSmallActionButtonsSwitch.isChecked = SessionSettings.instance.smallActionButtons

        binding.optionSmallActionButtonsSwitch.setOnCheckedChangeListener { button, _ ->
            SessionSettings.instance.smallActionButtons = button.isChecked
        }

        binding.optionPaintPanelTextureTitle.setOnClickListener {
            binding.creditsContainer.visibility = View.VISIBLE
        }

        setupNumRecentColorsChoices()

        if (!SessionSettings.instance.tablet) {
            Animator.animateTitleFromTop(binding.optionsTitleText)

            Animator.animateHorizontalViewEnter(binding.optionPaintPanelTextureTitle, false)
            Animator.animateHorizontalViewEnter(binding.panelRecyclerView, true)
            Animator.animateHorizontalViewEnter(binding.optionRightHanded, false)
            Animator.animateHorizontalViewEnter(binding.optionCanvasLockContainer, true)
            Animator.animateHorizontalViewEnter(binding.optionCanvasLockColorContainer, false)
        }
    }

    override fun onResume() {
        super.onResume()

        binding.inputName.setText(SessionSettings.instance.displayName)

        if (SessionSettings.instance.pincodeSet) {
            binding.signInButton.text = "Signed in"
            binding.signInButton.isEnabled = false

            binding.recoveryPincodeButton.text = "Change access pincode"
        }

        binding.fragmentContainer.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupNumRecentColorsChoices() {
        // option num recent colors
        for (v in binding.optionNumRecentColorsChoiceLayout.children) {
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

            binding.fragmentContainer.visibility = View.VISIBLE
        }
    }

    private fun showCanvasExportFragment() {
        fragmentManager?.apply {
            val canvasExportFragment = CanvasExportFragment()
            canvasExportFragment.fragmentListener = this@OptionsFragment

            beginTransaction().replace(R.id.fragment_container, canvasExportFragment).commit()

            binding.fragmentContainer.visibility = View.VISIBLE
        }
    }

    fun sendNameCheck(name: String) {
        if (name.length > 20) {
            return
        }

        val requestQueue = Volley.newRequestQueue(context)

        val request = object: JsonObjectRequest(
            Request.Method.GET,
            Utils.baseUrlApi + "/api/v1/devices/checkname/" + name,
            null,
            { response ->
                activity?.runOnUiThread {
                    val taken = !response.getBoolean("a")
                    if (taken) {
                        binding.inputName.setBackgroundDrawable(
                            ResourcesCompat.getDrawable(
                                resources,
                                R.drawable.input_display_name_red,
                                null
                            )
                        )
                        binding.changeNameButton.isEnabled = false
                    } else {
                        binding.inputName.setBackgroundDrawable(
                            ResourcesCompat.getDrawable(
                                resources,
                                R.drawable.input_display_name_green,
                                null
                            )
                        )
                        binding.changeNameButton.isEnabled = true
                    }
                }

            },
            { error ->
                binding.changeNameButton.text = "Error"
                binding.changeNameButton.isEnabled = false
                binding.inputName.isEnabled = false
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
        val requestQueue = Volley.newRequestQueue(context)

        val requestParams = HashMap<String, String>()

        requestParams["name"] = name

        val paramsJson = JSONObject(requestParams as Map<String, String>)

        val request = object: JsonObjectRequest(
            Request.Method.POST,
            Utils.baseUrlApi + "/api/v1/devices/${SessionSettings.instance.uniqueId}",
            paramsJson,
            { response ->
                SessionSettings.instance.displayName = response.getString("name")
                binding.changeNameButton.text = "Updated"
                binding.changeNameButton.isEnabled = false
                binding.inputName.isEnabled = false
            },
            { error ->
                binding.changeNameButton.text = "Error"
                binding.changeNameButton.isEnabled = false
                binding.inputName.isEnabled = false
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
        binding.fragmentContainer.visibility = View.GONE
    }
}