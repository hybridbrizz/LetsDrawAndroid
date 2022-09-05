package com.ericversteeg.radiofrost.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.ericversteeg.radiofrost.R
import com.ericversteeg.radiofrost.helper.Animator
import com.ericversteeg.radiofrost.helper.Utils
import com.ericversteeg.radiofrost.listener.StatsFragmentListener
import com.ericversteeg.radiofrost.model.SessionSettings
import com.ericversteeg.radiofrost.model.StatTracker
import com.ericversteeg.radiofrost.view.AchievementIcon
import com.ericversteeg.radiofrost.view.ActionButtonView
import kotlinx.android.synthetic.main.fragment_interactive_canvas.*
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

        //back_button.actionBtnView = back_action
        //back_action.type = ActionButtonView.Type.BACK_SOLID

        stats_image.type = ActionButtonView.Type.STATS
        stats_image.isStatic = true
        stats_image.representingColor = ActionButtonView.whitePaint.color

        achievements_image.type = ActionButtonView.Type.ACHIEVEMENTS
        achievements_image.isStatic = true

        /*back_button.setOnClickListener {
            statsFragmentListener?.onStatsBack()
        }*/

        stat_num_pixels_painted_single.text = NumberFormat.getIntegerInstance().format(
            StatTracker.instance.getStatValue(
                StatTracker.instance.numPixelsPaintedSingleKey
            )
        )
        stat_num_pixels_painted_world.text = NumberFormat.getIntegerInstance().format(
            StatTracker.instance.getStatValue(
                StatTracker.instance.numPixelsPaintedWorldKey
            )
        )
        stat_num_pixel_overwrites_in.text = NumberFormat.getIntegerInstance().format(
            StatTracker.instance.getStatValue(
                StatTracker.instance.numPixelOverwritesInKey
            )
        )
        stat_num_pixel_overwrites_out.text = NumberFormat.getIntegerInstance().format(
            StatTracker.instance.getStatValue(
                StatTracker.instance.numPixelOverwritesOutKey
            )
        )
        stat_paint_accrued.text = NumberFormat.getIntegerInstance().format(
            StatTracker.instance.getStatValue(
                StatTracker.instance.totalPaintAccruedKey
            )
        )
        stat_world_xp.text = StatTracker.instance.getWorldLevel().toString()

        achievement_progress_text_single.text = StatTracker.instance.getAchievementProgressString(
            StatTracker.EventType.PIXEL_PAINTED_SINGLE
        )
        achievement_progress_text_world.text = StatTracker.instance.getAchievementProgressString(
            StatTracker.EventType.PIXEL_PAINTED_WORLD
        )
        achievement_progress_text_overwrite_in.text = StatTracker.instance.getAchievementProgressString(
            StatTracker.EventType.PIXEL_OVERWRITE_IN
        )
        achievement_progress_text_overwrite_out.text = StatTracker.instance.getAchievementProgressString(
            StatTracker.EventType.PIXEL_OVERWRITE_OUT
        )
        achievement_progress_text_paint.text = StatTracker.instance.getAchievementProgressString(
            StatTracker.EventType.PAINT_RECEIVED
        )

        if (!SessionSettings.instance.tablet) {
            Animator.animateTitleFromTop(stats_image)

            Animator.animateHorizontalViewEnter(stat_num_pixels_painted_single_num_container, true)
            Animator.animateHorizontalViewEnter(stat_num_pixels_painted_world_num_container, false)
            Animator.animateHorizontalViewEnter(stat_num_pixel_overwrites_in_num_container, true)
            Animator.animateHorizontalViewEnter(stat_num_pixel_overwrites_out_num_container, false)
            Animator.animateHorizontalViewEnter(stat_paint_accrued_num_container, true)
            Animator.animateHorizontalViewEnter(stat_world_xp_container, false)
        }

        val statContainers = arrayOf(
            stat_num_pixels_painted_single_container,
            stat_num_pixels_painted_world_container,
            stat_num_pixel_overwrites_in_container,
            stat_num_pixel_overwrites_out_container,
            stat_paint_accrued_container
        )

        val eventTypes = arrayOf(
            StatTracker.EventType.PIXEL_PAINTED_SINGLE,
            StatTracker.EventType.PIXEL_PAINTED_WORLD,
            StatTracker.EventType.PIXEL_OVERWRITE_IN,
            StatTracker.EventType.PIXEL_OVERWRITE_OUT,
            StatTracker.EventType.PAINT_RECEIVED
        )

        for (i in statContainers.indices) {
            val container = statContainers[i]
            container.setOnClickListener {
                val eventType = eventTypes[i]
                val thresholdsPassed = StatTracker.instance.thresholdsPassed(eventType)

                val cHeight = (thresholdsPassed / 8 * Utils.dpToPx(context, 55)) + Utils.dpToPx(
                    context,
                    60
                )

                val cLayoutParams = achievement_icon_container.layoutParams as ConstraintLayout.LayoutParams
                cLayoutParams.height = cHeight
                achievement_icon_container.layoutParams = cLayoutParams

                context?.apply {
                    for (t in 0 until thresholdsPassed) {
                        val icon = AchievementIcon(this)

                        val layoutParams = FrameLayout.LayoutParams(
                            Utils.dpToPx(this, 50), Utils.dpToPx(
                                this,
                                50
                            )
                        )

                        icon.layoutParams = layoutParams

                        val margin = Utils.dpToPxF(this, 5)
                        val size = Utils.dpToPxF(this, 50)

                        val x = (t % 8) * size + (t % 8) * margin + margin
                        val y = (t / 8) * size + (t / 8) * margin + margin

                        icon.x = x
                        icon.y = y

                        icon.setType(eventType, t + 1)

                        achievement_icon_container.addView(icon)
                    }
                }

                achievement_icon_background.visibility = View.VISIBLE
            }
        }

        achievement_icon_background.setOnClickListener {
            achievement_icon_container.removeAllViews()

            it.visibility = View.INVISIBLE
        }
    }
}