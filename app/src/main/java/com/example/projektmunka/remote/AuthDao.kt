package com.example.projektmunka.dataremote

import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class AuthDao {

    private val auth = FirebaseAuth.getInstance()

    private val _lastResult = MutableSharedFlow<AuthResult>()
    val lastResult = _lastResult.asSharedFlow()

    private val _currentUser = MutableStateFlow(auth.currentUser)
    val currentUser = _currentUser.asStateFlow()

    private val _resetPasswordSendResult = MutableSharedFlow<Void>()
    val resetPasswordSendResult = _resetPasswordSendResult.asSharedFlow()

    private val googleSignInClient: GoogleSignInClient? = null

    suspend fun signInWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        val result = auth.signInWithCredential(credential).await()
        _lastResult.emit(result)
        _currentUser.emit(auth.currentUser)
    }

    suspend fun register(email: String, password: String) {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        _lastResult.emit(result)
        _currentUser.emit(auth.currentUser)
    }

    suspend fun login(email: String, password: String) {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        _lastResult.emit(result)
        _currentUser.emit(auth.currentUser)
    }

    suspend fun logout() {
        auth.signOut()
        _currentUser.emit(null)
    }

    suspend fun resetPassword(email: String) {
        val result = auth.sendPasswordResetEmail(email).await()
        _resetPasswordSendResult.emit(result)
    }
}