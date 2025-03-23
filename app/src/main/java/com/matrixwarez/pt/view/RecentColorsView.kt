package com.matrixwarez.pt.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.annotation.RequiresApi
import kotlin.math.floor


class RecentColorsView: View {

    interface Listener {
        fun onSelectRecentColor(color: Int)
    }

    var listener: Listener? = null

    var recentColors = mutableListOf<Int>()
        set(value) {
            when (sameRecentColors(field, value)) {
                true -> {
                    field = value
                    invalidate()
                }
                false -> {
                    snapshotOldState()

                    field = value

                    snapshotNewState()
                    transitionToNewState()
                }
            }
        }

    var rows = 2
    var cols = 8

    private var itemWidth = 0

    private var oldStateBitmap: Bitmap? = null
    private var newStateBitmap: Bitmap? = null
    private var transitionProgress = 0f
    private var isTransitioning = false
    private var alphaPaint = Paint()

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
        alphaPaint = Paint()
        alphaPaint.isAntiAlias = true
    }

    fun snapshotOldState() {
        if (width == 0 || height == 0) return

        // Capture the current state
        oldStateBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(oldStateBitmap!!)
        // Draw the current state but skip any transition logic
        drawToCanvas(canvas)
    }

    fun snapshotNewState() {
        if (width == 0 || height == 0) return

        // Draw the new state to a bitmap
        newStateBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(newStateBitmap!!)
        drawToCanvas(canvas)
    }

    @SuppressLint("WrongCall")
    fun transitionToNewState() {
        if (width == 0 || height == 0) return

        // Start animation
        isTransitioning = true
        transitionProgress = 0f

        Log.d("Recent Colors View", "Transition to new state")

        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.setDuration(300) // Adjust duration as needed
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.addUpdateListener { animation: ValueAnimator ->
            transitionProgress = animation.animatedValue as Float
            invalidate()
        }
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                isTransitioning = false
                oldStateBitmap = null
                newStateBitmap = null
                invalidate()
            }
        })
        animator.start()
    }

    @SuppressLint("WrongCall")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // By Claude
        if (!isTransitioning) {
            drawToCanvas(canvas)
            return
        }

        // Draw the old state solid
        alphaPaint.alpha = 255
        canvas.drawBitmap(oldStateBitmap!!, 0f, 0f, alphaPaint)

        // Draw the new state with increasing alpha
        val newStateOpacity = (transitionProgress * 255).toInt()
        alphaPaint.alpha = newStateOpacity
        canvas.drawBitmap(newStateBitmap!!, 0f, 0f, alphaPaint)
    }

    private fun drawToCanvas(canvas: Canvas) {
        itemWidth = width / cols

        val paint = Paint()

        for (i in 0 until rows) {
            for (j in 0 until cols) {
                val x = itemWidth * (cols - 1 - j)
                val y = itemWidth * i

                val index = i * cols + j

                paint.color = recentColors[index]
                canvas.drawRect(
                    Rect(x, y, x + itemWidth, y + itemWidth),
                    paint
                )
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
            val x = cols - 1 - floor(event.x / itemWidth).toInt()
            val y = floor(event.y / itemWidth).toInt()

            val index = y * cols + x
            if (index in recentColors.indices) {
                listener?.onSelectRecentColor(recentColors[index])
                return true
            }

        }
        return false
    }

    private fun sameRecentColors(list1: List<Int>, list2: List<Int>): Boolean {
        if (list1.size != list2.size) return false

        for (i in list1.indices) {
            val c1 = list1[i]
            val c2 = list2[i]

            if (c1 != c2) {
                return false
            }
        }

        return true
    }
}