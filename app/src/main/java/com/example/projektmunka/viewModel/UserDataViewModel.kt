package com.example.projektmunka.viewModel

import androidx.lifecycle.viewModelScope
import com.example.firstapp.repository.FireStoreRepository
import com.example.projektmunka.data.User
import com.example.projektmunka.data.UserLocation
import com.example.projektmunka.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class UserDataViewModel @Inject constructor(private val authRepository: AuthRepository, val fireStoreRepository: FireStoreRepository): BaseViewModel() {

    val currentUserData = fireStoreRepository.currentUserData

    init {
        getUserData()
    }

    private fun getUserData() {
        viewModelScope.launch(Dispatchers.IO) {
            authRepository.currentUser.collect {
                if (it != null) {
                    fireStoreRepository.getUserProfileData(it.uid)
                }
            }
        }
    }

    fun getAllUsers(): MutableList<User> {
        return runBlocking {
            // Use withContext to switch to a background thread
            withContext(Dispatchers.IO) {
                // Call the suspend function within a coroutine
                fireStoreRepository.getAllUsers()
            }
        }
    }

    fun getUserLocation(user: User) : UserLocation {

        return runBlocking {
            // Use withContext to switch to a background thread
            withContext(Dispatchers.IO) {
                // Call the suspend function within a coroutine
                fireStoreRepository.getAllUserLocations(user).first {it.userId == user.id}
            }
        }
    }
}