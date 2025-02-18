package com.matrixwarez.pt.compose

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.matrixwarez.pt.model.InteractiveCanvas


@Composable
fun ClientCanvasLocationsView(clientsInfoState: MutableState<List<Pair<String, Int>>?>,
                              redrawCountState: MutableIntState, interactiveCanvas: InteractiveCanvas) {
    val density = LocalDensity.current

    val clientsInfo by clientsInfoState

    Box(modifier = Modifier.fillMaxSize().drawWithContent {
        clientsInfo?.forEachIndexed { index, it ->
            val (name, centerPixelId) = it

            val centerX = centerPixelId % interactiveCanvas.cols
            val centerY = centerPixelId / interactiveCanvas.cols

            val screenPoint = interactiveCanvas.unitToScreenPoint(centerX.toFloat(), centerY.toFloat())

            screenPoint?.let {
                Log.d("Screen Point", "center($centerX, $centerY) -> ${it.x}, ${it.y}")
            }

            if (redrawCountState.value > 3) {}

            val color = when {
                index == 0 -> Color.Blue
                else -> Color.Red
            }

            screenPoint?.let {
                drawCircle(
                    color = color,
                    radius = density.run { 10.dp.toPx() },
                    center = Offset(screenPoint.x.toFloat(), screenPoint.y.toFloat())
                )
            }
        }
        drawContent()
    })
}