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
import androidx.lifecycle.lifecycleScope
import com.example.projektmunka.data.ImportanceEvaluator
import com.example.projektmunka.data.Node
import com.example.projektmunka.data.OverpassResponse
import com.example.projektmunka.data.Route
import com.example.projektmunka.databinding.ActivityOsmPoiactivityBinding
import com.example.projektmunka.utils.addMarker
import com.example.projektmunka.utils.addMarkers
import com.example.projektmunka.utils.addWaypoints
import com.example.projektmunka.utils.calculateGeodesicDistance
import com.example.projektmunka.utils.calculateRouteArea
import com.example.projektmunka.utils.calculateRouteLength
import com.example.projektmunka.utils.calculateSearchArea
import com.example.projektmunka.utils.countSelfIntersections
import com.example.projektmunka.utils.displayCircularRoute
import com.example.projektmunka.utils.drawRoute
import com.example.projektmunka.utils.fetchCityGraph
import com.example.projektmunka.utils.fetchNodes
import com.example.projektmunka.utils.findNearestOSMNode
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jgrapht.Graph
import org.jgrapht.graph.DefaultUndirectedWeightedGraph
import org.jgrapht.graph.DefaultWeightedEdge
import org.json.JSONObject
import org.osmdroid.api.IMapController
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.IOException
import java.net.URLEncoder
import org.jgrapht.GraphPath
import org.jgrapht.alg.shortestpath.DijkstraShortestPath
import org.jgrapht.traverse.BreadthFirstIterator
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.math.*

class OsmPOIActivity : AppCompatActivity() {

    lateinit var mMap: MapView
    lateinit var controller: IMapController
    lateinit var mMyLocationOverlay: MyLocationNewOverlay
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

        //val bbox = "47.506,19.036,47.510,19.042"  //"47.497,19.035,47.4972,19.0352"


        awaitUpdateCurrentLocation()
    }

    private val locationListener = LocationListener { location -> // Update the map center to the new location
        val newLocation = GeoPoint(location.latitude, location.longitude)
        mMap.controller.setCenter(newLocation)
    }

    fun awaitUpdateCurrentLocation() {
        return runBlocking {
            async(Dispatchers.IO) {
                updateCurrentLocation()
            }.await()
            println("awaited updateCurrentLocation")
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
        val maxWalkingTime = 2.00
        val desiredRouteLength = maxWalkingTime * 4
        val rOpt = calculateROpt(1.1, 2)
        val searchArea = calculateSearchArea(rOpt)

        GlobalScope.launch(Dispatchers.IO) {
            // Use the findNearestOSMNode function to get the nearest node
            val nearestNode = findNearestOSMNode(currentLocation!!, 300.0) ?: return@launch

            val nodes = fetchNodes(nearestNode.lat, nearestNode.lon, 800.0) ?: return@launch
            val evaluatedNodes = nodes.let { evaluateNodes(it) }

            val importantPOIs = evaluatedNodes.let { selectImportantPOIs(it, 0.1) }

            //generateInitialPopulation(importantPOIs, 5, 5, nearestNode)

            val cityGraph = fetchCityGraph(nearestNode.lat, nearestNode.lon, 800.0) ?: return@launch

            val nearestNodeNonIsolated = findClosestNonIsolatedNode(cityGraph, nearestNode, 0.0)!!

            for (poi in importantPOIs) {

                // HTTP kéréses verzió:
                val closestNonIsolatedNode = findClosestNonIsolatedNode(cityGraph, poi, 0.0)

                // Gráfban keresős verzió:
                // Ez is jó, de ez addig iterál, amig nem talál elég közeli nem izolált node-ot
                // nagy cityGraph-nál ez lassabb lehet, mint a HTTP kéréses, de le kéne mérni
                //val closestNonIsolatedNode = findClosestNonIsolatedNode(cityGraph, poi, 0.0)

                poiToClosestNonIsolatedNode[poi] = closestNonIsolatedNode!!
            }

            val bestRoute = geneticAlgorithm(cityGraph, importantPOIs, 5, desiredRouteLength, searchArea, nearestNodeNonIsolated, 20, 5, 10)
            val connectedRoute = connectPois(nearestNodeNonIsolated, bestRoute, cityGraph)
            println()
            displayCircularRoute(mMap, bestRoute, connectedRoute, nearestNodeNonIsolated)
            //val waypoints = addWaypoints(connectedRoute, 0.2, cityGraph)
            //addMarkers(mMap, waypoints)
        }
    }

    suspend fun fetchLastLocation() = withContext(Dispatchers.IO) {

        val maxWalkingTime = 2.00
        val desiredRouteLength = maxWalkingTime * 4
        val rOpt = calculateROpt(1.1, 2)
        val searchArea = calculateSearchArea(rOpt)

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
        }

        else {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    location?.let {
                        // Handle the location update here
                        currentLocation = it
                        val location = currentLocation
                        if (location != null) {
                            GlobalScope.launch(Dispatchers.IO) {
                                // Use the findNearestOSMNode function to get the nearest node
                                val nearestNode = findNearestOSMNode(location, 300.0)
                                println("Nearest node$nearestNode")


                                nearestNode?.let {

                                    val nodes = fetchNodes(nearestNode.lat, nearestNode.lon, 800.0)
                                    val evaluatedNodes = nodes?.let { it1 -> evaluateNodes(it1) }

                                    val importantPOIs = evaluatedNodes?.let { it1 ->
                                        selectImportantPOIs(
                                            it1, 0.1)
                                    }



                                    // Call generateInitialPopulation with all required parameters
                                    if (importantPOIs != null) {
                                        generateInitialPopulation(
                                            importantPOIs,
                                            5,
                                            5,
                                            nearestNode
                                        )
                                    }


                                    val cityGraph = fetchCityGraph(nearestNode.lat, nearestNode.lon, 800.0)


                                    // Iterate through the graph and draw lines for edges
                                    if (cityGraph != null) {
                                        val nearestNodeNonIsolated = findClosestNonIsolatedNode(cityGraph, nearestNode, 0.0)!!
                                        val dijkstra = DijkstraShortestPath(cityGraph)
                                        if (importantPOIs != null) {
                                            for (poi in importantPOIs) {

                                                // HTTP kéréses verzió:
                                                val closestNonIsolatedNode = findClosestNonIsolatedNode(cityGraph, poi, 0.0)

                                                // Gráfban keresős verzió:
                                                // Ez is jó, de ez addig iterál, amig nem talál elég közeli nem izolált node-ot
                                                // nagy cityGraph-nál ez lassabb lehet, mint a HTTP kéréses, de le kéne mérni
                                                //val closestNonIsolatedNode = findClosestNonIsolatedNode(cityGraph, poi, 0.0)

                                                println(poi.id.toString() + " -> " + closestNonIsolatedNode!!.id)
                                                println(dijkstra.getPath(nearestNodeNonIsolated, closestNonIsolatedNode) != null)
                                                poiToClosestNonIsolatedNode[poi] = closestNonIsolatedNode!!
                                            }

                                            val bestRoute = geneticAlgorithm(cityGraph, importantPOIs, 5, desiredRouteLength, searchArea, nearestNodeNonIsolated, 20, 5, 10)
                                            val connectedRoute = connectPois(nearestNodeNonIsolated, bestRoute, cityGraph)
                                            displayCircularRoute(mMap, bestRoute, connectedRoute, nearestNodeNonIsolated)
                                        }
                                    }
                                }

                            }
                        }
                    }
                }
        }
    }

    fun calculateROpt(pedestrianSpeed: Double, maxWalkingTime: Int): Double {
        val maxWalkingTimeInSeconds = maxWalkingTime * 3600
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

    fun geneticAlgorithm(
        graph: Graph<Node, DefaultWeightedEdge>,
        keyPois: List<Node>,
        numKeyPois: Int,
        desiredRouteLength: Double,
        searchArea: Double,
        userLocation: Node,
        populationSize: Int,
        survivorRate: Int,
        maxGenerations: Int
    ) : Route {

        // Initialization: Generate an initial population of routes
        var population =
            generateInitialPopulation(keyPois, numKeyPois, populationSize, userLocation)

        val n_children = (populationSize - survivorRate) / survivorRate

        repeat(maxGenerations) {

            // Calculate fitness scores for each route in the population
            val fitnessScores =
                population.map { route -> evaluateFitness(userLocation, route, desiredRouteLength, searchArea, graph) }
            println("Best fitness: " + fitnessScores.max())

            // Selection: Choose routes for the next generation based on fitness and survivor rate
            val selectedRoutes = selectNodes(population, fitnessScores, survivorRate)

            // Create a new generation
            val newPopulation = mutableListOf<Route>()

            // Crossover: Create offspring routes by PMX crossover
            val randomIndices = selectedRoutes.indices.shuffled().take(2) // Select two random indices
            val parent1 = selectedRoutes[randomIndices[0]]
            val parent2 = selectedRoutes[randomIndices[1]]
            val cutPoints = Pair(1,3)
            val offspring = PMXCrossover(parent1, parent2, cutPoints)

            val matchedPairs = mutableSetOf<Pair<Route, Route>>()

            for (parent in selectedRoutes) {
                newPopulation.add(parent)
                for (i in 0 until n_children) {
                    var partner: Route

                    // Select random partner for crossover (ensuring they haven't been matched before)
                    do {
                        partner = selectedRoutes.random()
                    } while (partner == parent)

                    // Add the current pair to the set of matched pairs
                    matchedPairs.add(Pair(parent, partner))

                    // Mutate offspring
                    val mutatedOffspring1 = exchangeMutation(offspring.first)
                    val mutatedOffspring2 = exchangeMutation(offspring.second)

                    // Randomly choose whether to add offspring1 or offspring2 to the new population
                    val addFirstOffSpring = (0..1).random()
                    if (addFirstOffSpring == 0) {
                        newPopulation.add(mutatedOffspring1)
                    } else {
                        newPopulation.add(mutatedOffspring2)
                    }
                }
            }
            // Replace the old population with the new one
            population = newPopulation
        }

        // Return the best path found after the specified number of generations
        val bestRoute = population.minBy{ route -> evaluateFitness(userLocation, route, desiredRouteLength, searchArea, graph) }

        return bestRoute
    }

    fun generateInitialPopulation(
        keyPois: List<Node>,
        numKeyPois: Int,
        populationSize: Int,
        userLocation: Node
    ): List<Route> {
        val initialPopulation = mutableListOf<Route>()
        val random = Random()

        repeat(populationSize) {
            // Shuffle the key POIs, remove the userLocation if it exists, and select the first numKeyPois
            val shuffledKeyPois = keyPois.shuffled(random)
            val route: MutableList<Node> = shuffledKeyPois.filter { it != userLocation }.take(numKeyPois).toMutableList()
            // Add the userLocation at both the start and end of the route
            //initialPopulation.add(Route2(listOf(userLocation) + route + listOf(userLocation)))
            initialPopulation.add(Route(route))
        }
        return initialPopulation
    }


    fun selectNodes(
        population: List<Route>,
        fitnessScore: List<Double>,
        survivorRate: Int
    ): List<Route> {

        val rankedNodes = population.zip(fitnessScore).sortedBy { it.second }
        return rankedNodes.take(survivorRate).map { it.first }
    }

    fun evaluateFitness(userLocation: Node, route : Route, desiredRouteLength: Double, searchArea: Double,
                        graph: Graph<Node, DefaultWeightedEdge>): Double {

        // Calculate total interestingness
        val totalInterestingness = route.path.sumOf { it.importance }

        val connectedRoute = connectPois(userLocation, route, graph)

        // Calculate routh length
        val routeLength = calculateRouteLength(connectedRoute)

        // Calculate the number of self-intersections
        val selfIntersections = countSelfIntersections(connectedRoute.path)

        // Calculate the area of the polygon outlined by the route
        val routeArea = calculateRouteArea(route)

        val a = (1 - routeLength / desiredRouteLength)
        val b = (1.0 / (1.0 + selfIntersections))
        val c = (routeArea / searchArea)
        val fitness = (totalInterestingness) * (1 - routeLength / desiredRouteLength).pow(2) * (1.0 / (1.0 + selfIntersections)) * (routeArea / searchArea)

        // Calculate fitness based in the formula
        //return (totalInterestingness) * (1  / abs(desiredRouteLength - routeLength)).pow(2) * (1.0 / (1.0 + selfIntersections * 100)) * (routeArea / searchArea)
        return  (-totalInterestingness) + (1  / abs(desiredRouteLength - routeLength)).pow(2) + (selfIntersections * 100.0) + (routeArea / searchArea)
        //return fitness
    }

    fun PMXCrossover(parent1: Route, parent2: Route, cutPoints: Pair<Int, Int>): Pair<Route, Route> {
        val size = parent1.path.size
        val offspring1 = MutableList<Node?>(size) { null }
        val offspring2 = MutableList<Node?>(size) { null }

        val (startIdx, endIdx) = cutPoints


        // Copy the segment between startIdx and endIdx from parent1 to offspring1 and from parent2 to offspring2
        for (i in startIdx..endIdx) {
            offspring1[i] = parent2.path[i]
            offspring2[i] = parent1.path[i]
        }

        for (i in 0 until size) {
            if (i < startIdx || i > endIdx) {
                var value1 = parent1.path[i]
                var value2 = parent2.path[i]

                while (offspring1.contains(value1)) {
                    val index = parent2.path.indexOf(value1)
                    value1 = parent1.path[index]
                }

                while (offspring2.contains(value2)) {
                    val index = parent1.path.indexOf(value2)
                    value2 = parent2.path[index]
                }

                // After the while loop, make sure value1 is not already in offspring1
                if (!offspring1.contains(value1)) {
                    offspring1[i] = value1
                }

                if (!offspring2.contains(value2)) {
                    offspring2[i] = value2
                }
            }
        }

        return Pair(Route(offspring1.toList() as MutableList<Node>), Route(offspring2.toList() as MutableList<Node>))
    }


    fun exchangeMutation(route: Route): Route {
        val random = Random()

        // Randomly select two distinct indices in the route
        var index1 = random.nextInt(route.path.size)
        var index2: Int

        do {
            index2 = random.nextInt(route.path.size)
        } while (index2 == index1)

        // Perform the exchange mutatuon by swapping the nodes at index1 and index2
        val mutatedRoute = route.path.toMutableList()
        val temp = mutatedRoute[index1]
        mutatedRoute[index1] = mutatedRoute[index2]
        mutatedRoute[index2] = temp

        return Route(mutatedRoute)
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

    fun connectPois(userLocation: Node, pois : Route, graph: Graph<Node, DefaultWeightedEdge>) : Route
    {
        val dijkstra = DijkstraShortestPath(graph)
        val connectedRoute = Route(mutableListOf())

        connectedRoute.path.addAll(dijkstra.getPath(userLocation, poiToClosestNonIsolatedNode[pois.path.first()]).vertexList)
        for (i in 0 .. pois.path.size - 2) {
            val current = pois.path[i]
            val next = pois.path[i + 1]

            val currentNonIsolated = poiToClosestNonIsolatedNode[current]
            val nextNonIsolated = poiToClosestNonIsolatedNode[next]

            connectedRoute.path.addAll(dijkstra.getPath(currentNonIsolated, nextNonIsolated).vertexList)
        }
        connectedRoute.path.addAll(dijkstra.getPath(poiToClosestNonIsolatedNode[pois.path.last()], userLocation).vertexList)

        return connectedRoute
    }
}