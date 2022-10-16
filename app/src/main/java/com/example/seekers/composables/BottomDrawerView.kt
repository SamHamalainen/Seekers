package com.example.seekers.composables

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.seekers.HeatMap
import com.example.seekers.R
import com.example.seekers.general.NavRoutes
import com.example.seekers.general.secondsToText
import com.example.seekers.ui.theme.Emerald
import com.example.seekers.ui.theme.Raisin
import com.example.seekers.utils.InGameStatus
import com.example.seekers.viewModels.HeatMapViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun BottomDrawerView(
    drawerState: BottomDrawerState,
    vm: HeatMapViewModel,
    showRadarDialog: (Boolean) -> Unit,
    showQRScanner: (Boolean) -> Unit,
    showQR: (Boolean) -> Unit,
    showPlayerList: (Boolean) -> Unit,
    showLeaveGame: (Boolean) -> Unit,
    isSeeker: Boolean,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val powerCountdown by vm.powerCountdown.observeAsState()

    BottomDrawer(
        gesturesEnabled = drawerState.isOpen,
        drawerState = drawerState,
        drawerBackgroundColor = Color.Transparent,
        drawerElevation = 0.dp,
        drawerContent = {
            Surface(
                shape = RoundedCornerShape(28.dp, 28.dp, 0.dp, 0.dp),
                color = Emerald,
                border = BorderStroke(1.dp, Raisin),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = Emerald)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = { scope.launch { drawerState.close() } },
                        content = {
                            Column(
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Raisin
                                )
                            }
                        })
                    if (isSeeker) {
                        IconButton(
                            onClick = {
                                scope.launch { drawerState.close() }
                                showRadarDialog(true)
                            },
                            content = {
                                Column(
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.Radar,
                                        contentDescription = "Radar",
                                        tint = Raisin
                                    )
                                }
                            },
                        )
                    } else {
                        IconButton(
                            onClick = {
                                scope.launch { drawerState.close() }
                                vm.updateShowPowersDialog(true)
                            },
                            content = {
                                Column(
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        modifier = Modifier.size(24.dp),
                                        painter = painterResource(id = R.drawable.magic_wand),
                                        contentDescription = "Radar",
                                        tint = Raisin
                                    )
                                }
                            },
                            enabled = powerCountdown == 0
                        )

                    }

                    IconButton(
                        onClick = {
                            scope.launch { drawerState.close() }
                            if (isSeeker) {
                                showQRScanner(true)
                            } else {
                                showQR(true)
                            }
                        },
                        modifier = Modifier.size(56.dp),
                        content = {
                            Column(
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (isSeeker)
                                    Icon(
                                        Icons.Default.QrCodeScanner,
                                        contentDescription = "",
                                        tint = Raisin,
                                        modifier = Modifier.size(44.dp)
                                    )
                                else
                                    Icon(
                                        Icons.Default.QrCode,
                                        contentDescription = "",
                                        tint = Raisin,
                                        modifier = Modifier.size(44.dp)
                                    )
                            }
                        })
                    IconButton(
                        onClick = {
                            showPlayerList(true)
                        },
                        content = {
                            Column(
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.List, contentDescription = "", tint = Raisin)
                                // Text(text = "Players", color = Color.White)
                            }
                        })
                    IconButton(
                        onClick = {
                            showLeaveGame(true)
                        },
                        content = {
                            Column(
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.ExitToApp,
                                    contentDescription = "",
                                    tint = Color.Red
                                )

                            }
                        })
                }
            }
        },
        content = { content() }
    )
}

@Composable
fun BottomDrawerFAB(onClick: () -> Unit) {
    FloatingActionButton(
        elevation = FloatingActionButtonDefaults.elevation(8.dp),
        modifier = Modifier.border(
            BorderStroke(1.dp, Raisin),
            shape = CircleShape
        ),
        shape = CircleShape,
        backgroundColor = Emerald,
        contentColor = Raisin,
        onClick = onClick
    ) {
        Icon(Icons.Filled.Dashboard, "", modifier = Modifier.size(38.dp))
    }
}