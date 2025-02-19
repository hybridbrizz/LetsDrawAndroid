package com.matrixwarez.pt.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matrixwarez.pt.model.InteractiveCanvas
import com.matrixwarez.pt.model.SessionSettings
import kotlin.random.Random

@Composable
fun ClientsInfoListView(modifier: Modifier = Modifier, clientsInfo: List<Pair<String, Int>>, interactiveCanvas: InteractiveCanvas) {
    val random = remember { Random(System.currentTimeMillis()) }

    LazyColumn(modifier = modifier.fillMaxWidth(0.4f).aspectRatio(16/9f).background(Color.DarkGray)) {
        items(clientsInfo) {
            ClientsInfoItemView(
                clientInfo = it,
                interactiveCanvas = interactiveCanvas,
                random = random
            )
        }
    }
}

@Composable
fun ClientsInfoItemView(clientInfo: Pair<String, Int>, interactiveCanvas: InteractiveCanvas, random: Random) {
    val context = LocalContext.current

    val (name, center) = clientInfo

    Row(modifier = Modifier
        .fillMaxWidth()
        .clickable {
            if (name == SessionSettings.instance.displayName) return@clickable

            val x = center % interactiveCanvas.cols
            val minX = x - 10
            val maxX = x + 10

            val y = center / interactiveCanvas.cols
            val minY = y - 10
            val maxY = y + 10

            val rx = random.nextInt(
                minX.coerceIn(0 until interactiveCanvas.cols),
                maxX.coerceIn(0 until interactiveCanvas.cols)
            )

            val ry = random.nextInt(
                minY.coerceIn(0 until interactiveCanvas.rows),
                maxY.coerceIn(0 until interactiveCanvas.rows)
            )

            interactiveCanvas.jumpToUnit(context, rx, ry)
        }
        .padding(16.dp)
    ) {
        var displayName = name
        if (displayName == SessionSettings.instance.displayName) {
            displayName = "$displayName (me)"
        }
        Text(
            text = displayName,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Normal,
            fontFamily = Inter
        )
    }
}