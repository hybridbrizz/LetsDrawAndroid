package com.matrixwarez.pt.fragment

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.children
import androidx.fragment.app.Fragment
import com.matrixwarez.pt.R
import com.matrixwarez.pt.helper.Animator
import com.matrixwarez.pt.helper.Utils
import com.matrixwarez.pt.listener.HowtoFragmentListener
import com.matrixwarez.pt.model.SessionSettings
import com.matrixwarez.pt.view.ActionButtonView
import kotlinx.android.synthetic.main.fragment_howto.*
import java.util.*

class HowtoFragment: Fragment() {

    var listener: HowtoFragmentListener? = null

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

        val stepTextViews = listOf(step1_text, step2_text, step3_text, step4_text, step5_text, step6_text,
                                    step7_text, step8_text, step9_text, step10_text)

        view.viewTreeObserver.addOnGlobalLayoutListener {
            for (textView in stepTextViews) {
                textView.measure(
                    View.MeasureSpec.makeMeasureSpec(textView.width, View.MeasureSpec.AT_MOST),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                )
                textView.layoutParams = (textView.layoutParams as ConstraintLayout.LayoutParams).also {
                    it.height = textView.measuredHeight
                }
            }
        }

        howto_frame_action.isStatic = true
        howto_export_move_action.isStatic = true

        howto_frame_action.type = ActionButtonView.Type.FRAME
        howto_export_move_action.type = ActionButtonView.Type.EXPORT

        val colorStrs = arrayOf("#000000", "#222034", "#45283C", "#663931", "#8F563B", "#DF7126", "#D9A066", "#EEC39A",
                                "#FBF236", "#99E550", "#6ABE30", "#37946E", "#4B692F", "#524B24", "#323C39", "#3F3F74")

        var i = 0
        for (v in recent_colors_container.children) {
            (v as ActionButtonView).type = ActionButtonView.Type.RECENT_COLOR
            v.representingColor = Color.parseColor(colorStrs[i])
            v.semiGloss = true
            v.isStatic = true

            i++
        }

        back_button.setOnClickListener {
            listener?.onHowtoBack()
        }

        if (!SessionSettings.instance.tablet) {
            Animator.animateTitleFromTop(title_text)
            Animator.animateTitleFromTop(back_button)

            Animator.animateHorizontalViewEnter(step1_text, true)
            Animator.animateHorizontalViewEnter(paint_action, true)

            Animator.animateHorizontalViewEnter(step2_text, false)
            Animator.animateHorizontalViewEnter(share_action, false)

            //Animator.animateHorizontalViewEnter(static_image_1, true)
        }

        //getPaintTimerInfo()

        Utils.setViewLayoutListener(view, object: Utils.ViewLayoutListener {
            override fun onViewLayout(view: View) {
                if (step1_text.height < 250) {
                    val layoutParams = step1_text.layoutParams
                    layoutParams.height = 250
                    step1_text.layoutParams = layoutParams
                }
            }
        })

        /*if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            scroll_view.setOnScrollChangeListener(object: View.OnScrollChangeListener {
                override fun onScrollChange(p0: View?, p1: Int, p2: Int, p3: Int, p4: Int) {
                    scroll_view.scrollY
                }
            })
        }*/
    }

    override fun onPause() {
        super.onPause()

        paintEventTimer?.cancel()
    }

    /*private fun getPaintTimerInfo() {
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
    }*/

    /*private fun setupPaintEventTimer() {
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
    }*/
}