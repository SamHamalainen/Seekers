package com.example.seekers.viewModels

import android.app.Application
import android.content.*
import android.graphics.Bitmap
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.CountDownTimer
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.navigation.NavHostController
import com.example.seekers.composables.Power
import com.example.seekers.general.NavRoutes
import com.example.seekers.services.GameService
import com.example.seekers.utils.*
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.toObject

/**
 * HeatMapViewModel: Contains all the logic behind the HeatMapScreen
 */

class HeatMapViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        const val TAG = "heatMapVM"
    }

    val firestore = FirebaseHelper

    // lobby
    val lobby = MutableLiveData<Lobby>()
    val radius = Transformations.map(lobby) {
        it.radius
    }
    val lobbyStatus = Transformations.map(lobby) {
        it.status
    }
    val center = Transformations.map(lobby) {
        LatLng(it.center.latitude, it.center.longitude)
    }

    // players
    val players = MutableLiveData<List<Player>>()
    val currentSeekers = MutableLiveData<List<Player>>()
    val playerStatus = Transformations.map(players) { list ->
        list.find { it.playerId == firestore.uid!! }?.inGameStatus
    }
    val isSeeker = MutableLiveData<Boolean>()
    private val playersWithoutSelf = Transformations.map(players) { players ->
        players.filter { it.playerId != FirebaseHelper.uid!! }
    }

    // map positions
    val heatPositions = Transformations.map(playersWithoutSelf) { players ->
        players.filter { it.inGameStatus == InGameStatus.HIDING.ordinal || it.inGameStatus == InGameStatus.DECOYED.ordinal }
            .map { LatLng(it.location.latitude, it.location.longitude) }
    }
    val movingPlayers = Transformations.map(playersWithoutSelf) { players ->
        players.filter { it.inGameStatus == InGameStatus.MOVING.ordinal }
    }

    // powers
    val showPowersDialog = MutableLiveData<Boolean>()
    val powerCountdown = MutableLiveData(0)
    val activePower = MutableLiveData<Power>()
    val showJammer = MutableLiveData<Boolean>()
    val canSeeSeeker = MutableLiveData<Boolean>()

    // news
    private var newsCount = 0
    val news = MutableLiveData<List<News>>()
    val hasNewNews = MutableLiveData<Boolean>()

    // countdown
    val countdown = MutableLiveData<Int>()
    private var countdownReceiver: BroadcastReceiver? = null

    /**
     *     Firestore listeners
     */

    // Starts listening to the players collection related to the current lobby in firestore
    fun getPlayers(gameId: String) {
        firestore.getPlayers(gameId = gameId)
            .addSnapshotListener { data, e ->
                data ?: run {
                    Log.e(TAG, "getPlayers: ", e)
                    return@addSnapshotListener
                }
                val playersFetched = data.toObjects(Player::class.java)
                val seekersFound = playersFetched.filter {
                    it.inGameStatus == InGameStatus.SEEKER.ordinal || it.inGameStatus == InGameStatus.JAMMED.ordinal
                }
                currentSeekers.postValue(seekersFound)
                players.postValue(playersFetched)
            }
    }

    // Starts listening to the lobby document in firestore thanks to it's gameId
    fun getLobby(gameId: String) {
        firestore.getLobby(gameId).addSnapshotListener { data, e ->
            data ?: run {
                Log.e(TAG, "getLobby: ", e)
                return@addSnapshotListener
            }
            val lobbyFetched = data.toObject(Lobby::class.java)
            lobby.value = lobbyFetched
        }
    }

    // Starts listening to the news collection related to the current lobby
    fun getNews(gameId: String) {
        firestore.getNews(gameId).addSnapshotListener { data, e ->
            data ?: kotlin.run {
                Log.e(GameService.TAG, "listenForNews: ", e)
                return@addSnapshotListener
            }
            val newsList = data.toObjects(News::class.java)
            if (newsList.size > newsCount) {
                news.value = newsList
                hasNewNews.value = true
            }
        }
    }

    /**
     *     Live data updaters
     */

    fun updateIsSeeker(newVal: Boolean) {
        isSeeker.value = newVal
    }

    fun updateShowPowersDialog(newVal: Boolean) {
        showPowersDialog.value = newVal
    }

    fun updateShowJammer(newVal: Boolean) {
        showJammer.value = newVal
    }

    fun updateCountdown(newVal: Int) {
        countdown.value = newVal
    }

    /**
     *     Firestore updaters
     */

    // Updates a user document in the users collection in firestore
    private fun updateUser(changeMap: Map<String, Any>, uid: String) =
        firestore.updateUser(changeMap = changeMap, userId = uid)

    // Updates a players in game status in firestore
    fun updateInGameStatus(status: Int, gameId: String, playerId: String) {
        firestore.updatePlayerInGameStatus(
            inGameStatus = status,
            gameId = gameId,
            playerId = playerId
        )
    }

    // Changes the status of a player to eliminated when they're found, records their time of elimination
    // and sets who they were found by
    fun setPlayerFound(gameId: String, playerId: String) {
        val changeMap = mapOf(
            Pair("inGameStatus", InGameStatus.ELIMINATED.ordinal),
            Pair("timeOfElimination", Timestamp.now()),
            Pair("foundBy", firestore.uid!!)
        )
        firestore.updatePlayer(changeMap, playerId, gameId)
    }

    // Adds a news item to firestore which triggers a notification when someone is found
    fun addFoundNews(gameId: String, nickname: String, playerId: String) {
        val news = News("", "$nickname was found!", Timestamp.now())
        firestore.addFoundNews(news, gameId, playerId)
    }

    // Sends a picture to firebase storage when someone is found
    fun sendSelfie(foundPlayerId: String, gameId: String, selfie: Bitmap, nickname: String) {
        firestore.sendSelfie(foundPlayerId, gameId, selfie, nickname)
    }

    // changes a lobby status to finished which triggers the end of a game
    fun setLobbyFinished(gameId: String) {
        firestore.getLobby(gameId).get()
            .addOnSuccessListener {
                val status = it.toObject<Lobby>()?.status
                if (status != LobbyStatus.FINISHED.ordinal) {
                    val map = mapOf(
                        Pair("status", LobbyStatus.FINISHED.ordinal),
                        Pair("endGameTime", Timestamp.now())
                    )
                    firestore.updateLobby(map, gameId)
                }
            }
    }

    // Handles the departure from a game by updating a player's user document, their in game status
    // and stopping the foreground service
    fun leaveGame(gameId: String, context: Context, navController: NavHostController) {
        updateUser(mapOf(Pair("currentGameId", "")), FirebaseHelper.uid!!)
        updateInGameStatus(InGameStatus.LEFT.ordinal, gameId, firestore.uid!!)
        stopService(context)
        navController.navigate(NavRoutes.StartGame.route)
    }

    /**
     * Service
     */

    // Starts the foreground game service
    fun startService(context: Context, gameId: String, isSeeker: Boolean) {
        GameService.start(
            context = context,
            gameId = gameId,
            isSeeker = isSeeker
        )
        receiveCountdown(context)
    }

    // Stops the foreground game service
    fun stopService(context: Context) {
        GameService.stop(
            context = context,
        )
    }

    // Stops receiving countdown broadcast, stops the foreground service and stops the step counter
    fun endLobby(context: Context) {
        unregisterReceiver(context)
        stopService(context)
        stopStepCounter()
    }

    // Starts receiving the countdown broadcast from the foreground service, which updates the in game timer
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
        println("registered")
    }

    // Stops receiving the countdown broadcast
    private fun unregisterReceiver(context: Context) {
        context.unregisterReceiver(countdownReceiver)
    }

    /**
     * Step counter
     */

    //Variables and functions for the step counter
    private var steps = 0
    private var distance = 0.0F
    private var running = false
    private val stepLength = 0.78F

    //private val context = application
    private var initialSteps = -1
    private val sensorManager: SensorManager =
        application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    val stepCounterSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val sharedPreference: SharedPreferences =
        application.getSharedPreferences("statistics", Context.MODE_PRIVATE)
    private var sharedPreferenceEditor: SharedPreferences.Editor = sharedPreference.edit()

    //https://www.geeksforgeeks.org/proximity-sensor-in-android-app-using-jetpack-compose/
    private val sensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor == stepCounterSensor) {
                if (running) {
                    event.values.firstOrNull()?.toInt()?.let { newSteps ->
                        if (initialSteps == -1) {
                            initialSteps = newSteps
                        }
                        val currentSteps = newSteps.minus(initialSteps)
                        steps = currentSteps
                        Log.d("steps", steps.toString())
                    }
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, p1: Int) {
            Log.d(sensor.toString(), p1.toString())
        }
    }

    fun startStepCounter() {
        running = true
        sensorManager.registerListener(
            sensorEventListener,
            stepCounterSensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    private fun stopStepCounter() {
        running = false
        sensorManager.unregisterListener(sensorEventListener)
        sharedPreferenceEditor.putInt("step count", steps)
        sharedPreferenceEditor.commit()
        countDistance()
        initialSteps = -1
    }

    private fun countDistance() {
        distance = stepLength.times(steps.toFloat())
        sharedPreferenceEditor.putFloat("distance moved", distance)
        sharedPreferenceEditor.commit()
    }

    /**
     * Power activators
     */

    // Shows the seekers on the map temporarily
    fun revealSeekers() {
        val power = Power.REVEAL
        showPowersDialog.value = false
        canSeeSeeker.value = true
        activePower.value = power
        powerCountdown.value = power.duration
        object : CountDownTimer(power.duration * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                powerCountdown.value = millisUntilFinished.div(1000).toInt()
            }
            override fun onFinish() {
                canSeeSeeker.value = false
                activePower.value = null
                this.cancel()
            }
        }.start()
    }

    // Sets own in game status as invisible temporarily. During that time, the player doesn't appear on seekers map
    fun activateInvisibility(gameId: String) {
        val power = Power.INVISIBILITY
        activePower.value = power
        powerCountdown.value = power.duration
        showPowersDialog.value = false
        val changeMap = mapOf(
            Pair("inGameStatus", InGameStatus.INVISIBLE.ordinal)
        )
        firestore.updatePlayer(changeMap, FirebaseHelper.uid!!, gameId)
        object : CountDownTimer(power.duration * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                powerCountdown.value = millisUntilFinished.div(1000).toInt()
            }

            override fun onFinish() {
                val changeMap2 = mapOf(
                    Pair("inGameStatus", InGameStatus.HIDING.ordinal)
                )
                firestore.updatePlayer(changeMap2, FirebaseHelper.uid!!, gameId)
                activePower.value = null
                this.cancel()
            }
        }.start()
    }

    // Sets all seekers as jammed temporarily. They cannot see the map during that time
    fun activateJammer(gameId: String) {
        val power = Power.JAMMER
        showPowersDialog.value = false
        activePower.value = power
        powerCountdown.value = power.duration
        currentSeekers.value?.forEach {
            val changeMap = mapOf(
                Pair("inGameStatus", InGameStatus.JAMMED.ordinal)
            )
            firestore.updatePlayer(changeMap, it.playerId, gameId)
        }
        object : CountDownTimer(power.duration * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                powerCountdown.value = millisUntilFinished.div(1000).toInt()
            }

            override fun onFinish() {
                currentSeekers.value?.forEach {
                    val changeMap2 = mapOf(
                        Pair("inGameStatus", InGameStatus.SEEKER.ordinal)
                    )
                    firestore.updatePlayer(changeMap2, it.playerId, gameId)
                }
                activePower.value = null
                this.cancel()
            }
        }.start()
    }

    // Sets own status to decoyed. Freezes own location temporarily
    fun deployDecoy(gameId: String) {
        val power = Power.DECOY
        activePower.value = power
        powerCountdown.value = power.duration
        showPowersDialog.value = false
        val changeMap = mapOf(
            Pair("inGameStatus", InGameStatus.DECOYED.ordinal)
        )
        firestore.updatePlayer(changeMap, FirebaseHelper.uid!!, gameId)
        object : CountDownTimer(power.duration * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                powerCountdown.value = millisUntilFinished.div(1000).toInt()
            }

            override fun onFinish() {
                val changeMap2 = mapOf(
                    Pair("inGameStatus", InGameStatus.HIDING.ordinal)
                )
                firestore.updatePlayer(changeMap2, FirebaseHelper.uid!!, gameId)
                activePower.value = null
                this.cancel()
            }
        }.start()
    }
}