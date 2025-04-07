package com.matrixwarez.pt.compose.menu

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.matrixwarez.pt.R
import com.matrixwarez.pt.compose.Inter
import com.matrixwarez.pt.model.Server


@OptIn(ExperimentalFoundationApi::class, ExperimentalGlideComposeApi::class)
@Composable
fun ServerItemView(server: Server, onClick: (Server) -> Unit, onLongClick: (Server) -> Unit = {}) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GlideImage(
            modifier = Modifier.fillMaxWidth().aspectRatio(1f).border(Dp.Hairline, color = Color.White),
            model = server.canvasImageUrl,
            loading = placeholder {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            },
            contentDescription = "${server.name} canvas image"
        )
        Row(
            modifier = Modifier
                .combinedClickable(
                    onClick = {

                    },
                    onLongClick = {
                        onLongClick(server)
                    }
                )
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            val onlineImage = when (server.online) {
                true -> painterResource(R.drawable.green_circle)
                false -> painterResource(R.drawable.red_circle)
            }

            Text(
                text = server.name,
                color = Color.White,
                fontFamily = Inter,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal
            )

            Spacer(modifier = Modifier.width(5.dp))

            Image(
                modifier = Modifier.size(10.dp),
                painter = onlineImage,
                contentDescription = "Online Image"
            )
        }
        Button(
            onClick = {
                onClick(server)
            },
            shape = RoundedCornerShape(5.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0.15f, 0.15f, 0.15f),
                contentColor = Color.White
            )
        ) {
            Text("Connect", fontFamily = Inter, fontSize = 10.sp)
        }
    }
}