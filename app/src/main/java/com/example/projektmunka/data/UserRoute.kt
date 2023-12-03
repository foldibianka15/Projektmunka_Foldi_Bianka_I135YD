package com.example.projektmunka.data

import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.ServerTimestamp
import java.util.*

data class UserRoute(
    var id: String = "",
    var userId: String = "",

    @ServerTimestamp
    var startDate: Date? = null,

    val endDate: Date? = null,

    val isFinished: Boolean = false,

    )
