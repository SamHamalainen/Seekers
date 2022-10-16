package com.example.seekers.utils

import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.*
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import java.io.ByteArrayOutputStream
import java.io.Serializable

/**
 * FirebaseHelper: Object containing all the functions that interact with Firebase
 */

object FirebaseHelper {
    private val lobbiesRef = Firebase.firestore.collection("lobbies")
    val usersRef = Firebase.firestore.collection("users")
    private const val TAG = "firestoreHelper"
    // get current user id
    val uid get() = Firebase.auth.uid

    // Add a new lobby when a game is created (see Lobby class below)
    fun addLobby(lobby: Lobby): String {
        val ref = lobbiesRef.document()
        val lobbyWithId = lobby.apply {
            id = ref.id
        }
        ref
            .set(lobbyWithId)
            .addOnSuccessListener {
                Log.d(TAG, "addLobby: " + "success (${ref.id})")
            }
            .addOnFailureListener {
                Log.e(TAG, "addLobby: ", it)
            }

        return ref.id
    }

    fun updateLobby(changeMap: Map<String, Any>, gameId: String) {
        val ref = lobbiesRef.document(gameId)
        ref
            .update(changeMap)
            .addOnSuccessListener {
                Log.d(TAG, "update: " + "success (${ref.id})")
            }
            .addOnFailureListener {
                Log.e(TAG, "update: ", it)
            }
    }

    // Reference to a lobby given it's gameId
    fun getLobby(gameId: String): DocumentReference {
        return lobbiesRef.document(gameId)
    }

    // Adding a player (see Player class below) to a lobby, given the lobby's gameId
    fun addPlayer(player: Player, gameId: String) {
        val playerRef = lobbiesRef.document(gameId).collection("players").document(player.playerId)
        playerRef
            .set(player)
            .addOnSuccessListener {
                Log.d(TAG, "addPlayer: " + "success (${playerRef.id})")
            }
            .addOnFailureListener {
                Log.e(TAG, "addPlayer: ", it)
            }
    }

    // Reference to the player collection of a given lobby
    fun getPlayers(gameId: String): CollectionReference {
        return lobbiesRef.document(gameId).collection("players")
    }

    // Reference to a single player within the players collection of a given lobby
    fun getPlayer(gameId: String, playerId: String): DocumentReference {
        return lobbiesRef.document(gameId).collection("players").document(playerId)
    }

    // Remove a player from a lobby
    fun removePlayer(gameId: String, playerId: String) {
        val playerRef = lobbiesRef.document(gameId).collection("players").document(playerId)
        playerRef.delete()
    }

    // Reference to a user document given its user id
    fun getUser(playerId: String): DocumentReference {
        return usersRef.document(playerId)
    }

    // Reference to the users collection with all the users who registered to the app
    fun getUsers(): CollectionReference {
        return usersRef
    }

    // Create a document for a user when they first authenticate
    fun addUser(map: Map<String, Any>, uid: String) {
        usersRef.document(uid)
            .set(map)
            .addOnSuccessListener {
                Log.d(TAG, "addUser: $uid")
            }
    }

    fun updateUser(userId: String, changeMap: Map<String, Any>) {
        usersRef.document(userId)
            .update(changeMap)
            .addOnSuccessListener {
                Log.d(TAG, "updateUser: $userId updated successfully")
            }
    }

    fun updatePlayer(changeMap: Map<String, Any>, playerId: String, gameId: String) {
        val playerRef = lobbiesRef.document(gameId).collection("players").document(playerId)
        playerRef.update(changeMap)
            .addOnSuccessListener {
                Log.d(TAG, "updatePlayerLocation: $playerId location updated")
            }
            .addOnFailureListener {
                Log.e(TAG, "update: ", it)
            }
    }

    // Update a player's in game status (see enum class InGameStatus below)
    fun updatePlayerInGameStatus(inGameStatus: Int, gameId: String, playerId: String) {
        val playerRef = lobbiesRef.document(gameId).collection("players").document(playerId)
        playerRef.update(mapOf(Pair("inGameStatus", inGameStatus)))
            .addOnSuccessListener {
                Log.d(TAG, "updatePlayerInGameStatus: $playerId status updated")
            }
            .addOnFailureListener {
                Log.e(TAG, "updatePlayerInGameStatus: ", it)
            }
    }

    // Add a selfie picture to Firebase storage and its corresponding news document to a given lobby
    fun sendSelfie(playerId: String, gameId: String, selfie: Bitmap, nickname: String) {
        val storageRef = Firebase.storage.reference.child("lobbies").child(gameId).child(playerId)
        val baos = ByteArrayOutputStream()
        selfie.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val bytes = baos.toByteArray()
        storageRef.putBytes(bytes)
            .addOnSuccessListener {
                Log.d(TAG, "sendSelfie: picture uploaded ($playerId)")
                val news = News(
                    picId = playerId,
                    text = "$nickname was found!",
                    timestamp = Timestamp.now()
                )
                addFoundNews(news = news, gameId, playerId)
            }
    }

    // Add a news item given a player and a lobby (see News class below)
    fun addFoundNews(news: News, gameId: String, playerId: String) {
        lobbiesRef.document(gameId).collection("news").document(playerId)
            .set(news)
            .addOnSuccessListener {
                Log.d(TAG, "addFoundNews: ${news.picId}")
            }
    }

    // Reference to all the news items related to a lobby
    fun getNews(gameId: String): Query {
        return lobbiesRef.document(gameId).collection("news")
            .orderBy("timestamp", Query.Direction.DESCENDING)
    }

    // Reference to a picture in Firebase Storage given a lobby and a picture id
    fun getSelfieImage(gameId: String, picId: String): StorageReference {
        return Firebase.storage.reference.child("lobbies").child(gameId).child(picId)
    }
}

// Represents a game with a set of rules.
// Its status determines the progress stage of the game.
class Lobby(
    var id: String = "",
    val center: GeoPoint = GeoPoint(0.0, 0.0),
    val maxPlayers: Int = 0,
    val timeLimit: Int = 0,
    val radius: Int = 0,
    val status: Int = 0,
    val startTime: Timestamp = Timestamp.now(),
    val endGameTime: Timestamp = Timestamp.now(),
    val countdown: Int = 0
) : Serializable

// Represents a player in a lobby.
// Its inLobbyStatus determines whether it is the creator or a joining player.
// Its inGameStatus determines its status during the game (see InGameStatus below)
class Player(
    val nickname: String = "",
    val avatarId: Int = 0,
    val playerId: String = "",
    val inLobbyStatus: Int = 0,
    val inGameStatus: Int = 0,
    var distanceStatus: Int = 0,
    val location: GeoPoint = GeoPoint(0.0, 0.0),
    val timeOfElimination: Timestamp = Timestamp(0L, 0),
    val foundBy: String = ""
) : Serializable

// Represents an event in a game, for example when a player was found.
// A picture can be associated to it and stored in Firebase storage
class News(
    val picId: String = "",
    val text: String = "",
    val timestamp: Timestamp = Timestamp.now(),
) : Serializable

// A player's status within a lobby
enum class InLobbyStatus {
    CREATOR,
    JOINED,
}

// A player's status in a game.
// It can be about its current role, if it is currently moving, if it is currently using a power, etc.
enum class InGameStatus {
    SEEKER,
    HIDING,
    MOVING,
    ELIMINATED,
    INVISIBLE,
    JAMMED,
    DECOYED,
    LEFT
}

// Classification for a players with regards to its distance to a seeker
enum class PlayerDistance {
    NOT_IN_RADAR,
    WITHIN10,
    WITHIN50,
    WITHIN100
}

// Advancement stage of a game lobby
enum class LobbyStatus {
    CREATED,
    ACTIVE,
    COUNTDOWN,
    FINISHED,
    DELETED,
}