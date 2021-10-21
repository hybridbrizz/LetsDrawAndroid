package com.ericversteeg.liquidocean.listener

import com.ericversteeg.liquidocean.model.Palette

interface DrawFrameConfigFragmentListener {
    fun createDrawFrame(centerX: Int, centerY: Int, width: Int, height: Int, color: Int)
}