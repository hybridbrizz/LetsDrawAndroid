package com.matrixwarez.pt.compose

import android.util.Log
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.matrixwarez.pt.model.InteractiveCanvas
import com.matrixwarez.pt.model.SessionSettings

@Composable
fun ClientSummaryLocationsView(clientsInfoState: MutableState<List<Triple<String, Int, Int>>?>,
                              interactiveCanvas: InteractiveCanvas
) {
    val density = LocalDensity.current

    val clientsInfo by clientsInfoState

    Box(modifier = Modifier.fillMaxSize().drawWithContent {
        clientsInfo?.forEachIndexed { index, it ->
            val (name, centerPixelId) = it

            var centerX = (centerPixelId % interactiveCanvas.cols).toFloat()
            centerX = centerX / interactiveCanvas.cols.toFloat() * drawContext.size.width

            var centerY = (centerPixelId / interactiveCanvas.cols).toFloat()
            centerY = centerY / interactiveCanvas.rows.toFloat() * drawContext.size.height

            Log.d("Center Test", "($centerX, $centerY)")

            val color = when {
                index == 0 -> Color.Blue
                else -> Color.Red
            }

            if (name != SessionSettings.instance.displayName) {
                drawCircle(
                    color = color,
                    radius = density.run { 2.dp.toPx() },
                    center = Offset(centerX,centerY)
                )
            }
        }
        drawContent()
    })
}