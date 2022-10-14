package com.example.seekers

import android.app.Application
import android.content.ContentValues.TAG
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
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
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.seekers.general.CustomButton
import com.google.firebase.Timestamp
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
    val playersFoundByMe by vm.playersFoundByMe.observeAsState()
    val playersHidden by vm.playersHidden.observeAsState()

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
                horizontalAlignment = Alignment.Start,
                modifier = Modifier
                    .padding(30.dp)
            ) {
                Text("STATISTICS", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(15.dp))
                StatRow(text = "Players found", value = playersFoundByMe.toString())
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
        CustomButton(text = "Start a new game") {
            FirebaseHelper.updateUser(FirebaseHelper.uid!!, mapOf(Pair("currentGameId", "")))
            navController.navigate(NavRoutes.StartGame.route)
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
    BackHandler(enabled = false) {

    }

}

@Composable
fun StatRow(text: String, value: String) {
    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.padding(5.dp).fillMaxWidth()) {
        Text(text = text)
        Box(modifier = Modifier.width(100.dp), contentAlignment = Alignment.CenterEnd) {
            Text(text = value)
        }
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
    val playersFoundByMe = Transformations.map(players) { players ->
        players.count {
            it.foundBy == FirebaseHelper.uid!!
        }
    }
    val playersHidden = Transformations.map(players) { players ->
        players.count {
            it.inGameStatus == InGameStatus.HIDING.ordinal || it.inGameStatus == InGameStatus.MOVING.ordinal
        }
    }

    fun getTimeAsSeeker(gameId: String, playerId: String) {
        FirebaseHelper.getLobby(gameId).get().addOnSuccessListener { documentSnapshot ->
            val lobby = documentSnapshot.toObject<Lobby>()!!
            val startTime = (lobby.startTime.toDate().time.div(1000)).div(60).toInt()
            val endTime = (lobby.endGameTime.toDate().time.div(1000)).div(60).toInt()
            Log.d("EndGame", "end time: $endTime")
            FirebaseHelper.getPlayer(gameId, playerId).get().addOnSuccessListener {
                val player = it.toObject(Player::class.java)!!
                val eliminationTime = player.timeOfElimination.toDate().time
                val refTimestamp = Timestamp(1L, 1).toDate().time
                val seekerTime = endTime.minus(startTime)
                Log.d("EndGame", "elim time: $eliminationTime")
                Log.d("EndGame", "ref time: $refTimestamp")
                Log.d("EndGame", "seeker time: $seekerTime")
                if (eliminationTime < refTimestamp) {
                    timeSurvived.value = 0
                } else {
                    timeSurvived.value = seekerTime
                }
                timeAsSeeker.value = lobby.timeLimit.minus(seekerTime)
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