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
import android.widget.EditText
import androidx.core.text.isDigitsOnly
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.matrixwarez.pt.R
import com.matrixwarez.pt.helper.Utils
import kotlinx.android.synthetic.main.color_picker_layout.cancel_button
import kotlinx.android.synthetic.main.color_picker_layout.current_color_view
import kotlinx.android.synthetic.main.color_picker_layout.h_edit_text
import kotlinx.android.synthetic.main.color_picker_layout.ok_button
import kotlinx.android.synthetic.main.color_picker_layout.previous_color_view
import kotlinx.coroutines.launch
import java.util.LinkedList
import kotlin.math.roundToInt

class ColorPickerFragment: Fragment() {

    interface ColorListener {
        fun onColor(color: Int)
        fun requestClose()
    }

    private lateinit var rgbColorWheel: RGBColorWheel
    private lateinit var hPalette: HPalette
    private lateinit var sPalette: SPalette
    private lateinit var bPalette: BPalette

    private lateinit var hexEditText: EditText
    private lateinit var hEditText: EditText
    private lateinit var sEditText: EditText
    private lateinit var bEditText: EditText

    private val listeners = LinkedList<ColorListener>()

    private var pcv: PickedColorValues? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.color_picker_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        rgbColorWheel = view.findViewById(R.id.rgb_color_wheel)
        hPalette = view.findViewById(R.id.h_palette)
        sPalette = view.findViewById(R.id.s_palette)
        bPalette = view.findViewById(R.id.b_palette)

        hexEditText = view.findViewById(R.id.hex_text)
        hEditText = view.findViewById(R.id.h_edit_text)
        sEditText = view.findViewById(R.id.s_edit_text)
        bEditText = view.findViewById(R.id.b_edit_text)

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

        pcv?.let {
            rgbColorWheel.setPCV(it)
            hPalette.setPCV(it)
            sPalette.setPCV(it)
            bPalette.setPCV(it)

            updateText(it)
        }

        previous_color_view.background = ColorDrawable(pcv?.toColor() ?: 0)

        ok_button.setOnClickListener {
            listeners.forEach { it.onColor(pcv?.toColor() ?: 0) }
            listeners.forEach { it.requestClose() }
        }

        cancel_button.setOnClickListener {
            listeners.forEach { it.requestClose() }
        }

        pcv?.let {
            addTextChangedListener(hEditText, 360, "h")
            addTextChangedListener(sEditText, 100, "s")
            addTextChangedListener(bEditText, 100, "b")
            addTextChangedListener(hexEditText, -1, "hex")
        }
    }

    fun setColor(color: Int) {
        pcv = PickedColorValues.fromColor(color)

        if (view != null) {
            pcv?.let {
                rgbColorWheel.setPCV(it)
                hPalette.setPCV(it)
                sPalette.setPCV(it)
                bPalette.setPCV(it)

                updateText(it)
            }
        }
    }

    private fun updateText(pcv: PickedColorValues) {
        hEditText.setText("${(pcv.h * 360).roundToInt()}")
        sEditText.setText("${(pcv.s * 100).roundToInt()}")
        bEditText.setText("${(pcv.b * 100).roundToInt()}")

        hexEditText.setText(Utils.colorIntToHex(pcv.toColor()))

        current_color_view.background = ColorDrawable(pcv.toColor())
    }

    fun listen(listener: ColorListener) {
        listeners.add(listener)
    }

    fun addTextChangedListener(view: EditText, maxValue: Int, type: String) {
        view.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                val text = view.text.toString()
                Log.d("Text Test", text)
                if (text.isDigitsOnly() && text.isNotBlank()) {
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

                        current_color_view.background = ColorDrawable(pcv?.toColor() ?: 0)
                    }
                }
                else if (text.matches(Regex("#[0-9A-F]{6}")) || text.matches(Regex("[0-9A-F]{6}"))) {
                    if (type == "hex") {
                        pcv?.let {
                            it.setFromColor(Color.parseColor(text))

                            hEditText.setText("${(it.h * 360).roundToInt()}")
                            sEditText.setText("${(it.s * 100).roundToInt()}")
                            bEditText.setText("${(it.b * 100).roundToInt()}")
                        }

                        rgbColorWheel.invalidate()
                        hPalette.moveIndicator()
                        sPalette.invalidate()
                        bPalette.invalidate()
                    }
                }
            }

            override fun afterTextChanged(p0: Editable?) {

            }
        })
    }
}