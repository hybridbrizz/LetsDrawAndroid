package com.ericversteeg.radiofrost.view

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.view.children
import androidx.core.widget.ImageViewCompat
import com.ericversteeg.radiofrost.helper.Utils

class ButtonFrame: FrameLayout {
    enum class ToggleState {
        NONE,
        SINGLE,
        DOUBLE
    }

    var toggleState = ToggleState.NONE

    var isLight = true
        set(value) {
            field = value

            baseColor = if (value) {
                Color.parseColor("#FFFFFF")
            } else {
                Color.parseColor("#000000")
            }
            select(false)
        }

    private var baseColor = Color.parseColor("#DDFFFFFF")
    private val highlightColor = Color.parseColor("#FAD452")

    var color: Int? = null
        set(value) {
            field = value
            select(false)
        }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context): super(context) {

    }

    constructor(context: Context, attributeSet: AttributeSet): super(context, attributeSet) {

    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        select(false)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        super.onTouchEvent(event)

        when (event.action) {
            MotionEvent.ACTION_UP -> {
                select(false)
                //performClick()
                invalidate()
            }
            MotionEvent.ACTION_CANCEL -> {
                select(false)
                invalidate()
            }
            MotionEvent.ACTION_DOWN -> {
                select(true)
            }
        }

        return true
    }

    private fun getImageView(): ImageView? {
        for (child in children) {
            if (child is ImageView) {
                return child
            }
        }
        return null
    }

    private fun getTextView(): TextView? {
        for (child in children) {
            if (child is TextView) {
                return child
            }
        }
        return null
    }

    fun select(selected: Boolean) {
        if (color != null) {
            if (selected) {
                setTint(Utils.brightenColor(color!!, 0.15F))
            }
            else {
                setTint(color!!)
            }
        }
        else {
            if (selected) {
                setTint(highlightColor)
            }
            else {
                setTint(baseColor)
            }
        }
    }

    private fun setTint(color: Int) {
        val imageView = getImageView()
        val textView = getTextView()

        if (imageView != null) {
            ImageViewCompat.setImageTintList(getImageView()!!, ColorStateList.valueOf(color))
        }
        else if (textView != null) {
            textView.setTextColor(color)
        }
    }
}