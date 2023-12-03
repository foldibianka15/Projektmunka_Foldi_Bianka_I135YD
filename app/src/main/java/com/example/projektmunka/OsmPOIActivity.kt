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
import com.example.firstapp.data.models.remote.services.FireStoreService
import com.example.projektmunka.data.ImportanceEvaluator
import com.example.projektmunka.data.Node
import com.example.projektmunka.data.OverpassResponse
import com.example.projektmunka.data.Route
import com.example.projektmunka.databinding.ActivityOsmPoiactivityBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jgrapht.Graph
import org.jgrapht.GraphPath
import org.jgrapht.alg.shortestpath.DijkstraShortestPath
import org.jgrapht.graph.DefaultUndirectedWeightedGraph
import org.jgrapht.graph.DefaultWeightedEdge
import org.json.JSONObject
import org.osmdroid.api.IMapController
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.IOException
import java.net.URLEncoder
import java.util.Random
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

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

        println("test2")
        val fss  = FireStoreService()
        println("Test1")
        GlobalScope.launch(Dispatchers.Main) {
            // Call your suspend function here
            fss.getUserRouteData("vuyztQQwnRJeLgopspUL")

            fss.currentUserRouteData.collect {
                it?.let { route ->
                    println(route.startDate)
                    fss.updateUserRouteField("userId", "abc", "vuyztQQwnRJeLgopspUL")
                }
            }
        }

        println("Test")
    }

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            // Update the map center to the new location
            val newLocation = GeoPoint(location.latitude, location.longitude)
            mMap.controller.setCenter(newLocation)
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
        } else {
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

                                    val nodes =
                                        fetchNodes(nearestNode.lat, nearestNode.lon, 800.0)
                                    val evaluatedNodes =
                                        nodes?.let { it1 -> evaluateNodes(it1) }

                                    val importantPOIs = evaluatedNodes?.let { it1 ->
                                        selectImportantPOIs(
                                            it1, 0.1
                                        )
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


                                    val cityGraph = fetchCityGraph(
                                        nearestNode.lat,
                                        nearestNode.lon,
                                        800.0
                                    )


                                    // Iterate through the graph and draw lines for edges
                                    if (cityGraph != null) {
                                        val nearestNodeNonIsolated =
                                            findClosestNonIsolatedNode(
                                                cityGraph,
                                                nearestNode,
                                                0.0
                                            )!!
                                        val dijkstra = DijkstraShortestPath(cityGraph)
                                        if (importantPOIs != null) {
                                            for (poi in importantPOIs) {

                                                // HTTP kéréses verzió:
                                                val closestNonIsolatedNode =
                                                    findClosestNonIsolatedNode(
                                                        cityGraph,
                                                        poi,
                                                        0.0
                                                    )

                                                // Gráfban keresős verzió:
                                                // Ez is jó, de ez addig iterál, amig nem talál elég közeli nem izolált node-ot
                                                // nagy cityGraph-nál ez lassabb lehet, mint a HTTP kéréses, de le kéne mérni
                                                //val closestNonIsolatedNode = findClosestNonIsolatedNode(cityGraph, poi, 0.0)

                                                println(poi.id.toString() + " -> " + closestNonIsolatedNode!!.id)
                                                println(
                                                    dijkstra.getPath(
                                                        nearestNodeNonIsolated,
                                                        closestNonIsolatedNode
                                                    ) != null
                                                )
                                                poiToClosestNonIsolatedNode[poi] =
                                                    closestNonIsolatedNode!!
                                            }

                                            for (poi in importantPOIs) {
                                                println(poi.id)
                                            }
                                            println()

                                            geneticAlgorithm(
                                                cityGraph,
                                                importantPOIs,
                                                5,
                                                desiredRouteLength,
                                                searchArea,
                                                nearestNodeNonIsolated,
                                                20,
                                                5,
                                                10
                                            )
                                        }
                                    }
                                }

                            }
                        }
                    }
                }
        }
    }

    suspend fun findNearestNonIsolatedNode(
        poi: Node, radius: Double,
        userLocation: Node, graph: Graph<Node, DefaultWeightedEdge>
    ): Node? =
        withContext(Dispatchers.IO) {

            val client = OkHttpClient.Builder().build()

            // Formulate an Overpass query to find the nearest node
            val query = "[out:json];" +
                    "node(around:${radius},${poi.lat},${poi.lon});" +
                    "out;"

            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://overpass-api.de/api/interpreter?data=$encodedQuery"
            val request = Request.Builder()
                .url(url)
                .build()

            try {
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    println("Failed to fetch data: ${response.code}")
                }

                val osmData = response.body?.string() ?: ""
                val osmJson = JSONObject(osmData)

                val elements = osmJson.getJSONArray("elements")
                println("elements size: " + elements.length())

                if (elements.length() > 0) {
                    val nodes = mutableListOf<Node>()

                    for (i in 0 until elements.length()) {
                        val element = elements.getJSONObject(i)

                        if (element.getString("type") == "node") {
                            val nodeId = element.getLong("id")
                            val nodeLat = element.getDouble("lat")
                            val nodeLon = element.getDouble("lon")
                            val nodeTags = element.optJSONObject("tags") ?: JSONObject()

                            val tagsMap = mutableMapOf<String, String>()
                            val tagKeys = nodeTags.keys()

                            for (key in tagKeys) {
                                tagsMap[key] = nodeTags.getString(key)
                            }

                            // Create a Node object with the retrieved data
                            val node = Node(
                                id = nodeId,
                                lat = nodeLat,
                                lon = nodeLon,
                                tags = tagsMap,
                                importance = 0
                            )

                            if (!isIsolatedNode(node, userLocation, graph)) {
                                nodes.add(node)
                            }
                        }
                    }

                    return@withContext nodes.minByOrNull {
                        calculateGeodesicDistance(
                            it,
                            poi
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return@withContext null
        }

    fun isIsolatedNode(
        node: Node,
        userLocation: Node,
        graph: Graph<Node, DefaultWeightedEdge>
    ): Boolean {
        if (graph.containsVertex(node)) {
            val dijkstra = DijkstraShortestPath(graph)
            return dijkstra.getPath(userLocation, node) == null
        }
        return true
    }

    fun drawLine(
        sourceLat: Double,
        sourceLon: Double,
        targetLat: Double,
        targetLon: Double
    ) {
        val line = Polyline()
        line.addPoint(GeoPoint(sourceLat, sourceLon))
        line.addPoint(GeoPoint(targetLat, targetLon))
        mMap.overlays.add(line)
    }

    suspend fun fetchNodes(lat: Double, lon: Double, rOpt: Double): List<Node>? =
        withContext(Dispatchers.IO) {

            val client = OkHttpClient.Builder().build()
            //val rOpt = calculateROpt(1.1, 1)

            // Define the Overpass query to select nodes within a radius from a point
            val query = "[out:json];" +
                    "node(around:${rOpt},${lat},${lon});" +
                    "out;"

            // "[out:json];node(around:1000.0,47.506,19.036);out;"

            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://overpass-api.de/api/interpreter?data=$encodedQuery"
            println("Generated URL: $url")
            val request = Request.Builder()
                .url(url)
                .build()

            try {
                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    println("Failed to fetch data: ${response.code}")
                    return@withContext emptyList()
                }

                val osmData = response.body?.string() ?: ""
                val osmJson = JSONObject(osmData)
                val elements = osmJson.getJSONArray("elements")

                val nodes = mutableListOf<Node>()

                for (i in 0 until elements.length()) {
                    val element = elements.getJSONObject(i)
                    if (element.has("tags")) {
                        val tagsObject = element.getJSONObject("tags")
                        val tagsMap = mutableMapOf<String, String>()

                        // Convert tags to a Map<String, String>
                        val tagKeys = tagsObject.keys()
                        for (key in tagKeys) {
                            tagsMap[key] = tagsObject.getString(key)
                        }

                        val node = Node(
                            id = element.getLong("id"),
                            lat = element.getDouble("lat"),
                            lon = element.getDouble("lon"),
                            tags = tagsMap
                        )
                        nodes.add(node)
                    }
                }

                return@withContext nodes

            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext emptyList()
            }
        }

    fun parseOverpassResponse(response: String): OverpassResponse {
        val json = Json { ignoreUnknownKeys = true }
        return json.decodeFromString(OverpassResponse.serializer(), response)
    }

    suspend fun fetchCityGraph(
        lat: Double,
        lon: Double,
        rOpt: Double
    ): Graph<Node, DefaultWeightedEdge>? = withContext(Dispatchers.IO) {

        val client = OkHttpClient.Builder().build()

        // Calculate the bounding box
        val bbox = calculateBoundingBox(lat, lon, rOpt)

        /*val bbox = "47.497,19.035,47.500,19.038"

val url = "https://overpass-api.de/api/interpreter?data=" +
        "[out:json];" +
        "way($bbox)[highway];" +
        "(._;>;);" +
        "out;"*/

        /*val query = "[out:json];" +
        "node(around:${rOpt},${lat},${lon});" +
        "out;"*/

        /* val query = "[out:json];" +
         "way($bbox)[highway];" +
         "(._;>;);" +
         "out;"*/

        val query = "[out:json];" +
                "(" +
                "  node(around:${rOpt},${lat},${lon});" +
                "  way(around:${rOpt},${lat},${lon})[highway];" +
                ");" +
                "out;"

        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "https://overpass-api.de/api/interpreter?data=$encodedQuery"
        println("Generated URL: $url")
        val request = Request.Builder()
            .url(url)
            .build()

        return@withContext runCatching {
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                println("Failed to fetch data: ${response.code}")
                throw IOException("Failed to fetch data: ${response.code}")
            }

            val responseBody = response.body?.string()
            if (responseBody != null) {
                val graphData = parseOverpassResponse(responseBody)

                if (graphData.elements != null) {
                    return@runCatching createCityGraph(graphData)
                }
            }

            throw IOException("Failed to parse Overpass response")
        }.onFailure {
            it.printStackTrace()
            // Handle the exception or log it as needed
            // Use a logger to log the exception
            val logger = Logger.getLogger("YourLoggerName")
            logger.log(Level.SEVERE, "Exception during fetchCityGraph", it)
            // Print the exception details to the console
            println("Exception during fetchCityGraph: ${it.message}")
        }.getOrNull()

        /* try{

     val response = client.newCall(request).execute()

     if (!response.isSuccessful) {
         println("Failed to fetch data: ${response.code}")
         throw IOException("Failed to fetch data: ${response.code}")
     }

     val responseBody = response.body?.string()
     if (responseBody != null) {
         val graphData = parseOverpassResponse(responseBody)

         if (graphData.elements != null) {
             return@withContext createCityGraph(graphData)
         }
     }

 } catch (e: Exception) {
     e.printStackTrace()
 }
 return@withContext null

 return@withContext runCatching {
     val response = client.newCall(request).execute()

     if (!response.isSuccessful) {
         println("Failed to fetch data: ${response.code}")
         throw IOException("Failed to fetch data: ${response.code}")
     }

     val responseBody = response.body?.string()
     if (responseBody != null) {
         val graphData = parseOverpassResponse(responseBody)

         if (graphData.elements != null) {
             return@withContext createCityGraph(graphData)
         }
     }

     throw IOException("Failed to parse Overpass response")
 }.onFailure {
     it.printStackTrace()
     // Handle the exception or log it as needed
 }.getOrNull()*/
    }

    fun calculateBoundingBox(lat: Double, lon: Double, radius: Double): String {
        val earthRadius = 6371.0 // Earth radius in kilometers

        // Convert radius from meters to kilometers
        val radiusKm = radius / 1000.0

        // Calculate the bounding box
        val latMin = lat - Math.toDegrees(radiusKm / earthRadius)
        val latMax = lat + Math.toDegrees(radiusKm / earthRadius)
        val lonMin =
            lon - Math.toDegrees(radiusKm / earthRadius / Math.cos(Math.toRadians(lat)))
        val lonMax =
            lon + Math.toDegrees(radiusKm / earthRadius / Math.cos(Math.toRadians(lat)))

        return "$latMin,$lonMin,$latMax,$lonMax"
    }


    fun createCityGraph(data: OverpassResponse): Graph<Node, DefaultWeightedEdge>? {
        val graph =
            DefaultUndirectedWeightedGraph<Node, DefaultWeightedEdge>(DefaultWeightedEdge::class.java)

        // Add nodes to the graph
        data.elements?.filter { it.type == "node" && it.lat != null && it.lon != null }
            ?.forEach { node ->
                graph.addVertex(Node(node.id, node.lat, node.lon))
            }

        val nodes = graph.vertexSet().toMutableList()


        // Add edges to the graph based on ways
        data.elements?.filter { it.type == "way" }
            ?.forEach { way ->
                for (i in 0 until (way.nodes?.size ?: 0) - 1) {
                    val source = nodes.find { it.id == way.nodes?.getOrNull(i) }
                    val target = nodes.find { it.id == way.nodes?.getOrNull(i + 1) }

                    if (source != null && target != null && source != target) {
                        val edge = graph.addEdge(source, target)

                        if (edge != null) {
                            val edgeWeight = calculateGeodesicDistance(source, target)
                            graph.setEdgeWeight(edge, edgeWeight)
                        } else {
                            // Print additional information to identify the cause of failure
                            println("Failed to add edge between $source and $target")
                            println("Nodes in graph: ${graph.vertexSet().size}")
                            println("Nodes in data: ${data.elements?.filter { it.type == "node" }?.size}")
                            println("Coordinates: $source -> $target")
                        }
                    }
                }
            }

        println("graph$graph")
        return graph
    }


    // Function to retrieve the nearest OSM node based on user's location
    suspend fun findNearestOSMNode(userLocation: Location, radius: Double): Node? =
        withContext(Dispatchers.IO) {

            val client = OkHttpClient.Builder().build()

            val userLatitude = userLocation.latitude
            val userLongitude = userLocation.longitude

            // Formulate an Overpass query to find the nearest node
            val query = "[out:json];" +
                    "node(around:${radius},${userLatitude},${userLongitude});" +
                    "out;"

            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://overpass-api.de/api/interpreter?data=$encodedQuery"
            val request = Request.Builder()
                .url(url)
                .build()

            try {
                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    println("Failed to fetch data: ${response.code}")
                }

                val osmData = response.body?.string() ?: ""
                val osmJson = JSONObject(osmData)

                val elements = osmJson.getJSONArray("elements")

                if (elements.length() > 0) {
                    val nodes = mutableListOf<Node>()

                    for (i in 0 until elements.length()) {
                        val element = elements.getJSONObject(i)

                        if (element.getString("type") == "node") {
                            val nodeId = element.getLong("id")
                            val nodeLat = element.getDouble("lat")
                            val nodeLon = element.getDouble("lon")
                            val nodeTags = element.optJSONObject("tags") ?: JSONObject()

                            val tagsMap = mutableMapOf<String, String>()
                            val tagKeys = nodeTags.keys()

                            for (key in tagKeys) {
                                tagsMap[key] = nodeTags.getString(key)
                            }

                            // Create a Node object with the retrieved data
                            val node = Node(
                                id = nodeId,
                                lat = nodeLat,
                                lon = nodeLon,
                                tags = tagsMap,
                                importance = 0
                            )

                            // Evaluate the importance of the node
                            val importance = ImportanceEvaluator.evaluate(node)
                            node.importance = importance

                            nodes.add(node)
                        }
                    }

                    nodes.sortBy { calculateGeodesicDistance(it, userLocation) }
                    return@withContext nodes.getOrNull(0)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return@withContext null
        }


    fun calculateROpt(pedestrianSpeed: Double, maxWalkingTime: Int): Double {
        val maxWalkingTimeInSeconds = maxWalkingTime * 3600
        val rMax = (pedestrianSpeed * maxWalkingTimeInSeconds) / 2

        return (1.0 / 3.0) * 2 * rMax
    }

    fun calculateSearchArea(rOptInMeters: Double): Double {
        // Convert rOpt from meters to kilometers
        val rOptInKilometers = rOptInMeters / 1000.0

        val pi = 3.14159265
        val area = pi * rOptInKilometers * rOptInKilometers
        return area
    }

    fun evaluateNodes(nodes: List<Node>): List<Node>? {
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

    fun calculateGeodesicDistance(node1: Node, node2: Node): Double {
        val radius = 6371.0 // Earth's radius in kilometers

        val lat1Rad = Math.toRadians(node1.lat)
        val lon1Rad = Math.toRadians(node1.lon)
        val lat2Rad = Math.toRadians(node2.lat)
        val lon2Rad = Math.toRadians(node2.lon)

        val dLat = lat2Rad - lat1Rad
        val dLon = lon2Rad - lon1Rad

        val a = sin(dLat / 2).pow(2) + cos(lat1Rad) * cos(lat2Rad) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return radius * c
    }

    fun calculateGeodesicDistance(node: Node, location: Location): Double {
        val radius = 6371.0 // Earth's radius in kilometers

        val lat1Rad = Math.toRadians(node.lat)
        val lon1Rad = Math.toRadians(node.lon)
        val lat2Rad = Math.toRadians(location.latitude)
        val lon2Rad = Math.toRadians(location.longitude)

        val dLat = lat2Rad - lat1Rad
        val dLon = lon2Rad - lon1Rad

        val a = sin(dLat / 2).pow(2) + cos(lat1Rad) * cos(lat2Rad) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return radius * c
    }

    fun calculateRouteLength(route: Route): Double {
        var totalLength = 0.0

        for (i in 0 until route.path.size - 1) {
            val node1 = route.path[i]
            val node2 = route.path[i + 1]
            val distance = calculateGeodesicDistance(node1, node2)
            totalLength += distance
        }

        return totalLength
    }

    fun calculateRouteLengthInTime(route: Route): Double {
        val averagePedestrianSpeed = 1.4 // meters per second
        val touristWalkSpeed = 0.8 * averagePedestrianSpeed // 20% slower

        var totalTime = 0.0

        for (i in 0 until route.path.size - 1) {
            val node1 = route.path[i]
            val node2 = route.path[i + 1]
            val distance = calculateGeodesicDistance(node1, node2)

            // Calculate time based on the adjusted speed during tourist walk
            val time = distance * 1000 / touristWalkSpeed
            totalTime += time
        }

        return totalTime / 3600.0 // Convert seconds to hours
    }

    fun geneticAlgorithm(
        graph: Graph<Node, DefaultWeightedEdge>?,
        keyPois: List<Node>,
        numKeyPois: Int,
        desiredRouteLength: Double,
        searchArea: Double,
        userLocation: Node,
        populationSize: Int,
        survivorRate: Int,
        maxGenerations: Int
    ): Route {

        // Initialization: Generate an initial population of routes
        var population =
            generateInitialPopulation(keyPois, numKeyPois, populationSize, userLocation)

        val n_children = (populationSize - survivorRate) / survivorRate

        var gen = 0
        repeat(maxGenerations) {

            // Calculate fitness scores for each route in the population
            val fitnessScores =
                population.map { route ->
                    evaluateFitness(
                        userLocation,
                        route,
                        desiredRouteLength,
                        searchArea,
                        graph!!
                    )
                }

            // Selection: Choose routes for the next generation based on fitness and survivor rate
            val selectedRoutes = selectNodes(population, fitnessScores, survivorRate)

            println("Best fitness: " + fitnessScores.min() + "iteration: " + gen++)

            // Create a new generation
            val newPopulation = mutableListOf<Route>()

            // Crossover: Create offspring routes by PMX crossover
            val randomIndices =
                selectedRoutes.indices.shuffled().take(2) // Select two random indices
            val parent1 = selectedRoutes[randomIndices[0]]
            val parent2 = selectedRoutes[randomIndices[1]]
            val cutPoints = Pair(1, 3)
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
        val bestRoute = population.minBy { route ->
            evaluateFitness(
                userLocation,
                route,
                desiredRouteLength,
                searchArea,
                graph!!
            )
        }

        println("About to enter drawshortestpath on map")
        // Draw the shortest path on the map
        drawShortestPathOnMap(bestRoute, graph)

        bestRoute.path.add(0, userLocation)
        bestRoute.path.add(bestRoute.path.size - 1, userLocation)

        return bestRoute
    }


    private fun drawShortestPathOnMap(
        route: Route,
        cityGraph: Graph<Node, DefaultWeightedEdge>?
    ) {
        val nodes = route.path

        for (i in 0 until nodes.size - 1) {
            val sourceNode = nodes[i]
            val targetNode = nodes[i + 1]

            if (cityGraph != null) {
                println("citygraph not null")
            } else {
                println("citygraph is null")
            }
            val dijkstra = DijkstraShortestPath(cityGraph)

            addMarker(nodes[0].lat, nodes[0].lon)
            // Attempt to find a direct path
            val directPath = dijkstra.getPath(sourceNode, targetNode)
            if (directPath != null) {
                println("halál")
                drawPathOnMap(directPath)
            } else {
                // Find the nearest node to the source with outgoing edges
                println("tej")
                val nearestSourceNode =
                    cityGraph?.let { findClosestNonIsolatedNode(it, sourceNode, 0.1) }
                if (i == 0) {
                    println("id sn$sourceNode.id")
                    println("id$sourceNode.lat")
                    println("id$sourceNode.lon")

                    println("id nSn$nearestSourceNode.id")
                    println("id$nearestSourceNode.lat")
                    println("id$nearestSourceNode.lon")
                }

                // Find the nearest node to the target with outgoing edges
                val nearestTargetNode =
                    cityGraph?.let { findClosestNonIsolatedNode(it, targetNode, 0.1) }

                if (nearestSourceNode != null) {
                    addMarker(nearestSourceNode.lat, nearestSourceNode.lon)
                }

                if (nearestSourceNode != null && nearestTargetNode != null) {
                    val dijkstra = DijkstraShortestPath(cityGraph)
                    val shortestPath =
                        dijkstra.getPath(nearestSourceNode, nearestTargetNode)

                    if (shortestPath != null) {
                        // Draw the path on the map
                        drawPathOnMap(shortestPath)

                        // There is a path between the nearest nodes, connect them or perform any other actions
                        println("Connecting ${nearestSourceNode} and ${nearestTargetNode}")
                    } else {
                        // There is no path between the nearest nodes
                        println("No path between nearest nodes")
                    }
                } else {
                    // Nearest nodes not found for one or both of the isolated nodes
                    println("Nearest nodes not found for one or both of the isolated nodes")
                }
            }

            //addMarker(sourceNode.lat, sourceNode.lon)

            println("edges: " + cityGraph!!.outgoingEdgesOf(sourceNode).size)

        }
    }

    fun findClosestNonIsolatedNode(
        graph: Graph<Node, DefaultWeightedEdge>,
        isolatedNode: Node,
        exitDistance: Double
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

    /*  private fun findNearestNodeWithEdges(cityGraph: Graph<Node, DefaultWeightedEdge>?, node: Node): Node? {
  val dijkstra = DijkstraShortestPath(cityGraph)

  // Get all nodes in the graph
  val allNodes = cityGraph?.vertexSet() ?: emptySet()

  // Filter out the current node
  val potentialNeighbors = allNodes.filter { it != node }

  // Find the nearest node with outgoing edges
  val nearestNode = potentialNeighbors.minByOrNull { potentialNeighbor ->
      val outgoingEdges = cityGraph?.outgoingEdgesOf(potentialNeighbor) ?: emptySet()
      val shortestPathLength: Double? = outgoingEdges
          .mapNotNull { edge ->
              val target = cityGraph?.getEdgeTarget(edge)
              val shortestPath = dijkstra.getPath(node, target)
              shortestPath?.length?.toDouble() // Ensure the length is explicitly cast to Double
          }
          .minOrNull()

      shortestPathLength ?: Double.POSITIVE_INFINITY
  }

  return nearestNode
}*/


    private fun drawPathOnMap(path: GraphPath<Node, DefaultWeightedEdge>?) {
        if (path != null) {
            val pathNodes = path.vertexList
            for (j in 0 until pathNodes.size - 1) {
                val pathSource = pathNodes[j]
                val pathTarget = pathNodes[j + 1]

                drawLine(pathSource.lat, pathSource.lon, pathTarget.lat, pathTarget.lon)
            }
        }
    }

    private fun addMarker(lat: Double, lon: Double) {
        val marker = Marker(mMap)
        marker.position = GeoPoint(lat, lon)
        mMap.overlays.add(marker)
    }

    fun selectNodes(
        population: List<Route>,
        fitnessScore: List<Double>,
        survivorRate: Int
    ): List<Route> {

        val rankedNodes = population.zip(fitnessScore).sortedBy { it.second }
        return rankedNodes.take(survivorRate).map { it.first }
    }


    fun PMXCrossover2(
        parent1: List<Int>,
        parent2: List<Int>,
        cutPoints: Pair<Int, Int>
    ): Pair<List<Int>, List<Int>> {
        val size = parent1.size
        val offspring1 = MutableList(size) { 0 }
        val offspring2 = MutableList(size) { 0 }

        val (startIdx, endIdx) = cutPoints


        // Copy the segment between startIdx and endIdx from parent1 to offspring1 and from parent2 to offspring2
        for (i in startIdx..endIdx) {
            offspring1[i] = parent2[i]
            offspring2[i] = parent1[i]
        }

        for (i in 0 until size) {
            if (i < startIdx || i > endIdx) {
                var value1 = parent1[i]
                var value2 = parent2[i]

                while (offspring1.contains(value1)) {
                    val index = parent2.indexOf(value1)
                    value1 = parent1[index]
                }

                while (offspring2.contains(value2)) {
                    val index = parent1.indexOf(value2)
                    value2 = parent2[index]
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

        return Pair(offspring1, offspring2)
    }


    fun PMXCrossover(
        parent1: Route,
        parent2: Route,
        cutPoints: Pair<Int, Int>
    ): Pair<Route, Route> {
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

        return Pair(
            Route(offspring1.toList() as MutableList<Node>),
            Route(offspring2.toList() as MutableList<Node>)
        )
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

    fun evaluateFitness(
        userLocation: Node, route: Route, desiredRouteLength: Double, searchArea: Double,
        graph: Graph<Node, DefaultWeightedEdge>
    ): Double {

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
        val fitness =
            (totalInterestingness) * (1 - routeLength / desiredRouteLength).pow(2) * (1.0 / (1.0 + selfIntersections)) * (routeArea / searchArea)

        // Calculate fitness based in the formula
        return (totalInterestingness) * (1 - routeLength / desiredRouteLength).pow(2) * (1.0 / (1.0 + selfIntersections)) * (routeArea / searchArea)
    }

    fun connectPois(
        userLocation: Node,
        pois: Route,
        graph: Graph<Node, DefaultWeightedEdge>
    ): Route {
        val dijkstra = DijkstraShortestPath(graph)
        val connectedRoute = Route(mutableListOf())

        connectedRoute.path.addAll(
            dijkstra.getPath(
                userLocation,
                poiToClosestNonIsolatedNode[pois.path.first()]
            ).vertexList
        )

        for (i in 0..pois.path.size - 2) {
            val current = pois.path[i]
            val next = pois.path[i + 1]

            val currentNonIsolated = poiToClosestNonIsolatedNode[current]
            val nextNonIsolated = poiToClosestNonIsolatedNode[next]

            connectedRoute.path.addAll(
                dijkstra.getPath(
                    currentNonIsolated,
                    nextNonIsolated
                ).vertexList
            )
        }

        connectedRoute.path.addAll(
            dijkstra.getPath(
                poiToClosestNonIsolatedNode[pois.path.last()],
                userLocation
            ).vertexList
        )


        return connectedRoute
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
            val route: MutableList<Node> =
                shuffledKeyPois.filter { it != userLocation }.take(numKeyPois)
                    .toMutableList()
            // Add the userLocation at both the start and end of the route
            //initialPopulation.add(Route2(listOf(userLocation) + route + listOf(userLocation)))
            initialPopulation.add(Route(route))
        }
        return initialPopulation
    }

    fun countSelfIntersections(route: List<Node>): Int {
        val uniqueNodes = HashSet<Node>()
        var intersectionCount = 0

        for (node in route) {
            if (!uniqueNodes.add(node)) {
                intersectionCount++
            }
        }
        return intersectionCount
    }

    fun calculateRouteArea(route: Route): Double {
        if (route.path.size < 3) {
            return 0.0
        }

        var area = 0.0

        for (i in 0 until route.path.size) {
            val currentNode = route.path[i]
            val nextNode = route.path[(i + 1) % route.path.size] // To close the loop

            // Convert latitude and longitude to radians
            val currentLatRad = Math.toRadians(currentNode.lat)
            val currentLonRad = Math.toRadians(currentNode.lon)
            val nextLatRad = Math.toRadians(nextNode.lat)
            val nextLonRad = Math.toRadians(nextNode.lon)

            // Use the schoelace formula to calculate the signed area
            area += (nextLonRad - currentLonRad) * (2 + sin(currentLatRad) + sin(nextLatRad))
        }

        area *= 6371.0 * 6371.0 / 2.0 // Earth's radius in kilometer

        return abs(area)
    }
}


