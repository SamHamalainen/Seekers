package com.example.seekers

import android.annotation.SuppressLint
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.SharedPreferences
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.CountDownTimer
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.seekers.general.*
import com.example.seekers.general.QRCodeComponent
import com.example.seekers.ui.theme.Powder
import com.example.seekers.ui.theme.Raisin
import com.example.seekers.ui.theme.Emerald
import com.example.seekers.ui.theme.Mango
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ktx.toObject
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.heatmaps.HeatmapTileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.*

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
                                RadarDialog(gameId = gameId) { showRadarDialog = false }
                            }

                            if (showQR) {
                                ShowMyQRDialog {
                                    showQR = false
                                }
                            }

                            if (showJammer) {
                                Jammer()
                            }

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

@Composable
fun Jammer() {
    Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Text(text = "You have been jammed", modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
fun RadarDialog(
    gameId: String,
    onDismiss: () -> Unit
) {
    val height = LocalConfiguration.current.screenHeightDp * 0.8
    Dialog(onDismissRequest = { onDismiss() }) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Powder,
            modifier = Modifier.height(height.dp)
        ) {
            RadarScreen(gameId = gameId)
        }
    }
}

@Composable
fun NewsButton(modifier: Modifier = Modifier, onClick: () -> Unit, hasNew: Boolean) {
    Box {
        IconButton(
            onClick = { onClick() },
            content = {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = "news",
                        tint = Raisin
                    )
                }
            })
        if (hasNew) {
            Surface(
                color = Color.Red, shape = CircleShape, modifier = Modifier
                    .size(8.dp)
                    .align(
                        Alignment.TopEnd
                    )
            ) {}
        }
    }
}

@Composable
fun NewsDialog(newsList: List<News>, gameId: String, onDismiss: () -> Unit) {
    val screenHeight = LocalConfiguration.current.screenHeightDp
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .height((screenHeight * 0.8).dp)
                .fillMaxWidth(), backgroundColor = Powder, shape = RoundedCornerShape(8.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "EVENTS",
                    fontSize = 22.sp,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
                LazyColumn(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(newsList) {
                        NewsItem(news = it, gameId = gameId)
                    }
                }
            }

        }
    }
}

@Composable
fun NewsItem(news: News, gameId: String) {
    var bitmap: Bitmap? by remember { mutableStateOf(null) }
    LaunchedEffect(Unit) {
        if (news.picId.isNotBlank()) {
            val ONE_MEGABYTE: Long = 1024 * 1024
            FirebaseHelper.getSelfieImage(gameId = gameId, news.picId)
                .getBytes(ONE_MEGABYTE)
                .addOnSuccessListener {
                    val retrievedBitmap = BitmapFactory.decodeByteArray(it, 0, it.size)
                    bitmap = retrievedBitmap
                }
        }
    }

    Card(
        shape = RoundedCornerShape(8.dp),
        backgroundColor = Color.White,
        elevation = 5.dp,
        modifier = Modifier.padding(16.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            bitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "selfie",
                    modifier = Modifier
                        .aspectRatio(it.width.toFloat() / it.height)
                        .fillMaxWidth()
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = news.text)
                Text(
                    text = "${timeStampToTimeString(news.timestamp)}",
                    style = MaterialTheme.typography.subtitle1,
                    modifier = Modifier
                        .align(
                            Alignment.TopEnd
                        )
                        .padding(4.dp)
                )
            }
        }
    }
}

fun timeStampToTimeString(timestamp: Timestamp): String? {
    val localDateTime =
        timestamp.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    return localDateTime.format(formatter)
}

@Composable
fun ShowMyQRDialog(onDismiss: () -> Unit) {
    val playerId = FirebaseHelper.uid!!
    val qrBitmap = generateQRCode(playerId)
    Dialog(onDismissRequest = onDismiss) {
        Card(backgroundColor = Color.White, shape = RoundedCornerShape(8.dp)) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                QRCodeComponent(bitmap = qrBitmap)
            }
        }
    }
}

@Composable
fun PlayerFoundDialog(playerFound: Player?, onCancel: () -> Unit, onTakePic: () -> Unit) {
    Dialog(onDismissRequest = onCancel) {
        Card(backgroundColor = Color.White, shape = RoundedCornerShape(8.dp)) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(text = "You found ${playerFound?.nickname}", fontSize = 22.sp)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CustomButton(text = "Take a selfie") {
                        onTakePic()
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    CustomButton(text = "Close") {
                        onCancel()
                    }
                }
            }
        }
    }
}

@Composable
fun SendSelfieDialog(
    selfie: Bitmap,
    onDismiss: () -> Unit,
    sendPic: () -> Unit,
    takeNew: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Card(backgroundColor = Color.White, shape = RoundedCornerShape(8.dp)) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp),

                ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Image(
                        bitmap = selfie.asImageBitmap(),
                        contentDescription = "selfie",
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .aspectRatio(selfie.width.toFloat() / selfie.height)
                            .fillMaxWidth()
                    )
                    CustomButton(text = "Cancel") {
                        onDismiss()
                    }
                    CustomButton(text = "Take another") {
                        takeNew()
                    }
                    CustomButton(text = "Send") {
                        sendPic()
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun QRScannerDialog(onDismiss: () -> Unit, onScanned: (String) -> Unit) {
    val context = LocalContext.current
    Dialog(
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = onDismiss
    ) {
        Box(Modifier.fillMaxSize()) {
            QRScanner(context = context, onScanned = onScanned)
            Button(onClick = onDismiss, modifier = Modifier.align(Alignment.BottomCenter)) {
                Text(text = "Cancel")
            }
        }
    }
}

@Composable
fun GameTimer(vm: HeatMapViewModel) {
    val countdown by vm.countdown.observeAsState()
    countdown?.let {
        Row(
            modifier = Modifier
                .border(BorderStroke(1.dp, Raisin))
                .padding(2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(Icons.Default.Alarm, contentDescription = "", tint = Raisin)
            Box(modifier = Modifier.width(90.dp), contentAlignment = Alignment.Center) {
                val timeText = secondsToText(it)
                if (timeText == "Time's up!")
                    Text(text = timeText, color = Raisin, fontSize = 16.sp)
                else
                    Text(text = timeText, color = Raisin, fontSize = 20.sp)
            }
        }
    }
}

class HeatMapViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        val TAG = "heatMapVM"
    }

    var newsCount = 0
    val news = MutableLiveData<List<News>>()
    val hasNewNews = MutableLiveData<Boolean>()
    val firestore = FirebaseHelper
    val lobby = MutableLiveData<Lobby>()
    val radius = Transformations.map(lobby) {
        it.radius
    }
    val lobbyStatus = Transformations.map(lobby) {
        it.status
    }
    val center = Transformations.map(lobby) {
        LatLng(it.center.latitude, it.center.longitude)
    }

    val players = MutableLiveData<List<Player>>()
    val currentSeekers = MutableLiveData<List<Player>>()
    val canSeeSeeker = MutableLiveData<Boolean>()
    val showPowersDialog = MutableLiveData<Boolean>()
    val powerCountdown = MutableLiveData(0)
    val activePower = MutableLiveData<Power>()
    val showJammer = MutableLiveData<Boolean>()
    val playerStatus = Transformations.map(players) { list ->
        list.find { it.playerId == firestore.uid!! }?.inGameStatus
    }
    val isSeeker = MutableLiveData<Boolean>()
    val playersWithoutSelf = Transformations.map(players) { players ->
        players.filter { it.playerId != FirebaseHelper.uid!! }
    }
    val heatPositions = Transformations.map(playersWithoutSelf) { players ->
        players.filter { it.inGameStatus == InGameStatus.HIDING.ordinal || it.inGameStatus == InGameStatus.DECOYED.ordinal }
            .map { LatLng(it.location.latitude, it.location.longitude) }
    }
    val movingPlayers = Transformations.map(playersWithoutSelf) { players ->
        players.filter { it.inGameStatus == InGameStatus.MOVING.ordinal }
    }
    val eliminatedPlayers = Transformations.map(playersWithoutSelf) { players ->
        players.filter { it.inGameStatus == InGameStatus.ELIMINATED.ordinal }
    }
    val countdown = MutableLiveData<Int>()
    var countdownReceiver: BroadcastReceiver? = null

    fun addMockPlayers(gameId: String) {
        val mockPlayers = listOf(
            Player(
                nickname = "player 1",
                avatarId = 1,
                inGameStatus = InGameStatus.HIDING.ordinal,
                location = GeoPoint(60.22338389989929, 24.756749169655805),
                playerId = "player 1",
                distanceStatus = PlayerDistance.WITHIN50.ordinal
            ),
            Player(
                nickname = "player 2",
                avatarId = 5,
                inGameStatus = InGameStatus.MOVING.ordinal,
                location = GeoPoint(60.22374887627318, 24.759200708558442),
                playerId = "player 2",
                distanceStatus = PlayerDistance.WITHIN100.ordinal
            ),
            Player(
                nickname = "player 3",
                avatarId = 1,
                inGameStatus = InGameStatus.HIDING.ordinal,
                location = GeoPoint(60.223032239987354, 24.758830563735074),
                playerId = "player 3",
                distanceStatus = PlayerDistance.WITHIN10.ordinal
            ),
            Player(
                nickname = "player 4",
                avatarId = 1,
                inGameStatus = InGameStatus.MOVING.ordinal,
                location = GeoPoint(60.224550744400226, 24.756561415035257),
                playerId = "player 4",
                distanceStatus = PlayerDistance.WITHIN50.ordinal
            ),
            Player(
                nickname = "player 5",
                avatarId = 1,
                inGameStatus = InGameStatus.ELIMINATED.ordinal,
                location = GeoPoint(60.223405212500005, 24.75958158221728),
                playerId = "player 5",
                distanceStatus = PlayerDistance.WITHIN50.ordinal
            ),
            Player(
                nickname = "player 6",
                avatarId = 1,
                inGameStatus = InGameStatus.HIDING.ordinal,
                location = GeoPoint(60.223841983003645, 24.759626485065098),
                playerId = "player 6",
                distanceStatus = PlayerDistance.WITHIN50.ordinal
            ),
            Player(
                nickname = "player 7",
                avatarId = 5,
                inGameStatus = InGameStatus.MOVING.ordinal,
                location = GeoPoint(60.22357557804847, 24.756681419911455),
                playerId = "player 7",
                distanceStatus = PlayerDistance.WITHIN100.ordinal
            ),
            Player(
                nickname = "player 8",
                avatarId = 1,
                inGameStatus = InGameStatus.HIDING.ordinal,
                location = GeoPoint(60.22314399742664, 24.757781125478843),
                playerId = "player 8",
                distanceStatus = PlayerDistance.WITHIN10.ordinal
            ),
            Player(
                nickname = "player 9",
                avatarId = 1,
                inGameStatus = InGameStatus.MOVING.ordinal,
                location = GeoPoint(60.22311735646131, 24.759814239674167),
                playerId = "player 9",
                distanceStatus = PlayerDistance.WITHIN50.ordinal
            ),
            Player(
                nickname = "player 10",
                avatarId = 1,
                inGameStatus = InGameStatus.ELIMINATED.ordinal,
                location = GeoPoint(60.223405212500005, 24.75958158221728),
                playerId = "player 10",
                distanceStatus = PlayerDistance.WITHIN50.ordinal
            ),
        )
        mockPlayers.forEach {
            firestore.addPlayer(it, gameId)
        }
    }

    fun getPlayers(gameId: String) {
        firestore.getPlayers(gameId = gameId)
            .addSnapshotListener { data, e ->
                data ?: run {
                    Log.e(TAG, "getPlayers: ", e)
                    return@addSnapshotListener
                }
                val playersFetched = data.toObjects(Player::class.java)
                val seekersFound = playersFetched.filter {
                    it.inGameStatus == InGameStatus.SEEKER.ordinal || it.inGameStatus == InGameStatus.JAMMED.ordinal
                }
                currentSeekers.postValue(seekersFound)
                players.postValue(playersFetched)
            }
    }

    fun updateIsSeeker(newVal: Boolean) {
        isSeeker.value = newVal
    }

    fun updateShowPowersDialog(newVal: Boolean) {
        showPowersDialog.value = newVal
    }

    fun updateShowJammer(newVal: Boolean) {
        showJammer.value = newVal
    }

//    fun getTime(gameId: String) {
//        val now = Timestamp.now().toDate().time.div(1000)
//        firestore.getLobby(gameId = gameId).get()
//            .addOnSuccessListener {
//                val lobby = it.toObject(Lobby::class.java)
//                lobby?.let {
//                    val startTime = lobby.startTime.toDate().time / 1000
//                    val countdown = lobby.countdown
//                    val timeLimit = lobby.timeLimit * 60
//                    val gameEndTime = (startTime + countdown + timeLimit)
//                    timeRemaining.postValue(gameEndTime.minus(now).toInt() + 1)
//                }
//            }
//    }

    fun updateCountdown(newVal: Int) {
        countdown.value = newVal
    }

    fun getLobby(gameId: String) {
        firestore.getLobby(gameId).addSnapshotListener { data, e ->
            data ?: run {
                Log.e(TAG, "getLobby: ", e)
                return@addSnapshotListener
            }
            val lobbyFetched = data.toObject(Lobby::class.java)
            lobby.postValue(lobbyFetched)
        }
    }

    fun updateUser(changeMap: Map<String, Any>, uid: String) =
        firestore.updateUser(changeMap = changeMap, userId = uid)

    fun updatePlayer(changeMap: Map<String, Any>, gameId: String, uid: String) {
        firestore.updatePlayer(changeMap, uid, gameId)
    }

    fun setPlayerInGameStatus(status: Int, gameId: String, playerId: String) {
        firestore.updatePlayerInGameStatus(
            inGameStatus = status,
            gameId = gameId,
            playerId = playerId
        )
    }

    fun setPlayerFound(gameId: String, playerId: String) {
        val changeMap = mapOf(
            Pair("inGameStatus", InGameStatus.ELIMINATED.ordinal),
            Pair("timeOfElimination", Timestamp.now()),
            Pair("foundBy", firestore.uid!!)
        )
        firestore.updatePlayer(changeMap, playerId, gameId)
    }

    fun addFoundNews(gameId: String, nickname: String, playerId: String) {
        val news = News("", "$nickname was found!", Timestamp.now())
        firestore.addFoundNews(news, gameId, playerId)
    }

    fun sendSelfie(foundPlayerId: String, gameId: String, selfie: Bitmap, nickname: String) {
        firestore.sendSelfie(foundPlayerId, gameId, selfie, nickname)
    }

    fun getNews(gameId: String) {
        firestore.getNews(gameId).addSnapshotListener { data, e ->
            data ?: kotlin.run {
                Log.e(GameService.TAG, "listenForNews: ", e)
                return@addSnapshotListener
            }
            val newsList = data.toObjects(News::class.java)
            if (newsList.size > newsCount) {
                news.value = newsList
                hasNewNews.value = true
            }
        }
    }

    fun setLobbyFinished(gameId: String) {
        val map = mapOf(
            Pair("status", LobbyStatus.FINISHED.ordinal),
            Pair("endGameTime", Timestamp.now())
        )
        firestore.updateLobby(map, gameId)
    }

    fun endLobby(context: Context) {
        stopService(context)
        stopStepCounter()
    }

    fun leaveGame(gameId: String, context: Context, navController: NavHostController) {
        updateUser(mapOf(Pair("currentGameId", "")), FirebaseHelper.uid!!)
        setPlayerInGameStatus(InGameStatus.LEFT.ordinal, gameId, firestore.uid!!)
        stopService(context)
        navController.navigate(NavRoutes.StartGame.route)
    }

    fun receiveCountdown(context: Context) {
        val countdownFilter = IntentFilter()
        countdownFilter.addAction(GameService.COUNTDOWN_TICK)
        countdownReceiver = object : BroadcastReceiver() {
            override fun onReceive(p0: Context?, p1: Intent?) {
                val countdown = p1?.getIntExtra(GameService.TIME_LEFT, 0)!!
                updateCountdown(countdown)
            }
        }
        context.registerReceiver(countdownReceiver, countdownFilter)
        println("registered")
    }

    fun unregisterReceiver(context: Context) {
        context.unregisterReceiver(countdownReceiver)
    }

    fun startService(context: Context, gameId: String, isSeeker: Boolean) {
        GameService.start(
            context = context,
            gameId = gameId,
            isSeeker = isSeeker
        )
    }

    fun stopService(context: Context) {
        GameService.stop(
            context = context,
        )
    }

    //Variables and functions for the step counter
    private var steps = 0
    private var distance = 0.0F
    private var running = false
    private val stepLength = 0.78F

    //private val context = application
    private var initialSteps = -1
    private val sensorManager: SensorManager =
        application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    val stepCounterSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val sharedPreference: SharedPreferences =
        application.getSharedPreferences("statistics", Context.MODE_PRIVATE)
    private var sharedPreferenceEditor: SharedPreferences.Editor = sharedPreference.edit()

    //https://www.geeksforgeeks.org/proximity-sensor-in-android-app-using-jetpack-compose/
    private val sensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor == stepCounterSensor) {
                if (running == true) {
                    event.values.firstOrNull()?.toInt()?.let { newSteps ->
                        if (initialSteps == -1) {
                            initialSteps = newSteps
                        }
                        val currentSteps = newSteps.minus(initialSteps)
                        steps = currentSteps
                        Log.d("steps", steps.toString())
                    }
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, p1: Int) {
            Log.d(sensor.toString(), p1.toString())
        }
    }

    fun startStepCounter() {
        running = true
        sensorManager.registerListener(
            sensorEventListener,
            stepCounterSensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    fun stopStepCounter() {
        running = false
        sensorManager.unregisterListener(sensorEventListener)
        sharedPreferenceEditor.putInt("step count", steps)
        sharedPreferenceEditor.commit()

        countDistance()
        //val value = sharedPreference.getInt("step count", 0)
        //Log.d("steps from shared preferences", value.toString())

        //Toast.makeText(context, "steps taken: $value", Toast.LENGTH_LONG).show()
        initialSteps = -1
    }

    private fun countDistance() {
        distance = stepLength.times(steps.toFloat())
        sharedPreferenceEditor.putFloat("distance moved", distance)
        sharedPreferenceEditor.commit()
    }

    fun revealSeekers() {
        val power = Power.REVEAL
        showPowersDialog.value = false
        canSeeSeeker.value = true
        activePower.value = power
        powerCountdown.value = power.duration
        object : CountDownTimer(power.duration * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                powerCountdown.value = millisUntilFinished.div(1000).toInt()
            }

            override fun onFinish() {
                canSeeSeeker.value = false
                activePower.value = null
                this.cancel()
            }
        }.start()
    }

    fun activateInvisibility(gameId: String) {
        val power = Power.INVISIBILITY
        activePower.value = power
        powerCountdown.value = power.duration
        showPowersDialog.value = false
        val changeMap = mapOf(
            Pair("inGameStatus", InGameStatus.INVISIBLE.ordinal)
        )
        firestore.updatePlayer(changeMap, FirebaseHelper.uid!!, gameId)
        object : CountDownTimer(power.duration * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                powerCountdown.value = millisUntilFinished.div(1000).toInt()
            }

            override fun onFinish() {
                val changeMap2 = mapOf(
                    Pair("inGameStatus", InGameStatus.HIDING.ordinal)
                )
                firestore.updatePlayer(changeMap2, FirebaseHelper.uid!!, gameId)
                activePower.value = null
                this.cancel()
            }
        }.start()
    }

    fun activateJammer(gameId: String) {
        val power = Power.JAMMER
        showPowersDialog.value = false
        activePower.value = power
        powerCountdown.value = power.duration
        currentSeekers.value?.forEach {
            val changeMap = mapOf(
                Pair("inGameStatus", InGameStatus.JAMMED.ordinal)
            )
            firestore.updatePlayer(changeMap, it.playerId, gameId)
        }
        object : CountDownTimer(power.duration * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                powerCountdown.value = millisUntilFinished.div(1000).toInt()
            }

            override fun onFinish() {
                currentSeekers.value?.forEach {
                    val changeMap2 = mapOf(
                        Pair("inGameStatus", InGameStatus.SEEKER.ordinal)
                    )
                    firestore.updatePlayer(changeMap2, it.playerId, gameId)
                }
                activePower.value = null
                this.cancel()
            }
        }.start()
    }

    fun deployDecoy(gameId: String) {
        val power = Power.DECOY
        activePower.value = power
        powerCountdown.value = power.duration
        showPowersDialog.value = false
        val changeMap = mapOf(
            Pair("inGameStatus", InGameStatus.DECOYED.ordinal)
        )
        firestore.updatePlayer(changeMap, FirebaseHelper.uid!!, gameId)
        object : CountDownTimer(power.duration * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                powerCountdown.value = millisUntilFinished.div(1000).toInt()
            }

            override fun onFinish() {
                val changeMap2 = mapOf(
                    Pair("inGameStatus", InGameStatus.HIDING.ordinal)
                )
                firestore.updatePlayer(changeMap2, FirebaseHelper.uid!!, gameId)
                activePower.value = null
                this.cancel()
            }
        }.start()
    }
}

