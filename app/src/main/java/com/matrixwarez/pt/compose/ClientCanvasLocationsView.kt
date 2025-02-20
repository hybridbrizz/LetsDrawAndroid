package com.matrixwarez.pt.compose

import android.graphics.Typeface
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawContext
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matrixwarez.pt.model.InteractiveCanvas
import com.matrixwarez.pt.model.SessionSettings


@Composable
fun ClientCanvasLocationsView(clientsInfoState: MutableState<List<Triple<String, Int, Int>>?>,
                              redrawCountState: MutableIntState,
                              lineColorIsDarkState: MutableState<Boolean>,
                              showServerListState: MutableState<Boolean>,
                              mapMarkerIndexState: MutableIntState,
                              interactiveCanvas: InteractiveCanvas) {

    val density = LocalDensity.current

    val clientsInfo by clientsInfoState

    val lineColorIsDark by lineColorIsDarkState

    val showServerList by showServerListState

    val fontSize = 18.sp

    val textMeasurer = rememberTextMeasurer()

    Box(modifier = Modifier.fillMaxSize().drawWithContent {
        clientsInfo?.forEachIndexed { index, it ->
            val (name, centerPixelId) = it

            val textSize = measureTextSize(
                textMeasurer = textMeasurer,
                text = name,
                fontSize = fontSize,
                fontWeight = FontWeight.Normal
            )

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

            if (name != SessionSettings.instance.displayName) {
                screenPoint?.let {
                    val lineColor = when (lineColorIsDark) {
                        true -> Color.Black
                        false -> Color.White
                    }

                    val center = Offset(screenPoint.x.toFloat(), screenPoint.y.toFloat())
                    val firstLineEnd = center.plus(Offset(density.run { 10.dp.toPx() }, density.run { -20.dp.toPx() }))
                    val secondLineEnd = firstLineEnd.plus(Offset(density.run { textSize.width.toDp().toPx() }, 0f))

                    drawLine(
                        color = lineColor,
                        start = center,
                        end = firstLineEnd,
                        strokeWidth = 1.dp.toPx()
                    )

                    drawLine(
                        color = lineColor,
                        start = firstLineEnd,
                        end = secondLineEnd,
                        strokeWidth = 1.dp.toPx()
                    )

                    drawTextAtCenterPoint(
                        text = name,
                        x = secondLineEnd.x - (secondLineEnd.x - firstLineEnd.x) / 2,
                        y = firstLineEnd.y - textSize.height / 2,
                        size = fontSize.toPx(),
                        textColor = lineColor,
                        drawContext = drawContext
                    )

                    drawCircle(
                        color = color,
                        radius = density.run { 10.dp.toPx() },
                        center = center
                    )
                }
            }
        }
        drawContent()
    }) {
        if (showServerList && clientsInfo != null) {
            ClientsInfoListView(
                modifier = Modifier.align(Alignment.Center),
                interactiveCanvas = interactiveCanvas,
                clientsInfo = clientsInfo!!,
                mapMarkerIndexState = mapMarkerIndexState
            )
        }
    }
}



fun measureTextSize(textMeasurer: TextMeasurer, text: String, fontSize: TextUnit, fontWeight: FontWeight): IntSize {
    val textMeasurement = textMeasurer.measure(
        text = text,
        style = TextStyle(
            fontSize = fontSize,
            fontWeight = fontWeight
        )
    )
    return textMeasurement.size
}

fun drawTextAtCenterPoint(text: String, x: Float, y: Float, size: Float,
                          textColor: Color, fontWeight: FontWeight = FontWeight.Normal,
                          drawContext: DrawContext
) {

    // Thanks Claude
    val textPaint = Paint()
        .asFrameworkPaint()
        .apply {
            isAntiAlias = true
            textSize = size
            color = textColor.toArgb()
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, fontWeight.weight)
        }

    val textBounds = android.graphics.Rect()
    textPaint.getTextBounds(
        text, 0,
        text.length, textBounds
    )

    // Draw the text
    drawContext.canvas.nativeCanvas.drawText(
        text,
        x,
        y,
        textPaint
    )
}