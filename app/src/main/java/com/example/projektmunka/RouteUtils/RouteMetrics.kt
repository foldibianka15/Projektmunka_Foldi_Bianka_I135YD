package com.example.projektmunka.RouteUtils

import android.location.Location
import com.example.projektmunka.data.Node
import com.example.projektmunka.data.Route
import java.util.HashSet
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

fun calculateSearchArea(rOptInMeters: Double): Double {
    // Convert rOpt from meters to kilometers
    val rOptInKilometers = rOptInMeters / 1000.0

    val pi = 3.14159265
    val area = pi * rOptInKilometers * rOptInKilometers
    return area
}