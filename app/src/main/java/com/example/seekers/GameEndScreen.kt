package com.example.seekers

import android.app.Application
import android.content.ContentValues.TAG
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
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
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.seekers.general.CustomButton
import com.google.firebase.firestore.ktx.toObject
import java.math.RoundingMode

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

    val steps by vm.steps.observeAsState()
    val distance by vm.distance.observeAsState()
    val timeAsSeeker by vm.timeAsSeeker.observeAsState()
    val timeSurvived by vm.timeSurvived.observeAsState()
    val players by vm.players.observeAsState()

    Column(
        verticalArrangement = Arrangement.SpaceAround,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(30.dp)
            .fillMaxSize(),
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        Text(text = "Game over!", fontSize = 35.sp, fontWeight = FontWeight.Bold)
        Card(elevation = 10.dp) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(30.dp)
            ) {
                Text("STATISTICS", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(15.dp))
                players?.let {
                    val total =
                        it.count { player ->
                            player.inGameStatus != InGameStatus.LEFT.ordinal
                        }
                    val hidingAmount =
                        it.count { player ->
                            player.inGameStatus == InGameStatus.HIDING.ordinal
                                    || player.inGameStatus == InGameStatus.MOVING.ordinal
                        }
                    val foundAmount = total.minus(hidingAmount)

                    Text(
                        text = "Players found: $foundAmount",
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = "Players hidden: $hidingAmount",
                        fontSize = 16.sp
                    )
                }
                Spacer(modifier = Modifier.height(5.dp))
                Text("Steps taken: $steps", fontSize = 16.sp)
                Spacer(modifier = Modifier.height(5.dp))
                Text("Distance walked: $distance meters", fontSize = 16.sp)
                Spacer(modifier = Modifier.height(5.dp))
                Text("Your time as seeker: $timeAsSeeker minutes", fontSize = 16.sp)
                Spacer(modifier = Modifier.height(5.dp))
                if (timeSurvived != null) {
                    if (timeSurvived!! >= 0) {
                        Text("Your time in hiding: $timeSurvived minutes", fontSize = 16.sp)
                    } else {
                        Text("Your time in hiding: 0 minutes", fontSize = 16.sp)
                    }
                }
            }
        }
        CustomButton(text = "Start a new game") {
            navController.navigate(NavRoutes.StartGame.route)
        }
        Spacer(modifier = Modifier.height(20.dp))
    }

}


class GameEndViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPreference: SharedPreferences = application.getSharedPreferences(
        "statistics",
        Context.MODE_PRIVATE
    )
    val timeAsSeeker = MutableLiveData<Int>(null)
    val timeSurvived = MutableLiveData<Int>(null)
    val steps = MutableLiveData(0)
    val distance = MutableLiveData(0.0F)
    val players = MutableLiveData<List<Player>>()

    fun getTimeAsSeeker(gameId: String, playerId: String) {
        FirebaseHelper.getLobby(gameId).get().addOnSuccessListener { documentSnapshot ->
            val lobby = documentSnapshot.toObject<Lobby>()
            val endTime = lobby?.let { it1 ->
                lobby.startTime.toDate().time.div(1000).toInt().plus(lobby.countdown).plus(
                    it1.timeLimit * 60
                )
            }
            FirebaseHelper.getPlayer(gameId, playerId).get().addOnSuccessListener {
                val player = it.toObject(Player::class.java)
                val eliminationTime = player?.timeOfElimination?.toDate()?.time?.div(1000)?.toInt()
                val seekerTime = eliminationTime?.let { it1 -> endTime?.minus(it1) }
                timeAsSeeker.value = seekerTime?.div(60)
                timeSurvived.value =
                    seekerTime?.let { it1 -> lobby?.timeLimit?.times(60)?.minus(it1)?.div(60) }
            }
        }
    }

    fun getSteps() {
        steps.value = sharedPreference.getInt("step count", 0)
        distance.value = sharedPreference.getFloat("distance moved", 0.0F).toBigDecimal()
            .setScale(1, RoundingMode.UP).toFloat()
    }

    fun getPlayers(gameId: String) {
        FirebaseHelper.getPlayers(gameId = gameId)
            .addSnapshotListener { data, e ->
                data ?: run {
                    Log.e(TAG, "getPlayers: ", e)
                    return@addSnapshotListener
                }
                val playersFetched = data.toObjects(Player::class.java)
                players.postValue(playersFetched)
            }
    }
}