package com.matrixwarez.pt.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.matrixwarez.pt.R
import com.matrixwarez.pt.helper.Utils
import com.matrixwarez.pt.listener.ArtExportFragmentListener
import com.matrixwarez.pt.model.InteractiveCanvas
import com.matrixwarez.pt.model.SessionSettings
import kotlinx.android.synthetic.main.fragment_art_export.*
import org.json.JSONObject

class ArtExportFragment: Fragment() {

    lateinit var art: List<InteractiveCanvas.RestorePoint>

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

        art_view.showBackground = true
        art_view.art = art

        art_view.setOnClickListener {

        }

        if (art.isNotEmpty()) {
            SessionSettings.instance.addToShowcase(art)
        }

        share_button.setOnClickListener {
            context?.apply {
                art_view.shareArt(this)
            }
        }

        save_button.setOnClickListener {
            context?.apply {
                art_view.saveArt(this)
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

    private fun sendArtPixels() {
        val requestQueue = Volley.newRequestQueue(context)

        context?.apply {
            val uniqueId = SessionSettings.instance.uniqueId

            uniqueId?.apply {
                val jsonObj = buildUploadRequestJson()

                val request = object: JsonObjectRequest(
                    Request.Method.POST,
                     "api/v1/canvas/object/upload",
                    jsonObj,
                    { response ->

                    },
                    { error ->

                    }) {

                    override fun getHeaders(): MutableMap<String, String> {
                        val headers = HashMap<String, String>()
                        headers["Content-Type"] = "application/json; charset=utf-8"
                        headers["key1"] = Utils.key1
                        return headers
                    }
                }

                request.tag = "download"
                requestQueue.add(request)
            }
        }
    }

    private fun buildUploadRequestJson(): JSONObject {
        val requestParams = HashMap<String, Any>()

        requestParams["w"] = getArtWidth()
        requestParams["h"] = getArtHeight()
        requestParams["pixels"] = pixelsJsonString(art)

        return JSONObject(requestParams as Map<String, Any>)
    }

    private fun pixelsJsonString(art: List<InteractiveCanvas.RestorePoint>): Array<Map<String, Int>?> {
        val restorePoints = arrayOfNulls<Map<String, Int>>(art.size)
        for (i in art.indices) {
            val restorePoint = art[i]
            val map = HashMap<String, Int>()

            map["x"] = restorePoint.point.x
            map["y"] = restorePoint.point.y
            map["color"] = restorePoint.color

            restorePoints[i] = map
        }

        return restorePoints
    }

    private fun getMinX(): Int {
        var min = -1
        art.apply {
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
        art.apply {
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
        art.apply {
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
        art.apply {
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
}