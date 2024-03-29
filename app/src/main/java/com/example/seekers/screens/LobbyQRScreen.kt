package com.example.seekers.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.seekers.composables.DismissLobbyDialog
import com.example.seekers.composables.EditRulesDialog
import com.example.seekers.composables.LeaveGameDialog
import com.example.seekers.composables.QRDialog
import com.example.seekers.general.CustomButton
import com.example.seekers.general.NavRoutes
import com.example.seekers.general.generateQRCode
import com.example.seekers.general.getAvatarId
import com.example.seekers.ui.theme.SizzlingRed
import com.example.seekers.ui.theme.avatarBackground
import com.example.seekers.utils.*
import com.example.seekers.viewModels.LobbyCreationScreenViewModel
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * This screen shows the lobby and players inside as list. You are able to show QR code to
 * others
 */
@Composable
fun LobbyQRScreen(
    navController: NavHostController,
    vm: LobbyCreationScreenViewModel = viewModel(),
    gameId: String,
    permissionVM: PermissionsViewModel,
) {
    val context = LocalContext.current
    val bitmapQR = generateQRCode(gameId)
    val playersInLobby by vm.playersInLobby.observeAsState(listOf())
    val lobby by vm.lobby.observeAsState()
    val isCreator by vm.isCreator.observeAsState()
    val scope = rememberCoroutineScope()
    // Dialogs
    val showLeaveDialog by vm.showLeaveGameDialog.observeAsState()
    val showDismissDialog by vm.showDismissLobbyDialog.observeAsState()
    val showEditRulesDialog by vm.showEditRulesDialog.observeAsState()
    val showQRDialog by vm.showQRDialog.observeAsState()

    // When launched check permissions and fetch lobby details and current user
    LaunchedEffect(Unit) {
        permissionVM.checkAllPermissions(context)
        scope.launch(Dispatchers.IO) {
            vm.getPlayersInLobby(gameId)
            vm.getLobby(gameId)
            vm.getPlayer(gameId, FirebaseHelper.uid!!)
        }
    }

    // When lobby has been fetched check its status and do action based on it
    // Deleted -> lobby closed, update user to not be in a game anymore, navigate back to StartGameScreen
    // Countdown -> navigate to CountdownScreen after small delay
    // Active -> Navigate to HeatMapScreen
    LaunchedEffect(lobby) {
        lobby?.let {
            when (it.status) {
                LobbyStatus.DELETED.ordinal -> {
                    if (isCreator != true) {
                        Toast.makeText(
                            context,
                            "The lobby was closed by the host",
                            Toast.LENGTH_LONG
                        )
                            .show()
                    }
                    vm.updateUser(FirebaseHelper.uid!!, mapOf(Pair("currentGameId", "")))
                    navController.navigate(NavRoutes.StartGame.route)
                }
                LobbyStatus.COUNTDOWN.ordinal -> {
                    scope.launch {
                        delay(1000)
                        navController.navigate(NavRoutes.Countdown.route + "/$gameId")
                    }
                }
                LobbyStatus.ACTIVE.ordinal -> {
                    navController.navigate(NavRoutes.Heatmap.route + "/$gameId")
                }
            }
        }
    }

    // Checking if current user is in the list of players in lobby
    // When player has been kicked out of the lobby they will be notified and redirected to StartGameScreen.
    LaunchedEffect(playersInLobby) {
        if (playersInLobby.isNotEmpty()) {
            val currentPlayer = playersInLobby.find { it.playerId == FirebaseHelper.uid!! }
            if (currentPlayer == null) {
                Toast.makeText(context, "You were kicked from the lobby", Toast.LENGTH_LONG).show()
                vm.updateUser(FirebaseHelper.uid!!, mapOf(Pair("currentGameId", "")))
                navController.navigate(NavRoutes.StartGame.route)
            }
        }
    }

    Scaffold(topBar = {
        TopAppBar(
            backgroundColor = Color.Transparent,
            elevation = 0.dp
        ) {
            TopAppBarLobby(vm = vm)
        }
    }
    ) {
        Column(
            Modifier.padding(it),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Participants", fontSize = 20.sp, modifier = Modifier.padding(15.dp))
            Participants(
                modifier = Modifier
                    .weight(3f)
                    .padding(horizontal = 15.dp),
                players = playersInLobby,
                isCreator = isCreator == true,
                vm = vm,
                gameId = gameId
            )
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Column {
                    CustomButton(text = "${if (isCreator == true) "Edit" else "Check"} Rules") {
                        vm.updateShowDialog(editRules = true)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    if (isCreator == true) {
                        CustomButton(text = "Start Game") {
                            vm.updateLobby(
                                mapOf(
                                    Pair("status", LobbyStatus.COUNTDOWN.ordinal),
                                    Pair("startTime", FieldValue.serverTimestamp())
                                ),
                                gameId
                            )
                        }
                    }
                }
            }
        }
        if (showQRDialog == true) {
            QRDialog(
                bitmap = bitmapQR,
                onDismissRequest = { vm.updateShowDialog(qrDialog = false) }
            )
        }
        if (showEditRulesDialog == true) {
            EditRulesDialog(
                vm,
                gameId,
                isCreator == true,
                onDismissRequest = { vm.updateShowDialog(editRules = false) })
        }
        if (showLeaveDialog == true) {
            LeaveGameDialog(
                onDismissRequest = { vm.updateShowDialog(leaveGame = false) },
                onConfirm = {
                    vm.removePlayer(gameId, FirebaseHelper.uid!!)
                    vm.updateUser(
                        FirebaseHelper.uid!!,
                        mapOf(Pair("currentGameId", ""))
                    )
                    navController.navigate(NavRoutes.StartGame.route)
                })
        }
        if (showDismissDialog == true) {
            DismissLobbyDialog(
                onDismissRequest = { vm.updateShowDialog(dismissLobby = false) },
                onConfirm = {
                    val changeMap = mapOf(
                        Pair("status", LobbyStatus.DELETED.ordinal)
                    )
                    vm.updateUser(
                        FirebaseHelper.uid!!,
                        mapOf(Pair("currentGameId", ""))
                    )
                    vm.updateLobby(changeMap, gameId)
                })
        }
        // When user presses system back button show correct dialog
        // prevents user from navigating back to lobby creation
        BackHandler(enabled = true) {
            if (isCreator == true) {
                vm.updateShowDialog(dismissLobby = true)
            } else {
                vm.updateShowDialog(leaveGame = true)
            }
        }
    }

}

// Custom TopAppBar for lobby
@Composable
fun TopAppBarLobby(vm: LobbyCreationScreenViewModel) {
    val isCreator by vm.isCreator.observeAsState()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        IconButton(modifier = Modifier.align(Alignment.CenterStart), onClick = {
            vm.updateShowDialog(qrDialog = true)
        }) {
            Icon(Icons.Outlined.QrCode2, "QR", modifier = Modifier.size(40.dp))
        }
        Text(
            text = "Scan QR to join!",
            fontSize = 20.sp,
            modifier = Modifier.align(Alignment.Center)
        )
        Card(
            backgroundColor = SizzlingRed,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .clickable {
                    if (isCreator == true) {
                        vm.updateShowDialog(dismissLobby = true)
                    } else {
                        vm.updateShowDialog(leaveGame = true)
                    }
                }) {
            Icon(
                imageVector = Icons.Filled.Logout,
                contentDescription = "Leave",
                tint = Color.White,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

// List of Participants
@Composable
fun Participants(
    modifier: Modifier = Modifier,
    players: List<Player>,
    isCreator: Boolean,
    vm: LobbyCreationScreenViewModel,
    gameId: String
) {
    // remember PlayerCards index for kicking correct person
    var kickableIndex: Int? by remember { mutableStateOf(null) }
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
        }
        // List of players sorted that host is always on top
        itemsIndexed(players.sortedBy { it.inLobbyStatus }) { index, player ->
            PlayerCard(
                player = player,
                isCreator = isCreator,
                vm = vm,
                gameId = gameId,
                setKickableIndex = { kickableIndex = index },
                isKickable = kickableIndex == index
            )
        }
    }

}

// Individual PlayerCard shown in lobby with avatar and nickname
@Composable
fun PlayerCard(
    player: Player,
    isCreator: Boolean,
    vm: LobbyCreationScreenViewModel,
    gameId: String,
    setKickableIndex: () -> Unit,
    isKickable: Boolean
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        elevation = 10.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .clickable {
                    if (isCreator) {
                        setKickableIndex()
                    }
                }
        ) {
            Card(
                shape = CircleShape,
                border = BorderStroke(2.dp, Color.Black),
                backgroundColor = avatarBackground,
                modifier = Modifier
                    .padding(10.dp)
            ) {
                Image(
                    painter = painterResource(id = getAvatarId(player.avatarId)),
                    contentDescription = "avatar",
                    modifier = Modifier
                        .size(50.dp)
                        .padding(10.dp)
                )
            }
            Text(
                text = "${player.nickname} ${if (player.inLobbyStatus == InLobbyStatus.CREATOR.ordinal) "(Host)" else ""}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width((screenWidth * 0.5).dp)
            )
            if (isKickable && player.inLobbyStatus == InLobbyStatus.JOINED.ordinal) {
                Button(
                    onClick = {
                        vm.removePlayer(gameId = gameId, player.playerId)
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = SizzlingRed,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .padding(10.dp)
                ) {
                    Text(text = "Kick")
                }
            } else {
                Button(
                    modifier = Modifier
                        .padding(10.dp)
                        .alpha(0f),
                    onClick = {},
                ) {
                    Text(text = "Kick")
                }
            }
        }
    }
}