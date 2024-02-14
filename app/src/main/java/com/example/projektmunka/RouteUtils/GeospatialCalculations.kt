package com.example.projektmunka.RouteUtils

import android.location.Location
import android.util.Log
import com.example.projektmunka.data.Node
import com.example.projektmunka.data.Route
import com.example.projektmunka.utils.NominatimReverseGeocoding
import com.google.firebase.firestore.GeoPoint
import org.jgrapht.Graph
import org.jgrapht.graph.DefaultWeightedEdge
import java.util.HashSet
import java.util.concurrent.CountDownLatch
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
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

fun calculateDistance(node1: Node, node2: Node): Double {
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

fun calculateRouteLength(graph: Graph<Node, DefaultWeightedEdge>, route: Route): Double {
    var length = 0.0
    for (i in 0 until route.path.size - 1) {
        val source = route.path[i]
        val target = route.path[i + 1]

        if (source != target) {
            length += graph.getEdgeWeight(graph.getEdge(source, target))
        }
    }
    return length
}

fun calculateSearchArea(rOptInMeters: Double): Double {
    // Convert rOpt from meters to kilometers
    val rOptInKilometers = rOptInMeters / 1000.0

    val pi = 3.14159265
    val area = pi * rOptInKilometers * rOptInKilometers
    return area
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

fun calculateGeodesicDistanceInMeters(point1: GeoPoint, point2: GeoPoint): Double {
    val radius = 6371.0 // Earth's radius in kilometers

    val lat1Rad = Math.toRadians(point1.latitude)
    val lon1Rad = Math.toRadians(point1.longitude)
    val lat2Rad = Math.toRadians(point2.latitude)
    val lon2Rad = Math.toRadians(point2.longitude)

    val dLat = lat2Rad - lat1Rad
    val dLon = lon2Rad - lon1Rad

    val a = sin(dLat / 2).pow(2) + cos(lat1Rad) * cos(lat2Rad) * sin(dLon / 2).pow(2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))

    return radius * c * 1000
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

fun performReverseGeocoding(
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

fun performReverseGeocodingBlocking(address: String): Pair<Double, Double>? {
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