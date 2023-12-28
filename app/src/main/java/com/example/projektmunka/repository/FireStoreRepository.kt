package com.example.firstapp.repository

import android.graphics.Bitmap
import com.example.projektmunka.data.User
import com.example.projektmunka.data.UserLocation
import com.example.projektmunka.remote.UserDataService
import com.example.projektmunka.remote.UserLocationService
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FireStoreRepository(val userDataService: UserDataService, val userLocationService: UserLocationService) {

    val currentUserData = userDataService.currentUserData
    val uploadPhotoResult = userDataService.uploadPhotoResult

    private val currentUserLocationData = userLocationService.currentUserLocationData

    suspend fun getUserProfileData(userId: String){
        userDataService.getUserProfileData(userId)
    }

    suspend fun registerFromGoogleAccount(account: GoogleSignInAccount) {
        val user = User(
            id = account.id ?: "",
            firstName = "", // You can leave these empty initially
            lastName = "",  // or set them to default values
            email = account.email ?: "",
            // You can set other user properties here
        )
        userDataService.registerUserWithGoogle(user)
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
        userDataService.registerUserIntoFirestore(user)
    }

    suspend fun checkForExistingUser(googleId: String, email: String): User? {

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
        userDataService.updateUserProfileData(userInfo)
    }

    suspend fun uploadPhoto(bitmap: Bitmap, id: String){
        userDataService.uploadImageCloudStorage(bitmap, id)
    }

    suspend fun getAllUsers() : MutableList<User> {
        return userDataService.getAllUsers()
    }

    suspend fun getAllUserLocations(user: User) : MutableList<UserLocation> {
        return userLocationService.getAllUserLocations()
    }
}