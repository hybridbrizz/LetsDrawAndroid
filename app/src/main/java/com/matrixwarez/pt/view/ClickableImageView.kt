package com.matrixwarez.pt.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

class ClickableImageView: AppCompatImageView {

    var oldDrawable: Drawable? = null

    constructor(context: Context) : super(context) {
        commonInit()
    }

    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet) {
        commonInit()
    }

    constructor(context: Context, attributeSet: AttributeSet, v0: Int) : super(
        context,
        attributeSet,
        v0
    ) {
        commonInit()
    }

    private fun commonInit() {

    }

    /*override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            oldDrawable = drawable
            setBackgroundColor(ActionButtonView.altGreenPaint.color)
        }
        else if (ev.action == MotionEvent.ACTION_UP || ev.action == MotionEvent.ACTION_CANCEL) {
            setBackgroundDrawable(oldDrawable)
            setBackgroundColor(Color.TRANSPARENT)
        }

        return true
    }*/
}