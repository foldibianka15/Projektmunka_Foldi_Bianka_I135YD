package com.example.projektmunka.remote

import com.example.projektmunka.data.UserRoute
import com.example.projektmunka.utils.Constants
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class UserRouteService {

    private val fireStore = FirebaseFirestore.getInstance()

    private val _currentUserRouteData = MutableStateFlow<UserRoute?>(null)
    val currentUserRouteData = _currentUserRouteData.asStateFlow()

    suspend fun getUserRouteData(id: String) {
        val result = fireStore.collection(Constants.USER_ROUTES)
            .document(id)
            .get().await()
        _currentUserRouteData.emit(result.toObject(UserRoute::class.java))
    }

    suspend fun registerUserRouteIntoFirestore(userRouteInfo: UserRoute) {

        fireStore.collection(Constants.USER_ROUTES)
            .document(userRouteInfo.id)
            .set(userRouteInfo, SetOptions.merge())
            .await()
    }

    suspend fun updateUserRouteField(key: String, value: String, id: String){
        fireStore.collection(Constants.USER_ROUTES)
            .document(id)
            .update(key, value)
            .await()
    }


    suspend fun getAllUserRoutes(): MutableList<UserRoute> {
        val userRoutes = mutableListOf<UserRoute>()

        val result = fireStore.collection(Constants.USER_ROUTES).get().await()

        for (document in result.documents) {
            val userRoute = document.toObject(UserRoute::class.java)
            if (userRoute != null) {
                userRoutes.add(userRoute)
            }
        }

        return userRoutes
    }
}