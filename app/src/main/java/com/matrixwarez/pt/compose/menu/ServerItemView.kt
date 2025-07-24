package com.matrixwarez.pt.compose.menu

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.signature.ObjectKey
import com.matrixwarez.pt.R
import com.matrixwarez.pt.compose.Inter
import com.matrixwarez.pt.model.Server


@OptIn(ExperimentalFoundationApi::class, ExperimentalGlideComposeApi::class)
@Composable
fun ServerItemView(server: Server, editing: Boolean,
                   showDeleteConfirmationState: MutableState<Boolean> = mutableStateOf(false),
                   serverToRemoveState: MutableState<Server?> = mutableStateOf(null),
                   onClick: (Server) -> Unit, onLongClick: (Server) -> Unit = {}) {
    Column(
        modifier = Modifier.fillMaxWidth().clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(true, color = Color.White)
        ) {
            onClick(server)
        }
        .padding(top = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Log.d("Recompose", "Recompose with img url ${server.canvasImageUrl}")

        GlideImage(
            modifier = Modifier.fillMaxWidth(0.75f).aspectRatio(1f).border(1.dp, color = Color(android.graphics.Color.parseColor("#FF4D00")), shape = RoundedCornerShape(20.dp)).clip(
                RoundedCornerShape(20.dp)
            ),
            model = server.canvasImageUrl,
            contentDescription = "${server.name} canvas image"
        ) {
            it.signature(ObjectKey(System.currentTimeMillis() / 1000 / 60 / 15))
        }

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
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal
            )

            Spacer(modifier = Modifier.width(8.dp))

            Image(
                modifier = Modifier.size(14.dp),
                painter = onlineImage,
                contentDescription = "Online Image"
            )
        }
        when (editing) {
            true -> {
                Button(
                    modifier = Modifier.height(30.dp),
                    onClick = {
                        serverToRemoveState.value = server
                        showDeleteConfirmationState.value = true
                    },
                    shape = RoundedCornerShape(5.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 15.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "Remove",
                        fontFamily = Inter,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            false -> {
//                Button(
//                    modifier = Modifier.height(30.dp),
//                    onClick = {
//                        onClick(server)
//                    },
//                    shape = RoundedCornerShape(5.dp),
//                    colors = ButtonDefaults.buttonColors(
//                        containerColor = Color(0.15f, 0.15f, 0.15f),
//                        contentColor = Color.White
//                    ),
//                    contentPadding = PaddingValues(horizontal = 15.dp, vertical = 5.dp)
//                ) {
//                    Text(
//                        text = "Connect",
//                        fontFamily = Inter,
//                        fontSize = 11.sp,
//                        lineHeight = 14.sp,
//                        maxLines = 1,
//                        overflow = TextOverflow.Ellipsis
//                    )
//                }
            }
        }
    }
}