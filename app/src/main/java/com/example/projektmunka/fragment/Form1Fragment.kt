package com.example.projektmunka.fragment

import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import androidx.fragment.app.Fragment
import com.example.projektmunka.RouteOptimizers.CircularDifficultRouteGenerator
import com.example.projektmunka.RouteUtils.calculateROpt
import com.example.projektmunka.RouteUtils.calculateSearchArea
import com.example.projektmunka.RouteUtils.displayCircularRoute
import com.example.projektmunka.RouteUtils.evaluateNodes
import com.example.projektmunka.RouteUtils.fetchCityGraph
import com.example.projektmunka.RouteUtils.fetchNodes
import com.example.projektmunka.RouteUtils.findClosestNonIsolatedNode
import com.example.projektmunka.RouteUtils.findNearestOSMNode
import com.example.projektmunka.RouteUtils.getElevationData
import com.example.projektmunka.RouteUtils.selectImportantPOIs
import com.example.projektmunka.data.Node
import com.example.projektmunka.data.User
import com.example.projektmunka.databinding.FragmentForm2Binding
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.osmdroid.views.MapView

class Form1Fragment(osmMap: MapView, user: User, currentLocation: Location) : Fragment() {

    private val osmMap: MapView = osmMap
    private val user: User = user
    private val currentLocation: Location = currentLocation

    private lateinit var binding: FragmentForm2Binding
    lateinit var editTextAddress: TextInputEditText
    lateinit var radioGroupLocation: RadioGroup
    lateinit var editTextTargetLocation: TextInputEditText


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
            run()
        }
        return rootView
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

            val nodes = fetchNodes(nearestNode.lat, nearestNode.lon, 600.0) ?: return@launch
            val evaluatedNodes = nodes.let { evaluateNodes(it) }

            val importantPOIs = evaluatedNodes.let { selectImportantPOIs(it, 0.1) }

            val cityGraph = fetchCityGraph(nearestNode.lat, nearestNode.lon, 600.0) ?: return@launch
            runBlocking { async { getElevationData(cityGraph) } }.await()
            val nearestNonIsolatedNode = findClosestNonIsolatedNode(cityGraph, nearestNode, 0.0)!!

            for (poi in importantPOIs) {
                val closestNonIsolatedNode = findClosestNonIsolatedNode(cityGraph, poi, 0.0)
                poiToClosestNonIsolatedNode[poi] = closestNonIsolatedNode!!
            }

            val generator = CircularDifficultRouteGenerator(
                cityGraph,
                poiToClosestNonIsolatedNode,
                importantPOIs,
                5,
                desiredRouteLength,
                10.0,
                searchArea,
                20,
                5,
                50
            )
            val bestRoute = generator.runGeneticAlgorithm(nearestNonIsolatedNode)
            val connectedRoute = generator.connectPois(nearestNonIsolatedNode, bestRoute, cityGraph)
            displayCircularRoute(osmMap, bestRoute, connectedRoute, nearestNonIsolatedNode)
            // val milestones = addMilestones(connectedRoute, 0.9, cityGraph)
            //addMarkers(osmMap, milestones)
        }
    }
}