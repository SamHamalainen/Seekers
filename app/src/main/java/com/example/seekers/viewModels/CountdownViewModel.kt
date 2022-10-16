package com.example.seekers.viewModels

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.seekers.services.CountdownService
import com.example.seekers.utils.FirebaseHelper
import com.example.seekers.services.GameService
import com.example.seekers.utils.Lobby

class CountdownViewModel : ViewModel() {
    companion object {
        const val TAG = "COUNTDOWN_VIEW_MODEL"
    }
    val firestore = FirebaseHelper

    val initialValue = MutableLiveData<Int>()
    val countdown = MutableLiveData<Int>()
    private var countdownReceiver: BroadcastReceiver? = null

    fun updateCountdown(seconds: Int) {
        countdown.value = seconds
    }

    fun getInitialValue(gameId: String) {
        firestore.getLobby(gameId).get()
            .addOnSuccessListener {
                val lobby = it.toObject(Lobby::class.java)
                lobby ?: return@addOnSuccessListener
                val countdownVal = lobby.countdown
                if (initialValue.value == null) {
                    initialValue.value = countdownVal
                }
            }
    }

//    fun updateLobby(changeMap: Map<String, Any>, gameId: String) =
//        firestore.updateLobby(changeMap = changeMap, gameId = gameId)

    //    fun getInitialValue(gameId: String) {
//        firestore.getLobby(gameId).get()
//            .addOnSuccessListener {
//                val lobby = it.toObject(Lobby::class.java)
//                lobby ?: return@addOnSuccessListener
//                val start = lobby.startTime.toDate().time / 1000
//                val countdownVal = lobby.countdown
//                val now = Timestamp.now().toDate().time / 1000
//                val remainingCountdown = start + countdownVal - now + 2
//                println("remaining $remainingCountdown")
//                initialValue.postValue(remainingCountdown.toInt())
//            }
//    }
    fun startService(context: Context, gameId: String) {
        CountdownService.start(context, gameId)
        receiveCountdown(context)
    }

    fun stopService(context: Context) {
        CountdownService.stop(context)
        unregisterReceiver(context)
    }

    fun receiveCountdown(context: Context) {
        val countdownFilter = IntentFilter()
        countdownFilter.addAction(GameService.COUNTDOWN_TICK)
        countdownReceiver = object : BroadcastReceiver() {
            override fun onReceive(p0: Context?, p1: Intent?) {
                val countdown = p1?.getIntExtra(GameService.TIME_LEFT, 0)!!
                updateCountdown(countdown)
            }
        }
        context.registerReceiver(countdownReceiver, countdownFilter)
    }

    private fun unregisterReceiver(context: Context) {
        context.unregisterReceiver(countdownReceiver)
    }
}