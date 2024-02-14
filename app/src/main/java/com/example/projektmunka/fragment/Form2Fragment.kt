package com.example.projektmunka.fragment

import GeneratePaths
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.fragment.app.Fragment
import com.example.projektmunka.RouteUtils.ShenandoahsHikingDifficulty
import com.example.projektmunka.RouteUtils.addMarker
import com.example.projektmunka.RouteUtils.drawRoute
import com.example.projektmunka.RouteUtils.findNearestNode
import com.example.projektmunka.RouteUtils.getGraph
import com.example.projektmunka.RouteUtils.performReverseGeocoding
import com.example.projektmunka.RouteUtils.performReverseGeocodingBlocking
import com.example.projektmunka.data.Route
import com.example.projektmunka.data.User
import com.example.projektmunka.databinding.FragmentForm2Binding
import com.example.projektmunka.utils.NominatimReverseGeocoding
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.osmdroid.views.MapView
import java.util.concurrent.CountDownLatch

class Form2Fragment(osmMap: MapView, user: User, currentLocation: Location) : Fragment() {

    private val osmMap: MapView = osmMap
    private val user: User = user
    private val currentLocation: Location = currentLocation

    private lateinit var binding: FragmentForm2Binding
    lateinit var editTextAddress: TextInputEditText
    lateinit var radioGroupLocation: RadioGroup
    lateinit var editTextTargetLocation: TextInputEditText

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentForm2Binding.inflate(inflater, container, false)
        val rootView = binding.root

        editTextAddress = binding.editTextAddress
        radioGroupLocation = binding.radioGroupLocation
        editTextTargetLocation = binding.editTextTargetLocation
        val btnSetActualAddress = binding.btnSetActualAddress
        val btnCreateRoute = binding.btnCreateRoute

        btnSetActualAddress.setOnClickListener {
            // Handle the "Set my actual address" button click here
            // You might want to implement a separate logic for this button
            //setActualLocationMethod()
        }

        btnCreateRoute.setOnClickListener {
            test()
        }
        return rootView
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
                    source = Pair(currentLocation.latitude, currentLocation.longitude)
                }

                var destination = Pair(0.0, 0.0)

                performReverseGeocoding(
                    editTextTargetLocation.text.toString(),
                    { latitude, longitude ->
                        destination = Pair(latitude, longitude)
                        val path = generateRoute(source, destination, 30.0)

                    },
                    { /* Error handling code here */ }
                )
            }
        }
    }

    private fun generateRoute(
        source: Pair<Double, Double>,
        destination: Pair<Double, Double>,
        targetDifficulty: Double
    ): Route? {
        return runBlocking {
            val startNode = async(Dispatchers.IO) {
                findNearestNode(source.first, source.second)
            }.await() ?: return@runBlocking null

            val endNode = async(Dispatchers.IO) {
                findNearestNode(destination.first, destination.second)
            }.await() ?: return@runBlocking null

            val graph = getGraph(startNode, endNode)
            if (graph == null) {
                println("graph is null")
            } else {
                val bestRoute = GeneratePaths(
                    graph,
                    startNode,
                    endNode,
                    300,
                    ::ShenandoahsHikingDifficulty,
                    targetDifficulty,
                    0.0
                )

                drawRoute(osmMap, bestRoute!!)
                addMarker(osmMap, source.first, source.second)
                addMarker(osmMap, destination.first, destination.second)

                return@runBlocking bestRoute
            }
            return@runBlocking null
        }
    }
}
