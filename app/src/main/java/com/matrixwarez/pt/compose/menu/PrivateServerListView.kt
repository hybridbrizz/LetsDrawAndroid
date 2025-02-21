package com.matrixwarez.pt.compose.menu

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.matrixwarez.pt.model.Server
import com.matrixwarez.pt.model.SessionSettings


@SuppressLint("MutableCollectionMutableState")
@Composable
fun PrivateServerListView(onSelectServer: (Server) -> Unit) {

    val serverListState = remember { mutableStateOf(SessionSettings.instance.servers) }
    val serverList by serverListState

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.DarkGray)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {},
    ) {
        items(serverList) { server ->
            ServerItemView(
                server = server,
                onClick = {
                    onSelectServer(server)
                }
            )
        }
    }
}