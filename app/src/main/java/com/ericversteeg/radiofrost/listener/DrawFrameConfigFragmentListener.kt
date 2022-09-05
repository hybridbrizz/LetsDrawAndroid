package com.ericversteeg.radiofrost.listener

interface DrawFrameConfigFragmentListener {
    fun createDrawFrame(centerX: Int, centerY: Int, width: Int, height: Int, color: Int)
}