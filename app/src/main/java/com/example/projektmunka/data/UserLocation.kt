package com.example.projektmunka.data

import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ServerTimestamp
import java.util.*

class UserLocation (
    val id : String = "",

    var geoPoint: GeoPoint? = null,

    @ServerTimestamp
    var timeStamp: Date? = null,

    var userId: String? = null
)