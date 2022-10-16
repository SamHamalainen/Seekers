package com.example.seekers.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.CountDownTimer
import android.util.Size
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.seekers.*
import com.example.seekers.R
import com.example.seekers.composables.*
import com.example.seekers.general.*
import com.example.seekers.ui.theme.Emerald
import com.example.seekers.ui.theme.Raisin
import com.example.seekers.utils.*
import com.example.seekers.viewModels.HeatMapViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.heatmaps.HeatmapTileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@SuppressLint("MissingPermission", "UnusedMaterialScaffoldPaddingParameter")
@Composable
fun HeatMapScreen(
    vm: HeatMapViewModel = viewModel(),
    mapControl: Boolean,
    gameId: String,
    navController: NavHostController,
    permissionVM: PermissionsViewModel
) {
    val context = LocalContext.current
    // player
    val movingPlayers by vm.movingPlayers.observeAsState(listOf())
    val currentSeekers by vm.currentSeekers.observeAsState(listOf())
    val players by vm.players.observeAsState()
    var playerFound: Player? by remember { mutableStateOf(null) }
    val canSeeSeeker by vm.canSeeSeeker.observeAsState(false)

    // lobby
    val lobby by vm.lobby.observeAsState()
    val lobbyStatus by vm.lobbyStatus.observeAsState()

    // map
    val radius by vm.radius.observeAsState()
    var initialPosSet by remember { mutableStateOf(false) }
    val center by vm.center.observeAsState()
    val cameraPositionState = rememberCameraPositionState()
    var circleCoords by remember { mutableStateOf(listOf<LatLng>()) }

    // dialogs
    var showRadarDialog by remember { mutableStateOf(false) }
    var showLeaveGameDialog by remember { mutableStateOf(false) }
    var showQR by remember { mutableStateOf(false) }
    var showQRScanner by remember { mutableStateOf(false) }
    var showPlayerFound by remember { mutableStateOf(false) }
    var showPlayerList by remember { mutableStateOf(false) }
    var showSendSelfie by remember { mutableStateOf(false) }
    var showNews by remember { mutableStateOf(false) }
    val showPowers by vm.showPowersDialog.observeAsState(false)
    val showJammer by vm.showJammer.observeAsState(false)

    // permissions
    val locationAllowed by permissionVM.fineLocPerm.observeAsState(false)
    val cameraIsAllowed by permissionVM.cameraPerm.observeAsState(false)

    // powers
    val powerCountdown by vm.powerCountdown.observeAsState()
    val activePower by vm.activePower.observeAsState()

    // Other
    val scope = rememberCoroutineScope()
    val drawerState = rememberBottomDrawerState(BottomDrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }
    var lobbyEndCountdown by remember { mutableStateOf(60) }
    val isSeeker by vm.isSeeker.observeAsState()
    val playerStatus by vm.playerStatus.observeAsState()
    val news by vm.news.observeAsState()
    val hasNewNews by vm.hasNewNews.observeAsState(false)
    var selfie: Bitmap? by remember { mutableStateOf(null) }
    var lobbyIsOver by remember { mutableStateOf(false) }
    val selfieLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.TakePicturePreview()) {
            it?.let {
                selfie = it
                showSendSelfie = true
            }
        }

    var properties: MapProperties? by remember { mutableStateOf(null) }

    LaunchedEffect(Unit) {
        permissionVM.checkAllPermissions(context)
        vm.receiveCountdown(context)
        launch(Dispatchers.IO) {
            vm.getPlayers(gameId)
            vm.getLobby(gameId)
            vm.getNews(gameId)
        }
    }

    LaunchedEffect(currentSeekers) {
        currentSeekers?.let {
            val activePlayers = players?.count { it.inGameStatus != InGameStatus.LEFT.ordinal }
            if (currentSeekers.size == activePlayers && activePlayers > 1) {
                vm.setLobbyFinished(gameId)
                drawerState.close()
            }
        }
    }

    LaunchedEffect(lobbyStatus) {
        lobbyStatus?.let {
            when (it) {
                LobbyStatus.ACTIVE.ordinal -> {
                    vm.startStepCounter()
                }
                LobbyStatus.FINISHED.ordinal -> {
                    vm.unregisterReceiver(context)
                    lobbyIsOver = true
                    object : CountDownTimer(60 * 1000, 1000) {
                        override fun onTick(p0: Long) {
                            lobbyEndCountdown = p0.div(1000).toInt()
                        }

                        override fun onFinish() {
                            vm.endLobby(context = context)
                            navController.navigate(NavRoutes.EndGame.route + "/$gameId")
                        }
                    }.start()
                }
            }
        }
    }

    LaunchedEffect(playerStatus) {
        playerStatus?.let {
            if (isSeeker == null) {
                val thisPlayerIsSeeker = (it == InGameStatus.SEEKER.ordinal)
                vm.updateIsSeeker(thisPlayerIsSeeker)
                if (it != InGameStatus.ELIMINATED.ordinal) {
                    vm.startService(
                        context = context,
                        gameId = gameId,
                        isSeeker = thisPlayerIsSeeker
                    )
                }
                return@LaunchedEffect
            }
            when (it) {
                InGameStatus.ELIMINATED.ordinal -> {
                    showQR = false
                    vm.stopService(context)
                    vm.setPlayerInGameStatus(
                        InGameStatus.SEEKER.ordinal,
                        gameId,
                        FirebaseHelper.uid!!
                    )
                    vm.updateIsSeeker(true)
                    vm.startService(context = context, gameId = gameId, isSeeker = true)
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            "You are now a seeker! Let's find the other players!",
                            actionLabel = "Dismiss"
                        )
                    }
                }
                InGameStatus.JAMMED.ordinal -> {
                    vm.updateShowJammer(true)
                }
                InGameStatus.SEEKER.ordinal -> {
                    vm.updateShowJammer(false)
                }
            }
        }
    }

//    if (!initialPosSet) {
//        val density = LocalDensity.current
//        val width = with(density) {
//                LocalConfiguration.current.screenWidthDp.dp.toPx()
//            }.times(0.5).toInt()
//
//        LaunchedEffect(center) {
//            lobby?.let {
//                val center = LatLng(
//                    it.center.latitude,
//                    it.center.longitude
//                )
//                val radius = it.radius
//
//                val mapBounds = getBounds(center, radius)
//
//                val minZoom = getBoundsZoomLevel(
//                    mapBounds,
//                    Size(width, width)
//                ).toFloat()
//
//                properties = MapProperties(
//                    mapType = MapType.SATELLITE,
//                    isMyLocationEnabled = true,
//                    maxZoomPreference = 17.5F,
//                    minZoomPreference = minZoom,
//                    latLngBoundsForCameraTarget = mapBounds
//                )
//                cameraPositionState.position = CameraPosition.fromLatLngZoom(center, minZoom)
//
//                circleCoords = getCircleCoords(center, radius)
//
//                initialPosSet = true
//            }
//        }
//    }

    BottomDrawerView(
        vm = vm,
        drawerState = drawerState,
        isSeeker = isSeeker == true,
        showQRScanner = { showQRScanner = true },
        showQR = { showQR = true },
        showRadarDialog = { showRadarDialog = true },
        showLeaveGame = { showLeaveGameDialog = true },
        showPlayerList = { showPlayerList = true }
    ) {
        Scaffold(
            floatingActionButtonPosition = FabPosition.Center,
            isFloatingActionButtonDocked = true,
            floatingActionButton = {
                if (!lobbyIsOver) {
                    BottomDrawerFAB {
                        scope.launch { drawerState.open() }
                    }
                } else {
                    EndTimerSkip(lobbyEndCountdown = lobbyEndCountdown) {
                        vm.endLobby(context)
                        navController.navigate(NavRoutes.EndGame.route + "/$gameId")
                    }
                }
            },
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (locationAllowed) {
                    HeatMap(
                        state = cameraPositionState,
//                            center = center,
//                            radius = radius,
//                            properties = props,
//                            movingPlayers = movingPlayers,
//                            seekers = currentSeekers,
//                            canSeeSeeker = canSeeSeeker,
//                            circleCoords = circleCoords,
                        vm = vm
                    )

                    GameTopBar(modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(12.dp)
                        .height(44.dp)
                        .fillMaxWidth(), showNews = { showNews = true },
                        vm = vm
                    )

                    if (showRadarDialog) {
                        RadarDialog(
                            gameId = gameId,
                            onDismiss = { showRadarDialog = false }
                        )
                    }

                    if (showQR) {
                        ShowMyQRDialog(onDismiss = { showQR = false })
                    }

                    if (showJammer) Jammer()

                    if (showQRScanner && cameraIsAllowed) {
                        QRScannerDialog(onDismiss = { showQRScanner = false }) { id ->
                            vm.setPlayerFound(gameId, id)
                            players?.let {
                                val found = it.find { player -> player.playerId == id }
                                playerFound = found
                            }
                            showQRScanner = false
                            showPlayerFound = true
                        }
                    }

                    if (showPlayerFound) {
                        PlayerFoundDialog(playerFound = playerFound, onCancel = {
                            val nickname = playerFound?.nickname
                            vm.addFoundNews(
                                gameId,
                                nickname.toString(),
                                playerFound?.playerId.toString()
                            )
                            playerFound = null
                            showPlayerFound = false
                        }) {
                            selfieLauncher.launch(null)
                            showPlayerFound = false
                        }
                    }

                    if (showLeaveGameDialog) {
                        LeaveGameDialog(
                            onDismissRequest = { showLeaveGameDialog = false },
                            onConfirm = {
                                vm.leaveGame(gameId, context, navController)
                            })
                    }

                    selfie?.let {
                        if (showSendSelfie) {
                            SendSelfieDialog(
                                selfie = it,
                                onDismiss = {
                                    playerFound = null
                                    selfie = null
                                    showSendSelfie = false
                                },
                                sendPic = {
                                    vm.sendSelfie(
                                        playerFound!!.playerId,
                                        gameId,
                                        it,
                                        playerFound!!.nickname
                                    )
                                    playerFound = null
                                    selfie = null
                                    showSendSelfie = false
                                },
                                takeNew = {
                                    selfieLauncher.launch(null)
                                }
                            )
                        }
                    }

                    if (showNews && news != null) {
                        NewsDialog(newsList = news!!, gameId = gameId) {
                            showNews = false
                        }
                    }

                    if (showPlayerList && players != null) {
                        PlayerListDialog(
                            onDismiss = { showPlayerList = false },
                            players = players!!
                        )
                    }
                    if (showPowers) {
                        PowersDialog(
                            onDismiss = { vm.updateShowPowersDialog(false) },
                            vm = vm,
                            gameId
                        )
                    }
                } else {
                    Text(text = "Location permission needed")
                }
                if (activePower != null && powerCountdown != null) {
                    PowerActiveIndicator(
                        power = activePower!!,
                        countdown = powerCountdown!!,
                        modifier = Modifier
                            .align(
                                Alignment.TopStart
                            )
                            .padding(vertical = 64.dp)
                            .padding(horizontal = 12.dp)
                    )
                }
            }
        }
    }

    SnackbarHost(hostState = snackbarHostState)
    BackHandler(enabled = true) {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else {
            showLeaveGameDialog = true
        }
    }
}

