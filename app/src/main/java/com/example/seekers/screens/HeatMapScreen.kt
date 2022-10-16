package com.example.seekers.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.CountDownTimer
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.seekers.composables.HeatMap
import com.example.seekers.composables.*
import com.example.seekers.general.NavRoutes
import com.example.seekers.utils.*
import com.example.seekers.viewModels.HeatMapViewModel
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * HeatMapScreen: Screen where the game happens.
 * Contains:
 * - a map with the playing area and heat markers where players are hiding
 * - a bottom drawer which contains a game menu
 * - a top bar which shows the time and participants remaining, and a news button
 * - several dialogs for leaving the game, show players remaining, use powers, etc.
 */

@OptIn(ExperimentalMaterialApi::class)
@SuppressLint("MissingPermission", "UnusedMaterialScaffoldPaddingParameter")
@Composable
fun HeatMapScreen(
    vm: HeatMapViewModel = viewModel(),
    gameId: String,
    navController: NavHostController,
    permissionVM: PermissionsViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // players
    val currentSeekers by vm.currentSeekers.observeAsState(listOf())
    val players by vm.players.observeAsState()
    var playerFound: Player? by remember { mutableStateOf(null) }
    val isSeeker by vm.isSeeker.observeAsState()
    val playerStatus by vm.playerStatus.observeAsState()

    // lobby
    val lobbyStatus by vm.lobbyStatus.observeAsState()

    // map & map ui
    val cameraPositionState = rememberCameraPositionState()
    val drawerState = rememberBottomDrawerState(BottomDrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }
    var lobbyEndCountdown by remember { mutableStateOf(60) }
    var lobbyIsOver by remember { mutableStateOf(false) }
    val news by vm.news.observeAsState()

    // dialogs
    var showRadarDialog by remember { mutableStateOf(false) }
    var showLeaveGameDialog by remember { mutableStateOf(false) }
    var showQR by remember { mutableStateOf(false) }
    var showQRScanner by remember { mutableStateOf(false) }
    var showPlayerFound by remember { mutableStateOf(false) }
    var showPlayerList by remember { mutableStateOf(false) }
    var showSendSelfie by remember { mutableStateOf(false) }
    var showNews by remember { mutableStateOf(false) }

    // permissions
    val locationAllowed by permissionVM.fineLocPerm.observeAsState(false)
    val cameraIsAllowed by permissionVM.cameraPerm.observeAsState(false)

    // powers
    val powerCountdown by vm.powerCountdown.observeAsState()
    val activePower by vm.activePower.observeAsState()
    val showPowers by vm.showPowersDialog.observeAsState(false)
    val showJammer by vm.showJammer.observeAsState(false)

    // selfie
    var selfie: Bitmap? by remember { mutableStateOf(null) }
    val selfieLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.TakePicturePreview()) {
            it?.let {
                selfie = it
                showSendSelfie = true
            }
        }

    // Check permissions on launch, start the step counter and get all the Firebase listeners required
    LaunchedEffect(Unit) {
        permissionVM.checkAllPermissions(context)
        vm.startStepCounter()
        launch(Dispatchers.IO) {
            vm.getPlayers(gameId)
            vm.getLobby(gameId)
            vm.getNews(gameId)
        }
    }

    // Check the current player's status.
    // On launch, check if the current player is a seeker, then start the game foreground service.
    // If the player is eliminated, stop the game service, turn the player into a seeker and restart the service
    // If the player is jammed, hide the map until the player status is seeker again.
    LaunchedEffect(playerStatus) {
        playerStatus?.let {
            if (isSeeker == null) {
                val thisPlayerIsSeeker = (it == InGameStatus.SEEKER.ordinal || it == InGameStatus.JAMMED.ordinal)
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
                    vm.updateInGameStatus(
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

    // When the lobby status is finished, stop the game foreground service and wait 60s before navigation to the end screen
    LaunchedEffect(lobbyStatus) {
        lobbyStatus?.let {
            when (it) {
                LobbyStatus.FINISHED.ordinal -> {
                    vm.endLobby(context)
                    lobbyIsOver = true
                    object : CountDownTimer(60 * 1000, 1000) {
                        override fun onTick(p0: Long) {
                            lobbyEndCountdown = p0.div(1000).toInt()
                        }
                        override fun onFinish() {
                            navController.navigate(NavRoutes.EndGame.route + "/$gameId")
                        }
                    }.start()
                }
            }
        }
    }

    // When the last hiding player has been found, end the game
    LaunchedEffect(currentSeekers) {
        currentSeekers?.let {
            val activePlayers = players?.count { it.inGameStatus != InGameStatus.LEFT.ordinal }
            if (currentSeekers.size == activePlayers && activePlayers > 1) {
                vm.setLobbyFinished(gameId)
                drawerState.close()
            }
        }
    }

    // Bottom drawer with game menu
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

        // Game UI with map and other widgets
        Scaffold(
            floatingActionButtonPosition = FabPosition.Center,
            isFloatingActionButtonDocked = true,
            // During game: FAB to open the bottom drawer menu
            // After the game, button to skip to the end screen
            floatingActionButton = {
                if (!lobbyIsOver) {
                    BottomDrawerFAB {
                        scope.launch { drawerState.open() }
                    }
                } else {
                    EndTimerSkip(lobbyEndCountdown = lobbyEndCountdown) {
                        navController.navigate(NavRoutes.EndGame.route + "/$gameId")
                    }
                }
            }
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {

                GameTopBar(modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(12.dp)
                    .height(44.dp)
                    .fillMaxWidth(), showNews = { showNews = true },
                    vm = vm
                )

                if (locationAllowed) {

                    HeatMap(
                        state = cameraPositionState,
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

