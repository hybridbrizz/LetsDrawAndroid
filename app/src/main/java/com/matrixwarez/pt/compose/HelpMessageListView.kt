package com.matrixwarez.pt.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matrixwarez.pt.model.SessionSettings


@Composable
fun HelpMessageListView(modifier: Modifier = Modifier, onClose: () -> Unit) {
    LazyColumn(
        modifier = modifier
            .width(300.dp)
            .aspectRatio(3/4f)
            .background(Color.White, shape = RoundedCornerShape(10.dp)),
        verticalArrangement = Arrangement.spacedBy(30.dp)
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
                        color = Color.Black,
                        fontFamily = Inter,
                        fontWeight = FontWeight.Normal,
                        fontSize = 16.sp
                    )
                }
            }
        }

        item {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 60.dp)) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "A few tips to help you get started.",
                    textAlign = TextAlign.Center,
                    color = Color.Black,
                    fontFamily = Inter,
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        itemsIndexed(SessionSettings.instance.getHelpMessages()) { index, item ->
            Box(modifier = Modifier.padding(top = 5.dp, start = 40.dp, end = 40.dp)) {
                Text(
                    text = "${index + 1}.) $item",
                    color = Color.Black,
                    fontFamily = Inter,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}