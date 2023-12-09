package com.example.projektmunka

import MiBandManager
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.projektmunka.databinding.ActivityHeartRateBinding
import com.example.projektmunka.databinding.ActivityLoginBinding

class HeartRateActivity : AppCompatActivity() {
    private val miBandManager by lazy { MiBandManager(this) }

    private lateinit var binding: ActivityHeartRateBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHeartRateBinding.inflate(layoutInflater)
        setContentView(R.layout.activity_heart_rate)

        // Check if Bluetooth is available
        if (miBandManager.isBluetoothAvailable()) {
            // Enable Bluetooth if not already enabled
            miBandManager.enableBluetooth()
        } else {
            // Handle the case where Bluetooth is not available on the device
        }

        // Connect to Mi Band when a button is clicked (you can trigger this based on your UI)
        binding.connectMiBandButton.setOnClickListener {
            miBandManager.connectMiBand()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == MiBandManager.REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                // Bluetooth is enabled, you can proceed with Mi Band connection
                miBandManager.connectMiBand()
            } else {
                // User didn't enable Bluetooth, handle accordingly
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Disconnect from Mi Band when the activity is destroyed
        miBandManager.disconnectMiBand()
    }
}