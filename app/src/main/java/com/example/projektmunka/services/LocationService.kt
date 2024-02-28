package com.example.projektmunka.services

import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.example.firstapp.repository.UserDataRepository
import com.example.projektmunka.repository.UserLocationRepository
import com.example.projektmunka.utils.Constants.FASTEST_LOCATION_UPDATE_INTERVAL
import com.example.projektmunka.utils.Constants.LOCATION_UPDATE_INTERVAL
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class LocationService @Inject constructor(private val userDataRepository: UserDataRepository, private val userLocationRepository: UserLocationRepository) : Service() {

    private val job = SupervisorJob()
    val scope = CoroutineScope(Dispatchers.IO + job)

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    val currentUserData = userDataRepository.currentUserData

    private val _locationDataFlow = MutableStateFlow<Pair<Location, Long>?>(null)
    val locationDataFlow = _locationDataFlow.asStateFlow()
    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        startLocationUpdates()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
        job.cancel()
    }

    fun startLocationUpdates() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    val timestamp = System.currentTimeMillis()
                    val newLocationData = Pair(location, timestamp)
                    scope.launch {
                        _locationDataFlow.emit(newLocationData)
                    }
                }
            }
        }

        val locationRequest = LocationRequest.create().apply {
            interval = LOCATION_UPDATE_INTERVAL
            fastestInterval = FASTEST_LOCATION_UPDATE_INTERVAL
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    /*@SuppressLint("MissingPermission")
    private fun saveLocation(location: Location) {
        val userLocation = UserLocation(
            geoPoint = GeoPoint(location.latitude, location.longitude),
            timeStamp = Date(System.currentTimeMillis())
        )

        scope.launch {
            currentUserData.collect{ user ->
                val userId = user?.id ?: ""
                userLocationRepository.saveUserLocation(userLocation, userId)
            }
        }
    }*/
}