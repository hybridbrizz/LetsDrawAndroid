package com.matrixwarez.pt.view

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import com.matrixwarez.pt.listener.LongPressListener
import java.util.*

// optional frame to the add around an action button view to make a larger touch target for clicks
// make sure to set the onClickListener for this class instead of the action button view
class ActionButtonFrame: FrameLayout {

    var actionBtnView: ActionButtonView? = null

    var clickListener: OnClickListener? = null

    var longPressListener: LongPressListener? = null
    private var longPressDuration: Long = 2000
    private var longPressTimer: Timer? = null

    var cancelClick = false

    lateinit var activity: Activity

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

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            cancelClick = false

            if (longPressListener != null) {
                setLongPressTimer()
            }

            actionBtnView?.touchState = ActionButtonView.TouchState.ACTIVE
        }
        else if (ev.action == MotionEvent.ACTION_UP) {
            actionBtnView?.touchState = ActionButtonView.TouchState.INACTIVE

            if (!cancelClick) {
                if (longPressListener != null) {
                    longPressTimer?.cancel()
                }

                clickListener?.onClick(this)
            }
        }
        else if (ev.action != MotionEvent.ACTION_MOVE) {
            if (longPressListener != null) {
                longPressTimer?.cancel()
            }

            actionBtnView?.touchState = ActionButtonView.TouchState.INACTIVE
        }

        return actionBtnView != null
    }

    override fun setOnClickListener(listener: OnClickListener?) {
        clickListener = listener
    }

    fun setLongPressActionListener(activity: Activity, listener: LongPressListener?) {
        this.activity = activity
        longPressListener = listener
    }

    private fun setLongPressTimer() {
        longPressTimer = Timer()
        longPressTimer?.schedule(object: TimerTask() {
            override fun run() {
                activity.runOnUiThread {
                    longPressListener?.onLongPress()
                    cancelClick = true
                }
            }

        }, longPressDuration)
    }
}