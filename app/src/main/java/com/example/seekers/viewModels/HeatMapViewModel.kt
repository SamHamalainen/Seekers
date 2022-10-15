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
import com.example.seekers.*
import com.example.seekers.general.NavRoutes
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint

class HeatMapViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        val TAG = "heatMapVM"
    }

    var newsCount = 0
    val news = MutableLiveData<List<News>>()
    val hasNewNews = MutableLiveData<Boolean>()
    val firestore = FirebaseHelper
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

    val players = MutableLiveData<List<Player>>()
    val currentSeekers = MutableLiveData<List<Player>>()
    val canSeeSeeker = MutableLiveData<Boolean>()
    val showPowersDialog = MutableLiveData<Boolean>()
    val powerCountdown = MutableLiveData(0)
    val activePower = MutableLiveData<Power>()
    val showJammer = MutableLiveData<Boolean>()
    val playerStatus = Transformations.map(players) { list ->
        list.find { it.playerId == firestore.uid!! }?.inGameStatus
    }
    val isSeeker = MutableLiveData<Boolean>()
    val playersWithoutSelf = Transformations.map(players) { players ->
        players.filter { it.playerId != FirebaseHelper.uid!! }
    }
    val heatPositions = Transformations.map(playersWithoutSelf) { players ->
        players.filter { it.inGameStatus == InGameStatus.HIDING.ordinal || it.inGameStatus == InGameStatus.DECOYED.ordinal }
            .map { LatLng(it.location.latitude, it.location.longitude) }
    }
    val movingPlayers = Transformations.map(playersWithoutSelf) { players ->
        players.filter { it.inGameStatus == InGameStatus.MOVING.ordinal }
    }
    val eliminatedPlayers = Transformations.map(playersWithoutSelf) { players ->
        players.filter { it.inGameStatus == InGameStatus.ELIMINATED.ordinal }
    }
    val countdown = MutableLiveData<Int>()
    var countdownReceiver: BroadcastReceiver? = null

    fun addMockPlayers(gameId: String) {
        val mockPlayers = listOf(
            Player(
                nickname = "player 1",
                avatarId = 1,
                inGameStatus = InGameStatus.HIDING.ordinal,
                location = GeoPoint(60.22338389989929, 24.756749169655805),
                playerId = "player 1",
                distanceStatus = PlayerDistance.WITHIN50.ordinal
            ),
            Player(
                nickname = "player 2",
                avatarId = 5,
                inGameStatus = InGameStatus.MOVING.ordinal,
                location = GeoPoint(60.22374887627318, 24.759200708558442),
                playerId = "player 2",
                distanceStatus = PlayerDistance.WITHIN100.ordinal
            ),
            Player(
                nickname = "player 3",
                avatarId = 1,
                inGameStatus = InGameStatus.HIDING.ordinal,
                location = GeoPoint(60.223032239987354, 24.758830563735074),
                playerId = "player 3",
                distanceStatus = PlayerDistance.WITHIN10.ordinal
            ),
            Player(
                nickname = "player 4",
                avatarId = 1,
                inGameStatus = InGameStatus.MOVING.ordinal,
                location = GeoPoint(60.224550744400226, 24.756561415035257),
                playerId = "player 4",
                distanceStatus = PlayerDistance.WITHIN50.ordinal
            ),
            Player(
                nickname = "player 5",
                avatarId = 1,
                inGameStatus = InGameStatus.ELIMINATED.ordinal,
                location = GeoPoint(60.223405212500005, 24.75958158221728),
                playerId = "player 5",
                distanceStatus = PlayerDistance.WITHIN50.ordinal
            ),
            Player(
                nickname = "player 6",
                avatarId = 1,
                inGameStatus = InGameStatus.HIDING.ordinal,
                location = GeoPoint(60.223841983003645, 24.759626485065098),
                playerId = "player 6",
                distanceStatus = PlayerDistance.WITHIN50.ordinal
            ),
            Player(
                nickname = "player 7",
                avatarId = 5,
                inGameStatus = InGameStatus.MOVING.ordinal,
                location = GeoPoint(60.22357557804847, 24.756681419911455),
                playerId = "player 7",
                distanceStatus = PlayerDistance.WITHIN100.ordinal
            ),
            Player(
                nickname = "player 8",
                avatarId = 1,
                inGameStatus = InGameStatus.HIDING.ordinal,
                location = GeoPoint(60.22314399742664, 24.757781125478843),
                playerId = "player 8",
                distanceStatus = PlayerDistance.WITHIN10.ordinal
            ),
            Player(
                nickname = "player 9",
                avatarId = 1,
                inGameStatus = InGameStatus.MOVING.ordinal,
                location = GeoPoint(60.22311735646131, 24.759814239674167),
                playerId = "player 9",
                distanceStatus = PlayerDistance.WITHIN50.ordinal
            ),
            Player(
                nickname = "player 10",
                avatarId = 1,
                inGameStatus = InGameStatus.ELIMINATED.ordinal,
                location = GeoPoint(60.223405212500005, 24.75958158221728),
                playerId = "player 10",
                distanceStatus = PlayerDistance.WITHIN50.ordinal
            ),
        )
        mockPlayers.forEach {
            firestore.addPlayer(it, gameId)
        }
    }

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

    fun updateIsSeeker(newVal: Boolean) {
        isSeeker.value = newVal
    }

    fun updateShowPowersDialog(newVal: Boolean) {
        showPowersDialog.value = newVal
    }

    fun updateShowJammer(newVal: Boolean) {
        showJammer.value = newVal
    }

//    fun getTime(gameId: String) {
//        val now = Timestamp.now().toDate().time.div(1000)
//        firestore.getLobby(gameId = gameId).get()
//            .addOnSuccessListener {
//                val lobby = it.toObject(Lobby::class.java)
//                lobby?.let {
//                    val startTime = lobby.startTime.toDate().time / 1000
//                    val countdown = lobby.countdown
//                    val timeLimit = lobby.timeLimit * 60
//                    val gameEndTime = (startTime + countdown + timeLimit)
//                    timeRemaining.postValue(gameEndTime.minus(now).toInt() + 1)
//                }
//            }
//    }

    fun updateCountdown(newVal: Int) {
        countdown.value = newVal
    }

    fun getLobby(gameId: String) {
        firestore.getLobby(gameId).addSnapshotListener { data, e ->
            data ?: run {
                Log.e(TAG, "getLobby: ", e)
                return@addSnapshotListener
            }
            val lobbyFetched = data.toObject(Lobby::class.java)
            lobby.postValue(lobbyFetched)
        }
    }

    fun updateUser(changeMap: Map<String, Any>, uid: String) =
        firestore.updateUser(changeMap = changeMap, userId = uid)

    fun updatePlayer(changeMap: Map<String, Any>, gameId: String, uid: String) {
        firestore.updatePlayer(changeMap, uid, gameId)
    }

    fun setPlayerInGameStatus(status: Int, gameId: String, playerId: String) {
        firestore.updatePlayerInGameStatus(
            inGameStatus = status,
            gameId = gameId,
            playerId = playerId
        )
    }

    fun setPlayerFound(gameId: String, playerId: String) {
        val changeMap = mapOf(
            Pair("inGameStatus", InGameStatus.ELIMINATED.ordinal),
            Pair("timeOfElimination", Timestamp.now()),
            Pair("foundBy", firestore.uid!!)
        )
        firestore.updatePlayer(changeMap, playerId, gameId)
    }

    fun addFoundNews(gameId: String, nickname: String, playerId: String) {
        val news = News("", "$nickname was found!", Timestamp.now())
        firestore.addFoundNews(news, gameId, playerId)
    }

    fun sendSelfie(foundPlayerId: String, gameId: String, selfie: Bitmap, nickname: String) {
        firestore.sendSelfie(foundPlayerId, gameId, selfie, nickname)
    }

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

    fun setLobbyFinished(gameId: String) {
        val map = mapOf(
            Pair("status", LobbyStatus.FINISHED.ordinal),
            Pair("endGameTime", Timestamp.now())
        )
        firestore.updateLobby(map, gameId)
    }

    fun endLobby(context: Context) {
        stopService(context)
        stopStepCounter()
    }

    fun leaveGame(gameId: String, context: Context, navController: NavHostController) {
        updateUser(mapOf(Pair("currentGameId", "")), FirebaseHelper.uid!!)
        setPlayerInGameStatus(InGameStatus.LEFT.ordinal, gameId, firestore.uid!!)
        stopService(context)
        navController.navigate(NavRoutes.StartGame.route)
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
        println("registered")
    }

    fun unregisterReceiver(context: Context) {
        context.unregisterReceiver(countdownReceiver)
    }

    fun startService(context: Context, gameId: String, isSeeker: Boolean) {
        GameService.start(
            context = context,
            gameId = gameId,
            isSeeker = isSeeker
        )
    }

    fun stopService(context: Context) {
        GameService.stop(
            context = context,
        )
    }

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
                if (running == true) {
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

    fun stopStepCounter() {
        running = false
        sensorManager.unregisterListener(sensorEventListener)
        sharedPreferenceEditor.putInt("step count", steps)
        sharedPreferenceEditor.commit()

        countDistance()
        //val value = sharedPreference.getInt("step count", 0)
        //Log.d("steps from shared preferences", value.toString())

        //Toast.makeText(context, "steps taken: $value", Toast.LENGTH_LONG).show()
        initialSteps = -1
    }

    private fun countDistance() {
        distance = stepLength.times(steps.toFloat())
        sharedPreferenceEditor.putFloat("distance moved", distance)
        sharedPreferenceEditor.commit()
    }

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