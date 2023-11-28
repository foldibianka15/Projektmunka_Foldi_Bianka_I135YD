package com.example.projektmunka

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.example.projektmunka.databinding.ActivityForgotPasswordBinding
import com.example.projektmunka.viewModel.ResetPasswordViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding

    private val resetPasswordViewModel: ResetPasswordViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_login)
        binding.viewModel = resetPasswordViewModel
        binding.lifecycleOwner = this
        setContentView(binding.root)

        supportActionBar?.hide()

        lifecycleScope.launchWhenCreated {
            resetPasswordViewModel.resetPasswordResult.collect {
                it.let { result ->
                    if(result != null){
                        Toast.makeText(
                            this@ForgotPasswordActivity, "Your reset password email sent successfully.",
                            Toast.LENGTH_LONG
                        ).show()
                        val intent = Intent(this@ForgotPasswordActivity, LoginActivity::class.java)
                        startActivity(intent)

                    }
                    else{
                        Toast.makeText(
                            this@ForgotPasswordActivity, "Failed",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

            }
        }
    }
}