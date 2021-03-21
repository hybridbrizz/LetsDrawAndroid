package com.ericversteeg.liquidocean.helper

import android.content.Context
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.os.Build
import android.util.TypedValue
import android.view.View
import android.view.ViewTreeObserver
import kotlinx.android.synthetic.main.fragment_menu.*
import java.io.*

class Utils {
    interface ViewLayoutListener {
        fun onViewLayout(view: View)
    }

    companion object {
        val baseUrlApi = "https://192.168.200.69:5000"
        //val baseUrlApi = "https://ericversteeg.com:5000"
        val baseUrlSocket = "https://192.168.200.69:5010"
        //val baseUrlSocket = "https://ericversteeg.com:5010"

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

        fun isColorDark(color: Int): Boolean {
            val darkness =
                1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(
                    color
                )) / 255
            return darkness >= 0.85
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
    }
}