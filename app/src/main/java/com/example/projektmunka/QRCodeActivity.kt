package com.example.projektmunka

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.example.projektmunka.data.LocationInfo
import com.example.projektmunka.databinding.ActivityQrcodeBinding
import com.example.projektmunka.utils.ViewfinderRectangleView
import com.example.projektmunka.viewModel.UserDataViewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult
import com.journeyapps.barcodescanner.CaptureActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.coroutineScope
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

@AndroidEntryPoint
class QRCodeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQrcodeBinding
    private lateinit var qrCodeImageView: ImageView
    private lateinit var generateQRCodeButton: Button
    private lateinit var readQRCodeButton: Button
    private lateinit var addFriendButton: Button
    private lateinit var countdownTextView: TextView
    private lateinit var viewfinderRectangleView: ViewfinderRectangleView

    private var userPoints = 0
    private var content = ""
    private var locationManager: LocationManager? = null
    private val proximityThresholdMeters = 200.0 // Adjust the threshold as needed
    private val validTimeRangeMillis = 10 * 60 * 1000L // 7 minutes in milliseconds
    private lateinit var countdownTimer: CountDownTimer
    private var friendUserId : String? = null

    private val userDataViewModel: UserDataViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qrcode)

        binding = ActivityQrcodeBinding.inflate(layoutInflater)
        binding.viewModel = userDataViewModel
        binding.lifecycleOwner = this
        setContentView(binding.root)

        friendUserId = intent.getStringExtra("userId")

        initializeViews()
        setupClickListeners()
        startCountdownTimer()
    }

    private fun setupClickListeners() {
        generateQRCodeButton.setOnClickListener {
            generateAndDisplayQRCode()
        }

        readQRCodeButton.setOnClickListener {
            // Start QR code scanning when the user clicks the button
            startQRCodeScanner()
        }

        addFriendButton.setOnClickListener {
            if (friendUserId != null) {
                val friend = userDataViewModel.getUserById(friendUserId!!)
                userDataViewModel.addFiendToCurrentUser(friend)
            }
            else {
                Log.d("ERROR", "Add friend button is clicked but friend user id is null!")
            }
        }
    }

    private fun generateAndDisplayQRCode() {
        lifecycleScope.launchWhenCreated {
            val location = getCurrentLocation()
            userDataViewModel.currentUserData.collect {
                it?.let {
                    val timestamp =
                        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                    content =
                        "User:${it.firstName}_UserId:${it.id}_Random:${Random.nextInt(1000, 9999)}_Lat:${location?.latitude}_Long:${location?.longitude}_Timestamp:$timestamp"
                    println("Content$content")
                    val bitMatrix = generateQRCode(content)
                    val bitmap = createBitmap(bitMatrix)
                    qrCodeImageView.setImageBitmap(bitmap)
                   // viewfinderRectangleView.invalidate()
                }
            }
        }
    }

    private fun getCurrentLocation(): Location? {
        val locationManager =
            getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        }
        return null
    }

    fun generateQRCode(content: String): BitMatrix {
        val multiFormatWriter = MultiFormatWriter()
        try {
            return multiFormatWriter.encode(content, BarcodeFormat.QR_CODE, 500, 500)
        } catch (e: WriterException) {
            e.printStackTrace()
            throw RuntimeException("Error generating QR code", e)
        }
    }

    // Create a bitmap from a BitMatrix
    private fun createBitmap(matrix: BitMatrix): Bitmap {
        val width = matrix.width
        val height = matrix.height
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bmp.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bmp
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
        val result: IntentResult? =
            IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result != null) {
                if (result.contents != null) {
                    // Handle the scanned QR code content
                    val scannedContent = result.contents
                    // Display a popup window and send notifications to both users
                    // Implement this part based on your specific requirements
                    // You may use AlertDialog for the popup window and Firebase Cloud Messaging (FCM) for notifications
                    // Check proximity before further processing
                    if (isInProximity(scannedContent) && isInTimeRange(scannedContent)) {

                        displayPopup(scannedContent)
                    } else {
                        Toast.makeText(this, "The user is not close enough to you.", Toast.LENGTH_SHORT).show()
                    }

                }
            }
        }
    }

    private fun isInTimeRange(timestamp: String): Boolean {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val qrCodeTime = sdf.parse(timestamp)
        val currentTime = Date()
        val diffMinutes = ((currentTime.time - qrCodeTime.time)/ (1000 * 60)).toInt()
        return diffMinutes in 0..validTimeRangeMillis
    }

    private fun isInProximity(scannedContent: String): Boolean{
        val locationInfo = extractLocationInfo(scannedContent)

        val currentUserLocation = getCurrentLocation()
        return if (currentUserLocation != null && locationInfo != null) {
            val distance = calculateDistance(
                currentUserLocation.latitude, currentUserLocation.longitude,
                locationInfo.latitude, locationInfo.longitude
            )

            distance <= proximityThresholdMeters
        } else {
            false
        }
    }

    private fun extractLocationInfo(scannedContent: String): LocationInfo? {
        val parts = scannedContent.split("_")
        if (parts.size == 7) {
            val latitudeString = parts[3].filter { it.isDigit() || it == '.' }
            val longitudeString = parts[4].filter { it.isDigit() || it == '.' }

            val latitude = latitudeString.toDoubleOrNull()
            val longitude = longitudeString.toDoubleOrNull()

            if (latitude != null && longitude != null) {
                return LocationInfo(latitude, longitude)
            }
        }
        return null
    }

    private fun calculateDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Float {
        val result = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, result)
        return result[0]
    }

    private fun displayPopup(scannedContent: String) {
        val userName = extractUserName(scannedContent)
        val dialogBuilder = AlertDialog.Builder(this)
        val inflater = this.layoutInflater
        val dialogView = inflater.inflate(R.layout.popup_layout, null)
        dialogBuilder.setView(dialogView)

        dialogBuilder.setTitle("QR Code Scanned")
            .setMessage("Hi, my name is $userName") // Display relevant details
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun extractUserName(scannedContent: String): String? {
        val parts = scannedContent.split("-")
        for (part in parts) {
            if (part.startsWith("User:")) {
                return part.substringAfter("User:")
            }
        }
        return null
    }

    private fun initializeViews() {
        qrCodeImageView = findViewById(R.id.qrCodeImageView)
        generateQRCodeButton = findViewById(R.id.btnGenerateQRCode)
        readQRCodeButton = findViewById(R.id.btnReadQRCode)
        addFriendButton = findViewById(R.id.btnAddFriend)
        countdownTextView = findViewById(R.id.tvCountdown)
    }

    private fun startCountdownTimer() {
        countdownTimer = object : CountDownTimer(validTimeRangeMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val minutes = millisUntilFinished / 1000 / 60
                val seconds = millisUntilFinished / 1000 % 60
                updateCountdownText(minutes, seconds)
            }

            override fun onFinish() {
                updateCountdownText(0, 0)
                // Handle countdown finish, if needed
            }
        }
        countdownTimer.start()
    }

    private fun updateCountdownText(minutes: Long, seconds: Long) {
        val timeString = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        countdownTextView.text = "Time Remaining: $timeString"
        if (minutes.toInt() == 0 && seconds <= 10) {
            // Change text color to red when there are 10 seconds or less
            countdownTextView.setTextColor(Color.RED)
        }
    }

}