package com.example.projektmunka.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.fragment.app.Fragment
import com.example.projektmunka.databinding.FragmentForm1Binding
import com.example.projektmunka.utils.NominatimReverseGeocoding

class Form1Fragment : Fragment() {

    private lateinit var binding: FragmentForm1Binding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentForm1Binding.inflate(inflater, container, false)
        val rootView = binding.root

        val editTextAddress = binding.editTextAddress
        val editTextTargetLocation = binding.editTextTargetLocation
        val radioGroupLocation = binding.radioGroupLocation
        val btnSetActualAddress = binding.btnSetActualAddress
        val btnCreateRoute = binding.btnCreateRoute

        btnSetActualAddress.setOnClickListener {
            // Handle the "Set my actual address" button click here
            // You might want to implement a separate logic for this button
            //setActualLocationMethod()
        }

        btnCreateRoute.setOnClickListener {
            // Get the selected radio button
            val selectedRadioButtonId = radioGroupLocation.checkedRadioButtonId
            val selectedRadioButton = rootView.findViewById<RadioButton>(selectedRadioButtonId)

            // Get the address from the EditText
            val address = editTextAddress.text.toString()

            // Check which radio button is selected
            if (selectedRadioButton != null) {
                val selectedOptionText = selectedRadioButton.text.toString()

                if (selectedOptionText == "Choose Address") {
                    performReverseGeocoding(address,
                        { latitude, longitude ->
                            // Handle the result (latitude and longitude) here
                            Log.d(
                                "YourActivity",
                                "Received result. Latitude: $latitude, Longitude: $longitude"
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