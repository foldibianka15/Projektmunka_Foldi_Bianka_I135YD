package com.example.projektmunka.fragment

import GeneratePaths
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import com.example.projektmunka.RouteUtils.ShenandoahsHikingDifficulty
import com.example.projektmunka.RouteUtils.findNearestNode
import com.example.projektmunka.RouteUtils.getGraph
import com.example.projektmunka.data.Route
import com.example.projektmunka.data.User
import com.example.projektmunka.databinding.FragmentForm2Binding
import com.example.projektmunka.utils.NominatimReverseGeocoding
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.osmdroid.util.GeoPoint
import java.util.concurrent.CountDownLatch

class Form2Fragment(user : User, currentLocation : Location) : Fragment() {

    private lateinit var binding: FragmentForm2Binding

    lateinit var editTextAddress : TextInputEditText
    lateinit var radioGroupLocation : RadioGroup
    lateinit var editTextTargetLocation : TextInputEditText

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentForm2Binding.inflate(inflater, container, false)
        val rootView = binding.root

        val editTextAddress = binding.editTextAddress
        val radioGroupLocation = binding.radioGroupLocation
        val editTextTargetLocation = binding.editTextTargetLocation
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
                            Log.d("YourActivity", "Received result. Latitude: $latitude, Longitude: $longitude")
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

    private suspend fun performReverseGeocodingBlocking(address: String): Pair<Double, Double>? {
        // Use CountDownLatch to wait for the result
        val latch = CountDownLatch(1)

        var result: Pair<Double, Double>? = null

        val reverseGeocodingTask = NominatimReverseGeocoding { taskResult ->
            result = if (taskResult != null) {
                Pair(taskResult.first, taskResult.second)
            } else {
                null
            }
            latch.countDown() // Release the latch to signal completion
        }

        // Execute the reverse geocoding task
        reverseGeocodingTask.execute(address)

        // Suspend the coroutine until the result is available
        latch.await()

        return result
    }

    private fun test() {
        runBlocking {
            val selectedRadioButtonId = radioGroupLocation.checkedRadioButtonId
            val selectedRadioButton = binding.root.findViewById<RadioButton>(selectedRadioButtonId)

            val sourceAddress = editTextAddress.text.toString()
            
            if (selectedRadioButton != null) {
                var source = Pair(0.0, 0.0)

                val selectedOptionText = selectedRadioButton.text.toString()

                if (selectedOptionText == "Choose Address") {
                    source = async { performReverseGeocodingBlocking(sourceAddress) }.await()!!

                } else if (selectedOptionText == "Set Actual Location") {
                    source = Pair(0.0, 0.0)
                }

                val destination = async { performReverseGeocodingBlocking("asd") }.await()

                val path = generateRoute(source, destination!!, 25.0)
            }
        }
    }

    private fun generateRoute(source : Pair<Double, Double>, destination: Pair<Double,Double>, targetDifficulty : Double) : Route? {
        return runBlocking {
            val startNode = async(Dispatchers.IO) {
                findNearestNode(source.first, source.second)
            }.await() ?: return@runBlocking null

            val endNode = async(Dispatchers.IO) {
                findNearestNode(destination.first, destination.second)
            }.await() ?: return@runBlocking null

            val graph = getGraph(startNode, endNode) ?: return@runBlocking null

            val bestRoute = GeneratePaths(graph, startNode, endNode, 500, ::ShenandoahsHikingDifficulty, targetDifficulty, 1.5)
            return@runBlocking bestRoute
        }
    }
}
