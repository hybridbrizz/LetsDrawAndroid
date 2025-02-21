package com.matrixwarez.pt.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matrixwarez.pt.R

private val menuItems = listOf(
    MenuItem("Server List", R.drawable.server_person),
    MenuItem("Styles", R.drawable.colors),
    MenuItem("Yank Canvas", R.drawable.photo_camera),
    MenuItem("Leave", R.drawable.logout)
)

@Composable
fun CanvasMenuView(onServerList: () -> Unit, onStyles: () -> Unit,
                   onGrabImage: () -> Unit, onLeave: () -> Unit) {

    Box(modifier = Modifier.background(color = Color.DarkGray, shape = RoundedCornerShape(10.dp)).padding(16.dp)) {
        Column {
            Row {
                CanvasMenuItemView(item = menuItems[0]) {
                    onServerList()
                }
                Spacer(modifier = Modifier.width(16.dp))
                CanvasMenuItemView(item = menuItems[1]) {
                    onStyles()
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row {
                CanvasMenuItemView(item = menuItems[2]) {
                    onGrabImage()
                }
                Spacer(modifier = Modifier.width(16.dp))
                CanvasMenuItemView(item = menuItems[3]) {
                    onLeave()
                }
            }
        }
    }
}

@Composable
fun CanvasMenuItemView(item: MenuItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(100.dp)
            .background(color = Color.DarkGray, shape = RoundedCornerShape(10.dp))
            .border(2.dp, color = Color.White, shape = RoundedCornerShape(10.dp))
    ) {
        Text(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 5.dp),
            text = item.title.uppercase(),
            textAlign = TextAlign.Center,
            fontSize = 10.sp,
            fontFamily = Inter,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White))
        Box(
            modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 10.dp, bottomEnd = 10.dp)).clickable {
                onClick()
            },
            contentAlignment = Alignment.Center
        ) {
            Image(painter = painterResource(item.iconRes), contentDescription = "${item.title} icon", colorFilter = ColorFilter.tint(Color.White))
        }
    }
}

class MenuItem(val title: String, val iconRes: Int)