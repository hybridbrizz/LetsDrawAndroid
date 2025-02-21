package com.matrixwarez.pt.compose.menu

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import com.matrixwarez.pt.model.Server


@Composable
fun ServerListsView(serverListState: MutableState<List<Server>>, onSelectServer: (Server) -> Unit) {

    val pagerState = rememberPagerState {
        2
    }

    HorizontalPager(
        modifier = Modifier
            .fillMaxHeight(0.8f)
            .aspectRatio(16/9f),
        state = pagerState
    ) { page ->
        when (page == 0) {
            true -> PublicServerListView(
                serverListState = serverListState,
                onSelectServer = onSelectServer
            )
            false -> PrivateServerListView(
                onSelectServer = onSelectServer
            )
        }
    }
}