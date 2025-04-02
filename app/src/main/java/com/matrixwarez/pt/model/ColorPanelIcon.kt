package com.matrixwarez.pt.model

import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.matrixwarez.pt.R
import com.matrixwarez.pt.helper.Utils

data class ColorPanelIcon(
    val iconViews: List<View>,
    val bgView: View,
    val outerBgView: View,
    val isSelected: () -> Boolean
) {
    fun updateAppearance(context: Context, color: Int) {
        val isColorDark = Utils.isColorDark(color, 0.3f)

        // paint button
        when (isSelected()) {
            true -> {
                val drawableResId = when (isColorDark) {
                    true -> R.drawable.paint_button_background_light_selected
                    false -> R.drawable.paint_button_background_dark_selected
                }
                bgView.background = ContextCompat.getDrawable(context, drawableResId)

                val outerDrawableResId = when (isColorDark) {
                    true -> R.drawable.paint_button_light_selected_outer_border
                    false -> R.drawable.paint_button_dark_selected_outer_border
                }

                outerBgView.background = ContextCompat.getDrawable(context, outerDrawableResId)
            }
            false -> {
                val drawableResId = when (isColorDark) {
                    true -> R.drawable.paint_button_background_light_unselected
                    false -> R.drawable.paint_button_background_dark_unselected
                }
                bgView.background = ContextCompat.getDrawable(context, drawableResId)
                outerBgView.background = null
            }
        }

        val iconColor = when (isColorDark) {
            true -> Color.parseColor("#FFFFFFFF")
            false -> Color.parseColor("#FF000000")
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