package com.ericversteeg.radiofrost.listener

interface AchievementListener {
    fun onDisplayAchievement(info: Map<String, Any>, displayInterval: Long)
}