package com.matrixwarez.pt.compose.menu

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matrixwarez.pt.R
import com.matrixwarez.pt.compose.Inter
import com.matrixwarez.pt.model.Server


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ServerItemView(server: Server, onClick: (Server) -> Unit, onLongClick: (Server) -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    onClick(server)
                },
                onLongClick = {
                    onLongClick(server)
                }
            )
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val onlineImage = when (server.online) {
            true -> painterResource(R.drawable.green_circle)
            false -> painterResource(R.drawable.red_circle)
        }

        Image(
            modifier = Modifier.size(24.dp),
            painter = onlineImage,
            contentDescription = "Online Image"
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = server.name,
            color = Color.White,
            fontFamily = Inter,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "${server.connectionCount} / ${server.maxConnections}",
            color = Color.White,
            fontFamily = Inter,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal
        )
    }
}