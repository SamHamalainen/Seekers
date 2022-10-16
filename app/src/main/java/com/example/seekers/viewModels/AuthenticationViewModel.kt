package com.example.seekers.viewModels

import android.content.ContentValues
import android.util.Log
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.seekers.general.isEmailValid
import com.example.seekers.general.isPasswordValid
import com.example.seekers.utils.FirebaseHelper
import com.example.seekers.utils.Lobby
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

/**
 * This ViewModel is used for LoginScreen, CreateUserScreen, MainScreen
 * It handles Authentication with Firebase.
 * Validates credentials of logging in and creating the user
 */
class AuthenticationViewModel() : ViewModel() {

    // Firestore
    var fireBaseAuth = Firebase.auth
    var firestore = FirebaseHelper

    // User
    var user = MutableLiveData<FirebaseUser>(null)
    var userExistsInUsers = MutableLiveData<Boolean>()

    // Email
    var email = MutableLiveData<TextFieldValue>()
    var emailValidationError = MutableLiveData<Boolean>()
    var emailIsAvailable = MutableLiveData<Boolean>()

    // Password
    var password = MutableLiveData<TextFieldValue>()
    var passwordValidationError = MutableLiveData<Boolean>()

    // Game
    val currentGameId = MutableLiveData<String>()
    val gameStatus = MutableLiveData<Int>()

    fun updatePasswordTextField(newValue: TextFieldValue) {
        password.value = newValue
    }

    fun updateEmailTextField(newValue: TextFieldValue) {
        email.value = newValue
    }

    // Validate email and set error based on returned value
    fun validateEmail(email: String) {
        if (!isEmailValid(email)) {
            emailValidationError.postValue(true)
        } else
            emailValidationError.postValue(false)
    }

    // Validate password and set error based on returned value
    fun validatePassword(password: String): Boolean {
        return if (!isPasswordValid(password)) {
            passwordValidationError.postValue(true)
            false
        } else {
            passwordValidationError.postValue(false)
            true
        }
    }

    // Creates user document in Firestore Database
    fun addUserDoc(userId: String, changeMap: Map<String, Any>) {
        firestore.addUser(changeMap, userId)
    }

    // Sets firebaseUser as the user
    fun setUser(firebaseUser: FirebaseUser?) {
        user.value = firebaseUser
    }

    // Checks if email is available when creating an account
    // Compares email TextField input with all existing users "email" field
    fun checkEmailAvailability(email: String) {
        firestore.getUsers().whereEqualTo("email", email).get().addOnSuccessListener { result ->
            if (result.documents.size == 0) {
                emailIsAvailable.value = true
            } else {
                emailIsAvailable.value = false
                emailValidationError.value = true
            }
        }
    }

    // Check if user exists in Users collection in Firestore Database
    fun checkUserInUsers(userId: String) {
        firestore.usersRef.get().addOnSuccessListener {
            val userList = it.documents.map { docs ->
                docs.id
            }
            userExistsInUsers.postValue(userList.contains(userId))
        }
    }

    // Check if player is in a game and returns the gameId
    fun checkCurrentGame(playerId: String) {
        firestore.getUser(playerId).get()
            .addOnFailureListener {
                Log.e(ContentValues.TAG, "checkCurrentGame: ", it)
            }
            .addOnSuccessListener {
                val gameId = it.getString("currentGameId")
                gameId?.let { id ->
                    currentGameId.postValue(id)
                }
            }
    }

    // Checks the games current status
    fun checkGameStatus(gameId: String) {
        firestore.getLobby(gameId).get()
            .addOnSuccessListener {
                val fetchedLobby = it.toObject(Lobby::class.java)
                fetchedLobby?.let { lobby ->
                    println("checkGameStatus " + lobby.status.toString())
                    gameStatus.postValue(lobby.status)
                }
            }
    }
}