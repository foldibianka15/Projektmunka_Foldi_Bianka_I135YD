package com.example.projektmunka

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.AdapterView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import com.example.projektmunka.databinding.ActivityProfileBinding
import com.example.projektmunka.viewModel.ProfileViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.io.IOException

@AndroidEntryPoint
class ProfileActivity : AppCompatActivity() {

    private val userProfileViewModel: ProfileViewModel by viewModels()

    private lateinit var binding: ActivityProfileBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_profile)
        binding.viewModel = userProfileViewModel
        binding.lifecycleOwner = this
        setContentView(binding.root)
        binding.ivUserPhoto.setOnClickListener{
            selectImage()
        }
        binding.btnSubmit.setOnClickListener{
            userProfileViewModel.updateUserProfile()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == USER_PROFILE_IMAGE) {
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    try {
                        userProfileViewModel.bitmap = MediaStore.Images.Media.getBitmap(
                            getContentResolver(), data.data)
                        userProfileViewModel.uploadPhoto()
                        binding.ivUserPhoto.setImageBitmap(userProfileViewModel.bitmap)
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(this, "Canceled", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun selectImage(){
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), USER_PROFILE_IMAGE)
    }
}