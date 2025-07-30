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
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matrixwarez.pt.R
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

    Scaffold(
        modifier = windowInsetMod,
        bottomBar = {
            NavigationBar(
                containerColor = Color.Black,
                contentColor = Color.White
            ) {
                NavigationBarItem(
                    selected = pagerState.targetPage == 0,
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_globe),
                            tint = Color.White,
                            contentDescription = "World Spaces"
                        )
                    },
                    label = {
                        Text("World", color = Color.White)
                    },
                    onClick = {
                        coroutineScope.launch {
                            pagerState.scrollToPage(0)
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = colorResource(R.color.colorAccent)
                    )
                )

                NavigationBarItem(
                    selected = pagerState.targetPage == 1,
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_group),
                            tint = Color.White,
                            contentDescription = "Group Spaces"
                        )
                    },
                    label = {
                        Text("Group", color = Color.White)
                    },
                    onClick = {
                        coroutineScope.launch {
                            pagerState.scrollToPage(1)
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = colorResource(R.color.colorAccent)
                    )
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .shadow(2.dp)
                .then(sizeMod)
                .background(Color.Black),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))
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
                        fontSize = 24.sp
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
    }

    LaunchedEffect(Unit) {
        loadingState.value = true
    }
}