package com.matrixwarez.pt.colorpicker

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.matrixwarez.pt.R
import kotlinx.android.synthetic.main.color_picker_layout.cancel_button
import kotlinx.android.synthetic.main.color_picker_layout.ok_button
import kotlinx.coroutines.launch
import java.util.LinkedList

class ColorPickerFragment: Fragment() {

    interface ColorListener {
        fun onColor(color: Int)
        fun requestClose()
    }

    private lateinit var sbPalette: SBPalette
    private lateinit var hPalette: HPalette
    private lateinit var sPalette: SPalette
    private lateinit var bPalette: BPalette

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
        sbPalette = view.findViewById(R.id.sb_palette)
        hPalette = view.findViewById(R.id.h_palette)
        sPalette = view.findViewById(R.id.s_palette)
        bPalette = view.findViewById(R.id.b_palette)

        sbPalette.sbSelectionListener = object: SBPalette.SBSelectionListener {
            override fun onSBChanged() {
                sPalette.invalidate()
                bPalette.invalidate()
            }
        }

        hPalette.hSelectionListener = object: HPalette.HColorSelectionListener {
            override fun onHChanged() {
                sbPalette.invalidate()
                sPalette.invalidate()
            }
        }

        sPalette.sSelectionListener = object: SPalette.SColorSelectionListener {
            override fun onSChanged() {
                sbPalette.moveIndicator()
                bPalette.invalidate()
            }
        }

        bPalette.bSelectionListener = object: BPalette.BColorSelectionListener {
            override fun onBChanged() {
                sbPalette.moveIndicator()
                sPalette.invalidate()
            }
        }

        Log.d("Color Picker Frame Test", "fragment with measures = ${sbPalette.width}")

        sbPalette.viewTreeObserver.addOnGlobalLayoutListener(object: OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                val listener = this
                lifecycleScope.launch {
                    sbPalette.resize()
                    sbPalette.background = ColorDrawable(Color.parseColor("#F32765"))
                    Log.d("Color Picker Frame Test", "sb palette width = ${sbPalette.width}")
                    sbPalette.viewTreeObserver.removeOnGlobalLayoutListener(listener)
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
                    sbPalette.viewTreeObserver.removeOnGlobalLayoutListener(listener)
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
            sbPalette.setPCV(it)
            hPalette.setPCV(it)
            sPalette.setPCV(it)
            bPalette.setPCV(it)
        }

        ok_button.setOnClickListener {
            listeners.forEach { it.onColor(pcv?.toColor() ?: 0) }
            listeners.forEach { it.requestClose() }
        }

        cancel_button.setOnClickListener {
            listeners.forEach { it.requestClose() }
        }
    }

    fun setColor(color: Int) {
        val hsbValues = FloatArray(3)
        Color.colorToHSV(color, hsbValues)

        pcv = PickedColorValues(
            h = hsbValues[0] / 360f,
            s = hsbValues[1],
            b = hsbValues[2]
        )

        if (view != null) {
            sbPalette.setPCV(pcv!!)
            hPalette.setPCV(pcv!!)
        }
    }

    fun listen(listener: ColorListener) {
        listeners.add(listener)
    }
}