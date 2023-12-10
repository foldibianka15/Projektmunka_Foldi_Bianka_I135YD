package com.example.projektmunka.uiData

import android.os.Parcel
import android.os.Parcelable

data class TourItem(
    val name: String,
    val description: String,
    val detailedDescription: String,
    val imageId: Int
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeString(description)
        parcel.writeString(detailedDescription)
        parcel.writeInt(imageId)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<TourItem> {
        override fun createFromParcel(parcel: Parcel): TourItem {
            return TourItem(parcel)
        }

        override fun newArray(size: Int): Array<TourItem?> {
            return arrayOfNulls(size)
        }
    }
}