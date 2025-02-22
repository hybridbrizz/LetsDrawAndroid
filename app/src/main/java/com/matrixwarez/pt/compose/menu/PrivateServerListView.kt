package com.matrixwarez.pt.compose.menu

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.matrixwarez.pt.compose.Inter
import com.matrixwarez.pt.model.Server
import com.matrixwarez.pt.model.SessionSettings
import com.matrixwarez.pt.service.ServerService


@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MutableCollectionMutableState")
@Composable
fun PrivateServerListView(serverService: ServerService,
                          privateServerListState: MutableState<List<Server>>,
                          loadingState: MutableState<Boolean>,
                          showAddFormState: MutableState<Boolean>,
                          onSelectServer: (Server) -> Unit) {

    val context = LocalContext.current

    val privateAndAdminServerList by privateServerListState
    val adminServerList = privateAndAdminServerList.filter { it.isAdmin }
    val privateServerList = privateAndAdminServerList.filter { !it.isAdmin }

    var isLoading by loadingState

    var showAddForm by showAddFormState

    var keyInput by remember { mutableStateOf("") }

    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var serverToRemove by remember { mutableStateOf<Server?>(null) }

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
            if (showAddForm) {
                item {
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
                                    Text("Access Key", fontFamily = Inter)
                                },
                                colors = TextFieldDefaults.colors(
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedContainerColor = Color.Transparent,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedPlaceholderColor = Color.White.copy(alpha = 0.5f),
                                    unfocusedPlaceholderColor = Color.White.copy(alpha = 0.5f),
                                    focusedIndicatorColor = Color.Blue,
                                    cursorColor = Color.Blue
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

                            val buttonColor = when (keyInput.isNotBlank()) {
                                true -> Color.Blue
                                false -> Color.Gray
                            }

                            Button(
                                onClick = {
                                    val trimmedInput = keyInput.uppercase().trim()
                                    if (!isLoading && !SessionSettings.instance.hasServer(trimmedInput)) {
                                        showAddForm = false
                                        isLoading = true
                                        serverService.getPrivateServer(trimmedInput) { _, server ->
                                            isLoading = false
                                            server?.let {
                                                SessionSettings.instance.addServer(context, server)
                                                privateServerListState.value =
                                                    SessionSettings.instance.servers.sortedBy { -it.lastVisited }
                                            }
                                        }
                                    }
                                },
                                shape = RectangleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = buttonColor
                                )
                            ) {
                                Text("Add", fontFamily = Inter)
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
                        },
                        onLongClick = {
                            serverToRemove = it
                            showDeleteConfirmation = true
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
                    },
                    onLongClick = {
                        serverToRemove = it
                        showDeleteConfirmation = true
                    }
                )
            }
        }

        if (showDeleteConfirmation) {
            AlertDialog(
                containerColor = Color.Black,
                textContentColor = Color.White,
                text = {
                    Text("Remove ${serverToRemove?.name ?: "{ERROR}"} from your private server list?")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            serverToRemove?.let { serverToRemove ->
                                SessionSettings.instance.removeServer(context, serverToRemove, true)
                                privateServerListState.value =
                                    SessionSettings.instance.servers.sortedBy { -it.lastVisited }
                            }
                            showDeleteConfirmation = false
                        }
                    ) {
                        Text("Remove")
                    }
                },
                dismissButton = {
                    Button(
                        onClick = {
                            showDeleteConfirmation = false
                        }
                    ) {
                        Text("Cancel")
                    }
                },
                onDismissRequest = {
                    showDeleteConfirmation = false
                }
            )
        }

        if (isLoading) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 2.dp
            )
        }
    }

    LaunchedEffect(showAddForm) {
        if (!showAddForm) {
            keyInput = ""
        }
    }
}