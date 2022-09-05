package com.ericversteeg.radiofrost.model

import android.graphics.Color
import android.graphics.Point
import org.junit.Before
import org.junit.Test

class SessionSettingsTests {

    @Test
    fun testAddPalette() {
        val name1 = "Test Palette"
        val name2 = "Test Palette 2"

        SessionSettings.instance.addPalette(name1)
        assert(SessionSettings.instance.palettes.size == 1)

        SessionSettings.instance.addPalette(name2)
        assert(SessionSettings.instance.palettes.size == 2)

        if (SessionSettings.instance.palettes.size > 1) {
            assert(SessionSettings.instance.palettes[1].name == name2)
        }
    }

    @Test
    fun testSelectedIndex() {
        val name1 = "Test Palette"
        val name2 = "Test Palette 2"
        val name3 = "Test Palette 3"

        SessionSettings.instance.addPalette(name1)
        SessionSettings.instance.addPalette(name2)
        SessionSettings.instance.addPalette(name3)

        SessionSettings.instance.selectedPaletteIndex = 1

        assert(SessionSettings.instance.palette.name == name2)

        SessionSettings.instance.selectedPaletteIndex = 2

        assert(SessionSettings.instance.palette.name == name3)

        SessionSettings.instance.selectedPaletteIndex = 0

        assert(SessionSettings.instance.palette.name == name1)
    }

    private fun testArt(): MutableList<InteractiveCanvas.RestorePoint> {
        val art: MutableList<InteractiveCanvas.RestorePoint> = ArrayList()

        art.add(InteractiveCanvas.RestorePoint(Point(30, 30), Color.BLACK, Color.BLACK))
        art.add(InteractiveCanvas.RestorePoint(Point(31, 30), Color.WHITE, Color.WHITE))
        art.add(InteractiveCanvas.RestorePoint(Point(30, 31), Color.WHITE, Color.WHITE))
        art.add(InteractiveCanvas.RestorePoint(Point(31, 31), Color.BLACK, Color.BLACK))

        return art
    }

    @Before
    fun clearPalettes() {
        SessionSettings.instance.palettes.clear()
    }
}