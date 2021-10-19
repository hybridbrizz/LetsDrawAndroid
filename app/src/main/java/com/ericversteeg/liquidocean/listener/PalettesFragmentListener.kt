package com.ericversteeg.liquidocean.listener

import com.ericversteeg.liquidocean.model.Palette

interface PalettesFragmentListener {
    fun onPaletteSelected(palette: Palette, index: Int)

    fun onPaletteDeleted(palette: Palette)
}