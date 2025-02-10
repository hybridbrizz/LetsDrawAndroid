package com.matrixwarez.pt.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.annotation.RequiresApi
import com.matrixwarez.pt.model.InteractiveCanvas
import com.matrixwarez.pt.model.SessionSettings

class InteractiveCanvasSummaryView: View {

    var interactiveCanvas: InteractiveCanvas? = null
    set(value) {
        field = value
        invalidate()
    }

    var drawBackground: Boolean = true

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

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        drawToCanvas(canvas)
    }

    private fun drawToCanvas(canvas: Canvas) {
        canvas.apply {
            save()

            val gridPpu = 20F

            val paint = Paint()

            val widthUnits = width / gridPpu.toInt() + 1
            val heightUnits = height / gridPpu.toInt() + 1

            val whitePaint =  ActionButtonView.whitePaint
            val grayPaint = ActionButtonView.photoshopGray
            val blackPaint = Paint()

            blackPaint.color = Color.BLACK

            // draw transparency background
            if (drawBackground) {
                for (x in 0 until widthUnits) {
                    for (y in 0 until heightUnits) {
                        if ((x + y) % 2 == 0) {
                            canvas.drawRect(
                                x * gridPpu,
                                y * gridPpu,
                                (x + 1) * gridPpu,
                                (y + 1) * gridPpu,
                                whitePaint
                            )
                        }
                        else {
                            canvas.drawRect(
                                x * gridPpu,
                                y * gridPpu,
                                (x + 1) * gridPpu,
                                (y + 1) * gridPpu,
                                grayPaint
                            )
                        }
                    }
                }
            }
            else {
                if (SessionSettings.instance.backgroundColorsIndex == InteractiveCanvas.BACKGROUND_BLACK) {
                    paint.color = Color.parseColor("#FF222222")
                }
                else {
                    paint.color = Color.BLACK
                }

                drawRect(0F, 0F, width.toFloat(), height.toFloat(), paint)
            }

            // draw interactive canvas summary
            interactiveCanvas?.apply {
                val displayPpu = width.toFloat() / cols

//                for (y in 0 until rows) {
//                    for (x in 0 until cols) {
//                        val color = arr[y][x]
//                        paint.color = color
//
//                        if (paint.color == 0) {
//                            paint.color = Color.BLACK
//                        }
//
//                        val left = floor(x * displayPpu)
//                        val top = floor(y * displayPpu)
//                        val right = left + 1
//                        val bottom = top + 1
//
//                        canvas.drawRect(
//                            left,
//                            top,
//                            right,
//                            bottom, paint
//                        )
//                    }
//                }

//                for (pixel in summary) {
//                    paint.color = pixel.color
//
//                    if (paint.color == 0) {
//                        paint.color = Color.BLACK
//                    }
//
//                    val left = floor(pixel.point.x * displayPpu)
//                    val top = floor(pixel.point.y * displayPpu)
//                    val right = left + 1
//                    val bottom = top + 1
//
//                    canvas.drawRect(
//                        left,
//                        top,
//                        right,
//                        bottom, paint
//                    )
//                }
            }

            /*val borderPaint = Paint()
            borderPaint.color = Color.WHITE
            borderPaint.strokeWidth = Utils.dpToPxF(context, 1)

            canvas.drawLine(0F, 0F, width.toFloat(), 0F, borderPaint)
            canvas.drawLine(0F, 0F, 0F, height.toFloat(), borderPaint)
            canvas.drawLine(width.toFloat(), 0F, width.toFloat(), height.toFloat(), borderPaint)
            canvas.drawLine(0F, height.toFloat(), width.toFloat(), height.toFloat(), borderPaint)*/

            restore()
        }
    }
}