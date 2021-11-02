package com.ericversteeg.liquidocean.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.ericversteeg.liquidocean.R
import com.ericversteeg.liquidocean.helper.AppDataExporter
import com.ericversteeg.liquidocean.listener.FragmentListener
import com.ericversteeg.liquidocean.model.SessionSettings
import com.ericversteeg.liquidocean.view.ActionButtonView
import kotlinx.android.synthetic.main.fragment_canvas_export.*


class CanvasExportFragment: Fragment() {

    var fragmentListener: FragmentListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_canvas_export, container, false)

        // setup views here

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        back_button_canvas_export.actionBtnView = back_action_canvas_export
        back_action_canvas_export.type = ActionButtonView.Type.BACK_SOLID

        back_button_canvas_export.setOnClickListener {
            fragmentManager?.apply {
                beginTransaction().remove(this@CanvasExportFragment).commit()
                fragmentListener?.onFragmentRemoved()
            }
        }

        canvas_export_button.setOnClickListener {
            val emailStr = email_input.text.trim()
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

    private fun showStatusText(text: String, color: Int = ActionButtonView.yellowPaint.color) {
        status_text.visibility = View.VISIBLE
        status_text.setTextColor(color)
        status_text.text = text
    }
}