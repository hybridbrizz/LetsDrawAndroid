package com.matrixwarez.pt.helper

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Point
import android.graphics.Shader
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.text.TextPaint
import android.util.TypedValue
import android.view.View
import android.view.ViewTreeObserver
import android.widget.TextView
import com.matrixwarez.pt.R
import com.matrixwarez.pt.model.SessionSettings
import com.matrixwarez.pt.view.InteractiveCanvasView
import kotlinx.android.synthetic.main.fragment_menu.*
import java.io.*
import java.util.*
import kotlin.math.min

class Utils {
    interface ViewLayoutListener {
        fun onViewLayout(view: View)
    }

    companion object {
        val baseServersUrl = "https://matrixwarez.com:5050/"

        //val baseUrlApi = "https://192.168.200.69:5000"
        //val baseUrlApi = "https://ericversteeg.com:5000"
        //val baseUrlApiAlt = "https://ericversteeg.com:5030"
        //val baseUrlSocket = "https://192.168.200.69:5010"
        //val baseUrlSocket = "https://ericversteeg.com:5010"
        //val baseUrlQueueSocket = "https://ericversteeg.com:5020"

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

        fun randomSettings() {
            SessionSettings.instance.gridLineMode = randomIndex(2)
            SessionSettings.instance.canvasGridLineColor = randomColor()
            SessionSettings.instance.panelBackgroundResIndex = randomIndex(SessionSettings.instance.panelResIds.size)

            val colorIndicatorType = randomIndex(3)
            if (colorIndicatorType == 0) {
                SessionSettings.instance.colorIndicatorSquare = true
                SessionSettings.instance.colorIndicatorFill = false
            }
            else if (colorIndicatorType == 1) {
                SessionSettings.instance.colorIndicatorSquare = false
                SessionSettings.instance.colorIndicatorFill = true
            }
            else {
                SessionSettings.instance.colorIndicatorSquare = false
                SessionSettings.instance.colorIndicatorFill = false
            }

            SessionSettings.instance.colorIndicatorOutline = randomBool()
            SessionSettings.instance.backgroundColorsIndex = randomIndex(7)
            SessionSettings.instance.paintColor = randomColor()
        }

        fun showErrorDialog(context: Context, message: String, onDismiss: () -> Unit) {
            (context as Activity?)?.runOnUiThread {
                AlertDialog.Builder(context)
                    .setMessage(message)
                    // The dialog is automatically dismissed when a dialog button is clicked.
                    .setPositiveButton(
                        "..."
                    ) { dialog, id ->
                        dialog?.dismiss()
                    }
                    .setOnDismissListener {
                        onDismiss.invoke()
                    }
                    .show()
            }
        }

        private fun randomBool(): Boolean {
            return Math.random() < 0.5
        }

        private fun randomIndex(size: Int): Int {
            return (Math.random() * size).toInt()
        }

        private fun randomColor(): Int {
            return Color.argb(255, randomIndex(256), randomIndex(256), randomIndex(256))
        }

        fun colorizeTextView(textView: TextView, colorString1: String, colorString2: String) {
            val paint: TextPaint = textView.paint
            val width: Float = paint.measureText("Tianjin, China")

            val textShader: Shader = LinearGradient(
                0F, 0F, width, textView.textSize, intArrayOf(
                    Color.parseColor(colorString1),
                    Color.parseColor(colorString2)
                ), null, Shader.TileMode.CLAMP
            )
            textView.paint.shader = textShader
        }

        fun colorizeTextView(textView: TextView, color1: Int, color2: Int) {
            val paint: TextPaint = textView.paint
            val width: Float = paint.measureText("Tianjin, China")

            val textShader: Shader = LinearGradient(
                0F, 0F, width, textView.textSize, intArrayOf(
                    color1,
                    color2
                ), null, Shader.TileMode.CLAMP
            )
            textView.paint.shader = textShader
        }
    }

    fun startSimulateDraw(interactiveCanvasView: InteractiveCanvasView) {
        Timer().schedule(object: TimerTask() {
            override fun run() {
                val rT = (Math.random() * 20 + 1).toInt()
                Timer().schedule(object: TimerTask() {
                    override fun run() {
                        simulateDraw(interactiveCanvasView)
                    }
                }, 1000L * rT)
            }

        }, 3000)
    }

    fun simulateDraw(interactiveCanvasView: InteractiveCanvasView) {
        val rSmallAmt = (Math.random() * 20 + 2).toInt()
        val rBigAmt = (Math.random() * 100 + 50).toInt()

        interactiveCanvasView.startPainting()

        val r = (Math.random() * 10).toInt()
        if (r < 2) {
            for (i in 0 until rBigAmt) {
                val rX = (Math.random() * interactiveCanvasView.interactiveCanvas.cols).toInt()
                val rY = (Math.random() * interactiveCanvasView.interactiveCanvas.rows).toInt()
                interactiveCanvasView.interactiveCanvas.paintUnitOrUndo(Point(rX, rY))
            }
        }
        else {
            for (i in 0 until rSmallAmt) {
                val rX = (Math.random() * interactiveCanvasView.interactiveCanvas.cols).toInt()
                val rY = (Math.random() * interactiveCanvasView.interactiveCanvas.rows).toInt()
                interactiveCanvasView.interactiveCanvas.paintUnitOrUndo(Point(rX, rY))
            }
        }

        interactiveCanvasView.endPainting(true)
    }
}