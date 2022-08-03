package com.ericversteeg.liquidocean.view

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.core.view.children
import androidx.core.widget.ImageViewCompat

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
            setTint(baseColor)
        }

    private var baseColor = Color.parseColor("#FFFFFF")
    private val highlightColor = Color.parseColor("#FAD452")

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context): super(context) {

    }

    constructor(context: Context, attributeSet: AttributeSet): super(context, attributeSet) {

    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        setTint(baseColor)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        super.onTouchEvent(event)

        when (event.action) {
            MotionEvent.ACTION_UP -> {
                setTint(baseColor)
                //performClick()
                invalidate()
            }
            MotionEvent.ACTION_CANCEL -> {
                setTint(baseColor)
                invalidate()
            }
            MotionEvent.ACTION_DOWN -> {
                setTint(highlightColor)
            }
        }

        return true
    }

    fun getImageView(): ImageView? {
        for (child in children) {
            if (child is ImageView) {
                return child
            }
        }
        return null
    }

    fun setTint(color: Int) {
        ImageViewCompat.setImageTintList(getImageView()!!, ColorStateList.valueOf(color))
    }
}