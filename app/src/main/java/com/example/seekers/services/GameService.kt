package com.example.seekers.services

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.CountDownTimer
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.seekers.*
import com.example.seekers.general.secondsToText
import com.example.seekers.utils.*
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * GameService: Foreground services that shows a time countdown until the end of a game.
 * Handles:
 * - the countdown before the game ends
 * - the sending of a player's location, even when the app is in the background
 * - the several notifications the players receive during the game
 */

class GameService : Service() {
    companion object {
        const val TAG = "LOCATION_SERVICE"
        const val SEEKER_NOTIFICATION = "SEEKER_NOTIFICATION"
        const val BOUNDS_NOTIFICATION = "BOUNDS_NOTIFICATION"
        const val FOUND_NOTIFICATION = "FOUND_NOTIFICATION"
        const val END_NOTIFICATION = "END_NOTIFICATION"
        const val MAIN_NOTIFICATION_ID = 1
        const val SEEKER_NOTIFICATION_ID = 2
        const val BOUNDS_NOTIFICATION_ID = 3
        const val FOUND_NOTIFICATION_ID = 4
        const val END_NOTIFICATION_ID = 5
        const val TIME_LEFT = "TIME_LEFT"
        const val COUNTDOWN_TICK = "COUNTDOWN_TICK"

        fun start(context: Context, gameId: String, isSeeker: Boolean) {
            val startIntent = Intent(context, GameService::class.java)
            startIntent.putExtra("gameId", gameId)
            startIntent.putExtra("isSeeker", isSeeker)
            ContextCompat.startForegroundService(context, startIntent)
        }

        fun stop(context: Context) {
            val stopIntent = Intent(context, GameService::class.java)
            context.stopService(stopIntent)
        }
    }

    private var newsListener: ListenerRegistration? = null
    private var lobbyListener: ListenerRegistration? = null
    var previousLoc: Location? = null
    private var callback: LocationCallback? = null
    private var isTracking = false
    val scope = CoroutineScope(Dispatchers.IO)
    private var newsCount = 0
    var seekerNearbySent = false
    var outOfBoundsSent = false
    private var timer: CountDownTimer? = null
    private var currentGameId: String? = null

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    private fun getClient() = LocationServices.getFusedLocationProviderClient(applicationContext)

    // Handles what is done when the current location is retrieved
    private fun getCallback(gameId: String, isSeeker: Boolean): LocationCallback {
        val locCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { curLoc ->
                    scope.launch {
                        updateLoc(previousLoc, curLoc, gameId)
                    }
                    if (!isSeeker) {
                        scope.launch {
                            if (!seekerNearbySent) {
                                checkDistanceToSeekers(curLoc, gameId)
                                delay(60 * 1000)
                                seekerNearbySent = false
                            }
                        }
                        scope.launch {
                            if (!outOfBoundsSent) {
                                checkOutOfBounds(gameId, curLoc)
                                delay(30 * 1000)
                                outOfBoundsSent = false
                            }
                        }
                    }
                }
            }
        }
        callback = locCallback
        return locCallback
    }

    // Sends the current player location to firestore
    private fun updatePlayerLoc(gameId: String, curLoc: Location) {
        Log.d(TAG, "updateLoc: sent location")
        FirebaseHelper.updatePlayer(
            mapOf(
                Pair(
                    "location",
                    GeoPoint(curLoc.latitude, curLoc.longitude)
                )
            ),
            FirebaseHelper.uid!!,
            gameId
        )
        previousLoc = curLoc
    }

    // Sets the current player's in game status to firestore
    private fun setInGameStatus(status: Int, gameId: String) {
        FirebaseHelper.updatePlayerInGameStatus(
            status,
            gameId,
            FirebaseHelper.uid!!
        )
    }

    // The location of a user is only send to firestore if the player has moved more than 5 meters
    // since the last time it was sent -> limits the communication with firestore when a user does not move.
    // If a hiding player decides to move, their status is updated to moving
    // and their accurate location will show on any seeker's map.
    // When they stop moving their status is reverted to hiding.
    // While a player is in the status invisible or decoyed, their location is not updated

    fun updateLoc(prevLoc: Location?, curLoc: Location, gameId: String) {
        if (prevLoc == null) {
            previousLoc = curLoc
            FirebaseHelper.updatePlayer(
                mapOf(
                    Pair(
                        "location",
                        GeoPoint(curLoc.latitude, curLoc.longitude)
                    )
                ),
                FirebaseHelper.uid!!,
                gameId
            )
            return
        }
        val distanceToPrev = prevLoc.distanceTo(curLoc)

        FirebaseHelper.getPlayer(gameId, FirebaseHelper.uid!!).get()
            .addOnSuccessListener {
                val player = it.toObject<Player>()
                when (player?.inGameStatus) {
                    InGameStatus.JAMMED.ordinal, InGameStatus.SEEKER.ordinal -> {
                        if (distanceToPrev > 5f) {
                            updatePlayerLoc(gameId, curLoc)
                        }
                    }
                    InGameStatus.HIDING.ordinal -> {
                        if (distanceToPrev > 5f) {
                            updatePlayerLoc(gameId, curLoc)
                            setInGameStatus(InGameStatus.MOVING.ordinal, gameId)
                        }
                    }
                    InGameStatus.MOVING.ordinal -> {
                        if (distanceToPrev <= 5f) {
                            setInGameStatus(InGameStatus.HIDING.ordinal, gameId)
                        }
                    }
                }
            }
    }

    // Checks the current players distance to all the seekers locations.
    // If any seeker is less than 20 meters away, they receive a notification to warn them.
    fun checkDistanceToSeekers(ownLocation: Location, gameId: String) {
        FirebaseHelper.getPlayers(gameId)
            .whereEqualTo("inGameStatus", InGameStatus.SEEKER.ordinal)
            .get()
            .addOnSuccessListener {
                val seekers = it.toObjects(Player::class.java)
                val distances = seekers.map { player ->
                    val results = FloatArray(1)
                    Location.distanceBetween(
                        player.location.latitude,
                        player.location.longitude,
                        ownLocation.latitude,
                        ownLocation.longitude,
                        results
                    )
                    results[0]
                }
                val proximityCriteria = 20f
                val numOfSeekersNearby = distances.filter { dist -> dist <= proximityCriteria }.size
                if (numOfSeekersNearby > 0) {
                    seekerNearbySent = true
                    sendSeekerNearbyNotification(num = numOfSeekersNearby)
                }
            }
    }

    // If a user is outside the playing area, they will get a notification that prompts them to return
    // to the playing area
    fun checkOutOfBounds(gameId: String, curLoc: Location) {
        FirebaseHelper.getLobby(gameId).get()
            .addOnSuccessListener {
                val lobby = it.toObject(Lobby::class.java)
                lobby?.let { lob ->
                    val results = FloatArray(1)
                    Location.distanceBetween(
                        lob.center.latitude,
                        lob.center.longitude,
                        curLoc.latitude,
                        curLoc.longitude,
                        results
                    )
                    val distanceToCenter = results[0]
                    if (distanceToCenter > lob.radius.toFloat()) {
                        outOfBoundsSent = true
                        sendOutOfBoundsNotification()
                    }
                }
            }
    }

    // Starts tracking the current player's location
    private fun startTracking(gameId: String, isSeeker: Boolean) {
        LocationHelper.requestLocationUpdates(
            getClient(),
            getCallback(gameId, isSeeker)
        )
        isTracking = true
    }

    // Stops tracking the current player's location
    private fun stopTracking(callback: LocationCallback) {
        LocationHelper.removeLocationUpdates(
            getClient(),
            callback
        )
    }

    // Returns the intent that is started when any notification is pressed (the MainActivity is started)
    private fun getPendingIntent(): PendingIntent = PendingIntent.getActivity(
        applicationContext,
        0,
        Intent(applicationContext, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE
    )

    // Sends a notification when seekers are nearby
    private fun sendSeekerNearbyNotification(num: Int) {
        val text = if (num == 1) "A seeker is nearby" else "There are $num seekers nearby"
        val notification = NotificationHelper.createNotification(
            context = applicationContext,
            title = "Watch out!",
            content = text,
            channelId = SEEKER_NOTIFICATION,
            priority = NotificationManager.IMPORTANCE_HIGH,
            category = Notification.CATEGORY_EVENT,
            pendingIntent = getPendingIntent(),
            autoCancel = true
        )
        with(NotificationManagerCompat.from(applicationContext)) {
            notify(SEEKER_NOTIFICATION_ID, notification)
        }
    }

    // Sends a notification when a user is out of bounds (outside of playing area)
    private fun sendOutOfBoundsNotification() {
        val notification = NotificationHelper.createNotification(
            context = applicationContext,
            title = "Out of bounds!",
            content = "Return to the playing area ASAP!",
            channelId = BOUNDS_NOTIFICATION,
            priority = NotificationManager.IMPORTANCE_HIGH,
            category = Notification.CATEGORY_EVENT,
            pendingIntent = getPendingIntent(),
            autoCancel = true
        )
        with(NotificationManagerCompat.from(applicationContext)) {
            notify(BOUNDS_NOTIFICATION_ID, notification)
        }
    }

    // Sends a notification when there's a new news item for the lobby in firestore.
    // e.g. some player was found
    private fun sendNewsNotification(content: String) {
        val notification = NotificationHelper.createNotification(
            context = applicationContext,
            title = "Player found!",
            content = content,
            channelId = FOUND_NOTIFICATION,
            priority = NotificationManager.IMPORTANCE_HIGH,
            category = Notification.CATEGORY_EVENT,
            pendingIntent = getPendingIntent(),
            autoCancel = true
        )
        with(NotificationManagerCompat.from(applicationContext)) {
            notify(FOUND_NOTIFICATION_ID, notification)
        }
    }

    // Sends a notification when a game has ended
    private fun sendEndGameNotification(content: String) {
        val notification = NotificationHelper.createNotification(
            context = applicationContext,
            title = "Game Over",
            content = content,
            channelId = FOUND_NOTIFICATION,
            priority = NotificationManager.IMPORTANCE_HIGH,
            category = Notification.CATEGORY_EVENT,
            pendingIntent = getPendingIntent(),
            autoCancel = true
        )
        with(NotificationManagerCompat.from(applicationContext)) {
            notify(END_NOTIFICATION_ID, notification)
        }
    }

    // Triggers a notification any time there is a new news item in firestore (someone was found)
    private fun listenForNews(gameId: String): ListenerRegistration {
        return FirebaseHelper.getNews(gameId)
            .addSnapshotListener { data, e ->
                Log.d(TAG, "listenForNews: player found ${data?.size()}")
                data ?: kotlin.run {
                    Log.e(TAG, "listenForNews: ", e)
                    return@addSnapshotListener
                }
                val newsList = data.toObjects(News::class.java)
                if (newsList.size > newsCount) {
                    Log.d(TAG, "listenForNews: send notif")
                    val latestNews = newsList[0]
                    sendNewsNotification(latestNews.text)
                    newsCount = newsList.size
                }
            }
    }

    // Triggers the notification that informs the players of the end of the game when the lobby status
    // becomes finished
    private fun listenToLobby(gameId: String): ListenerRegistration {
        return FirebaseHelper.getLobby(gameId)
            .addSnapshotListener { data, e ->
                data ?: kotlin.run {
                    Log.e(TAG, "listenToLobby: ", e)
                    return@addSnapshotListener
                }
                val lobby = data.toObject<Lobby>()
                if (lobby?.status == LobbyStatus.FINISHED.ordinal) {
                    val message = "The seekers have found the last player!\n" +
                            "The game session will end in one minute"
                    sendEndGameNotification(message)
                }
            }
    }

    // Creates the foreground service notification
    private fun buildMainNotification(timeLeft: Int?): Notification {
        val timeText = timeLeft?.let { secondsToText(it) } ?: "Initializing the timer"
        return NotificationHelper.createNotification(
            context = applicationContext,
            title = "Seekers - Game in progress",
            content = timeText,
            pendingIntent = getPendingIntent(),
        )
    }

    // Gets the time left before the end of the game even if the user restarts the app mid-game
    private fun getTime(gameId: String) {
        val now = Timestamp.now().toDate().time.div(1000)
        FirebaseHelper.getLobby(gameId = gameId).get()
            .addOnSuccessListener {
                val lobby = it.toObject(Lobby::class.java)
                lobby?.let {
                    val startTime = lobby.startTime.toDate().time / 1000
                    val countdown = lobby.countdown
                    val timeLimit = lobby.timeLimit * 60
                    val gameEndTime = (startTime + countdown + timeLimit)
                    val timeLeft = (gameEndTime.minus(now).toInt() + 1)
                    startTimer(timeLeft)
                }
            }
    }

    // Start the timer til the end of the game and broadcasts it.
    // Ends the game onFinish
    private fun startTimer(timeLeft: Int) {
        timer = object : CountDownTimer(timeLeft * 1000L, 1000) {
            override fun onTick(p0: Long) {
                val seconds = p0.div(1000).toInt()
                updateMainNotification(seconds)
                broadcastCountdown(seconds)
            }

            override fun onFinish() {
                timeUp()
                this.cancel()
            }
        }
        timer?.start()
    }

    // Broadcasts the countdown used in the game so that it is shown on the HeatMapScreen
    fun broadcastCountdown(seconds: Int) {
        val countDownIntent = Intent()
        countDownIntent.action = COUNTDOWN_TICK
        countDownIntent.putExtra(TIME_LEFT, seconds)
        sendBroadcast(countDownIntent)
    }

    // Stops the timer in case the game is over or the player leaves the game
    private fun stopTimer() {
        timer?.cancel()
    }

    // Updates the foreground notification with a new countdown value
    fun updateMainNotification(timeLeft: Int) {
        with(NotificationManagerCompat.from(applicationContext)) {
            notify(MAIN_NOTIFICATION_ID, buildMainNotification(timeLeft))
        }
    }

    // Sets the status of the current lobby to finished at the end of the countdown timer.
    // Stops the foreground service after that.
    fun timeUp() {
        sendEndGameNotification("Time is up and some players remained hidden! The seekers lose!")
        currentGameId?.let { id ->
            FirebaseHelper.getLobby(id).get().addOnSuccessListener {
                val status = it.toObject<Lobby>()?.status
                if (status != LobbyStatus.FINISHED.ordinal) {
                    val map = mapOf(
                        Pair("status", LobbyStatus.FINISHED.ordinal),
                        Pair("endGameTime", Timestamp.now())
                    )
                    FirebaseHelper.updateLobby(map, gameId = id)
                }
                scope.launch {
                    delay(2000)
                    stop(applicationContext)
                }
            }
        }
    }

    // Starts the foreground service and creates all the notification channels.
    // Starts tracking location and the listeners for updates in firestore
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val gameId = intent?.getStringExtra("gameId")!!
        currentGameId = gameId
        val isSeeker = intent.getBooleanExtra("isSeeker", false)
        newsListener = listenForNews(gameId)
        lobbyListener = listenToLobby(gameId)

        NotificationHelper.createNotificationChannel(
            context = applicationContext,
            SEEKER_NOTIFICATION,
            importanceLevel = NotificationManager.IMPORTANCE_HIGH
        )
        NotificationHelper.createNotificationChannel(
            context = applicationContext,
            FOUND_NOTIFICATION,
            importanceLevel = NotificationManager.IMPORTANCE_HIGH
        )
        NotificationHelper.createNotificationChannel(
            context = applicationContext,
            END_NOTIFICATION,
            importanceLevel = NotificationManager.IMPORTANCE_HIGH
        )
        NotificationHelper.createNotificationChannel(context = applicationContext)
        val notification = buildMainNotification(null)
        startForeground(MAIN_NOTIFICATION_ID, notification)
        startTracking(gameId, isSeeker)
        getTime(gameId)
        return START_NOT_STICKY
    }

    // When the service is destroyed, stops tracking location, the countdown timer,
    // the listeners for changes in firestore, and closes the foreground notification.
    override fun onDestroy() {
        super.onDestroy()
        if (isTracking) {
            callback?.let {
                stopTracking(it)
                stopTimer()
            }
        }
        timer?.cancel()
        newsListener?.remove()
        lobbyListener?.remove()
    }
}