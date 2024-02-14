package com.example.projektmunka.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.fragment.app.Fragment
import com.example.projektmunka.databinding.FragmentForm3Binding
import com.example.projektmunka.utils.NominatimReverseGeocoding

class Form3Fragment : Fragment() {

    private lateinit var binding: FragmentForm3Binding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentForm3Binding.inflate(inflater, container, false)
        val rootView = binding.root

        val editTextAddress = binding.editTextAddress
        val radioGroupLocation = binding.radioGroupLocation
        val editTextTargetLocation = binding.editTextTargetLocation
        val btnSetActualAddress = binding.btnSetActualAddress
        val btnCreateRoute = binding.btnCreateRoute
        val editTextTime = binding.editTextTime  // Add the new EditText for time input

        btnSetActualAddress.setOnClickListener {
            // Handle the "Set my actual address" button click here
            // setActualLocationMethod()
        }

        btnCreateRoute.setOnClickListener {
            // Get the selected radio button
            val selectedRadioButtonId = radioGroupLocation.checkedRadioButtonId
            val selectedRadioButton = rootView.findViewById<RadioButton>(selectedRadioButtonId)

            // Get the address and time from the EditText fields
            val address = editTextAddress.text.toString()
            val time = editTextTime.text.toString()

            // Check which radio button is selected
            if (selectedRadioButton != null) {
                val selectedOptionText = selectedRadioButton.text.toString()

                if (selectedOptionText == "Choose Address") {
                    performReverseGeocoding(
                        address,
                        { latitude, longitude ->
                            // Handle the result (latitude and longitude) here
                            Log.d(
                                "YourActivity",
                                "Received result. Latitude: $latitude, Longitude: $longitude, Time: $time"
                            )
                        },
                        {
                            // Handle the case where no location was found or an error occurred
                            Log.e("YourActivity", "Error or no location found.")
                        }
                    )

                } else if (selectedOptionText == "Set Actual Location") {
                    // Call another method for setting actual location
                }
            }

            val targetLocation = editTextTargetLocation.text.toString()

            performReverseGeocoding(
                targetLocation,
                { latitude, longitude ->
                    Log.d(
                        "YourActivity",
                        "Received result. Latitude: $latitude, Longitude: $longitude"
                    )
                },
                {
                    // Handle the case where no location was found or an error occurred
                    Log.e("YourActivity", "Error or no location found.")
                })
        }
        return rootView
    }

    private fun performReverseGeocoding(
        address: String,
        callback: (Double, Double) -> Unit,
        errorCallback: () -> Unit
    ) {
        val reverseGeocodingTask = NominatimReverseGeocoding { result ->
            if (result != null) {
                val latitude = result.first
                val longitude = result.second

                Log.d("YourActivity", "Latitude: $latitude, Longitude: $longitude")

                callback(latitude, longitude)
            } else {
                Log.e("YourActivity", "No location found.")

                errorCallback()
            }
        }
        reverseGeocodingTask.execute(address)
    }
}
