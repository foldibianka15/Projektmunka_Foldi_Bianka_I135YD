package com.example.projektmunka

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.projektmunka.databinding.ActivityMapBinding
import com.example.projektmunka.fragment.Form1Fragment
import com.example.projektmunka.fragment.Form2Fragment
import com.example.projektmunka.fragment.Form3Fragment
import com.example.projektmunka.fragment.Form4Fragment
import com.example.projektmunka.viewModel.UserDataViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomsheet.BottomSheetBehavior
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.osmdroid.api.IMapController
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MapActivity : BaseActivity() {

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<FrameLayout>
    private lateinit var binding: ActivityMapBinding
    private lateinit var mMap: MapView
    lateinit var controller: IMapController

    private lateinit var locationManager: LocationManager
    private lateinit var currentLocation : GeoPoint
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val userDataViewModel: UserDataViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        binding.viewModel = userDataViewModel
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        // Check for location permissions
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request location permissions if not granted
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                1
            )
        } else {
            // Request location updates
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000,
                1f,
                locationListener
            )
        }


        val nearbyUserButton: ImageButton = findViewById(R.id.nearbyUserButton)
        nearbyUserButton.setOnClickListener {
            val intent = Intent(this@MapActivity, NearbyUserActivity::class.java)
            startActivity(intent)
            finish()
        }


        // Set up the bottom sheet
        val bottomSheet: FrameLayout = findViewById(R.id.bottom_sheet)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.apply {
            peekHeight = 100
            this.state = BottomSheetBehavior.STATE_HIDDEN
        }

        val data: Int = intent.getIntExtra("key", 0)
        onMapItemSelected(data)

        displayMap()
    }


    override fun getLayoutResourceId(): Int {
        TODO("Not yet implemented")
    }


    fun displayMap() {
        //Configuration.getInstance().setUserAgentValue(BuildConfig.APPLICATION_ID);
        Configuration.getInstance().load(
            applicationContext,
            getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE)
        )

        mMap = binding.mMap
        mMap.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            isHorizontalMapRepetitionEnabled = false
            isVerticalMapRepetitionEnabled = false
            setMultiTouchControls(true)
            setBuiltInZoomControls(true)
        }

        controller = mMap.controller
        controller.setZoom(15.0)
    }
    suspend fun updateCurrentLocation(context: Context): Location? {
        return withContext(Dispatchers.IO) {
            val deferredLocation = CompletableDeferred<Location?>()

            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Request location permission
                ActivityCompat.requestPermissions(
                    context as Activity,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    1
                )
            } else {
                val fusedLocationClient =
                    LocationServices.getFusedLocationProviderClient(context)

                val locationRequest = LocationRequest.create().apply {
                    priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                }

                val locationCallback = object : LocationCallback() {
                    override fun onLocationResult(locationResult: LocationResult) {
                        super.onLocationResult(locationResult)
                        val location = locationResult.lastLocation
                        deferredLocation.complete(location)
                        fusedLocationClient.removeLocationUpdates(this)
                    }
                }

                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )

                // Wait for the result
                try {
                    return@withContext deferredLocation.await()
                } catch (e: CancellationException) {
                    // Handle cancellation if needed
                }
            }

            return@withContext null
        }
    }


    suspend fun updateCurrentLocation() = withContext(Dispatchers.IO) {
        if (ContextCompat.checkSelfPermission(
                this@MapActivity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this@MapActivity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
        } else {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    location?.let {
                        // Handle the location update here
                        currentLocation = GeoPoint(it.latitude, it.longitude)
                    }
                }
        }
    }

    suspend fun getCurrentLocation() : Location {
        if (ContextCompat.checkSelfPermission(
                this@MapActivity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this@MapActivity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
        }
        return withContext(Dispatchers.IO) {
            // Use async to launch a coroutine and await the result
            val location = async { fusedLocationClient.lastLocation.await() }
            location.await()
        }
    }

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            // Update the map center to the new location
            val newLocation = GeoPoint(location.latitude, location.longitude)
            currentLocation = newLocation
            mMap.controller.setCenter(newLocation)
        }
    }

    fun showBottomSheet() {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    fun hideBottomSheet() {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
    }

    private fun inflateFormLayout(formType: Int): View {
        val inflater = LayoutInflater.from(this)
        return when (formType) {
            1 -> inflater.inflate(R.layout.fragment_form1, null)
            2 -> inflater.inflate(R.layout.fragment_form2, null)
            3 -> inflater.inflate(R.layout.fragment_form3, null)
            4 -> inflater.inflate(R.layout.fragment_form4, null)
            else -> inflater.inflate(R.layout.fragment_form1, null) // Default to the first form
        }
    }

    private fun replaceBottomSheetContent(fragment: Fragment) {
        // Replace the content of the bottom sheet with the selected fragment
        supportFragmentManager.beginTransaction()
            .replace(R.id.bottom_sheet, fragment)
            .commit()
    }

    fun onMapItemSelected(selectedPosition: Int) {
        Log.d("MapActivity", "Item selected at position: $selectedPosition")

        // Determine which form type is selected
        val selectedFormType = when (selectedPosition) {
            0 -> 1
            1 -> 2
            2 -> 3
            3 -> 4
            else -> 1 // Default to the first form
        }

        lifecycleScope.launch {
            val location: Location? = updateCurrentLocation(this@MapActivity)

            val currentUser = userDataViewModel.currentUserData.value
            // Determine which fragment is selected based on the form type
            val selectedFragment = when (selectedFormType) {
                1 -> Form1Fragment()
                2 -> Form2Fragment(mMap, currentUser!!, location!!)
                3 -> Form3Fragment()
                4 -> Form4Fragment()
                else -> Form1Fragment()
            }

            replaceBottomSheetContent(selectedFragment)

            hideBottomSheet()
        }
    }
}