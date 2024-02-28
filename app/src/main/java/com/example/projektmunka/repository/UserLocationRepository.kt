package com.example.projektmunka.repository

import com.example.projektmunka.data.User
import com.example.projektmunka.data.UserLocation
import com.example.projektmunka.remote.UserLocationDao

class UserLocationRepository(private val userLocationDao: UserLocationDao) {

    val allUserLocation = userLocationDao.allUserLocations

    suspend fun saveUserLocation(userLocationInfo: UserLocation, userId: String){
        userLocationDao.saveUserLocationIntoFirestore(userLocationInfo, userId)
    }
    suspend fun getUserLocation(user: User) {
        userLocationDao.getUserLccation(user)
    }
}