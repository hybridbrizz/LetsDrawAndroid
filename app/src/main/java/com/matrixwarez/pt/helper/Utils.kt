package com.matrixwarez.pt.helper

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Point
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.text.TextPaint
import android.util.TypedValue
import android.view.View
import android.view.ViewTreeObserver
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.matrixwarez.pt.R
import com.matrixwarez.pt.model.Server
import com.matrixwarez.pt.model.SessionSettings
import com.matrixwarez.pt.view.InteractiveCanvasView
import kotlinx.android.synthetic.main.fragment_menu.*
import java.io.*
import java.util.*
import kotlin.math.max
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

        fun colorIntToHex(colorInt: Int): String {
            // Extract the RGB components from the color int
            val red = (colorInt shr 16) and 0xFF
            val green = (colorInt shr 8) and 0xFF
            val blue = colorInt and 0xFF

            // Format as a hex string with # prefix
            return String.format("#%02X%02X%02X", red, green, blue)
        }

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

        fun isColorDark(color: Int, threshold: Float): Boolean {
            val darkness =
                1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(
                    color
                )) / 255
            return darkness >= threshold
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

        /**
         * By Claude
         * Brightens a color by a specified amount while maintaining perceptual consistency.
         *
         * @param color The original color to brighten
         * @param brightnessAdjustment A value between 0.0 and 1.0 where:
         *   - 0.0 means no change
         *   - 1.0 means brighten fully toward white
         * @return The brightened color
         */
        fun brightenColor(color: Int, brightnessAdjustment: Float): Int {
            // Clamp brightness adjustment to valid range
            val adjustment = max(0f, min(1f, brightnessAdjustment))

            // Extract color components
            val alpha = Color.alpha(color)
            val red = Color.red(color)
            val green = Color.green(color)
            val blue = Color.blue(color)

            // Convert to HSL (Hue, Saturation, Lightness)
            val hsl = FloatArray(3)
            colorToHSL(red, green, blue, hsl)

            // Adjust lightness while preserving hue and reducing saturation
            // This provides more natural brightening than just increasing RGB values
            hsl[1] = hsl[1] * (1f - adjustment * 0.5f) // Reduce saturation as brightness increases
            hsl[2] = hsl[2] + (1f - hsl[2]) * adjustment // Increase lightness

            // Convert back to RGB
            val brightColor = hslToColor(alpha, hsl)

            return brightColor
        }

        /**
         * Converts RGB components to HSL.
         */
        private fun colorToHSL(red: Int, green: Int, blue: Int, hsl: FloatArray) {
            val r = red / 255f
            val g = green / 255f
            val b = blue / 255f

            val max = maxOf(r, g, b)
            val min = minOf(r, g, b)
            val delta = max - min

            // Calculate lightness
            val lightness = (max + min) / 2f
            hsl[2] = lightness

            // If max equals min, it's a shade of gray
            if (delta == 0f) {
                hsl[0] = 0f // Hue
                hsl[1] = 0f // Saturation
            } else {
                // Calculate saturation
                hsl[1] = if (lightness < 0.5f) {
                    delta / (max + min)
                } else {
                    delta / (2f - max - min)
                }

                // Calculate hue
                val deltaR = (((max - r) / 6f) + (delta / 2f)) / delta
                val deltaG = (((max - g) / 6f) + (delta / 2f)) / delta
                val deltaB = (((max - b) / 6f) + (delta / 2f)) / delta

                hsl[0] = when (max) {
                    r -> deltaB - deltaG
                    g -> (1f / 3f) + deltaR - deltaB
                    else -> (2f / 3f) + deltaG - deltaR
                }

                // Ensure hue is between 0 and 1
                if (hsl[0] < 0) hsl[0] += 1f
                if (hsl[0] > 1) hsl[0] -= 1f
            }
        }

        /**
         * Converts HSL values to RGB color with alpha.
         */
        private fun hslToColor(alpha: Int, hsl: FloatArray): Int {
            val h = hsl[0]
            val s = hsl[1]
            val l = hsl[2]

            val c = (1f - abs(2f * l - 1f)) * s
            val x = c * (1f - abs((h * 6f) % 2f - 1f))
            val m = l - c / 2f

            var r = 0f
            var g = 0f
            var b = 0f

            when ((h * 6f).toInt()) {
                0, 6 -> { r = c; g = x; b = 0f }
                1 -> { r = x; g = c; b = 0f }
                2 -> { r = 0f; g = c; b = x }
                3 -> { r = 0f; g = x; b = c }
                4 -> { r = x; g = 0f; b = c }
                5 -> { r = c; g = 0f; b = x }
            }

            return Color.argb(
                alpha,
                ((r + m) * 255).toInt().coerceIn(0, 255),
                ((g + m) * 255).toInt().coerceIn(0, 255),
                ((b + m) * 255).toInt().coerceIn(0, 255)
            )
        }

        /**
         * Helper function to get absolute value (equivalent to Math.abs)
         */
        private fun abs(value: Float): Float {
            return if (value < 0) -value else value
        }

        fun preloadThumbnails(context: Context, servers: List<Server>, startIndex: Int, amount: Int,
                                      onDone: () -> Unit, onError: () -> Unit = {}) {

            val indices = servers.indices
            if (startIndex !in indices || startIndex + amount - 1 !in indices) {
                onError()
                return
            }

            var numLoaded = 0

            for (i in startIndex until startIndex + amount) {
                Glide.with(context)
                    .load(servers[i].canvasImageUrl)
                    .listener(object: RequestListener<Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable>,
                            isFirstResource: Boolean
                        ): Boolean {
                            numLoaded += 1
                            if (numLoaded >= amount) {
                                onDone()
                            }
                            return false
                        }

                        override fun onResourceReady(
                            resource: Drawable,
                            model: Any,
                            target: Target<Drawable>?,
                            dataSource: DataSource,
                            isFirstResource: Boolean
                        ): Boolean {
                            numLoaded += 1
                            if (numLoaded >= amount) {
                                onDone()
                            }
                            return false
                        }
                    })
                    .preload()
            }
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
                interactiveCanvasView.interactiveCanvas.paintUnit(Point(rX, rY))
            }
        }
        else {
            for (i in 0 until rSmallAmt) {
                val rX = (Math.random() * interactiveCanvasView.interactiveCanvas.cols).toInt()
                val rY = (Math.random() * interactiveCanvasView.interactiveCanvas.rows).toInt()
                interactiveCanvasView.interactiveCanvas.paintUnit(Point(rX, rY))
            }
        }

        interactiveCanvasView.endPainting()
    }
}