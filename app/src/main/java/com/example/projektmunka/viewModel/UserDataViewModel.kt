package com.example.projektmunka.viewModel

import androidx.lifecycle.viewModelScope
import com.example.firstapp.repository.UserDataRepository
import com.example.projektmunka.data.User
import com.example.projektmunka.data.UserLocation
import com.example.projektmunka.repository.AuthRepository
import com.example.projektmunka.repository.UserLocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserDataViewModel @Inject constructor(
    private val userDataRepository: UserDataRepository,
) : BaseViewModel() {

    val currentUserData = userDataRepository.currentUserData

    init {
        getUserData()
    }

    private fun getUserData() {
        viewModelScope.launch(Dispatchers.IO) {
            currentUserData
                .filterNotNull()
                .collect { user ->
                    userDataRepository.getUserProfileData(user.id)
                }
        }
    }

}