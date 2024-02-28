package com.example.projektmunka.remote

import com.example.projektmunka.data.User
import com.example.projektmunka.data.UserLocation
import com.example.projektmunka.utils.Constants
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class UserLocationDao {

    private val fireStore = FirebaseFirestore.getInstance()

    private val _currentUserLocationData = MutableStateFlow<UserLocation?>(null)
    val currentUserLocationData = _currentUserLocationData.asStateFlow()

    private val _allUserLocations = MutableStateFlow<List<UserLocation?>>(emptyList())
    val allUserLocations = _allUserLocations.asStateFlow()

    private val _currentUserLocation = MutableStateFlow<UserLocation?>(null)
    val currentUserLocation = _currentUserLocation.asStateFlow()

    suspend fun getUserLocationData(id: String) {
        val result = fireStore.collection(Constants.USER_LOCATIONS)
            .document(id)
            .get().await()
        _currentUserLocationData.emit(result.toObject(UserLocation::class.java))
    }

    suspend fun saveUserLocationIntoFirestore(userLocationInfo: UserLocation, userId: String) {
        val userRef = fireStore.collection(Constants.USERS).document(userId)
        userLocationInfo.userRef = userRef

        fireStore.collection(Constants.USER_LOCATIONS)
            .document(userLocationInfo.id)
            .set(userLocationInfo, SetOptions.merge())
            .await()
    }

    suspend fun deleteUserLocationInFirestore(userLocationInfo: UserLocation) {
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

    suspend fun getAllUserLocations() {
        val userLocations = mutableListOf<UserLocation>()

        val result = fireStore.collection(Constants.USER_LOCATIONS).get().await()

        for (document in result.documents) {
            val userLocation = document.toObject(UserLocation::class.java)
            if (userLocation != null) {
                userLocations.add(userLocation)
            }
        }
        _allUserLocations.emit(userLocations)
    }

    suspend fun getUserLccation(user: User) {
        val querySnapshot = fireStore.collection(Constants.USER_LOCATIONS)
            .whereEqualTo("id", user.id)
            .limit(1)
            .get()
            .await()

        val userLocation = querySnapshot.documents.firstOrNull()?.toObject(UserLocation::class.java)
        _currentUserLocation.emit(userLocation)
    }
}