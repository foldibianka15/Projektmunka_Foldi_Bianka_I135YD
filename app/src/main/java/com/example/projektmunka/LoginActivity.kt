package com.example.projektmunka

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
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
import dagger.hilt.android.AndroidEntryPoint

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
                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                        startActivity(intent)

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
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val exception=task.exception
            try {
                // Google Sign In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)!!
                loginViewModel.signInWithGoogle(account)
            } catch (e: ApiException) {
                // Handle Google Sign-In failure
                // You can show an error message or take appropriate action.
                Toast.makeText(this, "Google Sign-In failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createRequest() {
        // Configure Google Sign In
        gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("336702136386-3lk56lce4b8v4k9ct33kis4r252gajvt.apps.googleusercontent.com")
            .requestEmail()
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