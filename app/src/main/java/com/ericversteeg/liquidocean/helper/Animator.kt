package com.ericversteeg.liquidocean.helper

import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import kotlinx.android.synthetic.main.fragment_stats.*

class Animator {
    companion object {
        fun animateHorizontalViewEnter(view: View, left: Boolean) {
            var offset = 2000
            if (!left) {
                offset = -offset
            }
            view.x += offset
            view.animate().translationXBy(-offset.toFloat()).setInterpolator(
                AccelerateDecelerateInterpolator()
            ).setDuration(150)
        }

        fun animateTitleFromTop(titleView: View) {
            titleView.y -= 300
            titleView.animate().translationYBy(300F).setInterpolator(AccelerateDecelerateInterpolator()).setDuration(150)
        }
    }
}