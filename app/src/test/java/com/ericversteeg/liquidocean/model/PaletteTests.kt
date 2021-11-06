package com.ericversteeg.liquidocean.model

import android.graphics.Color
import org.junit.Test

class PaletteTests {

    @Test
    fun testName() {
        val name = "Test Palette"
        val palette = Palette(name)

        assert(palette.name == name)
    }

    @Test
    fun testAddColor() {
        val palette = Palette("Test Palette")

        val color1 = Color.GREEN
        palette.addColor(color1)

        assert(palette.colors.size == 1)

        if (palette.colors.size > 0) {
            assert(palette.colors[0] == color1)
        }

        val color2 = Color.YELLOW
        val color3 = Color.CYAN

        palette.addColor(color2)
        palette.addColor(color3)

        assert(palette.colors.size == 3)

        if (palette.colors.size > 2) {
            assert(palette.colors[0] == color1)
            assert(palette.colors[1] == color2)
            assert(palette.colors[2] == color3)
        }
    }

    @Test
    fun testRemoveColor() {
        val palette = Palette("Test Palette")

        val color1 = Color.GREEN

        palette.addColor(color1)
        palette.removeColor(color1)

        assert(palette.colors.size == 0)

        val color2 = Color.YELLOW
        val color3 = Color.CYAN

        palette.addColor(color1)
        palette.addColor(color2)
        palette.addColor(color3)

        palette.removeColor(color2)

        assert(palette.colors.size == 2)

        if (palette.colors.size > 1) {
            assert(palette.colors[0] == color1)
            assert(palette.colors[1] == color3)
        }

        palette.removeColor(color2)

        assert(palette.colors.size == 2)

        palette.removeColor(color1)

        assert(palette.colors.size == 1)

        if (palette.colors.size > 0) {
            assert(palette.colors[0] == color3)
        }

        palette.removeColor(color3)

        assert(palette.colors.size == 0)
    }

    @Test
    fun testToMap() {
        val paletteName = "Test Palette"
        val palette = Palette(paletteName)

        val color1 = Color.GREEN
        val color2 = Color.YELLOW

        palette.addColor(color1)
        palette.addColor(color2)

        val map = palette.toMap()

        val keys = arrayOf("name", "colors")

        assert(map.keys.size == 2)

        for (key in map.keys) {
            assert(key != null)
            assert(key is String)

            val keyStr = key as String
            assert(keys.contains(keyStr))
        }

        val name = map["name"]
        assert(name != null)
        assert(name == paletteName)

        val colors = map["colors"]
        assert(name != null)
        assert(colors is Array<*>)

        val colorsArr = colors as Array<*>

        for (element in colorsArr) {
            assert(element is Int)
        }

        assert(colorsArr.size == 2)

        if (colorsArr.size > 1) {
            assert(colorsArr[0] == color1)
            assert(colorsArr[1] == color2)
        }
    }
}