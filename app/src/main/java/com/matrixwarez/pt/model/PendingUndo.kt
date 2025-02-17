package com.matrixwarez.pt.model

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PendingUndo(
    val restorePoints: List<InteractiveCanvas.RestorePoint>,
    val sendMessage: String,
    val onUndo: (restorePoints: List<InteractiveCanvas.RestorePoint>) -> Unit
) {
    private var job: Job? = null

    init {
        job = CoroutineScope(Dispatchers.Main.immediate).launch {
            withContext(Dispatchers.Default) {
                delay(500)
            }
            onUndo(restorePoints)
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
    }
}