package com.ericversteeg.liquidocean.helper

import android.content.Context
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.TypedValue
import android.view.View
import android.view.ViewTreeObserver
import com.ericversteeg.liquidocean.R
import kotlinx.android.synthetic.main.fragment_menu.*
import java.io.*
import kotlin.math.min

class Utils {
    interface ViewLayoutListener {
        fun onViewLayout(view: View)
    }

    companion object {
        //val baseUrlApi = "https://192.168.200.69:5000"
        val baseUrlApi = "https://ericversteeg.com:5000"
        //val baseUrlSocket = "https://192.168.200.69:5010"
        val baseUrlSocket = "https://ericversteeg.com:5010"

        val key1 = "8AHI!VR7299G7cq3YsP359HDkKz682oNT3QHh?yyehuvkyzdm674w45o"

        fun dpToPx(context: Context?, dp: Int): Int {
            context?.apply {
                return TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    dp.toFloat(),
                    context.resources.displayMetrics
                ).toInt()
            }
            return 0
        }

        fun dpToPxF(context: Context?, dp: Int): Float {
            context?.apply {
                return TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    dp.toFloat(),
                    context.resources.displayMetrics
                )
            }
            return 0F
        }

        fun isColorDark(color: Int): Boolean {
            val darkness =
                1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(
                    color
                )) / 255
            return darkness >= 0.85
        }

        fun brightenColor(color: Int, by: Float): Int {
            var red = Color.red(color)
            var green = Color.green(color)
            var blue = Color.blue(color)

            red = min(255, (red + red * by).toInt())
            green = min(255, (green + green * by).toInt())
            blue = min(255, (blue + blue * by).toInt())

            return Color.argb(255, red, green, blue)
        }

        fun isNetworkAvailable(context: Context): Boolean {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val nw      = connectivityManager.activeNetwork ?: return false
                val actNw = connectivityManager.getNetworkCapabilities(nw) ?: return false
                return when {
                    actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                    actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                    //for other device how are able to connect with Ethernet
                    actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                    //for check internet over Bluetooth
                    actNw.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> true
                    else -> false
                }
            } else {
                val nwInfo = connectivityManager.activeNetworkInfo ?: return false
                return nwInfo.isConnected
            }
        }

        fun setViewLayoutListener(view: View, completion: ViewLayoutListener) {
            view.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    completion.onViewLayout(view)
                    view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            })
        }

        fun isTablet(context: Context): Boolean {
            return context.resources.getBoolean(R.bool.isTablet)
        }
    }
}