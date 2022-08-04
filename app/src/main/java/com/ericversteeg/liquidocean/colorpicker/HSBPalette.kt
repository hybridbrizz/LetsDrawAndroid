package com.ericversteeg.liquidocean.colorpicker

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.ericversteeg.liquidocean.R
import com.ericversteeg.liquidocean.model.SessionSettings
import java.util.*

class HSBPalette: FrameLayout {

    interface ColorListener {
        fun onColor(color: Int)
    }

    private lateinit var sbPalette: SBPalette
    private lateinit var hPalette: HPalette
    private lateinit var sbIndicator: SBIndicator
    private lateinit var hIndicator: HIndicator

    val hsb = FloatArray(3)

    private val listeners = LinkedList<ColorListener>()

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context): super(context) {
        initLayout(context)
    }

    constructor(context: Context, attributeSet: AttributeSet): super(context, attributeSet) {
        initLayout(context)
    }

    private fun initLayout(context: Context) {
        val layout = LayoutInflater.from(context).inflate(R.layout.color_picker_layout, this, false)
        this.addView(layout)

        sbPalette = layout.findViewById(R.id.sb_palette)
        hPalette = layout.findViewById(R.id.h_palette)
        sbIndicator = layout.findViewById(R.id.sb_indicator)
        hIndicator = layout.findViewById(R.id.h_indicator)
    }

    fun init(startColor: Int) {
        val textContainer = findViewById<LinearLayout>(R.id.linear_layout_text_container)

        val hueTextView = findViewById<TextView>(R.id.hue_text)
        val saturationTextView = findViewById<TextView>(R.id.saturation_text)
        val brightnessTextView = findViewById<TextView>(R.id.brightness_text)

        val hsbValues = FloatArray(3)
        Color.colorToHSV(startColor, hsbValues)

        val sHue = hsbValues[0]
        val sSaturation = hsbValues[1]
        val sBrightness = hsbValues[2]

        hsb[0] = hsbValues[0]
        hsb[1] = hsbValues[1]
        hsb[2] = hsbValues[2]

        sbPalette.sbColorSelectionListener = object: SBPalette.SBColorSelectionListener {
            override fun onSBColorSelected(saturation: Float, brightness: Float) {
                hsb[1] = saturation
                hsb[2] = brightness

                saturationTextView.text = context.getString(R.string.saturation_value_text, (saturation * 100).toInt())
                brightnessTextView.text = context.getString(R.string.brightness_value_text, (brightness * 100).toInt())

                listeners.forEach { it.onColor(Color.HSVToColor(hsb)) }
            }
        }

        sbPalette.indicatorCallback = object: SBPalette.SBIndicatorCallback {
            override fun indicatorStartPosition(x: Float, y: Float) {
                sbIndicator.x = x - sbIndicator.width / 2
                sbIndicator.y = y - sbIndicator.height / 2
            }

            override fun moveIndicatorToPosition(x: Float, y: Float) {
                if (x >= 0 && x <= sbPalette.width) {
                    sbIndicator.x = x - sbIndicator.width / 2
                }
                if (y >= 0 && y <= sbPalette.height) {
                    sbIndicator.y = y - sbIndicator.height / 2
                }
            }
        }

        sbPalette.viewTreeObserver.addOnGlobalLayoutListener(object: ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                sbPalette.viewTreeObserver.removeOnGlobalLayoutListener(this)

                sbPalette.init()
                sbPalette.setSB(sSaturation, sBrightness)
            }
        })

        hPalette.indicatorCallback = object: HPalette.HIndicatorCallback {
            override fun indicatorStartPosition(x: Float) {
                hIndicator.x = x - hIndicator.width / 2
                hIndicator.y = hPalette.y
            }

            override fun moveIndicatorToPosition(x: Float) {
                if (x >= 0 && x <= hPalette.width) {
                    hIndicator.x = x - hIndicator.width / 2
                }
            }
        }

        hPalette.hColorSelectionListener = object: HPalette.HColorSelectionListener {
            override fun onHColorSelected(hue: Float) {
                sbPalette.setH(hue)
                hsb[0] = hue

                hueTextView.text = context.getString(R.string.hue_value_text, hue.toInt())

                listeners.forEach { it.onColor(Color.HSVToColor(hsb)) }
            }
        }

        hPalette.viewTreeObserver.addOnGlobalLayoutListener(object: ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                hPalette.viewTreeObserver.removeOnGlobalLayoutListener(this)

                hPalette.init()
                hPalette.setH(sHue)
            }
        })

        textContainer.setOnClickListener {
            if (it.alpha == 1F) {
                it.alpha = 0F
            }
            else {
                it.alpha = 1F
            }

            SessionSettings.instance.hsbTextVisible = it.alpha == 1F
            SessionSettings.instance.saveHsbTextVisible(context)
        }

        if (!SessionSettings.instance.hsbTextVisible) {
            textContainer.alpha = 0F
        }
    }

    fun listen(listener: ColorListener) {
        listeners.add(listener)
    }
}