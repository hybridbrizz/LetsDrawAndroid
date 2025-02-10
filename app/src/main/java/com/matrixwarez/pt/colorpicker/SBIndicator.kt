package com.matrixwarez.pt.colorpicker

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.matrixwarez.pt.helper.Utils

class SBIndicator: View {

    var paint = Paint()

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
        paint.color = Color.parseColor("#CCFFFFFF")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = Utils.dpToPx(context, 3).toFloat()
        paint.isAntiAlias = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.apply {
            save()

            drawIndicator(canvas)

            restore()
        }
    }

    private fun drawIndicator(canvas: Canvas) {
        canvas.drawCircle(width.toFloat() / 2, height.toFloat() / 2, width.toFloat() / 2 - Utils.dpToPx(context, 3), paint)
    }
}