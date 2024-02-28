package com.example.projektmunka.data

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date
import com.google.firebase.Timestamp

data class UserRouteTracker(
    val id: String,
    val userRouteId: DocumentReference? = null,
    @ServerTimestamp
    var startDate: Timestamp? = null,
    val endDate: Timestamp? = null,
    val duration: Double,
    val steps: Double,
    val calories: Double,
    val averageSpeed: Double,
    val heartRate: Double,
    val averageHeartRate: Double,
    val isFinished: Boolean = false
)