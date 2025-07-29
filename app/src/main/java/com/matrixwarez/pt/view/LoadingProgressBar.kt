package com.matrixwarez.pt.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.matrixwarez.pt.R
import kotlin.math.roundToInt

open class LoadingProgressBar: FrameLayout {

    private var progressView: View? = null

    var progress: Float = 0f
    set(value) {
        field = value
        animateProgress(value)
    }

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

    @RequiresApi(21)
    constructor(context: Context, attributeSet: AttributeSet, v0: Int, v1: Int) : super(
        context,
        attributeSet,
        v0,
        v1
    ) {
        commonInit()
    }

    private fun commonInit() {
        progressView = View(context)
        progressView!!.background = ColorDrawable(ContextCompat.getColor(context, R.color.colorAccent))
        addView(progressView, LayoutParams(0, LayoutParams.MATCH_PARENT))
        clipChildren = true
        clipToOutline = true
    }

    private fun animateProgress(newProgress: Float) {
        val oldProgress = if (width == 0) {
            0f
        }
        else {
            (progressView?.layoutParams?.width ?: 0) / width.toFloat()
        }
        val progressAnimator = ValueAnimator.ofFloat(oldProgress, newProgress)
        progressAnimator.duration = 200
        progressAnimator.addUpdateListener {
            val width = ((it.animatedValue as Float) * width).roundToInt()
            progressView?.layoutParams?.width = width
            progressView?.requestLayout()
        }
        progressAnimator.start()
    }
}