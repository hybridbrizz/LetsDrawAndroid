package com.ericversteeg.liquidocean.view

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import androidx.annotation.RequiresApi

// optional frame to the add around an action button view to make a larger touch target for clicks
// make sure to set the onClickListener for this class instead of the action button view
class ActionButtonFrame: FrameLayout {

    var actionBtnView: ActionButtonView? = null

    var clickListener: OnClickListener? = null

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
            actionBtnView?.touchState = ActionButtonView.TouchState.ACTIVE
        }
        else if (ev.action == MotionEvent.ACTION_UP) {
            actionBtnView?.touchState = ActionButtonView.TouchState.INACTIVE
            clickListener?.onClick(this)
        }

        return actionBtnView != null
    }

    override fun setOnClickListener(listener: OnClickListener?) {
        clickListener = listener
    }
}