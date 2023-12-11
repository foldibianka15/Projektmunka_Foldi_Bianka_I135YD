package com.example.projektmunka.RouteUtils

import android.location.Location
import com.example.projektmunka.data.Coordinate
import com.example.projektmunka.data.ElevationRequest
import com.example.projektmunka.data.ElevationResponse
import com.example.projektmunka.data.ImportanceEvaluator
import com.example.projektmunka.data.Node
import com.example.projektmunka.data.OverpassResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.jgrapht.Graph
import org.jgrapht.alg.shortestpath.DijkstraShortestPath
import org.jgrapht.graph.DefaultUndirectedWeightedGraph
import org.jgrapht.graph.DefaultWeightedEdge
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.util.logging.Level
import java.util.logging.Logger

public fun getGraph(startNode : Node, endNode : Node) : Graph<Node, DefaultWeightedEdge>? {
    return runBlocking {
        val minLat = (if (startNode.lat < endNode.lat) startNode.lat else endNode.lat) - 0.01
        val minLon = (if (startNode.lon < endNode.lon) startNode.lon else endNode.lon) - 0.01
        val maxLat = (if (startNode.lat > endNode.lat) startNode.lat else endNode.lat) + 0.01
        val maxLon = (if (startNode.lon > endNode.lon) startNode.lon else endNode.lon) + 0.01
        val bbox = "$minLat,$minLon,$maxLat,$maxLon"

        val graph = async(Dispatchers.IO) {
            callOverpass(bbox)
        }.await()

        if (graph != null)
        {
            val elevations = async(Dispatchers.IO) {
                getElevationData(graph)
            }.await()

            return@runBlocking graph
        }
        return@runBlocking graph
    }
}

suspend fun findNearestNonIsolatedNode(poi : Node, radius: Double,
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

                        if (!isIsolatedNode(node, userLocation, graph))
                        {
                            nodes.add(node)
                        }
                    }
                }

                return@withContext nodes.minByOrNull { calculateGeodesicDistance(it, poi)  }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }

fun isIsolatedNode(node : Node, userLocation: Node, graph: Graph<Node, DefaultWeightedEdge>) : Boolean {
    if (graph.containsVertex(node)) {
        val dijkstra = DijkstraShortestPath(graph)
        return dijkstra.getPath(userLocation, node) == null
    }
    return true
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
    //val bbox = calculateBoundingBox(lat, lon, rOpt)

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
}

fun createCityGraph(data: OverpassResponse): Graph<Node, DefaultWeightedEdge>? {
    println("1")
    val graph =
        DefaultUndirectedWeightedGraph<Node, DefaultWeightedEdge>(DefaultWeightedEdge::class.java)

    println("2")
    // Add nodes to the graph
    data.elements?.filter { it.type == "node" && it.lat != null && it.lon != null }
        ?.forEach { node ->
            graph.addVertex(Node(node.id, node.lat, node.lon))
        }

    val nodes = graph.vertexSet().toMutableList()

    println("3")
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

    println("4")
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

suspend fun findNearestNode(
    userLatitude: Double,
    userLongitude: Double,
): Node? =
    withContext(Dispatchers.IO) {

        val client = OkHttpClient.Builder().build()
        val userLocation = Node(-1, userLatitude, userLongitude)

        // 47.535,19.026,47.556,19.040
        // minLat,minLon,maxLat,maxLon
        val bound = 0.01
        val minLat = userLatitude - bound
        val minLon = userLongitude - bound
        val maxLat = userLatitude + bound
        val maxLon = userLongitude + bound

        val bbox = "$minLat,$minLon,$maxLat,$maxLon"

        val url = "https://overpass-api.de/api/interpreter?data=" +
                "[out:json];" +
                "way($bbox)[highway];" +
                "(._;>;);" +
                "out;"

        val encodedQuery = URLEncoder.encode(url, "UTF-8")
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
                var closestNode: Node? = null
                var minDistance = Double.MAX_VALUE

                for (i in 0 until elements.length()) {
                    val element = elements.getJSONObject(i)

                    if (element.getString("type") == "node") {
                        val nodeId = element.getLong("id")
                        val nodeLat = element.getDouble("lat")
                        val nodeLon = element.getDouble("lon")

                        // Create a Node object with the retrieved data
                        val node = Node(
                            id = nodeId,
                            lat = nodeLat,
                            lon = nodeLon,
                        )

                        if (closestNode == null || calculateDistance(
                                node,
                                userLocation
                            ) < minDistance
                        ) {
                            closestNode = node
                            minDistance = calculateDistance(node, userLocation)
                        }
                    }
                }

                return@withContext closestNode
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }

suspend fun callOverpass(bbox: String): Graph<Node, DefaultWeightedEdge>? {
    val cityName = "Budapest"
    val countryName = "HU"

    return runBlocking {
        // 47.555980,19.027330,47.534161,19.039650
        // 47.535,19.026,47.556,19.040
        // minLat,minLon,maxLat,maxLon

        val client = OkHttpClient.Builder().build()

        val deferred = async(Dispatchers.IO) {
            val url = "https://overpass-api.de/api/interpreter?data=" +
                    "[out:json];" +
                    "way($bbox)[highway];" +
                    "(._;>;);" +
                    "out;"

            println(url)
            val request = Request.Builder()
                .url(url)
                .build()

            try {
                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    println("Failed to fetch data: ${response.code}")
                    return@async null
                }

                val responseBody = response.body?.string()
                if (responseBody != null) {
                    // Parse the JSON response to work with the map data
                    val graphData = parseOverpassResponse(responseBody)

                    // Check if elements is null
                    if (graphData.elements != null) {
                        // Create a city graph
                        println("Returning city graph")
                        return@async createCityGraph(graphData)
                    } else {
                        // Handle the case where elements is null
                        println("Elements data is null.")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return@async null
        }

        // Wait for the result
        return@runBlocking deferred.await()
    }
}
suspend fun getElevationData(graph: Graph<Node, DefaultWeightedEdge>): List<Double>? {
    val coordinates = mutableListOf<Coordinate>()

    for (node in graph.vertexSet()) {
        coordinates.add(Coordinate(node.lat, node.lon))
    }

    val client = OkHttpClient()
    val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    val requestBody = Json { }.encodeToString(ElevationRequest(coordinates))
    val request = Request.Builder()
        .url("https://api.open-elevation.com/api/v1/lookup")
        .post(requestBody.toRequestBody(jsonMediaType))
        .build()

    val response: Response = client.newCall(request).execute()

    println(response.message)
    if (!response.isSuccessful) {
        throw Exception("Request failed with code ${response.code}")
    }

    val responseBody = response.body?.string()
    response.close()

    val elevationResponse = Json.decodeFromString<ElevationResponse>(responseBody ?: "")

    for ((node, res) in graph.vertexSet().zip(elevationResponse.results)) {
        node.elevation = res.elevation
    }

    return null
}


