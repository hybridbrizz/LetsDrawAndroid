package com.matrixwarez.pt.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import androidx.annotation.RequiresApi
import com.matrixwarez.pt.helper.Utils

class AutoCompleteView: View {

    var autoCompletedString: String? = null
    lateinit var prefixString: String
    lateinit var textPaint: Paint

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

    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas?.apply {
            save()

            autoCompletedString?.apply {
                if (prefixString.length < length) {
                    val toCompleteString = substring(prefixString.length)

                    val toCompleteBounds = Rect()
                    textPaint.getTextBounds(autoCompletedString, prefixString.length, length, toCompleteBounds)

                    val prefixBounds = Rect()
                    textPaint.getTextBounds(prefixString, 0, prefixString.length, prefixBounds)

                    val color = textPaint.color

                    textPaint.color = Color.parseColor("#333333")
                    canvas.drawText(toCompleteString, prefixBounds.width().toFloat() + Utils.dpToPx(context, 4) + toCompleteBounds.left, height.toFloat() - Utils.dpToPx(context, 19), textPaint)

                    textPaint.color = color
                }
            }

            restore()
        }
    }
}