package com.example.seekers.viewModels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.example.seekers.utils.*
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng

class LobbyCreationScreenViewModel(application: Application) : AndroidViewModel(application) {

    val TAG = "LobbyVM"

    // Firebase
    val firestore = FirebaseHelper

    // Players
    val maxPlayers = MutableLiveData<Int>()
    val playersInLobby = MutableLiveData(listOf<Player>())
    val isCreator = MutableLiveData<Boolean>()

    // Lobby
    val lobby = MutableLiveData<Lobby>()
    val timeLimit = MutableLiveData<Int>()
    val countdown = MutableLiveData<Int>()
    val center: MutableLiveData<LatLng> = MutableLiveData<LatLng>(null)
    val radius = MutableLiveData(50)

    // Map
    val showMap = MutableLiveData(false)
    val currentLocation = MutableLiveData<LatLng>()
    private val client = LocationServices.getFusedLocationProviderClient(application)
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(p0: LocationResult) {
            p0.lastLocation?.let {
                currentLocation.postValue(LatLng(it.latitude, it.longitude))
            }
        }
    }

    // Other
    val maxPlayersError = MutableLiveData(false)
    val timeLimitError = MutableLiveData(false)
    val countDownError = MutableLiveData(false)
    val showMaxPlayersError = MutableLiveData(false)
    val showTimeLimitError = MutableLiveData(false)
    val showCountDownError = MutableLiveData(false)
    val showQRDialog = MutableLiveData(false)
    val showLeaveGameDialog = MutableLiveData(false)
    val showDismissLobbyDialog = MutableLiveData(false)
    val showEditRulesDialog = MutableLiveData(false)

    // Map functions

    fun updateCenter(location: LatLng) {
        center.value = location
    }

    fun updateRadius(newVal: Int) {
        radius.value = newVal
    }

    fun updateShowMap(newVal: Boolean) {
        showMap.value = newVal
    }

    fun requestLoc() {
        LocationHelper.requestLocationUpdates(
            client = client,
            locationCallback = locationCallback
        )
    }

    fun removeLocationUpdates() {
        LocationHelper.removeLocationUpdates(client, locationCallback)
    }

    // Player functions

    fun addPlayer(player: Player, gameId: String) = firestore.addPlayer(player, gameId)

    fun removePlayer(gameId: String, playerId: String) =
        firestore.removePlayer(gameId = gameId, playerId = playerId)

    fun getPlayersInLobby(gameId: String) {
        firestore.getPlayers(gameId)
            .addSnapshotListener { list, e ->
                list ?: run {
                    Log.e(TAG, "getPlayers: ", e)
                    return@addSnapshotListener
                }
                val playerList = list.toObjects(Player::class.java)
                playersInLobby.postValue(playerList)
            }
    }

    fun getPlayer(gameId: String, playerId: String) {
        firestore.getPlayer(gameId, playerId).get()
            .addOnSuccessListener { data ->
                val player = data.toObject(Player::class.java)
                player?.let {
                    isCreator.postValue(it.inLobbyStatus == InLobbyStatus.CREATOR.ordinal)
                }
            }
            .addOnFailureListener {
                Log.e(TAG, "getPlayer: ", it)
            }
    }

    fun updateUser(userId: String, changeMap: Map<String, Any>) =
        firestore.updateUser(userId, changeMap)

    fun updateMaxPlayers(newVal: Int?) {
        maxPlayers.value = newVal
    }

    // Lobby functions

    fun addLobby(lobby: Lobby) = firestore.addLobby(lobby)

    fun getLobby(gameId: String) {
        firestore.getLobby(gameId).addSnapshotListener { data, e ->
            data?.let {
                val lobbyFetched = it.toObject(Lobby::class.java)
                if (lobbyFetched != null) {
                    lobby.postValue(lobbyFetched)
                    maxPlayers.postValue(lobbyFetched.maxPlayers)
                    timeLimit.postValue(lobbyFetched.timeLimit)
                    countdown.postValue(lobbyFetched.countdown)
                    radius.postValue(lobbyFetched.radius)
                    center.postValue(LatLng(lobbyFetched.center.latitude, lobbyFetched.center.longitude))
                }
            }
        }
    }

    fun updateLobby(changeMap: Map<String, Any>, gameId: String) =
        firestore.updateLobby(changeMap, gameId)

    fun updateTimeLimit(newVal: Int?) {
        timeLimit.value = newVal
    }

    fun updateCountdown(newVal: Int?) {
        countdown.value = newVal
    }
    fun updateShowDialog(
        qrDialog: Boolean = false,
        leaveGame: Boolean = false,
        dismissLobby: Boolean = false,
        editRules: Boolean = false
    ) {
        showQRDialog.value = qrDialog
        showLeaveGameDialog.value = leaveGame
        showDismissLobbyDialog.value = dismissLobby
        showEditRulesDialog.value = editRules
    }

}