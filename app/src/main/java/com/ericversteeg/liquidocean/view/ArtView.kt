package com.ericversteeg.liquidocean.view

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.AttributeSet
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.FileProvider
import com.ericversteeg.liquidocean.model.InteractiveCanvas
import java.io.*
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.round


class ArtView: View {

    var art: List<InteractiveCanvas.RestorePoint>? = null
    set(value) {
        field = value
        invalidate()
    }

    var ppu = 10F

    val margin = 2

    constructor(context: Context) : super(context) {
        commonInit()
    }

    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet) {
        commonInit()
    }

    constructor(context: Context, attributeSet: AttributeSet, v0: Int) : super(
        context,
        attributeSet,
        v0
    ) {
        commonInit()
    }

    @RequiresApi(21)
    constructor(context: Context, attributeSet: AttributeSet, v0: Int, v1: Int) : super(
        context,
        attributeSet,
        v0,
        v1
    ) {
        commonInit()
    }

    private fun commonInit() {

    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        drawToCanvas(canvas)
    }

    private fun drawToCanvas(canvas: Canvas?, drawBackground: Boolean = true) {
        canvas?.apply {
            save()

            val paint = Paint()

            adjustPpu()

            val minX = getMinX()
            val minY = getMinY()

            val offsetX = (width - getArtWidth() * ppu) / 2
            val offsetY = (height - getArtHeight() * ppu) / 2

            //val adjGridX = abs(round(offsetX / ppu) - offsetX / ppu)
            //val adjGridY = abs(round(offsetY / ppu) - offsetY / ppu)

            val gridPpu = 20F

            // draw transparency background
            val widthUnits = width / gridPpu.toInt() + 1
            val heightUnits = height / gridPpu.toInt() + 1

            val whitePaint =  ActionButtonView.whitePaint
            val grayPaint = ActionButtonView.photoshopGray

            if (drawBackground) {
                for (x in 0 until widthUnits) {
                    for (y in 0 until heightUnits) {
                        if ((x + y) % 2 == 0) {
                            canvas.drawRect(
                                x * gridPpu,
                                y * gridPpu,
                                (x + 1) * gridPpu,
                                (y + 1) * gridPpu,
                                whitePaint
                            )
                        }
                        else {
                            canvas.drawRect(
                                x * gridPpu,
                                y * gridPpu,
                                (x + 1) * gridPpu,
                                (y + 1) * gridPpu,
                                grayPaint
                            )
                        }
                    }
                }
            }

            art?.apply {
                for (pixelPoint in this) {
                    paint.color = pixelPoint.color
                    canvas.drawRect(
                        ((pixelPoint.point.x - minX) * ppu) + offsetX,
                        ((pixelPoint.point.y - minY) * ppu) + offsetY,
                        ((pixelPoint.point.x - minX + 1) * ppu) + offsetX,
                        ((pixelPoint.point.y - minY + 1) * ppu) + offsetY, paint
                    )
                }
            }

            restore()
        }
    }

    private fun getMinX(): Int {
        var min = -1
        art?.apply {
            for (pixelPoint in this) {
                if (pixelPoint.point.x < min || min == -1) {
                    min = pixelPoint.point.x
                }
            }
        }

        return min
    }

    private fun getMaxX(): Int {
        var max = -1
        art?.apply {
            for (pixelPoint in this) {
                if (pixelPoint.point.x > max || max == -1) {
                    max = pixelPoint.point.x
                }
            }
        }

        return max
    }

    private fun getMinY(): Int {
        var min = -1
        art?.apply {
            for (pixelPoint in this) {
                if (pixelPoint.point.y < min || min == -1) {
                    min = pixelPoint.point.y
                }
            }
        }

        return min
    }

    private fun getMaxY(): Int {
        var max = -1
        art?.apply {
            for (pixelPoint in this) {
                if (pixelPoint.point.y > max || max == -1) {
                    max = pixelPoint.point.y
                }
            }
        }

        return max
    }

    private fun getArtWidth(): Int {
        return getMaxX() - getMinX() + 1
    }

    private fun getArtHeight(): Int {
        return getMaxY() - getMinY() + 1
    }

    private fun adjustPpu() {
        val artW = getArtWidth()
        val artH = getArtHeight()

        val fillWidthPpu =  width / artW.toFloat()
        val fillHeightPpu = height / artH.toFloat()

        ppu = min(fillWidthPpu, fillHeightPpu)

        ppu *= 0.8F
    }

    fun saveArt(context: Context) {
        val conf: Bitmap.Config = Bitmap.Config.ARGB_8888 // see other conf types
        val bitmap: Bitmap = Bitmap.createBitmap(width, height, conf) // this creates a MUTABLE bitmap

        val canvas = Canvas(bitmap)
        drawToCanvas(canvas, false)

        //Generating a file name
        val filename = "${System.currentTimeMillis()}.jpg"

        //Output stream
        var fos: OutputStream? = null

        //For devices running android >= Q
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            //getting the contentResolver
            context.contentResolver?.also { resolver ->

                //Content resolver will process the contentvalues
                val contentValues = ContentValues().apply {

                    //putting file information in content values
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                }

                //Inserting the contentValues to contentResolver and getting the Uri
                val imageUri: Uri? =
                    resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

                //Opening an outputstream with the Uri that we got
                fos = imageUri?.let { resolver.openOutputStream(it) }
            }
        } else {
            //These for devices running on android < Q
            //So I don't think an explanation is needed here
            val imagesDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val image = File(imagesDir, filename)
            fos = FileOutputStream(image)
        }

        fos?.use {
            //Finally writing the bitmap to the output stream that we opened
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            Toast.makeText(context, "Saved to Photos", Toast.LENGTH_LONG).show()
        }
    }

    fun shareArt(context: Context) {
        val conf: Bitmap.Config = Bitmap.Config.ARGB_8888 // see other conf types
        val bmp: Bitmap = Bitmap.createBitmap(width, height, conf) // this creates a MUTABLE bitmap

        val canvas = Canvas(bmp)
        drawToCanvas(canvas, false)

        val share = Intent(Intent.ACTION_SEND)
        share.type = "image/png"
        val bytes = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.PNG, 100, bytes)
        val f =
            File(context.cacheDir.absolutePath + File.separator.toString() + "temporary_file.jpg")
        try {
            f.createNewFile()
            val fo = FileOutputStream(f)
            fo.write(bytes.toByteArray())
        } catch (e: IOException) {
            e.printStackTrace()
        }
        share.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(context, "com.ericversteeg.liquidocean.fileprovider", f))

        context.startActivity(Intent.createChooser(share, "Share Image"))
    }
}