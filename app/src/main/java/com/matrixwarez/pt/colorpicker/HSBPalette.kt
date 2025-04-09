package com.matrixwarez.pt.colorpicker

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.matrixwarez.pt.R
import com.matrixwarez.pt.model.SessionSettings
import java.util.*

class HSBPalette: FrameLayout {

    interface ColorListener {
        fun onColor(color: Int)
    }

    private lateinit var sbPalette: SBPalette
    private lateinit var hPalette: HPalette

    private val listeners = LinkedList<ColorListener>()

    private var pcv: PickedColorValues? = null

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context): super(context) {
        initLayout(context)
    }

    constructor(context: Context, attributeSet: AttributeSet): super(context, attributeSet) {
        initLayout(context)
    }

    private fun initLayout(context: Context) {
        background = ColorDrawable(Color.RED)

        val layout = LayoutInflater.from(context).inflate(R.layout.color_picker_layout, this, false)
        this.addView(layout)

        sbPalette = layout.findViewById(R.id.sb_palette)
        hPalette = layout.findViewById(R.id.h_palette)

        sbPalette.sbSelectionListener = object: SBPalette.SBSelectionListener {
            override fun onSBChanged() {

            }
        }

        hPalette.hSelectionListener = object: HPalette.HColorSelectionListener {
            override fun onHChanged() {
                sbPalette.invalidate()
            }
        }

        sbPalette.viewTreeObserver.addOnGlobalLayoutListener {
            sbPalette.resize()
        }

        hPalette.viewTreeObserver.addOnGlobalLayoutListener {
            hPalette.resize()
        }
    }

    fun setColor(color: Int) {
        val hsbValues = FloatArray(3)
        Color.colorToHSV(color, hsbValues)

        pcv = PickedColorValues(
            h = hsbValues[0],
            s = hsbValues[1],
            b = hsbValues[2]
        )

        sbPalette.setPCV(pcv!!)
        hPalette.setPCV(pcv!!)
    }

    fun listen(listener: ColorListener) {
        listeners.add(listener)
    }

    val textChangeListener = object: TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            try {
                val color = Color.parseColor("#$s")

                //hideKeyboard()
            }
            catch (exception: Exception) {

            }
        }

        override fun afterTextChanged(s: Editable?) {

        }
    }

//        color_hex_string_input.addTextChangedListener(textChangeListener)
//
//        color_hex_string_input.setOnEditorActionListener { textView, actionId, keyEvent ->
//            if (actionId == EditorInfo.IME_ACTION_DONE) {
//                hideKeyboard()
//            }
//            true
//        }
}