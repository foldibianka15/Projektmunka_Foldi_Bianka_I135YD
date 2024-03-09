package com.example.projektmunka.repository

import com.example.firstapp.repository.UserDataRepository
import com.example.projektmunka.dataremote.AuthDao
import com.google.android.gms.auth.api.signin.GoogleSignInAccount

class AuthRepository(private val authDao: AuthDao, val userDataRepository: UserDataRepository) {

    val lastResult = authDao.lastResult

    //val isLoggedIn = authService.isLoggedIn
    val currentUser = authDao.currentUser
    val resetPasswordResult = authDao.resetPasswordSendResult

    suspend fun signInWithGoogle(account: GoogleSignInAccount) {
        authDao.signInWithGoogle(account)
    }

    suspend fun register(email: String, password: String, firstName: String, lastName: String) {
        authDao.register(email, password)
        val user = currentUser.value
        if (user != null) {
            userDataRepository.registerUserIntoFireStore(user, firstName, lastName)
        }
    }

    suspend fun login(email: String, password: String) {
        authDao.login(email, password)
    }

    suspend fun resetPassword(email: String) {
        authDao.resetPassword(email)
    }

    suspend fun logout() {
        authDao.logout()
    }
}