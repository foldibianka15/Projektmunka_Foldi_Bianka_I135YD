package com.example.projektmunka.remote

import com.example.projektmunka.data.UserRouteTracker
import com.example.projektmunka.utils.Constants
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class UserRouteTrackerService {

    private val fireStore = FirebaseFirestore.getInstance()

    private val _currentUserRouteTrackerData = MutableStateFlow<UserRouteTracker?>(null)
    val currentUserRouteTrackerData = _currentUserRouteTrackerData.asStateFlow()

    suspend fun getUserRouteTrackerData(id: String) {
        val result = fireStore.collection(Constants.USER_ROUTE_TRACKERS)
            .document(id)
            .get().await()
        _currentUserRouteTrackerData.emit(result.toObject(UserRouteTracker::class.java))
    }

    suspend fun registerUserRouteTrackerIntoFirestore(userRouteTrackerInfo: UserRouteTracker) {

        fireStore.collection(Constants.USER_ROUTE_TRACKERS)
            .document(userRouteTrackerInfo.id)
            .set(userRouteTrackerInfo, SetOptions.merge())
            .await()
    }

    suspend fun updateUserRouteTrackerField(key: String, value: String, id: String) {
        fireStore.collection(Constants.USER_ROUTE_TRACKERS)
            .document(id)
            .update(key, value)
            .await()
    }


    suspend fun getAllUserRouteTrackers(): MutableList<UserRouteTracker> {
        val userRouteTrackers = mutableListOf<UserRouteTracker>()

        val result = fireStore.collection(Constants.USER_ROUTE_TRACKERS).get().await()

        for (document in result.documents) {
            val userRouteTracker = document.toObject(UserRouteTracker::class.java)
            if (userRouteTracker != null) {
                userRouteTrackers.add(userRouteTracker)
            }
        }

        return userRouteTrackers
    }
}