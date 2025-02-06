package com.ericversteeg.liquidocean.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.ericversteeg.liquidocean.databinding.FragmentStatsBinding
import com.ericversteeg.liquidocean.helper.Animator
import com.ericversteeg.liquidocean.helper.Utils
import com.ericversteeg.liquidocean.listener.StatsFragmentListener
import com.ericversteeg.liquidocean.model.SessionSettings
import com.ericversteeg.liquidocean.model.StatTracker
import com.ericversteeg.liquidocean.view.AchievementIcon
import com.ericversteeg.liquidocean.view.ActionButtonView
import java.text.NumberFormat

class StatsFragment: Fragment() {

    var statsFragmentListener: StatsFragmentListener? = null
    
    private var _binding: FragmentStatsBinding? = null
    val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //back_button.actionBtnView = back_action
        //back_action.type = ActionButtonView.Type.BACK_SOLID

        binding.statsImage.type = ActionButtonView.Type.STATS
        binding.statsImage.isStatic = true
        binding.statsImage.representingColor = ActionButtonView.whitePaint.color

        binding.achievementsImage.type = ActionButtonView.Type.ACHIEVEMENTS
        binding.achievementsImage.isStatic = true

        /*back_button.setOnClickListener {
            statsFragmentListener?.onStatsBack()
        }*/

        binding.statNumPixelsPaintedSingle.text = NumberFormat.getIntegerInstance().format(
            StatTracker.instance.getStatValue(
                StatTracker.instance.numPixelsPaintedSingleKey
            )
        )
        binding.statNumPixelsPaintedWorld.text = NumberFormat.getIntegerInstance().format(
            StatTracker.instance.getStatValue(
                StatTracker.instance.numPixelsPaintedWorldKey
            )
        )
        binding.statNumPixelOverwritesIn.text = NumberFormat.getIntegerInstance().format(
            StatTracker.instance.getStatValue(
                StatTracker.instance.numPixelOverwritesInKey
            )
        )
        binding.statNumPixelOverwritesOut.text = NumberFormat.getIntegerInstance().format(
            StatTracker.instance.getStatValue(
                StatTracker.instance.numPixelOverwritesOutKey
            )
        )
        binding.statPaintAccrued.text = NumberFormat.getIntegerInstance().format(
            StatTracker.instance.getStatValue(
                StatTracker.instance.totalPaintAccruedKey
            )
        )
        binding.statWorldXp.text = StatTracker.instance.getWorldLevel().toString()

        binding.achievementProgressTextSingle.text = StatTracker.instance.getAchievementProgressString(
            StatTracker.EventType.PIXEL_PAINTED_SINGLE
        )
        binding.achievementProgressTextWorld.text = StatTracker.instance.getAchievementProgressString(
            StatTracker.EventType.PIXEL_PAINTED_WORLD
        )
        binding.achievementProgressTextOverwriteIn.text = StatTracker.instance.getAchievementProgressString(
            StatTracker.EventType.PIXEL_OVERWRITE_IN
        )
        binding.achievementProgressTextOverwriteOut.text = StatTracker.instance.getAchievementProgressString(
            StatTracker.EventType.PIXEL_OVERWRITE_OUT
        )
        binding.achievementProgressTextPaint.text = StatTracker.instance.getAchievementProgressString(
            StatTracker.EventType.PAINT_RECEIVED
        )

        if (!SessionSettings.instance.tablet) {
            Animator.animateTitleFromTop(binding.statsImage)

            Animator.animateHorizontalViewEnter(binding.statNumPixelsPaintedSingleNumContainer, true)
            Animator.animateHorizontalViewEnter(binding.statNumPixelsPaintedWorldNumContainer, false)
            Animator.animateHorizontalViewEnter(binding.statNumPixelOverwritesInNumContainer, true)
            Animator.animateHorizontalViewEnter(binding.statNumPixelOverwritesOutNumContainer, false)
            Animator.animateHorizontalViewEnter(binding.statPaintAccruedNumContainer, true)
            Animator.animateHorizontalViewEnter(binding.statWorldXpContainer, false)
        }

        val statContainers = arrayOf(
            binding.statNumPixelsPaintedSingleContainer,
            binding.statNumPixelsPaintedWorldContainer,
            binding.statNumPixelOverwritesInContainer,
            binding.statNumPixelOverwritesOutContainer,
            binding.statPaintAccruedContainer
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

                val cLayoutParams = binding.achievementIconContainer.layoutParams as ConstraintLayout.LayoutParams
                cLayoutParams.height = cHeight
                binding.achievementIconContainer.layoutParams = cLayoutParams

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

                        binding.achievementIconContainer.addView(icon)
                    }
                }

                binding.achievementIconBackground.visibility = View.VISIBLE
            }
        }

        binding.achievementIconBackground.setOnClickListener {
            binding.achievementIconContainer.removeAllViews()

            it.visibility = View.INVISIBLE
        }
    }
}