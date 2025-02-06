package com.ericversteeg.liquidocean.fragment

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.ericversteeg.liquidocean.databinding.FragmentDrawFrameConfigBinding
import com.ericversteeg.liquidocean.helper.PanelThemeConfig
import com.ericversteeg.liquidocean.helper.Utils
import com.ericversteeg.liquidocean.listener.DrawFrameConfigFragmentListener
import com.ericversteeg.liquidocean.model.SessionSettings


class DrawFrameConfigFragment: Fragment() {

    var drawFrameConfigFragmentListener: DrawFrameConfigFragmentListener? = null

    var centerX = 0
    var centerY = 0

    lateinit var panelThemeConfig: PanelThemeConfig
    
    private var _binding: FragmentDrawFrameConfigBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentDrawFrameConfigBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (panelThemeConfig.actionButtonColor == Color.BLACK) {
            binding.titleText.setTextColor(Color.parseColor("#FF111111"))
            binding.titleText.setShadowLayer(3F, 2F, 2F, Color.parseColor("#7F333333"))

            binding.widthTitleText.setTextColor(Color.BLACK)
            binding.widthInput.setTextColor(Color.BLACK)
            binding.widthInput.setHintTextColor(Color.parseColor("#99000000"))

            binding.heightTitleText.setTextColor(Color.BLACK)
            binding.heightInput.setTextColor(Color.BLACK)
            binding.heightInput.setHintTextColor(Color.parseColor("#99000000"))
        }

        if (SessionSettings.instance.lastDrawFrameWidth > 0) {
            binding.widthInput.setText(SessionSettings.instance.lastDrawFrameWidth.toString())
            binding.heightInput.setText(SessionSettings.instance.lastDrawFrameHeight.toString())
        }

        binding.doneButton.setOnClickListener {
            if (binding.widthInput.text.toString().matches(Regex("\\d{1,3}")) &&
                binding.heightInput.text.toString().matches(Regex("\\d{1,3}"))) {

                val width = binding.widthInput.text.toString().toInt()
                val height = binding.heightInput.text.toString().toInt()

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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