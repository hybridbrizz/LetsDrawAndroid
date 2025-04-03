package com.matrixwarez.pt.view

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ColorPanelTouchTargetView: FrameLayout {

    private val coroutineScope = CoroutineScope(Dispatchers.Main.immediate)

    private var pressDownTime = 0L

    private var name = ""
    private var onPress: (() -> Unit)? = null

    private var onLongPressJob: Job? = null

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

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            pressDownTime = System.currentTimeMillis()
            onLongPressJob = coroutineScope.launch {
                withContext(Dispatchers.Default) {
                    delay(750)
                }
                onLongPress()
            }
        }
        else if (event.action == MotionEvent.ACTION_UP) {
            val duration = System.currentTimeMillis() - pressDownTime
            if (duration <= 750) {
                performClick()
                onPress?.invoke()
                onLongPressJob?.cancel()
                onLongPressJob = null
            }
        }
        return true
    }

    private fun onLongPress() {
        Toast.makeText(context, name, Toast.LENGTH_SHORT).show()
    }

    fun bindIconData(name: String, onPress: () -> Unit) {
        this.name = name
        this.onPress = onPress
    }
}