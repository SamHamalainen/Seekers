package com.example.seekers.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.seekers.utils.FirebaseHelper
import com.example.seekers.general.CustomButton
import com.example.seekers.general.NavRoutes
import com.example.seekers.viewModels.GameEndViewModel

@Composable
fun GameEndScreen(
    vm: GameEndViewModel = viewModel(),
    navController: NavHostController,
    gameId: String,
) {
    val playerId = FirebaseHelper.uid
    vm.getSteps()
    if (playerId != null) {
        vm.getTimeAsSeeker(gameId, playerId)
    }
    vm.getPlayers(gameId)

    Column(
        verticalArrangement = Arrangement.SpaceAround,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(30.dp)
            .fillMaxSize(),
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        Text(text = "Game over!", fontSize = 35.sp, fontWeight = FontWeight.Bold)
        EndGameStats(vm = vm)
        CustomButton(text = "Start a new game") {
            FirebaseHelper.updateUser(FirebaseHelper.uid!!, mapOf(Pair("currentGameId", "")))
            navController.navigate(NavRoutes.StartGame.route)
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
    BackHandler(enabled = true) {
        // Just to disable going back into the game in EndGameScreen
    }

}

@Composable
fun EndGameStats(vm: GameEndViewModel) {
    val steps by vm.steps.observeAsState()
    val distance by vm.distance.observeAsState()
    val timeAsSeeker by vm.timeAsSeeker.observeAsState()
    val timeSurvived by vm.timeSurvived.observeAsState()
    val players by vm.players.observeAsState()
    val playersFoundByMe by vm.playersFoundByMe.observeAsState()
    val playersHidden by vm.playersHidden.observeAsState()

    Card(elevation = 10.dp) {
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier
                .padding(30.dp)
        ) {
            Text("STATISTICS", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(15.dp))
            StatRow(text = "Players found by me", value = playersFoundByMe.toString())
            StatRow(text = "Players not found", value = playersHidden.toString())
            StatRow("Steps taken", value = steps.toString())
            StatRow("Distance walked", value = "$distance m")
            StatRow("Your time as seeker", value = "$timeAsSeeker min")
            if (timeSurvived != null) {
                if (timeSurvived!! >= 0) {
                    StatRow("Your time in hiding", value = "$timeSurvived min")
                } else {
                    StatRow("Your time in hiding", value = "0 min")
                }
            }
        }
    }
}
@Composable
fun StatRow(text: String, value: String) {
    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier
        .padding(5.dp)
        .fillMaxWidth()) {
        Text(text = text)
        Box(modifier = Modifier.width(100.dp), contentAlignment = Alignment.CenterEnd) {
            Text(text = value)
        }
    }
}


