package com.matrixwarez.pt.model

import android.content.Context
import android.graphics.Color
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.matrixwarez.pt.R
import com.matrixwarez.pt.helper.Utils
import com.matrixwarez.pt.view.ColorPanelTouchTargetView

data class ColorPanelIcon(
    val context: Context,
    val name: String,
    val iconViews: List<View>,
    val bgView: ColorPanelTouchTargetView,
    val outerBgView: View,
    val isSelected: () -> Boolean,
    val onPress: () -> Unit
) {
    init {
        bgView.bindIconData(name, onPress)
    }

    fun updateAppearance(color: Int) {
        val isColorDark = Utils.isColorDark(color, 0.3f)

        // paint button
        when (isSelected()) {
            true -> {
                val drawableResId = when (isColorDark) {
                    true -> R.drawable.paint_button_background_light_selected
                    false -> R.drawable.paint_button_background_light_selected
                }
                bgView.background = ContextCompat.getDrawable(context, drawableResId)

                val outerDrawableResId = when (isColorDark) {
                    true -> R.drawable.paint_button_light_selected_outer_border
                    false -> R.drawable.paint_button_light_selected_outer_border
                }

                outerBgView.background = ContextCompat.getDrawable(context, outerDrawableResId)
            }
            false -> {
                val drawableResId = when (isColorDark) {
                    true -> R.drawable.paint_button_background_light_unselected
                    false -> R.drawable.paint_button_background_light_unselected
                }
                bgView.background = ContextCompat.getDrawable(context, drawableResId)
                outerBgView.background = null
            }
        }

        val iconColor = when (isColorDark) {
            true -> Color.parseColor("#FFFFFFFF")
            false -> Color.parseColor("#FFFFFFFF")
        }

        iconViews.forEach {
            when (it) {
                is ImageView -> {
                    it.setColorFilter(iconColor)
                }
                is TextView -> {
                    it.setTextColor(iconColor)
                }
            }
        }
    }
}