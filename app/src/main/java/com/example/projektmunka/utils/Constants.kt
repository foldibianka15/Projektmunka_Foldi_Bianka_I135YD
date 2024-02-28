package com.example.projektmunka.utils

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.webkit.MimeTypeMap

object Constants {
    const val USERS: String = "users"
    const val USER_ROUTES: String = "userRoutes"
    const val USER_ROUTE_TRACKERS = "userRouteTrackers"
    const val MILESTONES: String = "milestones"
    const val USER_LOCATIONS: String = "userLocations"
    const val MILESTONES_LIST: String = "milestones_list"
    const val FIRSTAPP_PREFERENCES: String = "FirstAppPrefs"
    const val LOGGED_IN_USERNAME: String = "logged_in_username"
    const val EXTRA_USER_DETAILS: String = "extra_user_details"
    const val READ_STORAGE_PERMISSION_CODE = 2
    const val PICK_IMAGE_REQUEST_CODE = 1

    const val FIRST_NAME: String = "firstName"
    const val LAST_NAME: String = "lastName"
    const val MALE: String = "Male"
    const val FEMALE: String = "Female"

    const val MOBILE: String = "mobile"
    const val GENDER: String = "gender"
    const val USER_PROFILE_IMAGE:Int = 5
    const val TIMER_INTERVAL: Int = 1
    const val IMAGE: String = "image"
    const val COMPLETE_PROFILE: String = "profileCompleted"

    // Tracking Options
    const val LOCATION_UPDATE_INTERVAL = 5000L
    const val FASTEST_LOCATION_UPDATE_INTERVAL = 2000L

    const val PEDESTRIAN_SPEED = 1.1
    const val SEARCH_RADIUS_NEAREST_NODE = 300.0
    const val MAX_DISTANCE_SEARCH_IMPORTANT_POIS = 0.1
    const val EXIT_DiSTANCE_FIND_NEAREST_NON_ISOLATED_NODE = 0.0
    const val NUM_KEY_POIS = 5

    fun showImageChooser(activity: Activity) {
        val galleryIntent = Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )
        activity.startActivityForResult(galleryIntent, PICK_IMAGE_REQUEST_CODE)
    }

    fun getFileExtension(activity: Activity, uri: Uri?): String? {

        return MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(activity.contentResolver.getType(uri!!))
    }
}