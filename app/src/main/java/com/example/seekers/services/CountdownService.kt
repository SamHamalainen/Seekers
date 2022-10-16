package com.example.seekers.services

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.*
import androidx.core.app.NotificationManagerCompat
import com.example.seekers.*
import com.example.seekers.general.secondsToText
import com.example.seekers.utils.*
import com.google.firebase.Timestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * CountdownService: Foreground service that shows the countdown until a game starts as a notification.
 * The users do not need to have the app open on their phone to see the ongoing countdown.
 */

class CountdownService: Service() {
    companion object {

        // Start the foreground service with the current game's id as an intent
        fun start(context: Context, gameId: String) {
            val intent = Intent(context, CountdownService::class.java)
            intent.putExtra("gameId", gameId)
            context.startForegroundService(intent)
        }

        // Stops the foreground service
        fun stop(context: Context) {
            val intent = Intent(context, CountdownService::class.java)
            context.stopService(intent)
        }
    }


    val firestore = FirebaseHelper
    private var timer: CountDownTimer? = null
    private val scope = CoroutineScope(Dispatchers.Default)
    private var mediaPlayerHidingPhaseMusic: MediaPlayer? = null
    var mediaPlayerCountdown: MediaPlayer? = null

    // Getters for the music players during countdown
    private fun mediaPlayerHidingPhaseMusic(): MediaPlayer = MediaPlayer.create(applicationContext,
        R.raw.countdown_music
    )
    fun mediaPlayerCountdown() : MediaPlayer = MediaPlayer.create(applicationContext,
        R.raw.counting_down
    )

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    // Handles the vibration of the device at the end of the countdown
    fun vibrate(milliseconds: Long) {
        val vib = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
        vib.vibrate(VibrationEffect.createOneShot(milliseconds,255))
    }

    // Determines if the current user is a seeker
    private fun getIsSeeker(gameId: String) {
        FirebaseHelper.getPlayer(gameId, FirebaseHelper.uid!!).get()
            .addOnSuccessListener {
                val player = it.toObject(Player::class.java)
                val isSeeker = player?.inGameStatus == InGameStatus.SEEKER.ordinal
                getInitialValue(gameId, isSeeker)
            }
    }

    // Gets the initial value of the countdown. If the app is restarted, retrieves the remaining countdown
    private fun getInitialValue(gameId: String, isSeeker: Boolean) {
        FirebaseHelper.getLobby(gameId).get()
            .addOnSuccessListener {
                val lobby = it.toObject(Lobby::class.java)
                lobby ?: return@addOnSuccessListener
                val start = lobby.startTime.toDate().time / 1000
                val countdownVal = lobby.countdown
                val now = Timestamp.now().toDate().time / 1000
                val remainingCountdown = start + countdownVal - now + 3
                if (timer == null) {
                    startTimer(timeLeft = remainingCountdown.toInt(), gameId, isSeeker)
                }
            }
    }

    // Starts the countdown timer at the end of which the game starts
    // The countdown is broadcast so that it can used in the CountdownScreen
    private fun startTimer(timeLeft: Int, gameId: String, isSeeker: Boolean) {
        mediaPlayerHidingPhaseMusic = mediaPlayerHidingPhaseMusic().apply {
            isLooping = true
            this.start()
        }
        timer = object: CountDownTimer(timeLeft * 1000L, 1000) {
            override fun onTick(p0: Long) {
                if (p0 == 0L) {
                    updateMainNotification(0)
                    broadcastCountdown(0)
                    if (isSeeker) {
                        FirebaseHelper.updateLobby(
                            mapOf(Pair("status", LobbyStatus.ACTIVE.ordinal)),
                            gameId
                        )
                    }
                    this.cancel()
                    return
                }
                val seconds = p0.div(1000).toInt()
                updateMainNotification(seconds)
                broadcastCountdown(seconds)

                if (isSeeker) {
                    if (seconds == 10) {
                        mediaPlayerCountdown = mediaPlayerCountdown().apply {
                            this.start()
                        }
                    }
                } else {
                    if (seconds in 1..5) {
                        vibrate(100)
                    }
                    if (seconds == 0) {
                        vibrate(500)
                    }
                }


            }
            override fun onFinish() {
                vibrate(500)
                if (isSeeker) {
                    FirebaseHelper.updateLobby(
                        mapOf(Pair("status", LobbyStatus.ACTIVE.ordinal)),
                        gameId
                    )
                }
                this.cancel()
                scope.launch {
                    delay(1000)
                    stop(applicationContext)
                }
            }
        }
        timer?.start()
    }

    // Broadcasting the countdown
    fun broadcastCountdown(seconds: Int) {
        val countDownIntent = Intent()
        countDownIntent.action = GameService.COUNTDOWN_TICK
        countDownIntent.putExtra(GameService.TIME_LEFT, seconds)
        sendBroadcast(countDownIntent)
    }

    // Get the intent that will be started if the user presses the foreground notification
    private fun getPendingIntent(): PendingIntent = PendingIntent.getActivity(
        applicationContext,
        0,
        Intent(applicationContext, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE
    )

    // Creates the foreground notification which shows the countdown
    private fun buildMainNotification(timeLeft: Int?): Notification {
        val timeText = timeLeft?.let { secondsToText(it) } ?: "Initializing the timer"
        return NotificationHelper.createNotification(
            context = applicationContext,
            title = "Seekers - Time to hide!",
            content = timeText,
            pendingIntent = getPendingIntent(),
        )
    }

    // Updates the notification with a new countdown value
    fun updateMainNotification(timeLeft: Int) {
        with(NotificationManagerCompat.from(applicationContext)) {
            notify(GameService.MAIN_NOTIFICATION_ID, buildMainNotification(timeLeft))
        }
    }

    // Starts the foreground service with it's corresponding notification and the countdown timer
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val gameId = intent?.getStringExtra("gameId")!!
        NotificationHelper.createNotificationChannel(context = applicationContext)
        val notification = buildMainNotification(null)
        startForeground(GameService.MAIN_NOTIFICATION_ID, notification)
        getIsSeeker(gameId)
        return START_NOT_STICKY
    }

    // When the service is started, stops the music
    override fun onDestroy() {
        super.onDestroy()
        scope.launch {
            delay(3000)
            mediaPlayerCountdown?.stop()
            mediaPlayerCountdown?.release()
        }
        mediaPlayerHidingPhaseMusic?.stop()
        mediaPlayerHidingPhaseMusic?.release()
    }
}