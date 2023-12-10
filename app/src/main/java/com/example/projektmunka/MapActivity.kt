package com.example.projektmunka

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.example.projektmunka.databinding.ActivityMapBinding
import com.example.projektmunka.fragment.Form1Fragment
import com.example.projektmunka.fragment.Form2Fragment
import com.example.projektmunka.fragment.Form3Fragment
import com.example.projektmunka.fragment.Form4Fragment
import com.example.projektmunka.viewModel.UserDataViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import dagger.hilt.android.AndroidEntryPoint
import org.osmdroid.api.IMapController
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

@AndroidEntryPoint
class MapActivity : BaseActivity() {

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<FrameLayout>
    private lateinit var binding: ActivityMapBinding
    private lateinit var mMap: MapView
    lateinit var controller: IMapController
    private lateinit var locationManager: LocationManager

    private val userDataViewModel: UserDataViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        binding.viewModel = userDataViewModel
        setContentView(binding.root)

        /*val userProfileViewModel: ProfileViewModel by viewModels()

        // Observe changes in the userProfileViewModel.bitmap property
        userProfileViewModel.bitmap?.let { bitmap ->
            // Update the user profile picture in the navigation drawer
            // Assuming userProfilePictureImageView is an ImageView in your navigation drawer
            findViewById<ImageView>(R.id.imageViewUserProfile)?.setImageBitmap(bitmap)
        }*/

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
                0,
                0f,
                locationListener
            )
        }
    }

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            // Update the map center to the new location
            val newLocation = GeoPoint(location.latitude, location.longitude)
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

        val currentUser = userDataViewModel.currentUserData.value ?: return

        // Determine which fragment is selected based on the form type
        val selectedFragment = when (selectedFormType) {
            1 -> Form1Fragment()
            2 -> Form2Fragment(currentUser)
            3 -> Form3Fragment()
            4 -> Form4Fragment()
            else -> Form1Fragment()
        }

        replaceBottomSheetContent(selectedFragment)

        hideBottomSheet()
    }
}