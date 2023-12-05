package com.example.projektmunka

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.projektmunka.databinding.ActivityLoginBinding
import com.example.projektmunka.viewModel.LoginViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.request.DataReadRequest
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class LoginActivity : AppCompatActivity(), View.OnClickListener {

    private val rotateOpen: Animation by lazy { AnimationUtils.loadAnimation(this, R.anim.rotate_open_anim)}
    private val rotateClose: Animation by lazy { AnimationUtils.loadAnimation(this, R.anim.rotate_close_anim)}
    private val fromBottom: Animation by lazy { AnimationUtils.loadAnimation(this, R.anim.from_bottom_anim)}
    private val toBottom: Animation by lazy { AnimationUtils.loadAnimation(this, R.anim.to_bottom_anim)}

    private var clicked = false
    private lateinit var binding: ActivityLoginBinding

    private val loginViewModel: LoginViewModel by viewModels()

    private var googleSignInClient: GoogleSignInClient? = null
    lateinit var gso:GoogleSignInOptions

    companion object {
        private const val RC_SIGN_IN = 9001
        private const val GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        //binding = DataBindingUtil.setContentView(this, R.layout.activity_login)
        binding.viewModel = loginViewModel
        binding.lifecycleOwner = this
        setContentView(binding.root)

        supportActionBar?.hide()

        createRequest()

        lifecycleScope.launchWhenCreated {
            loginViewModel.error.collect{
                Toast.makeText(this@LoginActivity, it, Toast.LENGTH_LONG
                ).show()
            }}

        lifecycleScope.launchWhenCreated{
            loginViewModel.loginResult.collect {
                it.let { result ->
                    if(result != null){
                        Toast.makeText(
                            this@LoginActivity, "You are logged in successfully",
                            Toast.LENGTH_LONG
                        ).show()
                        val intent = Intent(this@LoginActivity, ProfileActivity::class.java)
                        startActivity(intent)
                        finish()

                    }
                    else{
                        Toast.makeText(
                            this@LoginActivity, "Login failed",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
        binding.tvFloat.setOnClickListener(this)
        binding.tvForgotpsw.setOnClickListener(this)
        binding.btnLogin.setOnClickListener(this)
        binding.tvRegister.setOnClickListener(this)
        binding.btnGoogleLogin.setOnClickListener(this)
    }

    override fun onClick(view: View?) {
        if(view != null) {
            when (view.id) {

                R.id.tv_float -> {
                    onFloatButtonClicked()
                }

                R.id.tv_forgotpsw -> {
                    val intent = Intent(this@LoginActivity, ForgotPasswordActivity::class.java)
                    startActivity(intent)
                }

                R.id.btn_login -> {
                    loginViewModel.loginRegisteredUser()
                }

                R.id.btn_google_login -> {
                    signInWithGoogle()
                    readHeartRateData()
                }

                R.id.tv_register -> {
                    val intent = Intent(this@LoginActivity, RegisterActivity::class.java)
                    startActivity(intent)
                }
            }
        }
    }

    // Handle the result of Google Sign-In
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            RC_SIGN_IN -> {
                // Handle Google Sign-In result
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                val exception = task.exception
                try {
                    // Google Sign In was successful, authenticate with Firebase
                    val account = task.getResult(ApiException::class.java)!!
                    loginViewModel.signInWithGoogle(account)

                    // Request fitness API permission
                    requestFitnessApiPermission()
                } catch (e: ApiException) {
                    // Handle Google Sign-In failure
                    // You can show an error message or take appropriate action.
                    Toast.makeText(this, "Google Sign-In failed", Toast.LENGTH_SHORT).show()

                    // Print or log the exception details

                }
            }

        }
    }
    /*private fun requestFitnessApiPermission() {
        val fitnessOptions = FitnessOptions.builder()
            .addDataType(DataType.TYPE_HEART_RATE_BPM, FitnessOptions.ACCESS_READ)
            .build()

        if (!GoogleSignIn.hasPermissions(GoogleSignIn.getLastSignedInAccount(this), fitnessOptions)) {
            GoogleSignIn.requestPermissions(
                this,
                GOOGLE_FIT_PERMISSIONS_REQUEST_CODE,
                GoogleSignIn.getLastSignedInAccount(this),
                fitnessOptions
            )
        } else {
            // Permission already granted, you can now read heart rate data
            readHeartRateData()
        }
    }*/

   /* private fun readHeartRateData() {
        // Build a request for the user's heart rate between now and the past 5 minutes
        val endTime = System.currentTimeMillis()
        val startTime = endTime - TimeUnit.MINUTES.toMillis(5)

        val readRequest = DataReadRequest.Builder()
            .read(DataType.TYPE_HEART_RATE_BPM)
            .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
            .build()

        // Get the Fitness API client and submit the read request
        val fitnessOptions = FitnessOptions.builder()
            .addDataType(DataType.TYPE_HEART_RATE_BPM, FitnessOptions.ACCESS_READ)
            .build()

        val account = GoogleSignIn.getAccountForExtension(this, fitnessOptions)
        Fitness.getHistoryClient(this, account)
            .readData(readRequest)
            .addOnSuccessListener { dataReadResponse ->
                // Process the heart rate data
                // dataReadResponse contains the heart rate data
            }
            .addOnFailureListener { e ->
                // Handle failure
                Toast.makeText(this, "Failed to read heart rate data", Toast.LENGTH_SHORT).show()
            }
    }*/


    private fun requestFitnessApiPermission() {
        val fitnessOptions = FitnessOptions.builder()
            .addDataType(DataType.TYPE_HEART_RATE_BPM, FitnessOptions.ACCESS_READ)
            .build()

        if (!GoogleSignIn.hasPermissions(GoogleSignIn.getLastSignedInAccount(this), fitnessOptions)) {
            GoogleSignIn.requestPermissions(
                this,
                GOOGLE_FIT_PERMISSIONS_REQUEST_CODE,
                GoogleSignIn.getLastSignedInAccount(this),
                fitnessOptions
            )
        } else {
            // Permission already granted, you can now read heart rate data
            readHeartRateData()
        }
    }

    private fun readHeartRateData() {
        // Set the start time to November 28, 2023, 8:00 AM
        val calStart = Calendar.getInstance()
        calStart.set(2023, Calendar.NOVEMBER, 28, 17, 45, 0)
        val startTime = calStart.timeInMillis

// Set the end time to November 28, 2023, 17:50
        val calEnd = Calendar.getInstance()
        calEnd.set(2023, Calendar.NOVEMBER, 28, 17, 50, 0)
        val endTime = calEnd.timeInMillis

        val readRequest = DataReadRequest.Builder()
            .read(DataType.TYPE_HEART_RATE_BPM)
            .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
            .build()

        // Get the Fitness API client and submit the read request
        val fitnessOptions = FitnessOptions.builder()
            .addDataType(DataType.TYPE_HEART_RATE_BPM, FitnessOptions.ACCESS_READ)
            .build()

        val account = GoogleSignIn.getAccountForExtension(this, fitnessOptions)
        Fitness.getHistoryClient(this, account)
            .readData(readRequest)
            .addOnSuccessListener { dataReadResponse ->
                // Process the heart rate data
                // dataReadResponse contains the heart rate data
            }
            .addOnFailureListener { e ->
                // Handle failure
                Log.e("FitnessAPI", "Failed to read heart rate data", e)
                println(e)
                Toast.makeText(this, "Failed to read heart rate data", Toast.LENGTH_SHORT).show()
            }
    }


    private fun createRequest() {
        // Configure Google Sign In
        gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("545072268503-4h9rrcd4tdboddm8og3hgrq6jasv3roq.apps.googleusercontent.com")
            .requestEmail()
            .requestScopes(Fitness.SCOPE_ACTIVITY_READ, Fitness.SCOPE_BODY_READ_WRITE)
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    // Start the Google Sign-In process
    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient?.signInIntent
        if (signInIntent != null) {
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }
    }

    private fun onFloatButtonClicked(){
        setVisibility(clicked)
        setAnimation(clicked)
        clicked = !clicked
    }

    private fun setVisibility(clicked: Boolean){
        if (!clicked) {
            binding.tvForgotpsw.visibility = View.VISIBLE
            binding.tvRegister.visibility = View.VISIBLE
        }
        else{
            binding.tvForgotpsw.visibility = View.INVISIBLE
            binding.tvRegister.visibility = View.INVISIBLE
        }
    }

    private fun setAnimation(clicked: Boolean){
        if (!clicked){
            binding.tvForgotpsw.startAnimation(fromBottom)
            binding.tvRegister.startAnimation(fromBottom)
            binding.tvFloat.startAnimation(rotateOpen)
        }
        else{
            binding.tvForgotpsw.startAnimation(toBottom)
            binding.tvRegister.startAnimation(toBottom)
            binding.tvFloat.startAnimation(rotateClose)
        }
    }
}