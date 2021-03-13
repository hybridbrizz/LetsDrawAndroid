package com.ericversteeg.liquidocean.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.fragment.app.Fragment
import com.ericversteeg.liquidocean.R
import com.ericversteeg.liquidocean.helper.Animator
import com.ericversteeg.liquidocean.listener.StatsFragmentListener
import com.ericversteeg.liquidocean.model.StatTracker
import com.ericversteeg.liquidocean.view.ActionButtonView
import kotlinx.android.synthetic.main.fragment_howto.*
import kotlinx.android.synthetic.main.fragment_interactive_canvas.*
import kotlinx.android.synthetic.main.fragment_interactive_canvas.back_action
import kotlinx.android.synthetic.main.fragment_interactive_canvas.back_button
import kotlinx.android.synthetic.main.fragment_options.*
import kotlinx.android.synthetic.main.fragment_stats.*
import java.text.NumberFormat

class HowtoFragment: Fragment() {

    var listener: StatsFragmentListener? = null

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
        howto_image.static = true
        howto_image.representingColor = ActionButtonView.whitePaint.color

        paint_action.type = ActionButtonView.Type.PAINT
        paint_action.static = true

        static_image_2.jsonResId = R.raw.mushroom_json

        back_button.setOnClickListener {
            listener?.onStatsBack()
        }

        Animator.animateTitleFromTop(howto_image)

        Animator.animateHorizontalViewEnter(step1_text, true)
        Animator.animateHorizontalViewEnter(static_image_1, true)
    }
}