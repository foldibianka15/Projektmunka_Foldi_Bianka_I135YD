package com.example.projektmunka

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.projektmunka.RouteOptimizers.CircularDifficultRouteGenerator
import com.example.projektmunka.RouteOptimizers.CircularRouteGenerator
import com.example.projektmunka.RouteUtils.ShenandoahsHikingDifficulty
import com.example.projektmunka.data.ImportanceEvaluator
import com.example.projektmunka.data.Node
import com.example.projektmunka.data.Route
import com.example.projektmunka.databinding.ActivityOsmPoiactivityBinding
import com.example.projektmunka.RouteUtils.calculateGeodesicDistance
import com.example.projektmunka.RouteUtils.calculateRouteArea
import com.example.projektmunka.RouteUtils.calculateRouteAscent
import com.example.projektmunka.RouteUtils.calculateRouteLength
import com.example.projektmunka.RouteUtils.calculateSearchArea
import com.example.projektmunka.RouteUtils.countSelfIntersections
import com.example.projektmunka.RouteUtils.displayCircularRoute
import com.example.projektmunka.RouteUtils.fetchCityGraph
import com.example.projektmunka.RouteUtils.fetchNodes
import com.example.projektmunka.RouteUtils.findNearestOSMNode
import com.example.projektmunka.RouteUtils.getElevationData
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jgrapht.Graph
import org.jgrapht.graph.DefaultWeightedEdge
import org.osmdroid.api.IMapController
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.MapView
import org.jgrapht.alg.shortestpath.DijkstraShortestPath
import org.osmdroid.util.GeoPoint
import java.util.*
import kotlin.math.*

class OsmPOIActivity : AppCompatActivity() {

    lateinit var mMap: MapView
    lateinit var controller: IMapController
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: Location? = null
    private lateinit var locationManager: LocationManager
    private var poiToClosestNonIsolatedNode: MutableMap<Node, Node> = mutableMapOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_osm_poiactivity)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val binding = ActivityOsmPoiactivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Configuration.getInstance().setUserAgentValue(BuildConfig.APPLICATION_ID);
        Configuration.getInstance().load(
            applicationContext,
            getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE)
        )

        mMap = binding.osmmap
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

        awaitUpdateCurrentLocation()
    }

    private val locationListener = LocationListener { location -> // Update the map ceter to the new location
        val newLocation = GeoPoint(location.latitude, location.longitude)
        mMap.controller.setCenter(newLocation)
    }

    fun awaitUpdateCurrentLocation() {
        return runBlocking {
            async(Dispatchers.IO) {
                updateCurrentLocation()
            }.await()
        }
    }

    suspend fun updateCurrentLocation() = withContext(Dispatchers.IO) {
        if (ContextCompat.checkSelfPermission(
                this@OsmPOIActivity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this@OsmPOIActivity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
        } else {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    location?.let {
                        // Handle the location update here
                        currentLocation = it
                        run()
                    }
                }
        }
    }

    private fun run() {
        val maxWalkingTimeInHours = 0.5 // órában megadva
        val desiredRouteLength = maxWalkingTimeInHours * 4   // 1. In fitness function we use distance measure in meters. Ld is the desired distance
        // caclulated as desired route time (provided by user, and this is M) multiplied by
        // average walking speed of 4 km per hour.
        val rOpt = calculateROpt(1.1, maxWalkingTimeInHours)
        val searchArea = calculateSearchArea(rOpt)

        GlobalScope.launch(Dispatchers.IO) {
            // Use the findNearestOSMNode function to get the nearest node
            val nearestNode = findNearestOSMNode(currentLocation!!, 300.0) ?: return@launch

            val nodes = fetchNodes(nearestNode.lat, nearestNode.lon, rOpt) ?: return@launch
            val evaluatedNodes = nodes.let { evaluateNodes(it) }

            val importantPOIs = evaluatedNodes.let { selectImportantPOIs(it, 0.1) }

            val cityGraph = fetchCityGraph(nearestNode.lat, nearestNode.lon, rOpt) ?: return@launch

            runBlocking { async { getElevationData(cityGraph)} }.await()

            val nearestNodeNonIsolated = findClosestNonIsolatedNode(cityGraph, nearestNode, 0.0)!!

            for (poi in importantPOIs) {
                val closestNonIsolatedNode = findClosestNonIsolatedNode(cityGraph, poi, 0.0)
                poiToClosestNonIsolatedNode[poi] = closestNonIsolatedNode!!
            }

            val generator = CircularRouteGenerator(cityGraph, poiToClosestNonIsolatedNode, importantPOIs, 5, desiredRouteLength, searchArea, 20, 5, 50)
            val bestRoute = generator.runGeneticAlgorithm(nearestNodeNonIsolated)
            val connectedRoute = generator.connectPois(nearestNodeNonIsolated, bestRoute, cityGraph)
            displayCircularRoute(mMap, bestRoute, connectedRoute, nearestNodeNonIsolated)
            //val waypoints = addWaypoints(connectedRoute, 0.2, cityGraph)
            //addMarkers(mMap, waypoints)
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