package com.ericversteeg.liquidocean.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.ericversteeg.liquidocean.databinding.FragmentCanvasExportBinding
import com.ericversteeg.liquidocean.helper.AppDataExporter
import com.ericversteeg.liquidocean.listener.FragmentListener
import com.ericversteeg.liquidocean.model.SessionSettings
import com.ericversteeg.liquidocean.view.ActionButtonView

class CanvasExportFragment: Fragment() {

    private var _binding: FragmentCanvasExportBinding? = null
    private val binding get() = _binding!!

    var fragmentListener: FragmentListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCanvasExportBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.backButtonCanvasExport.actionBtnView = binding.backActionCanvasExport
        binding.backActionCanvasExport.type = ActionButtonView.Type.BACK_SOLID

        binding.backButtonCanvasExport.setOnClickListener {
            fragmentManager?.apply {
                beginTransaction().remove(this@CanvasExportFragment).commit()
                fragmentListener?.onFragmentRemoved()
            }
        }

        binding.canvasExportButton.setOnClickListener {
            val emailStr = binding.emailInput.text.trim()
            if (emailStr.length < 50) {
                if (emailStr.matches(Regex("^[\\d\\w]+@[\\d\\w]+\\.[\\w]+$"))) {
                    context?.apply {
                        val sp = SessionSettings.instance.getSharedPrefs(this)
                        if (sp.contains("arr_canvas")) {
                            AppDataExporter.export(this, emailStr.toString())
                        }
                        else {
                            showStatusText("No canvas to export.")
                        }
                    }
                }
                else {
                    showStatusText("Not an email address.")
                }
            }
            else {
                showStatusText("Not an email address.")
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
}