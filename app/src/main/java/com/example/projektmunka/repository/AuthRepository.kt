package com.example.projektmunka.repository

import com.example.firstapp.repository.FireStoreRepository
import com.example.projektmunka.dataremote.AuthService
import com.google.android.gms.auth.api.signin.GoogleSignInAccount

class AuthRepository(val authService: AuthService, val fireStoreRepository: FireStoreRepository) {

    val lastResult = authService.lastResult
    //val isLoggedIn = authService.isLoggedIn
    val currentUser = authService.currentUser
    val resetPasswordResult = authService.resetPasswordSendResult

    suspend fun signInWithGoogle(account: GoogleSignInAccount) {
        authService.signInWithGoogle(account)
    }

    suspend fun register(email: String, password: String, firstName: String, lastName: String){
        authService.register(email, password)
        val user = currentUser.value
        if (user != null) {
            fireStoreRepository.registerUserIntoFireStore(user, firstName, lastName)
        }
    }

    suspend fun login(email: String, password: String){
       authService.login(email, password)
    }

    suspend fun resetPassword(email: String){
        authService.resetPassword(email)
    }

    suspend fun logout(){
        authService.logout()
    }
}