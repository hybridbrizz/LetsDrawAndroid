package com.matrixwarez.pt.compose.menu

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matrixwarez.pt.compose.Inter
import com.matrixwarez.pt.model.Server
import com.matrixwarez.pt.model.SessionSettings


@SuppressLint("MutableCollectionMutableState")
@Composable
fun PrivateServerListView(privateServerListState: MutableState<List<Server>>,
                          loadingState: MutableState<Boolean>,
                          onSelectServer: (Server) -> Unit) {

    val privateAndAdminServerList by privateServerListState
    val adminServerList = privateAndAdminServerList.filter { it.isAdmin }
    val privateServerList = privateAndAdminServerList.filter { !it.isAdmin }

    val isLoading by loadingState

    Box(contentAlignment = Alignment.Center) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.DarkGray)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {},
        ) {
            if (adminServerList.isNotEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                        Text(
                            "Mod",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontFamily = Inter,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                items(adminServerList) { server ->
                    ServerItemView(
                        server = server,
                        onClick = {
                            onSelectServer(server)
                        }
                    )
                }
            }
            if (adminServerList.isNotEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                        Text(
                            "Private",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontFamily = Inter,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            items(privateServerList) { server ->
                ServerItemView(
                    server = server,
                    onClick = {
                        onSelectServer(server)
                    }
                )
            }
        }

        if (isLoading) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 2.dp
            )
        }
    }
}