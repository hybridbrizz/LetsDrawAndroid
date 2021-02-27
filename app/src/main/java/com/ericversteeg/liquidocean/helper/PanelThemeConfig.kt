package com.ericversteeg.liquidocean.helper

import android.graphics.Color
import com.ericversteeg.liquidocean.R

class PanelThemeConfig(var darkPaintQtyBar: Boolean, var inversePaintEventInfo: Boolean, var paintColorIndicatorLineColor: Int, var actionButtonColor: Int) {

    companion object {
        fun buildConfig(textureResId: Int): PanelThemeConfig {
            when (textureResId) {
                R.drawable.wood_texture_light -> return defaultDarkTheme()
                R.drawable.wood_texture -> return defaultLightTheme()
                R.drawable.marble_2 -> return defaultDarkTheme()
                R.drawable.fall_leaves -> return defaultDarkTheme()
                R.drawable.water_texture -> return defaultDarkTheme()
                R.drawable.space_texture -> return defaultLightTheme()
                R.drawable.metal_floor_1 -> return defaultLightTheme()
                R.drawable.metal_floor_2 -> return defaultDarkTheme()
                R.drawable.foil -> return defaultLightTheme()
                R.drawable.rainbow_foil -> return defaultLightTheme()
                R.drawable.crystal_1 -> return defaultDarkTheme()
                R.drawable.crystal_2 -> return defaultDarkTheme()
                R.drawable.crystal_3 -> return defaultLightTheme()
                R.drawable.crystal_4 -> return defaultDarkTheme()
                R.drawable.crystal_5 -> return defaultDarkTheme()
                R.drawable.crystal_6 -> return defaultLightTheme()
                R.drawable.crystal_7 -> return defaultLightTheme()
                R.drawable.crystal_8 -> return defaultDarkTheme()
                R.drawable.crystal_9 -> return defaultDarkTheme()
                R.drawable.crystal_10 -> return defaultDarkTheme()
            }
            return defaultLightTheme()
        }

        private fun defaultDarkTheme(): PanelThemeConfig {
            return PanelThemeConfig(true, false, Color.BLACK, Color.BLACK)
        }

        private fun defaultLightTheme(): PanelThemeConfig {
            return PanelThemeConfig(false, true, Color.WHITE, Color.WHITE)
        }
    }
}