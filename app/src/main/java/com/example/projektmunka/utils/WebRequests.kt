package com.example.projektmunka.utils

import android.location.Location
import com.example.projektmunka.data.ImportanceEvaluator
import com.example.projektmunka.data.Node
import com.example.projektmunka.data.OverpassResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jgrapht.Graph
import org.jgrapht.alg.shortestpath.DijkstraShortestPath
import org.jgrapht.graph.DefaultUndirectedWeightedGraph
import org.jgrapht.graph.DefaultWeightedEdge
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.util.logging.Level
import java.util.logging.Logger

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
