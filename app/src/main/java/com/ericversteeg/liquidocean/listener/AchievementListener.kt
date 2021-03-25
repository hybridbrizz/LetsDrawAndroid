package com.ericversteeg.liquidocean.listener

import com.ericversteeg.liquidocean.model.StatTracker

interface AchievementListener {
    fun onDisplayAchievement(info: Map<String, Any>, displayInterval: Long)
}