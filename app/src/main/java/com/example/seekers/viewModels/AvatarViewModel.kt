package com.example.seekers.viewModels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.seekers.FirebaseHelper
import com.example.seekers.R

class AvatarViewModel() : ViewModel() {
    val avatarId = MutableLiveData<Int>(R.drawable.avatar_empty)
    val nickname = MutableLiveData("")
    val firestore = FirebaseHelper
    val nicknameError = MutableLiveData<Boolean>()
    val showNicknameEmpty = MutableLiveData<Boolean>()

}