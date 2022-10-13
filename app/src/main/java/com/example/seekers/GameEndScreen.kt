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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.seekers.general.CustomButton
import com.google.firebase.firestore.ktx.toObject

@Composable
fun GameEndScreen(
    vm: GameEndViewModel = viewModel(),
    navController: NavHostController,
    gameId: String,
) {
    val playerId=FirebaseHelper.uid
    vm.getSteps()
    if (playerId != null) {
        vm.getTimeAsSeeker(gameId, playerId)
    }

    val steps by vm.steps.observeAsState()
    val distance by vm.distance.observeAsState()
    val timeAsSeeker by vm.timeAsSeeker.observeAsState()
    val timeSurvived by vm.timeSurvived.observeAsState()

    Column(
        verticalArrangement = Arrangement.SpaceAround,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(30.dp)
            .fillMaxSize(),
    ) {
        Spacer(modifier = Modifier.height(30.dp))
        Text(text = "Game over!", fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Card() {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                .padding(30.dp)) {
                Text("STATISTICS", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(5.dp))
                Text("Steps taken: $steps", fontSize = 18.sp)
                Spacer(modifier = Modifier.height(5.dp))
                Text("Distance walked: $distance", fontSize = 18.sp)
                Spacer(modifier = Modifier.height(5.dp))
                Text("Your time as seeker: $timeAsSeeker", fontSize = 18.sp)
                Spacer(modifier = Modifier.height(5.dp))
                Text("Your time in hiding: $timeSurvived", fontSize = 18.sp)
            }
        }
        CustomButton(text = "Start a new game") {
            navController.navigate(NavRoutes.StartGame.route)
        }
        Spacer(modifier = Modifier.height(30.dp))
    }

}


class GameEndViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPreference: SharedPreferences = application.getSharedPreferences(
        "statistics",
        Context.MODE_PRIVATE
    )
    val timeAsSeeker = MutableLiveData<Int>(null)
    val timeSurvived = MutableLiveData<Int>(null)

    fun getTimeAsSeeker(gameId: String, playerId: String){
        FirebaseHelper.getLobby(gameId).get().addOnSuccessListener {
            val lobby = it.toObject<Lobby>()
            val endTime = lobby?.let { it1 ->
                lobby.startTime.toDate().time.div(1000).toInt().plus(lobby.countdown).plus(
                    it1.timeLimit*60)
            }
            FirebaseHelper.getPlayer(gameId, playerId).get().addOnSuccessListener {
                val player = it.toObject(Player::class.java)
                val eliminationTime = player?.timeOfElimination?.toDate()?.time?.div(1000)?.toInt()
                val seekerTime =  eliminationTime?.let { it1 -> endTime?.minus(it1) }
                timeAsSeeker.value = seekerTime

                timeSurvived.value = seekerTime?.let { it1 -> lobby?.timeLimit?.minus(it1) }
            }
        }
    }


    val steps = MutableLiveData(0)
    val distance = MutableLiveData(0.0F)

    fun getSteps() {
        steps.value = sharedPreference.getInt("step count", 0)
        distance.value = sharedPreference.getFloat("distance moved", 0.0F)
    }
}