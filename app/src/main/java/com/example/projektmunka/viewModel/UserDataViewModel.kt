package com.example.projektmunka.viewModel

import androidx.lifecycle.viewModelScope
import com.example.firstapp.repository.FireStoreRepository
import com.example.projektmunka.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserDataViewModel @Inject constructor(private val authRepository: AuthRepository, val fireStoreRepository: FireStoreRepository): BaseViewModel() {

    val currentUserData = fireStoreRepository.currentUserData

    init {
        getUserData()
    }

    fun getUserData() {
        viewModelScope.launch(Dispatchers.IO) {
            authRepository.currentUser.collect {
                if (it != null) {
                    fireStoreRepository.getUserProfileData(it.uid)
                }
            }
        }
    }
}