package com.ericversteeg.radiofrost.listener

import com.ericversteeg.radiofrost.model.Palette

interface PalettesFragmentListener {
    fun onPaletteSelected(palette: Palette, index: Int)

    fun onPaletteDeleted(palette: Palette)
}