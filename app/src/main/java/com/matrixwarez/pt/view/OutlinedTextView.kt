package com.matrixwarez.pt.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

class OutlinedTextView: AppCompatTextView {

    var strokeWidth = 2F
    var strokeColor = ActionButtonView.twoThirdGray

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

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (strokeWidth > 0) {
            //set paint to fill mode
            val p: Paint = paint
            p.style = Paint.Style.FILL

            //draw the fill part of text
            super.onDraw(canvas)
            //save the text color
            val currentTextColor: Int = getCurrentTextColor()

            p.style = Paint.Style.STROKE
            p.strokeWidth = strokeWidth
            setTextColor(strokeColor.color)

            // draw text stroke
            super.onDraw(canvas)
            // revert the color back to the one initially specified
            setTextColor(currentTextColor)
        } else {
            super.onDraw(canvas)
        }
    }
}