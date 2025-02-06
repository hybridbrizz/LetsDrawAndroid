package com.ericversteeg.liquidocean.fragment

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.ericversteeg.liquidocean.R
import com.ericversteeg.liquidocean.databinding.FragmentCanvasImportBinding
import com.ericversteeg.liquidocean.listener.FragmentListener
import com.ericversteeg.liquidocean.model.InteractiveCanvas
import com.ericversteeg.liquidocean.view.ActionButtonView


class CanvasImportFragment: Fragment() {

    var fragmentListener: FragmentListener? = null
    
    private var _binding: FragmentCanvasImportBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCanvasImportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.backButton.actionBtnView = binding.backAction
        binding.backAction.type = ActionButtonView.Type.BACK_SOLID

        binding.backButton.setOnClickListener {
            fragmentManager?.apply {
                beginTransaction().remove(this@CanvasImportFragment).commit()
                fragmentListener?.onFragmentRemoved()
            }
        }

        binding.canvasImportButton.setOnClickListener {
            val urlStr = binding.dataUrlInput.text.trim().toString()
            if (urlStr.isEmpty()) {
                showStatusText("Please enter a Pastebin url.")
            }
            else if (urlStr.length < 50) {
                context?.apply {
                    if (urlStr.contains("/")) {
                        val tokens = urlStr.split("/")
                        if (tokens.isNotEmpty()) {
                            getCanvasData(this, tokens[tokens.size - 1])
                        }
                        else {
                            showStatusText("Could not find code in url")
                        }
                    }
                    else {
                        getCanvasData(this, urlStr)
                    }
                }
            }
            else {
                showStatusText("Not a pastebin url")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showStatusText(text: String, color: Int = ActionButtonView.yellowPaint.color) {
        binding.statusText.visibility = View.VISIBLE
        binding.statusText.setTextColor(color)
        binding.statusText.text = text
    }

    private fun importCanvasData(context: Context, jsonStr: String) {
        val success = InteractiveCanvas.importCanvasFromJson(context, jsonStr)
        if (!success) {
            showStatusText("Error reading canvas data.")
        }
        else {
            showStatusText("Canvas data imported!", color = ActionButtonView.greenPaint.color)
        }
    }

    private fun getCanvasData(context: Context, code: String) {
        val url = "https://pastebin.com/raw/$code"
        val request = StringRequest(Request.Method.GET, url,
            {
                showCanvasReplaceAlert(context, it)
            },
            {
                showStatusText("Error downloading canvas data.")
            }
        )

        Volley.newRequestQueue(context).add(request)
    }

    private fun showCanvasReplaceAlert(context: Context, jsonStr: String) {
        val alert = AlertDialog.Builder(context)

        val editText = EditText(activity)
        alert.setMessage(getString(R.string.replace_canvas_dialog_message))

        alert.setView(editText)

        alert.setPositiveButton(
            "Import"
        ) { dialog, _ ->
            if (editText.text.toString() == getString(R.string.replace_canvas_dialog_confirm_string)) {
                importCanvasData(context, jsonStr)
                dialog?.dismiss()
            }
            else {
                showStatusText("Import cancelled.")
            }
        }

        alert.setNegativeButton("Cancel") { dialog, _ ->
            showStatusText("Import cancelled.")
            dialog?.dismiss()
        }

        alert.show()
    }
}