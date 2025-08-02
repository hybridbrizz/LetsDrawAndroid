package com.matrixwarez.pt.compose.menu

import android.graphics.drawable.Drawable
import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.signature.ObjectKey
import com.matrixwarez.pt.R
import com.matrixwarez.pt.compose.Inter
import com.matrixwarez.pt.model.Server


@OptIn(ExperimentalFoundationApi::class, ExperimentalGlideComposeApi::class)
@Composable
fun ServerItemView(server: Server, editing: Boolean, index: Int = 0,
                   showDeleteConfirmationState: MutableState<Boolean> = mutableStateOf(false),
                   serverToRemoveState: MutableState<Server?> = mutableStateOf(null),
                   onClick: (Server) -> Unit, onLongClick: (Server) -> Unit = {},
                   publicServerItemReadyOnScreen: () -> Unit = {}) {

    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val clickInteractionSource = remember { MutableInteractionSource() }
        val clickInteraction by clickInteractionSource.interactions.collectAsState(null)

        val contentColor by animateColorAsState(
            targetValue = when (clickInteraction) {
                is PressInteraction.Press -> colorResource(R.color.colorAccent)
                else -> Color.White
            },
            animationSpec = tween(
                durationMillis = 200,
                easing = LinearEasing
            )
        )

        Column(modifier = Modifier.fillMaxWidth(0.75f)) {
            Log.d("Recompose", "Recompose with img url ${server.canvasImageUrl}")

            Text(
                text = when (index > 0) {
                    true -> "#$index - ${server.name} (${server.size}x${server.size})"
                    false -> server.name
                },
                color = contentColor,
                fontFamily = Inter,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            GlideImage(
                modifier = Modifier.fillMaxWidth().aspectRatio(1f).border(1.dp, color = contentColor).clickable(
                    interactionSource = clickInteractionSource,
                    indication = ripple(true, color = Color.White)
                ) {
                    onClick(server)
                }
                .onGloballyPositioned {
                    publicServerItemReadyOnScreen()
                },
                model = server.canvasImageUrl,
                contentDescription = "${server.name} canvas image"
            ) {
                it
                    .signature(ObjectKey(System.currentTimeMillis() / 1000 / 60 / 15))
                    .thumbnail(
                        Glide.with(context)
                            .load(server.canvasImageUrl)
                            .onlyRetrieveFromCache(true)
                    )
                    .listener(object: RequestListener<Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable>,
                            isFirstResource: Boolean
                        ): Boolean {
                            return false
                        }

                        override fun onResourceReady(
                            resource: Drawable,
                            model: Any,
                            target: Target<Drawable>?,
                            dataSource: DataSource,
                            isFirstResource: Boolean
                        ): Boolean {
                            return false
                        }
                    })
            }

            when (editing) {
                true -> {
                    Button(
                        modifier = Modifier.padding(8.dp).height(30.dp).align(Alignment.CenterHorizontally),
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

        Spacer(modifier = Modifier.height(30.dp))
//
//        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colorResource(R.color.colorAccent).copy(0.5f)))
    }
}