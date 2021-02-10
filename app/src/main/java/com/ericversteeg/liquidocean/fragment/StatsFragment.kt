package com.ericversteeg.liquidocean.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.ericversteeg.liquidocean.R
import com.ericversteeg.liquidocean.listener.StatsFragmentListener
import com.ericversteeg.liquidocean.model.StatTracker
import com.ericversteeg.liquidocean.view.ActionButtonView
import kotlinx.android.synthetic.main.fragment_interactive_canvas.*
import kotlinx.android.synthetic.main.fragment_interactive_canvas.back_action
import kotlinx.android.synthetic.main.fragment_interactive_canvas.back_button
import kotlinx.android.synthetic.main.fragment_stats.*

class StatsFragment: Fragment() {

    var statsFragmentListener: StatsFragmentListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_stats, container, false)

        // setup views here

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        back_button.actionBtnView = back_action
        back_action.type = ActionButtonView.Type.BACK

        stats_image.type = ActionButtonView.Type.STATS

        back_button.setOnClickListener {
            statsFragmentListener?.onStatsBack()
        }

        stat_num_pixels_painted_single.text = StatTracker.instance.getStatValue(StatTracker.instance.numPixelsPaintedSingleKey).toString()
        stat_num_pixels_painted_world.text = StatTracker.instance.getStatValue(StatTracker.instance.numPixelsPaintedWorldKey).toString()
        stat_num_pixel_overwrites_in.text = StatTracker.instance.getStatValue(StatTracker.instance.numPixelOverwritesInKey).toString()
        stat_num_pixel_overwrites_out.text = StatTracker.instance.getStatValue(StatTracker.instance.numPixelOverwritesOutKey).toString()
        stat_paint_accrued.text = StatTracker.instance.getStatValue(StatTracker.instance.totalPaintAccruedKey).toString()
    }
}