package com.ericversteeg.radiofrost.model

import kotlin.collections.HashMap

class Palette(val name: String) {
    var colors: MutableList<Int> = ArrayList()

    fun addColor(color: Int) {
        if (!colors.contains(color)) {
            colors.add(color)
        }
    }

    fun removeColor(color: Int) {
        if (colors.contains(color)) {
            colors.remove(color)
        }
    }

    fun toMap(): Map<Any?, Any?> {
        val map = HashMap<Any?, Any?>()
        map["name"] = name
        map["colors"] = colors.toTypedArray()

        return map
    }

    companion object {
        const val maxColors = 32
    }
}