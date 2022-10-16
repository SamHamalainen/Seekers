package com.example.seekers.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.seekers.general.NavRoutes
import com.example.seekers.general.QRScanner
import com.example.seekers.utils.*
import com.example.seekers.viewModels.ScannerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * This screen is used for the user to scan QR code of another players device.
 * uses https://github.com/yuriy-budiyev/code-scanner library for the scanner.
 */
@Composable
fun QrScannerScreen(
    navController: NavHostController,
    vm: ScannerViewModel = viewModel(),
    nickname: String,
    avatarId: Int,
    permissionVM: PermissionsViewModel
) {
    val context = LocalContext.current
    val cameraIsAllowed by permissionVM.cameraPerm.observeAsState(false)
    var gameId: String? by remember { mutableStateOf(null) }
    val lobby by vm.lobby.observeAsState()
    val playersInLobby by vm.playersInLobby.observeAsState()

    // When launched check all permissions
    LaunchedEffect(Unit) {
        permissionVM.checkAllPermissions(context)
    }

    // When scanner returns gameId create player of users choices on AvatarPickerScreen
    // then check if lobby is currently full. If lobby is full notify user and navigate back to StartGameScreen
    // else join lobby and add player to players collection of the game.
    LaunchedEffect(gameId) {
        gameId?.let {
            val player = Player(
                nickname = nickname,
                avatarId = avatarId,
                playerId = FirebaseHelper.uid!!,
                inLobbyStatus = InLobbyStatus.JOINED.ordinal,
                inGameStatus = InGameStatus.HIDING.ordinal
            )
            val hasLobby = withContext(Dispatchers.IO) {
                vm.getLobby(it)
            }
            val hasPlayers = withContext(Dispatchers.IO) {
                vm.getNumberOfPlayersInLobby(it)
            }
            delay(1000)
            if (hasLobby && hasPlayers) {
                if (lobby?.maxPlayers == playersInLobby) {
                    Toast.makeText(
                        context,
                        "The lobby is currently full",
                        Toast.LENGTH_LONG
                    ).show()
                    navController.navigate(NavRoutes.StartGame.route)
                } else {
                    Log.d("lobbyJoin", "lobby max: ${lobby?.maxPlayers}")
                    FirebaseHelper.addPlayer(player, it)
                    FirebaseHelper.updateUser(
                        FirebaseHelper.uid!!,
                        mapOf(Pair("currentGameId", it))
                    )
                    navController.navigate(NavRoutes.LobbyQR.route + "/$it")
                }
            }


        }
    }
    if (cameraIsAllowed) {
        QRScanner(context = context, onScanned = { gameId = it })
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "Need camera permission")
        }
    }
}


