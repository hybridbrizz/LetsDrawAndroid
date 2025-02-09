package com.matrixwarez.pt.listener

import com.matrixwarez.pt.model.InteractiveCanvas

interface ArtExportListener {
    fun onArtExported(pixelPositions: List<InteractiveCanvas.RestorePoint>)
}