package com.example.projektmunka

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import com.example.projektmunka.databinding.ActivityLoginBinding
import com.example.projektmunka.databinding.ActivityQrcodeBinding
import com.example.projektmunka.viewModel.LoginViewModel
import com.example.projektmunka.viewModel.QRCodeViewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult
import com.journeyapps.barcodescanner.CaptureActivity

class QRCodeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQrcodeBinding

    private val qrCodeViewModel: QRCodeViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qrcode)

        binding = ActivityQrcodeBinding.inflate(layoutInflater)
        binding.viewModel = qrCodeViewModel
        binding.lifecycleOwner = this
        setContentView(binding.root)
        val content = "UniqueContentForYourApp" // Use any unique identifier for each user
        val bitMatrix = generateQRCode(content)
    }

    fun generateQRCode(content: String): BitMatrix {
        val multiFormatWriter = MultiFormatWriter()
        try {
            return multiFormatWriter.encode(content, BarcodeFormat.QR_CODE, 500, 500)
        } catch (e: WriterException) {
            e.printStackTrace()
            // Handle the exception
            throw RuntimeException("Error generating QR code", e)
        }
    }

    // Call this function to initiate QR code scanning
    private fun startQRCodeScanner() {
        IntentIntegrator(this)
            .setOrientationLocked(false)
            .setBeepEnabled(true)
            .setCaptureActivity(CaptureActivity::class.java)
            .initiateScan()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val result: IntentResult? = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents != null) {
                // Handle the scanned QR code content
                val scannedContent = result.contents
                // Display a popup window and send notifications to both users
                // Implement this part based on your specific requirements
                // You may use AlertDialog for the popup window and Firebase Cloud Messaging (FCM) for notifications
            } else {
                // Handle case when QR code scanning was canceled
            }
        }
    }
}