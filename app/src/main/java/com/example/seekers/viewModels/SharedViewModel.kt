package com.example.seekers.viewModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.example.seekers.services.GameService

class SharedViewModel(application: Application): AndroidViewModel(application) {
    val locService = MutableLiveData<GameService>()

    fun updateLocService(newVal: GameService) {
        locService.value = newVal
    }
}