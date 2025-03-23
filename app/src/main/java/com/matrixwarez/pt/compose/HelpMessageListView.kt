package com.matrixwarez.pt.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matrixwarez.pt.model.SessionSettings


@Composable
fun HelpMessageListView(modifier: Modifier = Modifier, onClose: () -> Unit) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth(0.7f)
            .background(Color.DarkGray, shape = RoundedCornerShape(10.dp))
            .border(1.dp, Color(android.graphics.Color.parseColor("#FAD452")).copy(0.5f), shape = RoundedCornerShape(10.dp))
    ) {
        item {
            Box(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(40.dp, 40.dp)
                        .clickable {
                            onClose()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "x",
                        color = Color.White,
                        fontFamily = Inter,
                        fontWeight = FontWeight.Normal,
                        fontSize = 16.sp
                    )
                }
            }
        }
        items(SessionSettings.instance.getHelpMessages()) { item ->
            Box(modifier = Modifier.padding(top = 5.dp, start = 20.dp, end = 20.dp)) {
                Text(
                    text = item,
                    color = Color.White,
                    fontFamily = Inter,
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.sp
                )
            }
        }
        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}