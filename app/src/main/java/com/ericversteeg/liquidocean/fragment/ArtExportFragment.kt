package com.ericversteeg.liquidocean.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.ericversteeg.liquidocean.databinding.FragmentArtExportBinding
import com.ericversteeg.liquidocean.helper.Utils
import com.ericversteeg.liquidocean.listener.ArtExportFragmentListener
import com.ericversteeg.liquidocean.model.InteractiveCanvas
import com.ericversteeg.liquidocean.model.SessionSettings
import com.ericversteeg.liquidocean.view.ActionButtonView
import org.json.JSONObject

class ArtExportFragment: Fragment() {

    lateinit var art: List<InteractiveCanvas.RestorePoint>

    var listener: ArtExportFragmentListener? = null
    
    private var _binding: FragmentArtExportBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentArtExportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.backActionExport.type = ActionButtonView.Type.BACK_SOLID
        binding.backButtonExport.actionBtnView = binding.backActionExport

        binding.shareAction.type = ActionButtonView.Type.EXPORT_SOLID
        binding.shareButton.actionBtnView = binding.shareAction

        binding.saveAction.type = ActionButtonView.Type.SAVE
        binding.saveButton.actionBtnView = binding.saveAction

        binding.backButtonExport.setOnClickListener {
            listener?.onArtExportBack()
        }

        binding.screenSizeSwitch.setOnCheckedChangeListener { _, isChecked ->
            binding.artView.actualSize = isChecked

            if (isChecked) {
                binding.actualSizeText.visibility = View.VISIBLE
                binding.screenSizeText.visibility = View.INVISIBLE
            }
            else {
                binding.actualSizeText.visibility = View.INVISIBLE
                binding.screenSizeText.visibility = View.VISIBLE
            }
        }

        binding.actualSizeText.setOnClickListener {
            binding.screenSizeSwitch.isChecked = !binding.screenSizeSwitch.isChecked
            binding.actualSizeText.visibility = View.INVISIBLE
        }

        binding.screenSizeText.setOnClickListener {
            binding.screenSizeSwitch.isChecked = !binding.screenSizeSwitch.isChecked
            binding.screenSizeText.visibility = View.INVISIBLE
        }

        binding.actualSizeText.visibility = View.INVISIBLE

        binding.artView.showBackground = true
        binding.artView.art = art

        binding.artView.setOnClickListener {

        }

        if (art.isNotEmpty()) {
            SessionSettings.instance.addToShowcase(art)
        }

        binding.shareButton.setOnClickListener {
            context?.apply {
                binding.artView.shareArt(this)
            }
        }

        binding.saveButton.setOnClickListener {
            context?.apply {
                binding.artView.saveArt(this)
            }
        }

        //sendArtPixels()

        Utils.setViewLayoutListener(view, object: Utils.ViewLayoutListener {
            override fun onViewLayout(view: View) {
                if (SessionSettings.instance.tablet) {
                    val layoutParams = (binding.artView.layoutParams as ConstraintLayout.LayoutParams)

                    layoutParams.topMargin = Utils.dpToPx(context, 80)

                    binding.artView.layoutParams = layoutParams
                }
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /*override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        Utils.setViewLayoutListener(binding.artView, object: Utils.ViewLayoutListener {
            override fun onViewLayout(view: View) {
                binding.artView.invalidate()
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
                    Utils.baseUrlApi + "/api/v1/canvas/object/upload",
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