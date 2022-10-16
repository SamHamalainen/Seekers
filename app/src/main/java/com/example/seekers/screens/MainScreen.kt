package com.example.seekers.screens

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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.seekers.R
import com.example.seekers.general.NavRoutes
import com.example.seekers.utils.LobbyStatus
import com.example.seekers.viewModels.AuthenticationViewModel
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * This Screen makes sure you are navigated to the correct screen after opening the app.
 * if not logged in brings to login screen otherwise to your accounts state.
 * - Not in game -> StartGameScreen
 * - In Lobby -> LobbyQRScreen
 * - In game -> HeatMapScreen
 * - After Game -> EndGameScreen
 */
@Composable
fun MainScreen(vm: AuthenticationViewModel = viewModel(), navController: NavController) {
    val loggedInUser: FirebaseUser? by vm.user.observeAsState(null)
    val gameStatus by vm.gameStatus.observeAsState()
    val gameId by vm.currentGameId.observeAsState()
    val userIsInUsers by vm.userExistsInUsers.observeAsState()
    var loading by remember { mutableStateOf(true) }


    // Set current user if available and have delay for loading screen.
    LaunchedEffect(Unit) {
        vm.setUser(vm.fireBaseAuth.currentUser)
        launch(Dispatchers.Default) {
            delay(2000)
            loading = false
        }
    }

    // Check if the user exists in users collections on firebase
    LaunchedEffect(loggedInUser) {
        loggedInUser?.let {
            vm.checkUserInUsers(it.uid)
        }
    }

    // If User exists check their game status.
    // else create new document into users collection on firebase
    // with their email address and empty gameId then navigate to the StartGameScreen.
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

    // If gameId is blank navigate to the StartGameScreen,
    // else check the games status
    LaunchedEffect(gameId) {
        gameId?.let {
            if (it.isBlank()) {
                navController.navigate(NavRoutes.StartGame.route)
            } else {
                vm.checkGameStatus(it)
            }
        }
    }

    // when gameStatus has been checked navigate user to the correct screen.
    // lobby created -> LobbyQRScreen
    // in countdown phase -> CountdownScreen
    // currently active -> HeatMapScreen
    // has finished but not left the stat screen -> EndGameScreen
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
        // if no current users logged in then show Login form otherwise launchedEffects handle navigating.
        if (loggedInUser == null && !loading) {
            LoginForm(
                vm = vm,
                navController = navController,
            )
        } else {
            LoadingScreen()
        }
    }
}

// Loading screen when more time is needed for handling checks
@Composable
fun LoadingScreen() {
    val screenHeight = LocalConfiguration.current.screenHeightDp

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(R.drawable.seekers_ver3),
                contentDescription = "seekers",
                modifier = Modifier.height((screenHeight * 0.2).dp)
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