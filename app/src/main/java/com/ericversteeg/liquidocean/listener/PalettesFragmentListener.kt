package com.ericversteeg.liquidocean.listener

import com.ericversteeg.liquidocean.model.Palette

interface PalettesFragmentListener {
    fun onPaletteSelected(palette: Palette)

    fun onPaletteDeleted(palette: Palette)
}