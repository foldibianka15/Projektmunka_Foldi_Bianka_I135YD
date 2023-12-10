package com.example.projektmunka.viewModel

import android.graphics.Bitmap
import androidx.databinding.Bindable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.firstapp.repository.FireStoreRepository
import com.example.projektmunka.data.User
import com.example.projektmunka.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.databinding.BaseObservable
import com.example.projektmunka.BR
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(private val authRepository: AuthRepository, private val fireStoreRepository: FireStoreRepository) : BaseViewModel() {

    val loginResult = authRepository.lastResult
    val uploadPhotoResult = fireStoreRepository.uploadPhotoResult
    val currentUserData = fireStoreRepository.currentUserData


    var bitmap: Bitmap? = null

    var firstName = MutableLiveData<String>()
    var lastName = MutableLiveData<String>()
    var email = MutableLiveData<String>()
    var age = MutableLiveData<String>()
    var gender = MutableLiveData<String>()
    var weight = MutableLiveData<String>()
    var password = MutableLiveData<String>()

    init {
        observeUserData()
    }

    fun observeUserData() {
        viewModelScope.launch(coroutineContext) {
            fireStoreRepository.currentUserData.collect {
                it?.let { user ->
                    println("nyala" + user.id)
                        email.value = user.email
                        firstName.value = user.firstName
                        lastName.value = user.lastName
                        weight.value = user.weight
                        age.value = user.age
                        gender.value = user.gender
                        println("nyúl: " + user.id)
                    }
                }

        }
    }

    fun updateUserProfile() {
        viewModelScope.launch(Dispatchers.IO) {
            authRepository.currentUser.value?.let { fireBaseUser ->

                    println("nyal" + fireBaseUser.uid)
                    val user = User(
                        id = fireBaseUser.uid,
                        lastName = lastName.value ?: "",
                        firstName = firstName.value ?: "",
                        email = fireBaseUser.email!!,
                        weight = weight.value ?: "",
                        age = age.value ?: "",
                        gender = gender.value ?: "",
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