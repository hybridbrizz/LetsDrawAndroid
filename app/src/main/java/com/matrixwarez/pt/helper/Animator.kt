package com.matrixwarez.pt.helper

import android.content.Context
import android.graphics.Color
import android.graphics.Point
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator

class Animator {
    interface CompletionHandler {
        fun onCompletion()
    }

    companion object {
        var context: Context? = null

        fun fadeInView(view: View, duration: Long = 1000) {
            view.alpha = 0F
            view.visibility = View.VISIBLE
            view.animate().setDuration(duration).setInterpolator(AccelerateDecelerateInterpolator()).alphaBy(1F)
        }

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

        private fun getSafePoint(view: View, parent: View, safeViews: List<View>): Point {
            context?.apply {
                val x = (Math.random() * parent.width).toInt() - view.width / 2
                val y = (Math.random() * parent.height).toInt() - view.height / 2

                val safeViewMargin = Utils.dpToPx(this, 0)

                for (safeView in safeViews) {
                    if (x > safeView.x - safeViewMargin && x < safeView.x + safeView.width + safeViewMargin &&
                        y > safeView.y - safeViewMargin && y < safeView.y + safeView.height + safeViewMargin) {
                        return getSafePoint(view, parent, safeViews)
                    }
                }

                return Point(x, y)
            }

            return Point(0, 0)
        }

        fun animatePixelColorEffect(view: View, parent: View, safeViews: List<View>) {
            context?.apply {

                val point = getSafePoint(view, parent, safeViews)
                val x = point.x
                val y = point.y

                view.x = x - view.width / 2F
                view.y = y - view.height / 2F
                view.alpha = 0F

                val rA = Math.random() / 5

                /*if (Math.random() < 0.15) {
                    rA = 1.0
                }*/

                val rR = (Math.random() * 256).toInt()
                val rG = (Math.random() * 256).toInt()
                val rB = (Math.random() * 256).toInt()

                val rD = (Math.random() * 1500).toInt() + 250L
                val rS = (Math.random() * 1500).toInt() + 250L

                view.setBackgroundColor(Color.argb(255, rR, rG, rB))
                view.animate().alphaBy(rA.toFloat()).setDuration(rD).withEndAction {
                    view.animate().setStartDelay(rS).alphaBy(-rA.toFloat()).setDuration(rD).withEndAction {
                        if (context != null) {
                            animatePixelColorEffect(view, parent, safeViews)
                        }
                    }
                }

                view.animate()
            }

        }

        fun animateMenuItems(views: List<List<View>>, cascade: Boolean, out: Boolean, inverse: Boolean, completion: CompletionHandler?) {
            val delays = intArrayOf(0, 50, 80, 100)
            if (!out) {
                var i = 0
                for (viewLayers in views) {
                    for (layer in viewLayers) {
                        if (inverse) {
                            layer.x -= 500
                        }
                        else {
                            layer.x += 500
                        }
                        layer.alpha = 0F

                        var tX = -500F
                        if (inverse) {
                            tX *= -1
                        }

                        if (cascade) {
                            layer.animate().setStartDelay(delays[i].toLong()).setDuration(150).alphaBy(1F).translationXBy(tX).setInterpolator(AccelerateDecelerateInterpolator()).withEndAction {
                                completion?.onCompletion()
                            }
                        }
                        else {
                            layer.animate().setDuration(150).alphaBy(1F).translationXBy(tX).setInterpolator(AccelerateDecelerateInterpolator()).withEndAction {
                                completion?.onCompletion()
                            }
                        }
                    }
                    i++
                }
            }
            else {
                var i = 0
                for (viewLayers in views) {
                    for (layer in viewLayers) {
                        var tX = 500F
                        if (inverse) {
                            tX *= -1
                        }
                        if (cascade) {
                            layer.animate().setStartDelay(delays[i].toLong()).setDuration(150).alphaBy(-1F).translationXBy(tX).setInterpolator(AccelerateDecelerateInterpolator()).withEndAction {
                                if (inverse) {
                                    layer.x += 500
                                }
                                else {
                                    layer.x -= 500
                                }

                                layer.visibility = View.INVISIBLE

                                completion?.onCompletion()
                            }
                        }
                        else {
                            layer.animate().setDuration(150).alphaBy(-1F).translationXBy(tX).setInterpolator(AccelerateDecelerateInterpolator()).withEndAction {
                                if (inverse) {
                                    layer.x += 500
                                }
                                else {
                                    layer.x -= 500
                                }

                                layer.visibility = View.INVISIBLE

                                completion?.onCompletion()
                            }
                        }
                    }
                    i++
                }
            }
        }
    }
}