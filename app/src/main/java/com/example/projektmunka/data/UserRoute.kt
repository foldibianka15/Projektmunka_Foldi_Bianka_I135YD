package com.example.projektmunka.data

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class UserRoute(
    var id: String = "",
    var userId: String = "",

    @ServerTimestamp
    var startDate: Date? = null,

    val endDate: Date? = null,

    val isFinished: Boolean = false,
)
