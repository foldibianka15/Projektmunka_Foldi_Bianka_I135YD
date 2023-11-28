package com.example.firstapp.repository

import android.graphics.Bitmap
import com.example.firstapp.data.models.remote.services.FireStoreService
import com.example.projektmunka.data.User
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FireStoreRepository(val fireStoreService: FireStoreService) {

    val currentUserData = fireStoreService.currentUserData
    val uploadPhotoResult = fireStoreService.uploadPhotoResult

    suspend fun getUserProfileData(userId: String){
        fireStoreService.getUserProfileData(userId)
    }

    suspend fun registerFromGoogleAccount(account: GoogleSignInAccount) {
        val user = User(
            id = account.id ?: "",
            firstName = "", // You can leave these empty initially
            lastName = "",  // or set them to default values
            email = account.email ?: "",
            // You can set other user properties here
        )
        fireStoreService.registerUserWithGoogle(user)
    }
    suspend fun registerUserIntoFireStore(
        userInfo: FirebaseUser,
        firstName: String,
        lastName: String
    ) {

        val user = User(
            id = userInfo.uid,
            firstName = firstName,
            lastName = lastName,
            email = userInfo.email!!
        )
        fireStoreService.registerUserIntoFirestore(user)
    }

    suspend fun checkForExistingUser(googleId: String, email: String): User? {
        // Replace this with actual code to query your database
        // You might use Firebase Firestore, Room, or another database system
        // Here's an example using Firebase Firestore as you are using it in your app

        val firestore = FirebaseFirestore.getInstance()
        val usersCollection = firestore.collection("users")

        // Check if a user with the same Google ID exists
        val googleIdQuery = usersCollection.whereEqualTo("googleId", googleId).get().await()
        if (!googleIdQuery.isEmpty) {
            return googleIdQuery.documents[0].toObject(User::class.java)
        }

        // Check if a user with the same email exists
        val emailQuery = usersCollection.whereEqualTo("email", email).get().await()
        if (!emailQuery.isEmpty) {
            return emailQuery.documents[0].toObject(User::class.java)
        }

        return null // No existing user found
    }

    suspend fun updateUser(userInfo: User) {
        fireStoreService.updateUserProfileData(userInfo)
    }

    suspend fun uploadPhoto(bitmap: Bitmap, id: String){
        fireStoreService.uploadImageCloudStorage(bitmap, id)
    }
}