package com.ericversteeg.liquidocean.fragment

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.ericversteeg.liquidocean.R
import com.ericversteeg.liquidocean.helper.PanelThemeConfig
import com.ericversteeg.liquidocean.helper.Utils
import com.ericversteeg.liquidocean.listener.DrawFrameConfigFragmentListener
import com.ericversteeg.liquidocean.model.SessionSettings
import kotlinx.android.synthetic.main.fragment_draw_frame_config.*


class DrawFrameConfigFragment: Fragment() {

    var drawFrameConfigFragmentListener: DrawFrameConfigFragmentListener? = null

    var centerX = 0
    var centerY = 0

    lateinit var panelThemeConfig: PanelThemeConfig

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

        if (panelThemeConfig.actionButtonColor == Color.BLACK) {
            title_text.setTextColor(Color.parseColor("#FF111111"))
            title_text.setShadowLayer(3F, 2F, 2F, Color.parseColor("#7F333333"))

            width_title_text.setTextColor(Color.BLACK)
            width_input.setTextColor(Color.BLACK)
            width_input.setHintTextColor(Color.parseColor("#99000000"))

            height_title_text.setTextColor(Color.BLACK)
            height_input.setTextColor(Color.BLACK)
            height_input.setHintTextColor(Color.parseColor("#99000000"))
        }

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

        Utils.setViewLayoutListener(view, object: Utils.ViewLayoutListener {
            override fun onViewLayout(view: View) {
                setBackground(view)
            }
        })
    }

    private fun setBackground(view: View) {
        context?.apply {
            val backgroundDrawable = ContextCompat.getDrawable(this, SessionSettings.instance.panelResIds[SessionSettings.instance.panelBackgroundResIndex]) as BitmapDrawable

            val scale = view.width / backgroundDrawable.bitmap.width.toFloat()

            val newWidth = (backgroundDrawable.bitmap.width * scale).toInt()
            val newHeight = (backgroundDrawable.bitmap.height * scale).toInt()
            val scaledBitmap = Bitmap.createScaledBitmap(backgroundDrawable.bitmap, newWidth,
                newHeight, false)
            val resizedBitmap = Bitmap.createBitmap(scaledBitmap, 0, scaledBitmap.height - view.height, view.width, view.height)
            val resizedBitmapDrawable = BitmapDrawable(resizedBitmap)

            view.setBackgroundDrawable(resizedBitmapDrawable)
        }
    }
}