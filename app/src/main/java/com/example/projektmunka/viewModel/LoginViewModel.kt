package com.example.projektmunka.viewModel

import androidx.lifecycle.viewModelScope
import com.example.firstapp.repository.UserDataRepository
import com.example.projektmunka.repository.AuthRepository
import com.example.projektmunka.utils.isFieldNotEmpty
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(val authRepository: AuthRepository, val userDataRepository: UserDataRepository) : BaseViewModel() {

    val loginResult = authRepository.lastResult

    var email = ""
    var password = ""

    fun loginRegisteredUser() {
        if (isFieldNotEmpty(email) && isFieldNotEmpty(password)) {
            viewModelScope.launch(coroutineContext) {
                authRepository.login(email, password)
            }
        }
    }

    // Create a function to perform Google Sign-In
    fun signInWithGoogle(account: GoogleSignInAccount) {
        viewModelScope.launch(coroutineContext) {

            val googleId = account.id ?: ""
            val email = account.email ?: ""

            // Check if the user with this Google ID or email already exists
            val existingUser = userDataRepository.checkForExistingUser(googleId, email)

            if (existingUser != null){
                authRepository.signInWithGoogle(account)
            } else {
                viewModelScope.launch(Dispatchers.IO) {
                    // After signing in with Google, create a user collection
                    userDataRepository.registerFromGoogleAccount(account)
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            // After signing in with Google, create a user collection
            userDataRepository.registerFromGoogleAccount(account)
        }
    }
}