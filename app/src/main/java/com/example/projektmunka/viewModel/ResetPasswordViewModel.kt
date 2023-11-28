package com.example.projektmunka.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projektmunka.repository.AuthRepository
import com.example.projektmunka.utils.isFieldNotEmpty
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ResetPasswordViewModel@Inject constructor(val authRepository: AuthRepository) : ViewModel() {

    val resetPasswordResult = authRepository.resetPasswordResult

    var email = ""

    fun resetPassword() {
        if (isFieldNotEmpty(email)) {
            viewModelScope.launch(Dispatchers.IO) {
                authRepository.resetPassword(email)
            }
        }
    }
}