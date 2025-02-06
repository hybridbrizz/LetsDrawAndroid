package com.ericversteeg.liquidocean.fragment

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.fragment.app.Fragment
import com.ericversteeg.liquidocean.databinding.FragmentHowtoBinding
import com.ericversteeg.liquidocean.helper.Animator
import com.ericversteeg.liquidocean.helper.Utils
import com.ericversteeg.liquidocean.listener.HowtoFragmentListener
import com.ericversteeg.liquidocean.model.SessionSettings
import com.ericversteeg.liquidocean.view.ActionButtonView
import java.util.Timer

class HowtoFragment: Fragment() {

    var listener: HowtoFragmentListener? = null

    var paintEventTimer: Timer? = null
    
    private var _binding: FragmentHowtoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHowtoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.backButton.actionBtnView = binding.backAction
        binding.backAction.type = ActionButtonView.Type.BACK_SOLID

        binding.paintAction.isStatic = true
        binding.shareAction.isStatic = true
        binding.howtoBackgroundAction.isStatic = true
        binding.howtoGridLineAction.isStatic = true
        binding.howtoSummaryAction.isStatic = true
        binding.howtoDotAction1.isStatic = true
        binding.howtoDotAction2.isStatic = true
        binding.howtoDotAction3.isStatic = true
        binding.howtoFrameAction.isStatic = true
        binding.howtoExportMoveAction.isStatic = true

        binding.paintAction.type = ActionButtonView.Type.PAINT
        binding.shareAction.type = ActionButtonView.Type.EXPORT
        binding.howtoBackgroundAction.type = ActionButtonView.Type.CHANGE_BACKGROUND
        binding.howtoGridLineAction.type = ActionButtonView.Type.GRID_LINES
        binding.howtoSummaryAction.type = ActionButtonView.Type.CANVAS_SUMMARY
        binding.howtoDotAction1.type = ActionButtonView.Type.DOT
        binding.howtoDotAction2.type = ActionButtonView.Type.DOT
        binding.howtoDotAction3.type = ActionButtonView.Type.DOT
        binding.howtoFrameAction.type = ActionButtonView.Type.FRAME
        binding.howtoExportMoveAction.type = ActionButtonView.Type.EXPORT

        val colorStrs = arrayOf("#000000", "#222034", "#45283C", "#663931", "#8F563B", "#DF7126", "#D9A066", "#EEC39A",
                                "#FBF236", "#99E550", "#6ABE30", "#37946E", "#4B692F", "#524B24", "#323C39", "#3F3F74")

        var i = 0
        for (v in binding.recentColorsContainer.children) {
            (v as ActionButtonView).type = ActionButtonView.Type.RECENT_COLOR
            v.representingColor = Color.parseColor(colorStrs[i])
            v.semiGloss = true
            v.isStatic = true

            i++
        }

        binding.backButton.setOnClickListener {
            listener?.onHowtoBack()
        }

        if (!SessionSettings.instance.tablet) {
            Animator.animateTitleFromTop(binding.titleText)

            Animator.animateHorizontalViewEnter(binding.step1Text, true)
            Animator.animateHorizontalViewEnter(binding.paintAction, true)

            Animator.animateHorizontalViewEnter(binding.step2Text, false)
            Animator.animateHorizontalViewEnter(binding.shareAction, false)

            //Animator.animateHorizontalViewEnter(static_image_1, true)
        }

        //getPaintTimerInfo()

        Utils.setViewLayoutListener(view, object: Utils.ViewLayoutListener {
            override fun onViewLayout(view: View) {
                if (binding.step1Text.height < 250) {
                    val layoutParams = binding.step1Text.layoutParams
                    layoutParams.height = 250
                    binding.step1Text.layoutParams = layoutParams
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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