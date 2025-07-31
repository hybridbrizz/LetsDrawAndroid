package com.matrixwarez.pt.compose.menu

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matrixwarez.pt.R
import com.matrixwarez.pt.compose.Inter
import com.matrixwarez.pt.model.Server
import com.matrixwarez.pt.model.SessionSettings
import com.matrixwarez.pt.service.ServerService
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.style.TextAlign


@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MutableCollectionMutableState")
@Composable
fun PrivateServerListView(serverService: ServerService,
                          privateServerListState: MutableState<List<Server>>,
                          loadingState: MutableState<Boolean>,
                          refreshingState: MutableState<Boolean>,
                          showAddFormState: MutableState<Boolean>,
                          onRefreshServerList: (Boolean) -> Unit,
                          onSelectServer: (Server) -> Unit) {

    val context = LocalContext.current

    val privateAndAdminServerList by privateServerListState
    val adminServerList = privateAndAdminServerList.filter { it.isAdmin }
    val privateServerList = privateAndAdminServerList.filter { !it.isAdmin }

    var isLoading by loadingState
    var isRefreshing by refreshingState

    var showAddForm by showAddFormState

    var keyInput by remember { mutableStateOf("") }

    var editingAdminServers by remember { mutableStateOf(false) }
    var editingPrivateServers by remember { mutableStateOf(false) }

    var serverToRemoveState = remember { mutableStateOf<Server?>(null) }
    val showDeleteConfirmationState = remember { mutableStateOf(false) }

    val prState = rememberPullToRefreshState()

    PullToRefreshBox(
        modifier = Modifier.fillMaxSize(),
        isRefreshing = isRefreshing,
        state = prState,
        indicator = {
            Indicator(
                modifier = Modifier.align(Alignment.TopCenter),
                isRefreshing = isRefreshing,
                state = prState,
                containerColor = colorResource(R.color.colorAccent),
                color = Color.White
            )
        },
        onRefresh = {
            onRefreshServerList(false)
        }
    ) {
        val bgColor = when (privateServerList.isEmpty() && adminServerList.isEmpty()) {
            true -> {
                Color.Transparent
            }
            false -> {
                Color.Black
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {

            if (privateServerList.isEmpty() && adminServerList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Text(
                        modifier = Modifier.align(Alignment.Center).width(240.dp),
                        text = "No groups added yet. Enter GROUP to add that space now!",
                        textAlign = TextAlign.Center,
                        fontSize = 16.sp,
                        color = Color.White,
                        fontFamily = Inter,
                        fontWeight = FontWeight.Light
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                LazyVerticalGrid(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(bgColor)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {},
                    columns = GridCells.Fixed(1),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalArrangement = Arrangement.spacedBy(30.dp)
                ) {
                    if (showAddForm) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
                            ) {
                                Row(modifier = Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically) {
                                    TextField(
                                        modifier = Modifier.width(200.dp),
                                        value = keyInput,
                                        onValueChange = {
                                            keyInput = it
                                        },
                                        singleLine = true,
                                        placeholder = {
                                            Text("Group Code", fontFamily = Inter)
                                        },
                                        colors = TextFieldDefaults.colors(
                                            unfocusedContainerColor = Color.Transparent,
                                            focusedContainerColor = Color.Transparent,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedPlaceholderColor = Color.White.copy(alpha = 0.5f),
                                            unfocusedPlaceholderColor = Color.White.copy(alpha = 0.5f),
                                            focusedIndicatorColor = Color.White,
                                            cursorColor = Color.White
                                        ),
                                        textStyle = TextStyle(
                                            fontFamily = Inter,
                                            fontSize = 16.sp
                                        ),
                                        keyboardOptions = KeyboardOptions(
                                            capitalization = KeyboardCapitalization.Characters,
                                            autoCorrectEnabled = false
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))

                                    Button(
                                        modifier = Modifier.height(30.dp),
                                        onClick = {
                                            val trimmedInput = keyInput.uppercase().trim()
                                            if (!isRefreshing && !SessionSettings.instance.hasServer(trimmedInput)) {
                                                showAddForm = false
                                                isLoading = true
                                                serverService.getPrivateServer(trimmedInput) { _, server ->
                                                    isLoading = false
                                                    server?.let {
                                                        SessionSettings.instance.addServer(context, server)
                                                        onRefreshServerList(false)
                                                    }
                                                }
                                            }
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
                                IconButton(
                                    modifier = Modifier.align(Alignment.CenterEnd),
                                    onClick = {
                                        showAddForm = false
                                    }
                                ) {
                                    Image(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Close add server",
                                        colorFilter = ColorFilter.tint(Color.White)
                                    )
                                }
                            }
                        }
                    }

                    if (adminServerList.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Mod",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    fontFamily = Inter
                                )
                                Image(
                                    modifier = Modifier.size(24.dp).align(Alignment.CenterEnd).clickable {
                                        editingAdminServers = !editingAdminServers
                                    },
                                    painter = painterResource(R.drawable.edit),
                                    contentDescription = "Edit Mod Servers"
                                )
                            }
                        }
                        itemsIndexed(adminServerList) { _, server ->
                            Column(modifier = Modifier.fillMaxWidth()) {
                                ServerItemView(
                                    server = server,
                                    editing = editingAdminServers,
                                    showDeleteConfirmationState = showDeleteConfirmationState,
                                    serverToRemoveState = serverToRemoveState,
                                    onClick = {
                                        onSelectServer(server)
                                    }
                                )
                            }
                        }
                    }

                    if (privateServerList.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Groups",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    fontFamily = Inter
                                )
                                Image(
                                    modifier = Modifier.padding(end = 20.dp).size(24.dp).align(Alignment.CenterEnd).clickable {
                                        editingPrivateServers = !editingPrivateServers
                                    },
                                    painter = painterResource(R.drawable.edit),
                                    contentDescription = "Edit Private Servers"
                                )
                            }
                        }
                        itemsIndexed(privateServerList) { _, server ->
                            Column(modifier = Modifier.fillMaxWidth()) {
                                ServerItemView(
                                    server = server,
                                    editing = editingPrivateServers,
                                    showDeleteConfirmationState = showDeleteConfirmationState,
                                    serverToRemoveState = serverToRemoveState,
                                    onClick = {
                                        onSelectServer(server)
                                    }
                                )
                            }
                        }
                    }
                }

                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                }
            }

            if (showDeleteConfirmationState.value) {
                AlertDialog(
                    containerColor = Color.Black,
                    textContentColor = Color.White,
                    text = {
                        Text("Remove ${serverToRemoveState.value?.name ?: "{ERROR}"} from your private server list?")
                    },
                    dismissButton = {
                        Button(
                            onClick = {
                                showDeleteConfirmationState.value = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0.15f, 0.15f, 0.15f),
                                contentColor = Color.White
                            )
                        ) {
                            Text("Cancel", fontFamily = Inter)
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                serverToRemoveState.value?.let { serverToRemove ->
                                    SessionSettings.instance.removeServer(context, serverToRemove, true)
                                    privateServerListState.value =
                                        SessionSettings.instance.servers.sortedBy { it.id }
                                }
                                showDeleteConfirmationState.value = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Red,
                                contentColor = Color.White
                            )
                        ) {
                            Text("Remove", fontFamily = Inter)
                        }
                    },
                    onDismissRequest = {
                        showDeleteConfirmationState.value = false
                    }
                )
            }
        }
    }

    LaunchedEffect(showAddForm) {
        if (!showAddForm) {
            keyInput = ""
        }
    }
}