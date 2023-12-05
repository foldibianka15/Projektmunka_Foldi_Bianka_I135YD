package com.example.firstapp.data.models.remote.services

import android.graphics.Bitmap
import com.example.projektmunka.data.User
import com.example.projektmunka.data.UserLocation
import com.example.projektmunka.data.UserRoute
import com.example.projektmunka.data.UserRouteTracker
import com.example.projektmunka.utils.Constants
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream

class FireStoreService {

    private val fireStore = FirebaseFirestore.getInstance()

    private val _currentUserData = MutableStateFlow<User?>(null)
    val currentUserData = _currentUserData.asStateFlow()

    private val _uploadPhotoResult = MutableSharedFlow<String>()
    val uploadPhotoResult = _uploadPhotoResult.asSharedFlow()

    private val _currentUserRouteData = MutableStateFlow<UserRoute?>(null)
    val currentUserRouteData = _currentUserRouteData.asStateFlow()

    private val _currentUserRouteTrackerData = MutableStateFlow<UserRouteTracker?>(null)
    val currentUserRouteTrackerData = _currentUserRouteTrackerData.asStateFlow()

    private val _currentUserLocationData = MutableStateFlow<UserLocation?>(null)
    val currentUserLocationData = _currentUserLocationData.asStateFlow()

    suspend fun getUserProfileData(id: String) {
        val result = fireStore.collection(Constants.USERS)
            .document(id)
            .get().await()
        _currentUserData.emit(result.toObject(User::class.java))
    }

    suspend fun registerUserWithGoogle(userInfo: User){
        fireStore.collection(Constants.USERS)
            .document(userInfo.id)
            .set(userInfo, SetOptions.merge()).await()
    }

    suspend fun registerUserIntoFirestore(userInfo: User) {
        fireStore.collection(Constants.USERS)
            .document(userInfo.id)
            .set(userInfo, SetOptions.merge()).await()
    }

    suspend fun updateUserProfileData(userInfo: User) {

        fireStore.collection(Constants.USERS)
            .document(userInfo.id)
            .set(userInfo, SetOptions.merge())
            .await()
    }

    suspend fun updateUserField(key: String, value: String, id: String){
        fireStore.collection(Constants.USERS)
            .document(id)
            .update(key, value)
            .await()
    }

    suspend fun getUserRouteData(id: String) {
        val result = fireStore.collection(Constants.USER_ROUTES)
            .document(id)
            .get().await()
        _currentUserRouteData.emit(result.toObject(UserRoute::class.java))
    }

    suspend fun registerUserRouteIntoFirestore(userRouteInfo: UserRoute) {

        fireStore.collection(Constants.USER_ROUTES)
            .document(userRouteInfo.id)
            .set(userRouteInfo, SetOptions.merge())
            .await()
    }

    suspend fun updateUserRouteField(key: String, value: String, id: String){
        fireStore.collection(Constants.USER_ROUTES)
            .document(id)
            .update(key, value)
            .await()
    }

    suspend fun getUserRouteTrackerData(id: String) {
        val result = fireStore.collection(Constants.USER_ROUTE_TRACKERS)
            .document(id)
            .get().await()
        _currentUserRouteTrackerData.emit(result.toObject(UserRouteTracker::class.java))
    }

    suspend fun registerUserRouteTrackerIntoFirestore(userRouteTrackerInfo: UserRouteTracker) {

        fireStore.collection(Constants.USER_ROUTE_TRACKERS)
            .document(userRouteTrackerInfo.id)
            .set(userRouteTrackerInfo, SetOptions.merge())
            .await()
    }

    suspend fun updateUserRouteTrackerField(key: String, value: String, id: String){
        fireStore.collection(Constants.USER_ROUTE_TRACKERS)
            .document(id)
            .update(key, value)
            .await()
    }

    suspend fun getUserLocationData(id: String) {
        val result = fireStore.collection(Constants.USER_LOCATIONS)
            .document(id)
            .get().await()
        _currentUserLocationData.emit(result.toObject(UserLocation::class.java))
    }

    suspend fun registerUserLocationIntoFirestore(userLocationInfo: UserLocation) {

        fireStore.collection(Constants.USER_LOCATIONS)
            .document(userLocationInfo.id)
            .set(userLocationInfo, SetOptions.merge())
            .await()
    }

    suspend fun deleteUserLocationIntoFirestore(userLocationInfo: UserLocation) {

        fireStore.collection(Constants.USER_LOCATIONS)
            .document(userLocationInfo.id)
            .delete()
            .await()
    }

    suspend fun updateUserLocationField(key: String, value: String, id: String){
        fireStore.collection(Constants.USER_LOCATIONS)
            .document(id)
            .update(key, value)
            .await()
    }

    suspend fun uploadImageCloudStorage(bitmap: Bitmap, id: String){
        val storage: FirebaseStorage = FirebaseStorage.getInstance()

        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data: ByteArray = baos.toByteArray()


        val uploadTask = storage.reference.child(id+".jpg")
           .putBytes(data).await()
           updateUserField("image", uploadTask.metadata!!.reference!!.downloadUrl.await().toString(), id)

        _uploadPhotoResult.emit("Upload success")
    }
}