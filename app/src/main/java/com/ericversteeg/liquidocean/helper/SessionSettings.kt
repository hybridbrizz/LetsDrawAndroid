package com.ericversteeg.liquidocean.helper

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color

class SessionSettings {

    private val spKey = "MyPrefs"

    var paintColor = Color.WHITE
    var dropsAmt = 0

    fun getSharedPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(spKey, Context.MODE_PRIVATE)
    }

    fun save(context: Context) {
        val ed = getSharedPrefs(context).edit()
        ed.putInt("paint_color", paintColor)
        ed.putInt("drops_amt", dropsAmt)
        ed.apply()
    }

    fun load(context: Context) {
        paintColor = getSharedPrefs(context).getInt("paint_color", Color.WHITE)
        dropsAmt = getSharedPrefs(context).getInt("drops_amt", 0)
    }

    companion object {
        val instance = SessionSettings()
    }
}