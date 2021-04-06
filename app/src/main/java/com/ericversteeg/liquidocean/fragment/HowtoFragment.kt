package com.ericversteeg.liquidocean.fragment

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.ericversteeg.liquidocean.R
import com.ericversteeg.liquidocean.helper.Animator
import com.ericversteeg.liquidocean.helper.Utils
import com.ericversteeg.liquidocean.listener.StatsFragmentListener
import com.ericversteeg.liquidocean.model.SessionSettings
import com.ericversteeg.liquidocean.model.StatTracker
import com.ericversteeg.liquidocean.view.ActionButtonView
import kotlinx.android.synthetic.main.fragment_howto.*
import kotlinx.android.synthetic.main.fragment_interactive_canvas.*
import kotlinx.android.synthetic.main.fragment_interactive_canvas.back_action
import kotlinx.android.synthetic.main.fragment_interactive_canvas.back_button
import kotlinx.android.synthetic.main.fragment_options.*
import kotlinx.android.synthetic.main.fragment_stats.*
import java.text.NumberFormat
import java.util.*

class HowtoFragment: Fragment() {

    var listener: StatsFragmentListener? = null

    var paintEventTimer: Timer? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_howto, container, false)

        // setup views here

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        back_button.actionBtnView = back_action
        back_action.type = ActionButtonView.Type.BACK_SOLID

        howto_image.type = ActionButtonView.Type.HOWTO
        howto_image.isStatic = true
        howto_image.representingColor = ActionButtonView.whitePaint.color

        paint_action.type = ActionButtonView.Type.PAINT
        paint_action.isStatic = true

        static_image_2.jsonResId = R.raw.hfs_json

        back_button.setOnClickListener {
            listener?.onStatsBack()
        }

        if (!SessionSettings.instance.tablet) {
            Animator.animateTitleFromTop(howto_image)

            Animator.animateHorizontalViewEnter(step1_text, true)
            Animator.animateHorizontalViewEnter(static_image_1, true)
        }

        getPaintTimerInfo()

        paint_qty_bar_howto.setOnClickListener {
            if (paint_time_info_howto_container.visibility == View.INVISIBLE) {
                paint_time_info_howto_container.visibility = View.VISIBLE
            }
            else {
                paint_time_info_howto_container.visibility = View.INVISIBLE
            }
        }

        Utils.setViewLayoutListener(view, object: Utils.ViewLayoutListener {
            override fun onViewLayout(view: View) {
                if (step1_text.height < 250) {
                    val layoutParams = step1_text.layoutParams
                    layoutParams.height = 250
                    step1_text.layoutParams = layoutParams
                }
            }
        })
    }

    override fun onPause() {
        super.onPause()

        paintEventTimer?.cancel()
    }

    private fun getPaintTimerInfo() {
        val requestQueue = Volley.newRequestQueue(context)

        val request = object: JsonObjectRequest(
            Request.Method.GET,
            Utils.baseUrlApi + "/api/v1/paint/time/sync",
            null,
            { response ->
                (context as Activity?)?.runOnUiThread {
                    val timeUntil = response.getInt("s").toLong()

                    if (timeUntil < 0) {
                        paint_time_info_howto.text = "???"
                    } else {
                        SessionSettings.instance.timeSync = timeUntil
                        setupPaintEventTimer()
                    }
                }
            },
            { error ->
                (context as Activity?)?.runOnUiThread {

                }
            }) {

            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = "application/json; charset=utf-8"
                headers["key1"] = Utils.key1
                return headers
            }
        }

        requestQueue.add(request)
    }

    private fun setupPaintEventTimer() {
        paintEventTimer = Timer()
        paintEventTimer?.schedule(object : TimerTask() {
            override fun run() {
                activity?.runOnUiThread {
                    if (System.currentTimeMillis() > SessionSettings.instance.nextPaintTime) {
                        SessionSettings.instance.nextPaintTime =
                            System.currentTimeMillis() + 300 * 1000
                    }

                    val m =
                        (SessionSettings.instance.nextPaintTime - System.currentTimeMillis()) / 1000 / 60
                    val s =
                        ((SessionSettings.instance.nextPaintTime - System.currentTimeMillis()) / 1000) % 60

                    if (m == 0L) {
                        try {
                            paint_time_info_howto.text = s.toString()
                        } catch (ex: IllegalStateException) {

                        }
                    } else {
                        try {
                            paint_time_info_howto.text = String.format("%02d:%02d", m, s)
                        } catch (ex: IllegalStateException) {

                        }
                    }
                }
            }
        }, 0, 1000)
    }
}