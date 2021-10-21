package com.ericversteeg.liquidocean.fragment

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.ericversteeg.liquidocean.R
import com.ericversteeg.liquidocean.listener.DrawFrameConfigFragmentListener
import com.ericversteeg.liquidocean.model.SessionSettings
import kotlinx.android.synthetic.main.fragment_draw_frame_config.*


class DrawFrameConfigFragment: Fragment() {

    var drawFrameConfigFragmentListener: DrawFrameConfigFragmentListener? = null

    var centerX = 0
    var centerY = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_draw_frame_config, container, false)

        // setup views here

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (SessionSettings.instance.lastDrawFrameWidth > 0) {
            width_input.setText(SessionSettings.instance.lastDrawFrameWidth.toString())
            height_input.setText(SessionSettings.instance.lastDrawFrameHeight.toString())
        }

        done_button.setOnClickListener {
            if (width_input.text.toString().matches(Regex("\\d{1,3}")) &&
                height_input.text.toString().matches(Regex("\\d{1,3}"))) {

                val width = width_input.text.toString().toInt()
                val height = height_input.text.toString().toInt()

                if (width > 0 && height > 0) {
                    SessionSettings.instance.lastDrawFrameWidth = width
                    SessionSettings.instance.lastDrawFrameHeight = height

                    drawFrameConfigFragmentListener?.createDrawFrame(centerX, centerY, width, height, SessionSettings.instance.frameColor)
                }
            }
        }
    }
}