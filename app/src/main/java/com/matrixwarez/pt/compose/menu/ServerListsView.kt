package com.matrixwarez.pt.compose.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matrixwarez.pt.compose.Inter
import com.matrixwarez.pt.model.Server
import com.matrixwarez.pt.service.ServerService
import kotlinx.coroutines.launch


@Composable
fun ServerListsView(serverService: ServerService, publicServerListState: MutableState<List<Server>>,
                    privateServerListState: MutableState<List<Server>>,
                    loadingState: MutableState<Boolean>, refreshingState: MutableState<Boolean>,
                    portraitState: MutableState<Boolean>,
                    onSelectServer: (Server) -> Unit, onRefreshServerList: (Boolean) -> Unit) {

    val coroutineScope = rememberCoroutineScope()

    val pagerState = rememberPagerState {
        2
    }

    val showAddFormState = remember { mutableStateOf(false) }

    val isPortrait by portraitState

    val sizeMod = when (isPortrait) {
        true -> Modifier.fillMaxSize()
        false -> Modifier
            .fillMaxHeight()
            .aspectRatio(11/12f)
    }

    val windowInsetMod = when (isPortrait) {
        true -> Modifier.windowInsetsPadding(WindowInsets.systemBars)
        false -> Modifier
    }

    Column(
        modifier = Modifier
            .shadow(2.dp)
            .then(sizeMod)
            .then(windowInsetMod)
            .background(Color.Black),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .border(1.dp, Color.White)
                    .padding(5.dp)
                    .background(Color(android.graphics.Color.parseColor("#FF4D00")))
                    .padding(5.dp),
            ) {
                Text(
                    text = "PIXELS: TOGETHER",
                    color = Color.White,
                    fontFamily = Inter,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                )
            }

            if (pagerState.targetPage == 1) {

                if (!showAddFormState.value) {
                    Button(
                        modifier = Modifier.height(30.dp).align(Alignment.CenterEnd).padding(end = 20.dp),
                        onClick = {
                            showAddFormState.value = true
                        },
                        shape = RoundedCornerShape(5.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0.15f, 0.15f, 0.15f),
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 15.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "Add",
                            fontFamily = Inter,
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .width(180.dp)
                .height(40.dp)
                .clip(RoundedCornerShape(50))
                .border(1.dp, Color.White, RoundedCornerShape(50))
        ) {
            Button(
                modifier = Modifier.weight(0.5f).height(40.dp),
                shape = RectangleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (pagerState.targetPage == 0) {
                        true -> Color.White
                        false -> Color.Black
                    },
                    contentColor = when (pagerState.targetPage == 0) {
                        true -> Color.Black
                        false -> Color.White
                    }
                ),
                onClick = {
                    coroutineScope.launch {
                        pagerState.scrollToPage(0)
                    }
                }
            ) {
                Text(
                    text = "World",
                    fontFamily = Inter,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp
                )
            }
            Button(
                modifier = Modifier.weight(0.5f).height(40.dp),
                shape = RectangleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (pagerState.targetPage == 1) {
                        true -> Color.White
                        false -> Color.Black
                    },
                    contentColor = when (pagerState.targetPage == 1) {
                        true -> Color.Black
                        false -> Color.White
                    }
                ),
                onClick = {
                    coroutineScope.launch {
                        pagerState.scrollToPage(1)
                    }
                }
            ) {
                Text(
                    text = "Private",
                    fontFamily = Inter,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        HorizontalPager(
            state = pagerState,
            userScrollEnabled = false
        ) { page ->
            when (page == 0) {
                true -> PublicServerListView(
                    serverListState = publicServerListState,
                    onSelectServer = onSelectServer,
                    loadingState = loadingState,
                    refreshingState = refreshingState,
                    onRefreshServerList = onRefreshServerList
                )
                false -> PrivateServerListView(
                    serverService = serverService,
                    onSelectServer = onSelectServer,
                    privateServerListState = privateServerListState,
                    showAddFormState = showAddFormState,
                    loadingState = loadingState,
                    refreshingState = refreshingState,
                    onRefreshServerList = onRefreshServerList
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        loadingState.value = true
    }
}