package com.example.projektmunka.repository

import com.example.firstapp.repository.UserDataRepository
import com.example.projektmunka.RouteUtils.calculateGeodesicDistanceInMeters
import com.example.projektmunka.data.User
import com.example.projektmunka.remote.UserDataDao
import com.example.projektmunka.remote.UserLocationDao

class NearbyUsersRepository(private val userDataDao: UserDataDao, private val userLocationDao: UserLocationDao)
{
    suspend fun getNearbyUsers(friendZone: Double): MutableList<User> {
        val currentUser = userDataDao.currentUserData.value ?: return mutableListOf()
        val currentUserLocation = userLocationDao.currentUserLocation
        val allUsers = userDataDao.allUsers.value.toMutableList()
        allUsers.remove(currentUser)
        val nearbyUsers = mutableListOf<User>()

        for (user in allUsers) {
            if (user != null) {
                userLocationDao.getUserLccation(user)
            }
            val userLocation = userLocationDao.currentUserLocation
            val distanceBetweenUsers =
                userLocation.value?.lastLocation?.let {
                    currentUserLocation.value?.lastLocation?.let { it1 ->
                        calculateGeodesicDistanceInMeters(
                            it1,
                            it
                        )
                    }
                }
            if (distanceBetweenUsers != null) {
                if (distanceBetweenUsers <= friendZone && !currentUser.friends.contains(user)) {
                    if (user != null) {
                        nearbyUsers.add(user)
                    }
                }
            }
        }

        return nearbyUsers
    }
}