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

/**
 * CountdownViewModel: Contains all the logic behind the CountdownScreen
 */

class CountdownViewModel : ViewModel() {
    val firestore = FirebaseHelper

    val initialValue = MutableLiveData<Int>()
    val countdown = MutableLiveData<Int>()
    private var countdownReceiver: BroadcastReceiver? = null

    fun updateCountdown(seconds: Int) {
        countdown.value = seconds
    }

    // Get the time left before the start of a game, in case the user restarted the app
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

    // Starts the foreground service which shows the countdown in a notification that the user can see
    // even if the app is in the background, that service also provides the countdown via broadcasting
    fun startService(context: Context, gameId: String) {
        CountdownService.start(context, gameId)
        receiveCountdown(context)
    }

    // Stops the foreground service
    fun stopService(context: Context) {
        CountdownService.stop(context)
        unregisterReceiver(context)
    }

    // Receives the current countdown value from the foreground service via broadcast
    private fun receiveCountdown(context: Context) {
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

    // Stops receiving countdown from foreground service
    private fun unregisterReceiver(context: Context) {
        context.unregisterReceiver(countdownReceiver)
    }
}