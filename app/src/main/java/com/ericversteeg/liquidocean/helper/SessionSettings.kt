package com.ericversteeg.liquidocean.helper

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color

class SessionSettings {

    private val spKey = "MyPrefs"

    var paintColor = Color.WHITE

    fun getSharedPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(spKey, Context.MODE_PRIVATE)
    }

    companion object {
        val instance = SessionSettings()
    }
}