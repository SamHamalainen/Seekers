package com.example.seekers.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.seekers.utils.LobbyStatus
import com.example.seekers.LoginForm
import com.example.seekers.R
import com.example.seekers.general.NavRoutes
import com.example.seekers.googleRememberFirebaseAuthLauncher
import com.example.seekers.viewModels.AuthenticationViewModel
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MainScreen(vm: AuthenticationViewModel = viewModel(), navController: NavController) {
    val loggedInUser: FirebaseUser? by vm.user.observeAsState(null)
    val gameStatus by vm.gameStatus.observeAsState()
    val gameId by vm.currentGameId.observeAsState()
    val userIsInUsers by vm.userIsInUsers.observeAsState()
    var loading by remember { mutableStateOf(true) }
    val height = LocalConfiguration.current.screenHeightDp

    LaunchedEffect(Unit) {
        vm.setUser(vm.fireBaseAuth.currentUser)
        launch(Dispatchers.Default) {
            delay(3000)
            loading = false
        }
    }

    LaunchedEffect(loggedInUser) {
        loggedInUser?.let {
            vm.checkUserInUsers(it.uid)
        }
    }

    LaunchedEffect(userIsInUsers) {
        userIsInUsers?.let {
            println("inuser: $it")
            if (it) {
                vm.checkCurrentGame(loggedInUser!!.uid)
            } else {
                val changeMap = mapOf(
                    Pair("currentGameId", ""),
                    Pair("email", loggedInUser!!.email!!)
                )
                vm.addUserDoc(loggedInUser!!.uid, changeMap)
                navController.navigate(NavRoutes.StartGame.route)
            }
        }
    }

    LaunchedEffect(gameId) {
        gameId?.let {
            if (it.isBlank()) {
                navController.navigate(NavRoutes.StartGame.route)
            } else {
                vm.checkGameStatus(it)
            }
        }
    }

    LaunchedEffect(gameStatus) {
        gameStatus?.let {
            gameId ?: return@LaunchedEffect
            when (it) {
                LobbyStatus.CREATED.ordinal -> {
                    navController.navigate(NavRoutes.LobbyQR.route + "/$gameId")
                }
                LobbyStatus.COUNTDOWN.ordinal -> {
                    navController.navigate(NavRoutes.Countdown.route + "/$gameId")
                }
                LobbyStatus.ACTIVE.ordinal -> {
                    navController.navigate(NavRoutes.Heatmap.route + "/$gameId")
                }
                LobbyStatus.FINISHED.ordinal -> {
                    navController.navigate(NavRoutes.EndGame.route + "/$gameId")
                }
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxHeight()
    ) {
        if (loggedInUser == null && !loading) {
            LoginForm(
                vm = vm,
                navController = navController,
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(R.drawable.seekers_ver3),
                        contentDescription = "seekers",
                        modifier = Modifier.height((height * 0.2).dp)
                    )
                    CircularProgressIndicator(
                        strokeWidth = 5.dp,
                        color = MaterialTheme.colors.primary,
                        modifier = Modifier
                            .size(100.dp)
                    )
                }

            }
        }
    }
}