package com.example.projektmunka.viewModel

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.firstapp.repository.FireStoreRepository
import com.example.projektmunka.data.User
import com.example.projektmunka.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(private val authRepository: AuthRepository, private val fireStoreRepository: FireStoreRepository) : BaseViewModel() {

    val loginResult = authRepository.lastResult
    val uploadPhotoResult = fireStoreRepository.uploadPhotoResult


    init {
        println()
        viewModelScope.launch(coroutineContext) {
            fireStoreRepository.getUserProfileData("S5JW1kV39xZLtykvDiOJq67xh3w1")
            fireStoreRepository.currentUserData.collect {
                it?.let { user ->
                    email = user.email
                    firstName = user.firstName
                    lastName = user.lastName
                    weight = user.weight
                    age = user.age
                    gender = user.gender
                }
            }
        }
    }

    var bitmap: Bitmap? = null
    var email = ""
    var password = ""
    var lastName = ""
    var firstName = ""
    var gender = ""
    var weight = "0.00"
    var age = ""



    fun updateUserProfile() {
        viewModelScope.launch(Dispatchers.IO) {
            authRepository.currentUser.value?.let { fireBaseUser ->
                val user = User(
                    id = fireBaseUser.uid,
                    lastName = lastName,
                    firstName = firstName,
                    email = fireBaseUser.email!!,
                    weight = weight,
                    age = age,
                    gender = gender,
                )
                fireStoreRepository.updateUser(user)
            }
        }
    }

    fun uploadPhoto() {
        viewModelScope.launch(coroutineContext) {
            bitmap?.let {
                fireStoreRepository.uploadPhoto(
                    it,
                    authRepository.currentUser.value!!.uid

                )
            }
        }
    }
}