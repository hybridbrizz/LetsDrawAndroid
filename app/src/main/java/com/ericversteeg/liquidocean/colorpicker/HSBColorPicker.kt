package com.ericversteeg.liquidocean.colorpicker

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.TextView
import com.ericversteeg.liquidocean.R

object HSBColorPicker {

    fun showDialog(context: Context, startColor: Int, listener: (color: Int) -> Unit) {
        val hsbValues = FloatArray(3)
        Color.colorToHSV(startColor, hsbValues)

        showDialog(context, hsbValues[0], hsbValues[1], hsbValues[2], listener)
    }

    private fun showDialog(context: Context, hue: Float, saturation: Float, brightness: Float, listener: (color: Int) -> Unit) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.color_picker, null)
        val alertDialog = AlertDialog.Builder(context).create()

        val hueTextView = dialogView.findViewById<TextView>(R.id.hue_text)
        val saturationTextView = dialogView.findViewById<TextView>(R.id.saturation_text)
        val brightnessTextView = dialogView.findViewById<TextView>(R.id.brightness_text)

        val sbPalette = dialogView.findViewById<SBPalette>(R.id.sb_palette)
        val sbIndicator = dialogView.findViewById<SBIndicator>(R.id.sb_indicator)

        sbPalette.sbColorSelectionListener = object: SBPalette.SBColorSelectionListener {
            override fun onSBColorSelected(saturation: Float, brightness: Float) {
                saturationTextView.text = context.getString(R.string.saturation_value_text, (saturation * 100).toInt())
                brightnessTextView.text = context.getString(R.string.brightness_value_text, (saturation * 100).toInt())
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
                sbPalette.setSB(saturation, brightness)
            }
        })

        val hPalette = dialogView.findViewById<HPalette>(R.id.h_palette)
        val hIndicator = dialogView.findViewById<HIndicator>(R.id.h_indicator)

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
                hueTextView.text = context.getString(R.string.hue_value_text, hue.toInt())

                sbPalette.setH(hue)
            }
        }

        hPalette.viewTreeObserver.addOnGlobalLayoutListener(object: ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                hPalette.viewTreeObserver.removeOnGlobalLayoutListener(this)

                hPalette.init()
                hPalette.setH(hue)
            }
        })

        val cancelButton = dialogView.findViewById<Button>(R.id.cancel_button)
        val selectButton = dialogView.findViewById<Button>(R.id.select_button)

        cancelButton.setOnClickListener {
            alertDialog.dismiss()
        }

        selectButton.setOnClickListener {
            val hsbValues = floatArrayOf(hPalette.hue, sbPalette.saturation, sbPalette.brightness)
            listener.invoke(Color.HSVToColor(hsbValues))

            alertDialog.dismiss()
        }

        alertDialog.setView(dialogView)
        alertDialog.show()
    }
}