package com.example.projektmunka.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projektmunka.repository.AuthRepository
import com.example.projektmunka.utils.isConfirmPassWordMatch
import com.example.projektmunka.utils.isFieldNotEmpty
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(val authRepository: AuthRepository) : ViewModel() {

    var lastResult = authRepository.lastResult

    var userName = ""
    var email = ""
    var password = ""
    var confirmPassword = ""
    var lastName = ""
    var firstName = ""

    fun registerUser() {
        if (isFieldNotEmpty(email) && isFieldNotEmpty(password) && isFieldNotEmpty(firstName)
            && isFieldNotEmpty(lastName) && isConfirmPassWordMatch(password, confirmPassword))
            viewModelScope.launch(Dispatchers.IO) {
                authRepository.register(email, password, firstName, lastName)
            }
    }
}