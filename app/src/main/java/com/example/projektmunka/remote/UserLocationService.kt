package com.example.projektmunka.remote

import com.example.projektmunka.data.UserLocation
import com.example.projektmunka.utils.Constants
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class UserLocationService {

    private val fireStore = FirebaseFirestore.getInstance()

    private val _currentUserLocationData = MutableStateFlow<UserLocation?>(null)
    val currentUserLocationData = _currentUserLocationData.asStateFlow()

    suspend fun getUserLocationData(id: String) {
        val result = fireStore.collection(Constants.USER_LOCATIONS)
            .document(id)
            .get().await()
        _currentUserLocationData.emit(result.toObject(UserLocation::class.java))
    }

    suspend fun registerUserLocationIntoFirestore(userLocationInfo: UserLocation) {

        fireStore.collection(Constants.USER_LOCATIONS)
            .document(userLocationInfo.id)
            .set(userLocationInfo, SetOptions.merge())
            .await()
    }

    suspend fun deleteUserLocationIntoFirestore(userLocationInfo: UserLocation) {

        fireStore.collection(Constants.USER_LOCATIONS)
            .document(userLocationInfo.id)
            .delete()
            .await()
    }

    suspend fun updateUserLocationField(key: String, value: String, id: String) {
        fireStore.collection(Constants.USER_LOCATIONS)
            .document(id)
            .update(key, value)
            .await()
    }

    suspend fun getAllUserLocations(): MutableList<UserLocation> {
        val userLocations = mutableListOf<UserLocation>()

        val result = fireStore.collection(Constants.USER_LOCATIONS).get().await()

        for (document in result.documents) {
            val userlocation = document.toObject(UserLocation::class.java)
            if (userlocation != null) {
                userLocations.add(userlocation)
            }
        }
        return userLocations
    }
}