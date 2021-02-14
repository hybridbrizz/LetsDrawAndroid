package com.ericversteeg.liquidocean.listener

import com.ericversteeg.liquidocean.model.InteractiveCanvas

interface ArtExportListener {
    fun onArtExported(pixelPositions: List<InteractiveCanvas.RestorePoint>)
}