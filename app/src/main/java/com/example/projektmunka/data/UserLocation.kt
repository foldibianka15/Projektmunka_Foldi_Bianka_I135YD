package com.example.projektmunka.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

class UserLocation(
    val id: String = "",

    var lastLocation: GeoPoint? = null,

    @ServerTimestamp
    var timeStamp: Timestamp? = null,

    var userRef: DocumentReference? = null,

    var userRouteTracker: DocumentReference? = null
)