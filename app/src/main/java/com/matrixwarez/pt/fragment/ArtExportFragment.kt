package com.matrixwarez.pt.fragment

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.matrixwarez.pt.R
import com.matrixwarez.pt.helper.Utils
import com.matrixwarez.pt.listener.ArtExportFragmentListener
import com.matrixwarez.pt.model.InteractiveCanvas
import com.matrixwarez.pt.model.SessionSettings
import kotlinx.android.synthetic.main.fragment_art_export.actual_size_text
import kotlinx.android.synthetic.main.fragment_art_export.art_view
import kotlinx.android.synthetic.main.fragment_art_export.back_button_export
import kotlinx.android.synthetic.main.fragment_art_export.canvas_bitmap
import kotlinx.android.synthetic.main.fragment_art_export.save_button
import kotlinx.android.synthetic.main.fragment_art_export.screen_size_switch
import kotlinx.android.synthetic.main.fragment_art_export.screen_size_text
import kotlinx.android.synthetic.main.fragment_art_export.share_button


class ArtExportFragment: Fragment() {

    var art: List<InteractiveCanvas.RestorePoint>? = null
    var interactiveCanvas: InteractiveCanvas? = null
    private var bitmap: Bitmap? = null

    var listener: ArtExportFragmentListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_art_export, container, false)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        back_button_export.setOnClickListener {
            listener?.onArtExportBack()
        }

        screen_size_switch.setOnCheckedChangeListener { _, isChecked ->
            art_view.actualSize = isChecked

            if (isChecked) {
                actual_size_text.visibility = View.VISIBLE
                screen_size_text.visibility = View.INVISIBLE
            }
            else {
                actual_size_text.visibility = View.INVISIBLE
                screen_size_text.visibility = View.VISIBLE
            }
        }

        actual_size_text.setOnClickListener {
            screen_size_switch.isChecked = !screen_size_switch.isChecked
            actual_size_text.visibility = View.INVISIBLE
        }

        screen_size_text.setOnClickListener {
            screen_size_switch.isChecked = !screen_size_switch.isChecked
            screen_size_text.visibility = View.INVISIBLE
        }

        actual_size_text.visibility = View.INVISIBLE

        if (interactiveCanvas != null) {
            screen_size_switch.visibility = View.GONE
            screen_size_text.visibility = View.GONE
            actual_size_text.visibility = View.GONE
        }

        art_view.showBackground = true

        art?.let {
            art_view.art = art
        }

        interactiveCanvas?.let {
            bitmap = createBitmapFromPixelArray(it.arr)
            canvas_bitmap.setImageBitmap(bitmap)
        }

        art_view.setOnClickListener {

        }

        if (art?.isNotEmpty() == true) {
            SessionSettings.instance.addToShowcase(art!!)
        }

        share_button.setOnClickListener {
            context?.apply {
                when (bitmap != null) {
                    true -> {
                        art_view.shareArt(this, bitmap)
                    }
                    false -> {
                        art_view.shareArt(this)
                    }
                }
            }
        }

        save_button.setOnClickListener {
            context?.apply {
                when (bitmap != null) {
                    true -> {
                        art_view.saveArt(this, bitmap)
                    }
                    false -> {
                        art_view.saveArt(this)
                    }
                }

            }
        }

        //sendArtPixels()

        Utils.setViewLayoutListener(view, object: Utils.ViewLayoutListener {
            override fun onViewLayout(view: View) {
                if (SessionSettings.instance.tablet) {
                    val layoutParams = (art_view.layoutParams as ConstraintLayout.LayoutParams)

                    layoutParams.topMargin = Utils.dpToPx(context, 80)

                    art_view.layoutParams = layoutParams
                }
            }
        })
    }

    /*override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        Utils.setViewLayoutListener(art_view, object: Utils.ViewLayoutListener {
            override fun onViewLayout(view: View) {
                art_view.invalidate()
            }
        })
    }*/

//    private fun sendArtPixels() {
//        val requestQueue = Volley.newRequestQueue(context)
//
//        context?.apply {
//            val uniqueId = SessionSettings.instance.uniqueId
//
//            uniqueId?.apply {
//                val jsonObj = buildUploadRequestJson()
//
//                val request = object: JsonObjectRequest(
//                    Request.Method.POST,
//                     "api/v1/canvas/object/upload",
//                    jsonObj,
//                    { response ->
//
//                    },
//                    { error ->
//
//                    }) {
//
//                    override fun getHeaders(): MutableMap<String, String> {
//                        val headers = HashMap<String, String>()
//                        headers["Content-Type"] = "application/json; charset=utf-8"
//                        headers["key1"] = Utils.key1
//                        return headers
//                    }
//                }
//
//                request.tag = "download"
//                requestQueue.add(request)
//            }
//        }
//    }

//    private fun buildUploadRequestJson(): JSONObject {
//        val requestParams = HashMap<String, Any>()
//
//        requestParams["w"] = getArtWidth()
//        requestParams["h"] = getArtHeight()
//        requestParams["pixels"] = pixelsJsonString(art)
//
//        return JSONObject(requestParams as Map<String, Any>)
//    }

//    private fun pixelsJsonString(art: List<InteractiveCanvas.RestorePoint>): Array<Map<String, Int>?> {
//        val restorePoints = arrayOfNulls<Map<String, Int>>(art.size)
//        for (i in art.indices) {
//            val restorePoint = art[i]
//            val map = HashMap<String, Int>()
//
//            map["x"] = restorePoint.point.x
//            map["y"] = restorePoint.point.y
//            map["color"] = restorePoint.color
//
//            restorePoints[i] = map
//        }
//
//        return restorePoints
//    }

//    private fun getMinX(): Int {
//        var min = -1
//        art?.apply {
//            for (pixelPoint in this) {
//                if (pixelPoint.point.x < min || min == -1) {
//                    min = pixelPoint.point.x
//                }
//            }
//        }
//
//        return min
//    }
//
//    private fun getMaxX(): Int {
//        var max = -1
//        art?.apply {
//            for (pixelPoint in this) {
//                if (pixelPoint.point.x > max || max == -1) {
//                    max = pixelPoint.point.x
//                }
//            }
//        }
//
//        return max
//    }
//
//    private fun getMinY(): Int {
//        var min = -1
//        art?.apply {
//            for (pixelPoint in this) {
//                if (pixelPoint.point.y < min || min == -1) {
//                    min = pixelPoint.point.y
//                }
//            }
//        }
//
//        return min
//    }
//
//    private fun getMaxY(): Int {
//        var max = -1
//        art?.apply {
//            for (pixelPoint in this) {
//                if (pixelPoint.point.y > max || max == -1) {
//                    max = pixelPoint.point.y
//                }
//            }
//        }
//
//        return max
//    }

//    private fun getArtWidth(): Int {
//        return getMaxX() - getMinX() + 1
//    }
//
//    private fun getArtHeight(): Int {
//        return getMaxY() - getMinY() + 1
//    }

    fun createBitmapFromPixelArray(pixels: Array<IntArray>): Bitmap? {
        if (pixels.isEmpty() || pixels[0].isEmpty()) {
            return null
        }

        // Get dimensions
        val height = pixels.size
        val width = pixels[0].size

        // Create a flat 1D array from 2D array (required by setPixels)
        val flatPixels = IntArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                var color = pixels[y][x]
                if (color == 0) {
                    color = Color.BLACK
                }
                flatPixels[y * width + x] = color
            }
        }

        // Create the bitmap with the exact dimensions
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)


        // Set all pixels at once
        bitmap.setPixels(flatPixels, 0, width, 0, 0, width, height)

        return bitmap
    }
}