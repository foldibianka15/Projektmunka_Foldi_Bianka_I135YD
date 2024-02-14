package com.example.projektmunka.data

data class UserRouteTracker(
    val id: String,
    val userRoute: UserRoute,
    val duration: Double,
    val steps: Double,
    val calories: Double,
    val averageSpeed: Double,
    val heartRate: Double,
    val averageHeartRate: Double,
)