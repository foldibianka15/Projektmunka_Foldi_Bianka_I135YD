package com.example.projektmunka.viewModel

import androidx.lifecycle.viewModelScope
import com.example.firstapp.repository.UserDataRepository
import com.example.projektmunka.RouteUtils.calculateGeodesicDistanceInMeters
import com.example.projektmunka.data.User
import com.example.projektmunka.repository.AuthRepository
import com.example.projektmunka.repository.NearbyUsersRepository
import com.google.firebase.auth.AuthResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NearbyUsersViewModel @Inject constructor(val nearbyUsersRepository: NearbyUsersRepository) : BaseViewModel() {

    private val _nearbyUsers = MutableSharedFlow<MutableList<User>>()
    val nearbyUsers = _nearbyUsers.asSharedFlow()

    fun getNearbyUsers(friendZone: Double) {

        viewModelScope.launch(coroutineContext) {
            val result = nearbyUsersRepository.getNearbyUsers(friendZone)
            _nearbyUsers.emit(result)
        }
    }
}