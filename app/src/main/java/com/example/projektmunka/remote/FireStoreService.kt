package com.example.firstapp.data.models.remote.services

import android.graphics.Bitmap
import com.example.projektmunka.data.User
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

    suspend fun uploadImageCloudStorage(bitmap: Bitmap, id: String){
        val storage: FirebaseStorage = FirebaseStorage.getInstance()

        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data: ByteArray = baos.toByteArray()

        //TODO
    /*   val uploadTask = storage.reference.child(id+".jpg")
           .putBytes(data).await()
           updateUserField("image", uploadTask.metadata!!.reference!!.downloadUrl.await().toString(), id)*/

        _uploadPhotoResult.emit("Upload success")
    }
}