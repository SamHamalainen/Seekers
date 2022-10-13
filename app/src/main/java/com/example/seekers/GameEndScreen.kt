package com.example.seekers

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController

@Composable
fun GameEndScreen(
    vm: GameEndViewModel = viewModel(),
    navController: NavHostController,
    gameId: String,
) {
    vm.setValues()

    val steps by vm.steps.observeAsState()
    val distance by vm.distance.observeAsState()

    Column(
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(30.dp)
            .fillMaxSize(),
    ) {
        Card(modifier = Modifier
            .padding(20.dp)) {
            Column() {
                Text("Steps taken: $steps")
                Text("Distance walked: $distance")
            }
        }
        Button(onClick = { navController.navigate(NavRoutes.Heatmap.route + "/$gameId") }) {
            Text("Back to the game")
        }
        Button(onClick = { navController.navigate(NavRoutes.StartGame.route) }) {
            Text("Start a new game")
        }
    }
}


class GameEndViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPreference: SharedPreferences = application.getSharedPreferences(
        "statistics",
        Context.MODE_PRIVATE
    )

    val steps = MutableLiveData(0)

    val distance = MutableLiveData(0.0F)

    fun setValues() {
        steps.value = sharedPreference.getInt("step count", 0)
        distance.value = sharedPreference.getFloat("distance moved", 0.0F)
    }
}