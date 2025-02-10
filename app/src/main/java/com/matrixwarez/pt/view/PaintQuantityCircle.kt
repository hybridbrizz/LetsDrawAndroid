package com.matrixwarez.pt.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.annotation.RequiresApi
import com.matrixwarez.pt.helper.PanelThemeConfig
import com.matrixwarez.pt.listener.PaintActionListener
import com.matrixwarez.pt.listener.PaintBarActionListener
import com.matrixwarez.pt.listener.PaintQtyListener
import com.matrixwarez.pt.model.SessionSettings
import java.util.*

class PaintQuantityCircle : View, PaintQtyListener, PaintActionListener {

    lateinit var panelThemeConfig: PanelThemeConfig

    var primaryPaint = Paint()
    var backgroundPaint = Paint()
    val borderPaint = Paint()

    var flashingError = false

    var actionListener: PaintBarActionListener? = null

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

    @RequiresApi(21) constructor(context: Context, attributeSet: AttributeSet, v0: Int, v1: Int) : super(
        context,
        attributeSet,
        v0,
        v1
    ) {
        commonInit()
    }

    private fun commonInit() {
        panelThemeConfig = PanelThemeConfig.defaultLightTheme()

        if (panelThemeConfig.darkPaintQtyBar) {
            backgroundPaint.color = ActionButtonView.darkGrayPaint.color
            borderPaint.color = Color.parseColor("#99000000")
        }
        else {
            backgroundPaint.color = ActionButtonView.whitePaint.color
            borderPaint.color = Color.parseColor("#99FFFFFF")
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        primaryPaint.color = SessionSettings.instance.paintBarColor

        canvas.let {
            it.save()

            var relQty = SessionSettings.instance.dropsAmt / SessionSettings.instance.maxPaintAmt.toFloat()

            borderPaint.style = Paint.Style.STROKE
            borderPaint.strokeWidth = 15F

            /*it.drawCircle(
                width / 2F,
                height / 2F,
                width / 2 - borderPaint.strokeWidth / 2,
                borderPaint
            )*/

            canvas.clipPath(Path().apply {
                addCircle(width / 2F, width / 2F, width / 2 - borderPaint.strokeWidth, Path.Direction.CW)
            })

            if (flashingError) {
                backgroundPaint.color = ActionButtonView.redPaint.color
            }
            if (panelThemeConfig.darkPaintQtyBar) {
                backgroundPaint.color = ActionButtonView.blackPaint.color
            }
            else {
                backgroundPaint.color = ActionButtonView.whitePaint.color
            }

            canvas.drawRect(0F, 0F, width.toFloat(), height.toFloat(), backgroundPaint)

            primaryPaint.color = SessionSettings.instance.paintBarColor
            canvas.drawRect(0F, (1 - relQty) * height, width.toFloat(), height.toFloat(), primaryPaint)

            it.restore()
        }
    }

    private fun flashError() {
        flashingError = true
        invalidate()
        Timer().schedule(object: TimerTask() {
            override fun run() {
                flashingError = false
                invalidate()
            }

        }, 1500)
    }

    override fun paintQtyChanged(qty: Int) {
        invalidate()
    }

    override fun onPaintStart() {
        if (SessionSettings.instance.dropsAmt == 0) {
            flashError()
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event?.action == MotionEvent.ACTION_DOWN) {
            /* if (firstClickTime == 0L) {
                firstClickTime = System.currentTimeMillis()
            }
            else {
                if (System.currentTimeMillis() - firstClickTime < 500L) {
                    actionListener?.onPaintBarDoubleTapped()
                }

                firstClickTime = 0L
            } */

            actionListener?.onPaintBarDoubleTapped()
        }

        return super.onTouchEvent(event)
    }
}