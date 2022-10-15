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
    val heatPositions by vm.heatPositions.observeAsState(listOf())
    val cameraPositionState = rememberCameraPositionState()
    var minZoom by remember { mutableStateOf(17F) }
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
    val tileProvider by remember {
        derivedStateOf {
            var provider: HeatmapTileProvider? = null
            if (heatPositions.isNotEmpty()) {
                provider = HeatmapTileProvider.Builder()
                    .data(heatPositions)
                    .build()
                provider.setRadius(200)
            }
            provider
        }
    }

    var properties: MapProperties? by remember { mutableStateOf(null) }
    val uiSettings by remember {
        mutableStateOf(
            MapUiSettings(
                compassEnabled = true,
                indoorLevelPickerEnabled = true,
                mapToolbarEnabled = false,
                myLocationButtonEnabled = false,
                rotationGesturesEnabled = mapControl,
                scrollGesturesEnabled = mapControl,
                scrollGesturesEnabledDuringRotateOrZoom = mapControl,
                tiltGesturesEnabled = mapControl,
                zoomControlsEnabled = false,
                zoomGesturesEnabled = mapControl
            )
        )
    }
    LaunchedEffect(Unit) {
        permissionVM.checkAllPermissions(context)
        vm.receiveCountdown(context)
        launch(Dispatchers.IO) {
            vm.getPlayers(gameId)
            vm.getLobby(gameId)
//            vm.getTime(gameId)
            vm.getNews(gameId)
        }
//        vm.addMockPlayers(gameId)
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
        println("playerStatus $playerStatus")
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
                    Toast.makeText(context, "You are now a seeker!", Toast.LENGTH_SHORT).show()
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

    if (!initialPosSet) {
        val density = LocalDensity.current
        val width =
            with(density) {
                LocalConfiguration.current.screenWidthDp.dp.toPx()
            }
                .times(0.5).toInt()
        LaunchedEffect(center) {
            lobby?.let {
                minZoom = getBoundsZoomLevel(
                    getBounds(
                        LatLng(
                            it.center.latitude,
                            it.center.longitude
                        ), it.radius
                    ),
                    Size(width, width)
                ).toFloat()
                properties = MapProperties(
                    mapType = MapType.SATELLITE,
                    isMyLocationEnabled = true,
                    maxZoomPreference = 17.5F,
                    minZoomPreference = minZoom,
                    latLngBoundsForCameraTarget =
                    getBounds(
                        LatLng(
                            it.center.latitude,
                            it.center.longitude
                        ), it.radius
                    )
                )
                cameraPositionState.position = CameraPosition.fromLatLngZoom(center!!, minZoom)
                initialPosSet = true
                launch(Dispatchers.Default) {
                    circleCoords = getCircleCoords(
                        LatLng(
                            it.center.latitude,
                            it.center.longitude
                        ), it.radius
                    )
                }

            }
        }
    }

    BottomDrawer(
        gesturesEnabled = drawerState.isOpen,
        drawerState = drawerState,
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
                    if (isSeeker == true) {
                        IconButton(
                            onClick = {
                                scope.launch { drawerState.close() }
                                showRadarDialog = true
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
                            if (isSeeker == true) {
                                if (!showQRScanner) {
                                    showQRScanner = true
                                }
                            } else {
                                if (!showQR) {
                                    showQR = true
                                }
                            }
                        },
                        modifier = Modifier.size(56.dp),
                        content = {
                            Column(
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (isSeeker == true)
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
                            if (!showPlayerList) {
                                showPlayerList = true
                            }
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
                            showLeaveGameDialog = true
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
        drawerBackgroundColor = Color.Transparent,
        drawerElevation = 0.dp,
        content = {
            Scaffold(
                floatingActionButtonPosition = FabPosition.Center,
                isFloatingActionButtonDocked = true,
                floatingActionButton = {
                    if (!lobbyIsOver) {
                        FloatingActionButton(
                            elevation = FloatingActionButtonDefaults.elevation(8.dp),
                            modifier = Modifier.border(
                                BorderStroke(1.dp, Raisin),
                                shape = CircleShape
                            ),
                            shape = CircleShape,
                            backgroundColor = Emerald,
                            contentColor = Raisin,
                            onClick = {
                                scope.launch { drawerState.open() }
                            }
                        ) {
                            Icon(Icons.Filled.Dashboard, "", modifier = Modifier.size(38.dp))
                        }
                    } else {
                        Card(
                            backgroundColor = Color.White,
                            modifier = Modifier
                                .padding(10.dp)
                                .clickable {
                                    vm.endLobby(context)
                                    navController.navigate(NavRoutes.EndGame.route + "/$gameId")
                                }
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "The game will soon end")
                                Text(text = "Press to skip")
                                Text(text = secondsToText(lobbyEndCountdown))
                            }
                        }
                    }
                },
            ) {
                properties?.let { props ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (locationAllowed) {
                            HeatMap(
                                state = cameraPositionState,
                                center = center,
                                radius = radius,
                                properties = props,
                                uiSettings = uiSettings,
                                movingPlayers = movingPlayers,
                                seekers = currentSeekers,
                                canSeeSeeker = canSeeSeeker,
                                tileProvider = tileProvider,
                                circleCoords = circleCoords
                            )
                            Card(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(12.dp)
                                    .height(44.dp)
                                    .fillMaxWidth(),
                                backgroundColor = Emerald,
                                border = BorderStroke(1.dp, Raisin),
                                shape = RoundedCornerShape(5.dp)
                            ) {
                                Box(
                                    Modifier.padding(8.dp),
                                ) {
                                    players?.let {
                                        val total =
                                            it.count { player ->
                                                player.inGameStatus != InGameStatus.LEFT.ordinal
                                            }
                                        val hidingAmount =
                                            it.count { player ->
                                                player.inGameStatus == InGameStatus.HIDING.ordinal
                                                        || player.inGameStatus == InGameStatus.MOVING.ordinal
                                                        || player.inGameStatus == InGameStatus.INVISIBLE.ordinal
                                                        || player.inGameStatus == InGameStatus.DECOYED.ordinal
                                            }
                                        Row(
                                            modifier = Modifier.align(Alignment.CenterStart),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.PeopleAlt,
                                                contentDescription = "",
                                                tint = Raisin
                                            )
                                            Spacer(Modifier.width(2.dp))
                                            Text(
                                                text = "$hidingAmount left",
                                                color = Raisin,
                                                fontSize = 16.sp
                                            )
                                        }
                                    }

                                    Box(modifier = Modifier.align(Alignment.Center)) {
                                        GameTimer(vm = vm)
                                    }

                                    Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                                        NewsButton(onClick = {
                                            showNews = true
                                            vm.hasNewNews.value = false
                                        }, hasNew = hasNewNews)
                                    }
                                }
                            }

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
                                        }) {
                                        selfieLauncher.launch(null)
                                    }
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
                            BackHandler(enabled = true) {
                                if (drawerState.isOpen) {
                                    scope.launch { drawerState.close() }
                                } else {
                                    showLeaveGameDialog = true
                                }
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
        }
    )
}

