package com.example.projektmunka.data
import android.os.Parcelable
import com.example.projektmunka.data.ActivityLevel
//import kotlinx.android.parcel.Parcelize
import kotlinx.parcelize.Parcelize
import java.util.Collections.emptyList

@Parcelize
data class User (
    val id: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val age: String = "",
    val weight: String = "",
    val image: String = "",
    val gender: String = "",
    val friends: MutableList<User> = emptyList(),
    val friendRequests: MutableList<User> = emptyList(),
    val profileCompleted: Int = 0): Parcelable

