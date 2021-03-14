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
import com.ericversteeg.liquidocean.model.SessionSettings
import com.ericversteeg.liquidocean.model.StatTracker
import com.ericversteeg.liquidocean.view.ActionButtonView
import kotlinx.android.synthetic.main.fragment_interactive_canvas.*
import kotlinx.android.synthetic.main.fragment_interactive_canvas.back_action
import kotlinx.android.synthetic.main.fragment_interactive_canvas.back_button
import kotlinx.android.synthetic.main.fragment_options.*
import kotlinx.android.synthetic.main.fragment_stats.*
import java.text.NumberFormat

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
        back_action.type = ActionButtonView.Type.BACK_SOLID

        stats_image.type = ActionButtonView.Type.STATS
        stats_image.static = true
        stats_image.representingColor = ActionButtonView.whitePaint.color

        achievements_image.type = ActionButtonView.Type.ACHIEVEMENTS
        achievements_image.static = true

        back_button.setOnClickListener {
            statsFragmentListener?.onStatsBack()
        }

        stat_num_pixels_painted_single.text = NumberFormat.getIntegerInstance().format(StatTracker.instance.getStatValue(StatTracker.instance.numPixelsPaintedSingleKey))
        stat_num_pixels_painted_world.text = NumberFormat.getIntegerInstance().format(StatTracker.instance.getStatValue(StatTracker.instance.numPixelsPaintedWorldKey))
        stat_num_pixel_overwrites_in.text = NumberFormat.getIntegerInstance().format(StatTracker.instance.getStatValue(StatTracker.instance.numPixelOverwritesInKey))
        stat_num_pixel_overwrites_out.text = NumberFormat.getIntegerInstance().format(StatTracker.instance.getStatValue(StatTracker.instance.numPixelOverwritesOutKey))
        stat_paint_accrued.text = NumberFormat.getIntegerInstance().format(StatTracker.instance.getStatValue(StatTracker.instance.totalPaintAccruedKey))
        stat_world_xp.text = StatTracker.instance.getWorldLevel().toString()

        achievement_progress_text_single.text = StatTracker.instance.getAchievementProgressString(StatTracker.EventType.PIXEL_PAINTED_SINGLE)
        achievement_progress_text_world.text = StatTracker.instance.getAchievementProgressString(StatTracker.EventType.PIXEL_PAINTED_WORLD)
        achievement_progress_text_overwrite_in.text = StatTracker.instance.getAchievementProgressString(StatTracker.EventType.PIXEL_OVERWRITE_IN)
        achievement_progress_text_overwrite_out.text = StatTracker.instance.getAchievementProgressString(StatTracker.EventType.PIXEL_OVERWRITE_OUT)
        achievement_progress_text_paint.text = StatTracker.instance.getAchievementProgressString(StatTracker.EventType.PAINT_RECEIVED)

        if (!SessionSettings.instance.tablet) {
            Animator.animateTitleFromTop(stats_image)

            Animator.animateHorizontalViewEnter(stat_num_pixels_painted_single_container, true)
            Animator.animateHorizontalViewEnter(stat_num_pixels_painted_world_container, false)
            Animator.animateHorizontalViewEnter(stat_num_pixel_overwrites_in_container, true)
            Animator.animateHorizontalViewEnter(stat_num_pixel_overwrites_out_container, false)
            Animator.animateHorizontalViewEnter(stat_paint_accrued_container, true)
            Animator.animateHorizontalViewEnter(stat_world_xp_container, false)
        }
    }
}