package com.ericversteeg.liquidocean.listener

import com.ericversteeg.liquidocean.model.StatTracker

interface AchievementListener {
    fun onDisplayAchievement(info: Map<StatTracker.EventType, Int>, displayInterval: Long)
}