package com.example.seekers.viewModels

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.seekers.utils.FirebaseHelper
import com.example.seekers.utils.Lobby

class ScannerViewModel : ViewModel() {
    companion object {
        const val TAG = "SCANNER_VIEW_MODEL"
    }

    val firestore = FirebaseHelper
    val lobby = MutableLiveData<Lobby>()
    val playersInLobby = MutableLiveData<Int>()

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