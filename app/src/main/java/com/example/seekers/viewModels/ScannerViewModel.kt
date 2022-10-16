package com.example.seekers.viewModels

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.seekers.utils.FirebaseHelper
import com.example.seekers.utils.Lobby

/**
 * This ViewModel handles actions after scanning the qr code of a lobby
 */
class ScannerViewModel : ViewModel() {
    companion object {
        const val TAG = "SCANNER_VIEW_MODEL"
    }

    val firestore = FirebaseHelper
    val lobby = MutableLiveData<Lobby>()
    val playersInLobby = MutableLiveData<Int>()

    // Get lobby after scanning to check maximum amount of players
    suspend fun getLobby(gameId: String): Boolean {
        firestore.getLobby(gameId).addSnapshotListener { data, e ->
            data ?: kotlin.run {
                Log.e(TAG, "getLobby: ", e)
                return@addSnapshotListener
            }
            lobby.value = data.toObject(Lobby::class.java)
        }
        return true
    }

    // Get the current amount of players in lobby scanned
    suspend fun getNumberOfPlayersInLobby(gameId: String): Boolean {
        firestore.getPlayers(gameId).addSnapshotListener { data, e ->
            data ?: kotlin.run {
                Log.e(TAG, "getLobby: ", e)
                return@addSnapshotListener
            }
            playersInLobby.postValue(data.documents.size)
        }
        return true
    }
}