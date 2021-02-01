package com.ericversteeg.liquidocean.helper

import android.content.Context
import android.graphics.Color
import android.os.Environment
import android.util.Log
import android.util.TypedValue
import java.io.*

class Utils {
    companion object {
        fun dpToPx(context: Context, dp: Int): Int {
            return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp.toFloat(),
                context.resources.displayMetrics
            ).toInt()
        }

        fun isColorDark(color: Int): Boolean {
            val darkness =
                1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(
                    color
                )) / 255
            return darkness >= 0.85
        }
    }
}