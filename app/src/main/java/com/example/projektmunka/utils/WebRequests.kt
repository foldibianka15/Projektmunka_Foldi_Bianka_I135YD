package com.example.projektmunka.utils

import com.example.firstapp.utils.calculateDistance
import com.example.projektmunka.data.Coordinate
import com.example.projektmunka.data.ElevationRequest
import com.example.projektmunka.data.ElevationResponse
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
import org.jgrapht.graph.DefaultUndirectedWeightedGraph
import org.jgrapht.graph.DefaultWeightedEdge
import org.json.JSONObject
import java.net.URLEncoder

fun parseOverpassResponse(response: String): OverpassResponse {
    val json = Json { ignoreUnknownKeys = true }
    return json.decodeFromString(OverpassResponse.serializer(), response)
}

fun createCityGraph(data: OverpassResponse): Graph<Node, DefaultWeightedEdge> {
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
                if (source != null && target != null) {
                    val edge = graph.addEdge(source, target)
                    val edgeWeight = calculateDistance(source, target)
                    graph.setEdgeWeight(edge, edgeWeight)
                }
            }
        }

    // Élek kiíratása vesszővel elválasztva
    val edgeStringBuilder = StringBuilder()
    for (edge in graph.edgeSet()) {
        val sourceNode = graph.getEdgeSource(edge)
        val targetNode = graph.getEdgeTarget(edge)
        edgeStringBuilder.append("$sourceNode -> $targetNode, ")
    }

    // Távolítsa el az utolsó vesszőt
    if (edgeStringBuilder.isNotEmpty()) {
        edgeStringBuilder.delete(edgeStringBuilder.length - 2, edgeStringBuilder.length)
    }

    // Kiírás
    //println("Élek: $edgeStringBuilder")

    return graph
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