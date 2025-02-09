package com.matrixwarez.pt.listener

import com.matrixwarez.pt.model.Palette

interface PalettesFragmentListener {
    fun onPaletteSelected(palette: Palette, index: Int)

    fun onPaletteDeleted(palette: Palette)
}