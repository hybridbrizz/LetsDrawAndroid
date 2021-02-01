package com.ericversteeg.liquidocean.helper

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color

class SessionSettings {

    private val spKey = "MyPrefs"

    var paintColor = Color.WHITE
    var dropsAmt = 0
    var dropsUsed = 0
    var startTimeMillis = 0L

    fun getSharedPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(spKey, Context.MODE_PRIVATE)
    }

    fun save(context: Context) {
        val ed = getSharedPrefs(context).edit()
        ed.putInt("paint_color", paintColor)
        ed.putInt("drops_amt", dropsAmt)
        ed.putInt("drops_used", dropsUsed)
        ed.putLong("start_time", startTimeMillis)
        ed.apply()
    }

    fun load(context: Context) {
        paintColor = getSharedPrefs(context).getInt("paint_color", Color.WHITE)
        // dropsAmt = getSharedPrefs(context).getInt("drops_amt", 0)
        dropsUsed = getSharedPrefs(context).getInt("drops_used", 0)
        startTimeMillis = getSharedPrefs(context).getLong("start_time", System.currentTimeMillis())
    }

    fun loadPixelData() {
        
    }

    companion object {
        val instance = SessionSettings()
    }
}