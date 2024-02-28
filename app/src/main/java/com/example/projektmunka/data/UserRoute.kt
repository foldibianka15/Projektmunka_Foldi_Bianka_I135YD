package com.example.projektmunka.data

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class UserRoute(
    var id: String = "",
    var userId: DocumentReference? = null,
    var route: Route
)
