package com.matrixwarez.pt.compose.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matrixwarez.pt.compose.Inter
import com.matrixwarez.pt.model.Server
import com.matrixwarez.pt.service.ServerService
import kotlinx.coroutines.launch


@Composable
fun ServerListsView(serverService: ServerService, publicServerListState: MutableState<List<Server>>,
                    privateServerListState: MutableState<List<Server>>,
                    loadingState: MutableState<Boolean>,
                    onSelectServer: (Server) -> Unit, onRefreshServerList: (Boolean) -> Unit) {

    val coroutineScope = rememberCoroutineScope()

    val pagerState = rememberPagerState {
        2
    }

    val showAddFormState = remember { mutableStateOf(false) }

    Column(modifier = Modifier
        .shadow(2.dp)
        .fillMaxHeight(0.8f)
        .aspectRatio(16/9f)
        .background(Color.DarkGray)
    ) {
        Row {
            Button(
                modifier = Modifier.weight(0.5f).height(60.dp),
                shape = RectangleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.DarkGray
                ),
                onClick = {
                    coroutineScope.launch {
                        pagerState.scrollToPage(0)
                    }
                }
            ) {
                Text(
                    text = "Public".uppercase(),
                    color = Color.White,
                    fontFamily = Inter,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp
                )
            }
            Button(
                modifier = Modifier.weight(0.5f).height(60.dp),
                shape = RectangleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.DarkGray
                ),
                onClick = {
                    coroutineScope.launch {
                        pagerState.scrollToPage(1)
                    }
                }
            ) {
                Text(
                    text = "Private".uppercase(),
                    color = Color.White,
                    fontFamily = Inter,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp
                )
            }
        }

        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(0.2f)))

        Row {
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = {
                onRefreshServerList(pagerState.targetPage == 0)
            }) {
                Icon(
                    modifier = Modifier.size(24.dp),
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Refresh",
                    tint = Color.White
                )
            }
            if (pagerState.targetPage == 1) {
                IconButton(onClick = {
                    showAddFormState.value = true
                }) {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add Private Server",
                        tint = Color.White
                    )
                }
            }
        }

        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(0.2f)))

        HorizontalPager(
            state = pagerState,
            userScrollEnabled = false
        ) { page ->
            when (page == 0) {
                true -> PublicServerListView(
                    serverListState = publicServerListState,
                    onSelectServer = onSelectServer,
                    loadingState = loadingState
                )
                false -> PrivateServerListView(
                    serverService = serverService,
                    onSelectServer = onSelectServer,
                    privateServerListState = privateServerListState,
                    showAddFormState = showAddFormState,
                    loadingState = loadingState
                )
            }
        }
    }
}