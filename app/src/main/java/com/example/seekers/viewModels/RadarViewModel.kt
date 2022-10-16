package com.example.seekers.viewModels

import android.location.Location
import android.location.LocationManager
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seekers.utils.FirebaseHelper
import com.example.seekers.utils.InGameStatus
import com.example.seekers.utils.Player
import com.example.seekers.utils.PlayerDistance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * RadarViewModel: Contains all the logic behind the RadarScreen
 */

class RadarViewModel : ViewModel() {
    val firestore = FirebaseHelper
    private val _scanningStatus = MutableLiveData<Int>()
    val scanningStatus: LiveData<Int> = _scanningStatus
    val players = MutableLiveData(listOf<Pair<Player, Float>>())

    // Creates a list of players with their distance to the current user, excluding the players
    // that are further than 100m and the current player
    private fun filterPlayersList(list: List<Player>, initialSeeker: Player) {
        viewModelScope.launch(Dispatchers.Default) {
            val seekersGeoPoint = initialSeeker.location
            val seekerConvertedToLocation = Location(LocationManager.GPS_PROVIDER)
            seekerConvertedToLocation.latitude = seekersGeoPoint.latitude
            seekerConvertedToLocation.longitude = seekersGeoPoint.longitude

            val playersWithDistance = mutableListOf<Pair<Player, Float>>()

            list.forEach { player ->
                val playerConvertedToLocation = Location(LocationManager.GPS_PROVIDER)
                playerConvertedToLocation.latitude = player.location.latitude
                playerConvertedToLocation.longitude = player.location.longitude

                val distanceFromSeeker =
                    seekerConvertedToLocation.distanceTo(playerConvertedToLocation)
                Log.d("location", "compare to: $distanceFromSeeker")
                if (distanceFromSeeker <= 10) {
                    player.distanceStatus = PlayerDistance.WITHIN10.ordinal
                    playersWithDistance.add(Pair(player, distanceFromSeeker))
                } else if (distanceFromSeeker > 10 && distanceFromSeeker <= 50) {
                    player.distanceStatus = PlayerDistance.WITHIN50.ordinal
                    playersWithDistance.add(Pair(player, distanceFromSeeker))
                } else if (distanceFromSeeker > 50 && distanceFromSeeker <= 100) {
                    player.distanceStatus = PlayerDistance.WITHIN100.ordinal
                    playersWithDistance.add(Pair(player, distanceFromSeeker))
                }
            }

            val playersFiltered =
                playersWithDistance.filter {
                    it.first.distanceStatus != PlayerDistance.NOT_IN_RADAR.ordinal
                            && it.first.playerId != FirebaseHelper.uid!!
                }
            players.postValue(playersFiltered.sortedBy { it.second })
        }
    }

    // Updates the status of the radar, mainly for the Lottie animation
    fun updateScanStatus(value: Int) {
        _scanningStatus.value = value
    }

    // Get a snapshot of all the players in the current game in firebase
    fun getPlayers(gameId: String): Boolean {
        firestore.getPlayers(gameId)
            .get().addOnSuccessListener { list ->
                val playerList = list.toObjects(Player::class.java)
                val seekerScanning = playerList.find { it.inGameStatus == InGameStatus.SEEKER.ordinal && it.playerId == FirebaseHelper.uid!! }
                if (seekerScanning != null) {
                    filterPlayersList(playerList, seekerScanning)
                }
            }
        return true
    }
}