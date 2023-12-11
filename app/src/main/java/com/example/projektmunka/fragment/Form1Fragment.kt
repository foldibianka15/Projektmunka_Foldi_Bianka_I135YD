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
import com.example.projektmunka.RouteOptimizers.CircularDifficultRouteGenerator
import com.example.projektmunka.RouteOptimizers.CircularRouteGenerator
import com.example.projektmunka.RouteUtils.ShenandoahsHikingDifficulty
import com.example.projektmunka.RouteUtils.addMarker
import com.example.projektmunka.RouteUtils.addMarkers
import com.example.projektmunka.RouteUtils.addMilestones
import com.example.projektmunka.RouteUtils.calculateGeodesicDistance
import com.example.projektmunka.RouteUtils.calculateSearchArea
import com.example.projektmunka.RouteUtils.displayCircularRoute
import com.example.projektmunka.RouteUtils.drawRoute
import com.example.projektmunka.RouteUtils.fetchCityGraph
import com.example.projektmunka.RouteUtils.fetchNodes
import com.example.projektmunka.RouteUtils.findNearestNode
import com.example.projektmunka.RouteUtils.findNearestOSMNode
import com.example.projektmunka.RouteUtils.getElevationData
import com.example.projektmunka.RouteUtils.getGraph
import com.example.projektmunka.data.ImportanceEvaluator
import com.example.projektmunka.data.Node
import com.example.projektmunka.data.Route
import com.example.projektmunka.data.User
import com.example.projektmunka.databinding.FragmentForm2Binding
import com.example.projektmunka.utils.NominatimReverseGeocoding
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jgrapht.Graph
import org.jgrapht.graph.DefaultWeightedEdge
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import java.util.concurrent.CountDownLatch

class Form1Fragment(osmMap : MapView, user : User, currentLocation : Location) : Fragment() {

    private val osmMap: MapView = osmMap
    private val user: User = user
    private val currentLocation: Location = currentLocation

    private lateinit var binding: FragmentForm2Binding
    lateinit var editTextAddress : TextInputEditText
    lateinit var radioGroupLocation : RadioGroup
    lateinit var editTextTargetLocation : TextInputEditText


    private var poiToClosestNonIsolatedNode: MutableMap<Node, Node> = mutableMapOf()
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
            println("0")
            run()
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

    private fun performReverseGeocodingBlocking(address: String): Pair<Double, Double>? {
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
                    source = Pair(currentLocation.latitude, currentLocation.longitude)
                }

            }
        }
    }

    private fun run() {
        val maxWalkingTimeInHours = 1.5 // órában megadva
        val desiredRouteLength = maxWalkingTimeInHours * 4   // 1. In fitness function we use distance measure in meters. Ld is the desired distance
        // caclulated as desired route time (provided by user, and this is M) multiplied by
        // average walking speed of 4 km per hour.
        val rOpt = calculateROpt(1.1, maxWalkingTimeInHours)
        val searchArea = calculateSearchArea(rOpt)
        println("1")

        GlobalScope.launch(Dispatchers.IO) {
            // Use the findNearestOSMNode function to get the nearest node
            val nearestNode = findNearestOSMNode(currentLocation, 300.0) ?: return@launch
            println("2")

            val nodes = fetchNodes(nearestNode.lat, nearestNode.lon, 600.0) ?: return@launch
            val evaluatedNodes = nodes.let { evaluateNodes(it) }

            val importantPOIs = evaluatedNodes.let { selectImportantPOIs(it, 0.1) }

            println("3")

            val cityGraph = fetchCityGraph(nearestNode.lat, nearestNode.lon, 600.0) ?: return@launch
            println("4")
            runBlocking { async { getElevationData(cityGraph) } }.await()
            println("5")
            val nearestNodeNonIsolated = findClosestNonIsolatedNode(cityGraph, nearestNode, 0.0)!!

            for (poi in importantPOIs) {
                val closestNonIsolatedNode = findClosestNonIsolatedNode(cityGraph, poi, 0.0)
                poiToClosestNonIsolatedNode[poi] = closestNonIsolatedNode!!
            }

            println("5")
            val generator = CircularDifficultRouteGenerator(cityGraph, poiToClosestNonIsolatedNode, importantPOIs, 5, desiredRouteLength, 10.0, searchArea, 20, 5, 50)
            val bestRoute = generator.runGeneticAlgorithm(nearestNodeNonIsolated)
            val connectedRoute = generator.connectPois(nearestNodeNonIsolated, bestRoute, cityGraph)
            displayCircularRoute(osmMap, bestRoute, connectedRoute, nearestNodeNonIsolated)
           // val milestones = addMilestones(connectedRoute, 0.9, cityGraph)
            //addMarkers(osmMap, milestones)
        }
    }

    fun calculateROpt(pedestrianSpeed: Double, maxWalkingTimeInHours: Double): Double {
        val maxWalkingTimeInSeconds = maxWalkingTimeInHours * 3600
        val rMax = (pedestrianSpeed * maxWalkingTimeInSeconds) / 2

        return (1.0 / 3.0) * 2 * rMax
    }

    fun evaluateNodes(nodes: List<Node>): List<Node> {
        val evaluatedNodes = nodes.map { node ->
            val importance = ImportanceEvaluator.evaluate(node)
            node.copy(importance = importance)
        }

        // Filter nodes with importance greater than 0
        return evaluatedNodes.filter { it.importance > 0 }
    }

    fun selectImportantPOIs(pois: List<Node>, maxDistance: Double): List<Node> {

        // Filter POIs by importance (> 2)
        val filteredPOIs = pois.filter { it.importance > 2 }

        // Group POIs by distance
        val groupedPOIs = mutableListOf<List<Node>>()

        // Create a copy of filtered POIs to work with
        val remainingPOIs = filteredPOIs.toMutableList()

        while (remainingPOIs.isNotEmpty()) {
            val currentGroup = mutableListOf<Node>()
            val seedPOI = remainingPOIs.removeAt(0)  // Select the first POI as the seed
            currentGroup.add(seedPOI)

            val iterator = remainingPOIs.iterator()
            while (iterator.hasNext()) {
                val poi = iterator.next()
                val distance = calculateGeodesicDistance(seedPOI, poi)

                if (distance <= maxDistance) {
                    // Add the POI to the current group
                    currentGroup.add(poi)
                    iterator.remove()
                }
            }
            // Add the current group to the list of grouped POIs
            groupedPOIs.add(currentGroup)
        }

        // Select the most important POI from each group
        val selectedPOIs = groupedPOIs.map { group ->
            group.maxByOrNull { it.importance }!!
        }
        return selectedPOIs
    }

    fun findClosestNonIsolatedNode(
        graph: Graph<Node, DefaultWeightedEdge>,
        isolatedNode: Node,
        exitDistance : Double
    ): Node? {
        // If the provided node is not isolated, return it
        if (graph.degreeOf(isolatedNode) > 0) {
            return isolatedNode
        }

        // Use BFS to find non-isolated nodes and their distances
        var closestNode: Node? = null
        var minDistance = Double.POSITIVE_INFINITY

        for (current in graph.vertexSet()) {
            if (graph.degreeOf(current) > 0) {
                val distance = calculateGeodesicDistance(isolatedNode, current)
                if (distance < minDistance) {
                    minDistance = distance
                    closestNode = current

                    if (minDistance <= exitDistance) {
                        return closestNode
                    }
                }
            }
        }

        return closestNode
    }
}
