package com.example.projektmunka

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.example.projektmunka.databinding.ActivityRegisterBinding
import com.example.projektmunka.viewModel.RegisterViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    private val registerViewModel: RegisterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_register)

        binding.viewModel = registerViewModel
        binding.lifecycleOwner = this
        setContentView(binding.root)

        setTitle("Register")

        lifecycleScope.launchWhenCreated {registerViewModel.lastResult.collect {it?.let { result -> Toast.makeText(
            this@RegisterActivity, if(result == null) "Registration failed" else "You are registered successfully" ,
            Toast.LENGTH_LONG
        ).show()  }
        }}

        binding.tvLogin.setOnClickListener {
            val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
            startActivity(intent)
        }

        binding.buttonRegister.setOnClickListener {
            registerViewModel.registerUser()
        }
    }

    fun userRegistrationSuccess() {
        Toast.makeText(
            this@RegisterActivity, resources.getString(R.string.register_success),
            Toast.LENGTH_LONG
        ).show()
    }
}