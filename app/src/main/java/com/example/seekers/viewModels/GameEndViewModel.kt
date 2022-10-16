package com.example.seekers.viewModels

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.example.seekers.utils.FirebaseHelper
import com.example.seekers.utils.InGameStatus
import com.example.seekers.utils.Lobby
import com.example.seekers.utils.Player
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.toObject
import java.math.RoundingMode

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
                    Log.e(ContentValues.TAG, "getPlayers: ", e)
                    return@addSnapshotListener
                }
                val playersFetched = data.toObjects(Player::class.java)
                players.postValue(playersFetched)
            }
    }
}