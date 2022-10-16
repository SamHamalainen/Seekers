package com.example.seekers.viewModels

import android.content.ContentValues
import android.util.Log
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.seekers.utils.FirebaseHelper
import com.example.seekers.utils.Lobby
import com.example.seekers.general.isEmailValid
import com.example.seekers.general.isPasswordValid
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class AuthenticationViewModel() : ViewModel() {

    var fireBaseAuth = Firebase.auth
    var user = MutableLiveData<FirebaseUser>(null)
    var userIsInUsers = MutableLiveData<Boolean>()
    var emailValidationError = MutableLiveData<Boolean>()
    var emailIsAvailable = MutableLiveData<Boolean>()
    var passwordValidationError = MutableLiveData<Boolean>()
    var firestore = FirebaseHelper
    val currentGameId = MutableLiveData<String>()
    val gameStatus = MutableLiveData<Int>()
    var email = MutableLiveData<TextFieldValue>()
    var password = MutableLiveData<TextFieldValue>()

    fun updatePasswordTextField(newValue: TextFieldValue) {
        password.value = newValue
    }
    fun updateEmailTextField(newValue: TextFieldValue) {
        email.value = newValue
    }

    fun validateEmail(email: String) {
        if (!isEmailValid(email)) {
            emailValidationError.postValue(true)
        } else
            emailValidationError.postValue(false)
    }

    fun validatePassword(password: String): Boolean {
        return if (!isPasswordValid(password)) {
            passwordValidationError.postValue(true)
            false
        } else {
            passwordValidationError.postValue(false)
            true
        }
    }

    fun updateUserDoc(userId: String, changeMap: Map<String, Any>) =
        firestore.updateUser(userId, changeMap)

    fun addUserDoc(userId: String, changeMap: Map<String, Any>) {
        firestore.addUser(changeMap, userId)
    }

    fun setUser(firebaseUser: FirebaseUser?) {
        user.value = firebaseUser
    }

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

    fun checkUserInUsers(userId: String) {
        firestore.usersRef.get().addOnSuccessListener {
            val userList = it.documents.map { docs ->
                docs.id
            }
            userIsInUsers.postValue(userList.contains(userId))
        }
    }

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

    fun checkGameStatus(gameId: String) {
        firestore.getLobby(gameId).get()
            .addOnSuccessListener {
                val lobby = it.toObject(Lobby::class.java)
                lobby?.let { lobby ->
                    println("checkGameStatus " + lobby.status.toString())
                    gameStatus.postValue(lobby.status)
                }
            }
    }
}