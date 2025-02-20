package com.matrixwarez.pt.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matrixwarez.pt.model.InteractiveCanvas
import com.matrixwarez.pt.model.SessionSettings
import kotlin.random.Random

val mapMarkerTypes = listOf("On", "Off", "Canvas Only", "Minimap Only")

@Composable
fun ClientsInfoListView(modifier: Modifier = Modifier, clientsInfo: List<Triple<String, Int, Int>>,
                        mapMarkerIndexState: MutableIntState, interactiveCanvas: InteractiveCanvas) {
    val random = remember { Random(System.currentTimeMillis()) }

    var mapMarkerIndex by mapMarkerIndexState

    Column(modifier = modifier.fillMaxWidth(0.4f)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(Color.DarkGray)
                .clickable(
                    indication = null,
                    interactionSource = remember {
                        MutableInteractionSource()
                    }) {},
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 10.dp),
                text = "Map markers:",
                color = Color.White,
                fontFamily = Inter,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal
            )
            Spacer(modifier = Modifier.weight(1f))
            Box(modifier = Modifier.fillMaxWidth(0.6f).fillMaxHeight().background(Color(50, 50, 50)).clickable {
                mapMarkerIndex += 1
            }, contentAlignment = Alignment.Center) {
                Text(
                    text = mapMarkerTypes[mapMarkerIndex % mapMarkerTypes.size],
                    color = Color.White,
                    fontFamily = Inter,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxWidth().aspectRatio(16 / 9f).background(Color.DarkGray)
        ) {
            items(clientsInfo) {
                ClientsInfoItemView(
                    clientInfo = it,
                    interactiveCanvas = interactiveCanvas,
                    random = random
                )
            }
        }
    }
}

@Composable
fun ClientsInfoItemView(clientInfo: Triple<String, Int, Int>, interactiveCanvas: InteractiveCanvas, random: Random) {
    val context = LocalContext.current

    val (name, center, color) = clientInfo

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(0.2f)))

        Row(
            modifier = Modifier
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
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            var displayName = name
            if (displayName == SessionSettings.instance.displayName) {
                displayName = "$displayName (me)"
            }

            Box(modifier = Modifier.size(20.dp).background(Color(color), shape = CircleShape))

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = displayName,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = Inter
            )
        }
    }
}