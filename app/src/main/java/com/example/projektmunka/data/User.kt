package com.example.projektmunka.data
//import kotlinx.android.parcel.Parcelize
import android.os.Parcelable
import com.google.firebase.firestore.DocumentReference
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import java.util.Collections.emptyList

@Parcelize
data class User(
    val id: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val age: String = "",
    val weight: String = "",
    val image: String = "",
    val gender: String = "",
    val friends: List<User> = emptyList(),
    val friendRequests: MutableList<User> = emptyList(),
    val profileCompleted: Int = 0
) : Parcelable

