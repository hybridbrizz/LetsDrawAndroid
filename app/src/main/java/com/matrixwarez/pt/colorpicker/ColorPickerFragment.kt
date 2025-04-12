package com.matrixwarez.pt.colorpicker

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ScrollView
import androidx.core.text.isDigitsOnly
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.matrixwarez.pt.R
import com.matrixwarez.pt.helper.Utils
import com.matrixwarez.pt.model.SessionSettings
import com.matrixwarez.pt.view.ColorPaletteView
import kotlinx.coroutines.launch
import java.util.LinkedList
import kotlin.math.roundToInt

class ColorPickerFragment: Fragment(), ColorPaletteView.Listener {

    interface ColorListener {
        fun onColor(color: Int)
        fun requestClose()
        fun requestPickCanvas()
        fun pickCanvasColor(color: Int)
    }

    private lateinit var scrollView: ScrollView
    private lateinit var contentView: View
    
    private lateinit var currentColorView: View
    private lateinit var previousColorView: View

    private lateinit var rgbColorWheel: RGBColorWheel
    private lateinit var hPalette: HPalette
    private lateinit var sPalette: SPalette
    private lateinit var bPalette: BPalette

    private lateinit var hexEditText: EditText
    private lateinit var hEditText: EditText
    private lateinit var sEditText: EditText
    private lateinit var bEditText: EditText

    private lateinit var okButton: Button
    private lateinit var cancelButton: Button
    private lateinit var pickCanvasButton: ImageButton
    private lateinit var loadPaletteButton: ImageButton

    private lateinit var colorPickerColorPalette: ColorPaletteView

    private lateinit var keyboardLiftView: View

    val listeners = LinkedList<ColorListener>()

    private var pcv: PickedColorValues? = null

    private var textChangeListeners = mutableMapOf<EditText, TextWatcher>()

    private var previousColor: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.color_picker_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        scrollView = view.findViewById(R.id.color_picker_scroll_view)
        contentView = view.findViewById(R.id.content_view)
        
        currentColorView = view.findViewById(R.id.current_color_view)
        previousColorView = view.findViewById(R.id.previous_color_view)

        rgbColorWheel = view.findViewById(R.id.rgb_color_wheel)
        hPalette = view.findViewById(R.id.h_palette)
        sPalette = view.findViewById(R.id.s_palette)
        bPalette = view.findViewById(R.id.b_palette)

        hexEditText = view.findViewById(R.id.hex_text)
        hEditText = view.findViewById(R.id.h_edit_text)
        sEditText = view.findViewById(R.id.s_edit_text)
        bEditText = view.findViewById(R.id.b_edit_text)

        okButton = view.findViewById(R.id.ok_button)
        cancelButton = view.findViewById(R.id.cancel_button)
        pickCanvasButton = view.findViewById(R.id.pick_canvas_button)
        loadPaletteButton = view.findViewById(R.id.load_palette_button)

        colorPickerColorPalette = view.findViewById(R.id.color_picker_color_palette)

        keyboardLiftView = view.findViewById(R.id.keyboard_lift_view)

        rgbColorWheel.rgbSelectionListener = object: RGBColorWheel.RGBSelectionListener {
            override fun onRGBChanged() {
                hPalette.moveIndicator()
                sPalette.invalidate()
                bPalette.invalidate()

                pcv?.let {
                    updateText(it)
                }
            }
        }

        hPalette.hSelectionListener = object: HPalette.HColorSelectionListener {
            override fun onHChanged() {
                rgbColorWheel.moveIndicator()
                sPalette.invalidate()
                bPalette.invalidate()

                pcv?.let {
                    updateText(it)
                }
            }
        }

        sPalette.sSelectionListener = object: SPalette.SColorSelectionListener {
            override fun onSChanged() {
                rgbColorWheel.moveIndicator()
                bPalette.invalidate()

                pcv?.let {
                    updateText(it)
                }
            }
        }

        bPalette.bSelectionListener = object: BPalette.BColorSelectionListener {
            override fun onBChanged() {
                rgbColorWheel.invalidate()
                sPalette.invalidate()

                pcv?.let {
                    updateText(it)
                }
            }
        }

        rgbColorWheel.viewTreeObserver.addOnGlobalLayoutListener(object: OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                val listener = this
                lifecycleScope.launch {
                    rgbColorWheel.postInvalidate()
                    rgbColorWheel.viewTreeObserver.removeOnGlobalLayoutListener(listener)
                }
            }
        })

        hPalette.viewTreeObserver.addOnGlobalLayoutListener(object: OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                val listener = this
                lifecycleScope.launch {
                    hPalette.resize()
                    hPalette.background = ColorDrawable(Color.BLUE)
                    Log.d("Color Picker Frame Test", "h palette width = ${hPalette.width}")
                    hPalette.viewTreeObserver.removeOnGlobalLayoutListener(listener)
                }
            }
        })

        sPalette.viewTreeObserver.addOnGlobalLayoutListener(object: OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                val listener = this
                lifecycleScope.launch {
                    sPalette.resize()
                    sPalette.background = ColorDrawable(Color.BLUE)
                    Log.d("Color Picker Frame Test", "h palette width = ${hPalette.width}")
                    sPalette.viewTreeObserver.removeOnGlobalLayoutListener(listener)
                }
            }
        })

        bPalette.viewTreeObserver.addOnGlobalLayoutListener(object: OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                val listener = this
                lifecycleScope.launch {
                    bPalette.resize()
                    bPalette.background = ColorDrawable(Color.BLUE)
                    bPalette.viewTreeObserver.removeOnGlobalLayoutListener(listener)
                }
            }
        })

        okButton.viewTreeObserver.addOnGlobalLayoutListener(object: OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                loadPaletteButton.layoutParams = loadPaletteButton.layoutParams.apply { width = okButton.width }
                okButton.viewTreeObserver.removeOnGlobalLayoutListener(this)

                val viewWidth = view.width - Utils.dpToPx(requireContext(), 80)
                colorPickerColorPalette.layoutParams =
                    colorPickerColorPalette.layoutParams.apply {
                        width = viewWidth
                        height = (viewWidth /
                                (colorPickerColorPalette.cols /
                                        colorPickerColorPalette.rows.toFloat())).roundToInt()
                    }
            }
        })

        pcv?.let {
            rgbColorWheel.setPCV(it)
            hPalette.setPCV(it)
            sPalette.setPCV(it)
            bPalette.setPCV(it)

            updateText(it)
        }

        colorPickerColorPalette.colors =
            SessionSettings.instance.colorPaletteColors?.toMutableList() ?: mutableListOf()

        if (Utils.isTablet(requireContext())) {
            colorPickerColorPalette.rows = 1
            colorPickerColorPalette.cols = 16
        }

        pickCanvasButton.setOnClickListener {
            listeners.forEach { it.requestPickCanvas() }
        }
        
        loadPaletteButton.setOnClickListener { 
            when (colorPickerColorPalette.mode == ColorPaletteView.Mode.SELECT) {
                true -> colorPickerColorPalette.mode = ColorPaletteView.Mode.LOAD
                false -> colorPickerColorPalette.mode = ColorPaletteView.Mode.SELECT
            }
        }

        colorPickerColorPalette.listener = this

        previousColorView.setOnClickListener {
            previousColor?.let {
                setColor(it, false)
            }
        }

        okButton.setOnClickListener {
            listeners.forEach { it.onColor(pcv?.toColor() ?: 0) }
            listeners.forEach { it.requestClose() }
        }

        cancelButton.setOnClickListener {
            listeners.forEach { it.requestClose() }
        }

        addFocusChangedListener(hEditText, 360, "h")
        addFocusChangedListener(sEditText, 100, "s")
        addFocusChangedListener(bEditText, 100, "b")
        addFocusChangedListener(hexEditText, -1, "hex")

        contentView.setOnClickListener {
            hexEditText.clearFocus()
            hEditText.clearFocus()
            sEditText.clearFocus()
            bEditText.clearFocus()
        }
    }

    fun setColor(color: Int, setPreviousColor: Boolean) {
        pcv = PickedColorValues.fromColor(color)

        if (view != null) {
            pcv?.let {
                rgbColorWheel.setPCV(it)
                hPalette.setPCV(it)
                sPalette.setPCV(it)
                bPalette.setPCV(it)

                updateText(it)

                currentColorView.background = ColorDrawable(pcv?.toColor() ?: 0)

                if (setPreviousColor) {
                    val pColor = pcv?.toColor() ?: 0
                    previousColorView.background = ColorDrawable(pColor)
                    previousColor = pColor
                }
            }

            scrollView.scrollBy(0, 10000)
        }
    }

    private fun updateText(pcv: PickedColorValues) {
        hEditText.setText("${(pcv.h * 360).roundToInt()}")
        sEditText.setText("${(pcv.s * 100).roundToInt()}")
        bEditText.setText("${(pcv.b * 100).roundToInt()}")

        hexEditText.setText(Utils.colorIntToHex(pcv.toColor()))

        currentColorView.background = ColorDrawable(pcv.toColor())
    }

    fun listen(listener: ColorListener) {
        listeners.add(listener)
    }

    private fun addTextChangedListener(view: EditText, maxValue: Int, type: String) {
        if (textChangeListeners.containsKey(view)) {
            return
        }

        val listener = object: TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                val text = view.text.toString().uppercase()

                if (text.matches(Regex("#[0-9A-F]{6}")) || text.matches(Regex("[0-9A-F]{6}"))) {
                    if (type == "hex") {
                        pcv?.let {
                            try {
                                val colorHex = if (!text.startsWith("#")) {
                                    "#$text"
                                }
                                else {
                                    text
                                }
                                it.setFromColor(Color.parseColor(colorHex))
                            }
                            catch (e: Exception) {}

                            hEditText.setText("${(it.h * 360).roundToInt()}")
                            sEditText.setText("${(it.s * 100).roundToInt()}")
                            bEditText.setText("${(it.b * 100).roundToInt()}")
                        }

                        rgbColorWheel.invalidate()
                        hPalette.moveIndicator()
                        sPalette.invalidate()
                        bPalette.invalidate()

                        currentColorView.background = ColorDrawable(pcv?.toColor() ?: 0)
                    }
                }
                else if (text.isDigitsOnly() && text.isNotBlank()) {
                    val value = text.toInt()
                    Log.d("Value Test", value.toString())
                    if (value in 0..maxValue) {
                        when (type) {
                            "h" -> {
                                Log.d("Value Test", "h set to $value")
                                pcv?.h = value.toFloat() / maxValue
                                rgbColorWheel.moveIndicator()
                                hPalette.moveIndicator()
                                sPalette.invalidate()
                                bPalette.invalidate()
                            }
                            "s" -> {
                                Log.d("Value Test", "s set to $value")
                                pcv?.s = value.toFloat() / maxValue
                                rgbColorWheel.moveIndicator()
                                sPalette.moveIndicator()
                                bPalette.invalidate()
                            }
                            "b" -> {
                                Log.d("Value Test", "b set to $value")
                                pcv?.b = value.toFloat() / maxValue
                                rgbColorWheel.invalidate()
                                sPalette.invalidate()
                                bPalette.moveIndicator()
                            }
                        }

                        currentColorView.background = ColorDrawable(pcv?.toColor() ?: 0)
                        hexEditText.setText(Utils.colorIntToHex(pcv?.toColor() ?: 0))
                    }
                }
            }

            override fun afterTextChanged(p0: Editable?) {

            }
        }

        view.addTextChangedListener(listener)
        textChangeListeners[view] = listener
    }

    private fun removeTextChangeListener(view: EditText) {
        val listener = textChangeListeners.remove(view)

        listener?.let {
            view.removeTextChangedListener(it)
        }
    }

    private fun addFocusChangedListener(view: EditText, maxValue: Int, type: String) {
        view.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                if (view != hexEditText) {
                    if (keyboardLiftView.visibility == View.GONE) {
                        keyboardLiftView.visibility = View.VISIBLE
                    }
                }
                addTextChangedListener(view, maxValue, type)
            }
            else {
                if (view != hexEditText) {
                    if (keyboardLiftView.visibility == View.VISIBLE) {
                        keyboardLiftView.visibility = View.GONE
                    }
                }
                removeTextChangeListener(view)
            }
        }
    }

    // Color Palette View Listener
    override fun onSelectColor(color: Int) {
        setColor(color, false)
    }

    override fun onRequestLoadColor(index: Int) {
        SessionSettings.instance.loadColorPaletteAtIndex(requireContext(), index, pcv?.toColor() ?: 0)
        colorPickerColorPalette.colors = SessionSettings.instance.colorPaletteColors?.toMutableList() ?: mutableListOf()
        colorPickerColorPalette.mode = ColorPaletteView.Mode.SELECT
    }
}