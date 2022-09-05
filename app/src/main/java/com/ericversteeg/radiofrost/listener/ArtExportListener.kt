package com.ericversteeg.radiofrost.listener

import com.ericversteeg.radiofrost.model.InteractiveCanvas

interface ArtExportListener {
    fun onArtExported(pixelPositions: List<InteractiveCanvas.RestorePoint>)
}