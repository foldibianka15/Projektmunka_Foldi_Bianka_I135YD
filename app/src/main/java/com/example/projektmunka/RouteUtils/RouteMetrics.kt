package com.example.projektmunka.RouteUtils


import com.example.projektmunka.data.Node
import com.example.projektmunka.data.Route
import org.jgrapht.Graph
import org.jgrapht.graph.DefaultWeightedEdge
import android.location.Location
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

fun calculateRouteAscent(route: Route): Double {
    var totalAscent = 0.0

    for (i in 0..route.path.size - 2) {
        val ascent = route.path[i + 1]!!.elevation - route.path[i]!!.elevation
        if (ascent > 0) {
            totalAscent += ascent
        }
    }

    return totalAscent
}

// (miles / 3) + (feet / 2000)
fun NaismitsRule(graph: Graph<Node, DefaultWeightedEdge>, route: Route): Double {
    val distanceInMiles = calculateRouteLength(graph, route) * 0.621371192
    val ascentInFeet = calculateRouteAscent(route) * 0.3048
    return ((distanceInMiles / 3) + (ascentInFeet / 2000)) * 60
}

// km, m
fun MunterMethod(graph: Graph<Node, DefaultWeightedEdge>, route: Route, rate: Double): Double {
    return (calculateRouteLength(graph, route) + (calculateRouteAscent(route) / 100)) / rate
}

// feet, miles
fun ShenandoahsHikingDifficulty(
    graph: Graph<Node, DefaultWeightedEdge>,
    route: Route
): Double {
    val distanceInMiles = calculateRouteLength(graph, route) * 0.621371192
    val ascentInFeet = calculateRouteAscent(route) * 0.3048
    return sqrt(ascentInFeet * 2 * distanceInMiles)
}

fun WeightedRouteDifficulty(
    graph: Graph<Node, DefaultWeightedEdge>,
    route: Route) : Double {

    return sqrt(calculateRouteAscent(route) * 1 + calculateRouteLength(graph, route) * 20)
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

fun calculateRouteGradientForSegment(graph: Graph<Node, DefaultWeightedEdge>, source: Node, target: Node) : Double {
    val AD = abs(target!!.elevation - source!!.elevation)
    val Dist = graph.getEdgeWeight(graph.getEdge(source, target))
    return  AD / Dist * 100
}

fun calculateWalkingSpeedForSegment(graph: Graph<Node, DefaultWeightedEdge>, source: Node, target: Node) : Double {
    val gradient = calculateRouteGradientForSegment(graph, source, target)

    if (gradient >= 4 && gradient < 8) {
        return 70.0;
    }
    else if (gradient >= 0 && gradient < 4) {
        return 80.0;
    }
    else if (gradient >= -4 && gradient < 0) {
        return 90.0;
    }
    else if (gradient >= -8 && gradient < -4) {
        return 100.0;
    }

    return 85.0;
}

fun calculateWalkingSpeed(graph: Graph<Node, DefaultWeightedEdge>, route: Route) : Double {
    var speed = 0.0
    for (i in 0 until route.path.size - 1) {
        val source = route.path[i]
        val target = route.path[i + 1]
        speed += calculateWalkingSpeedForSegment(graph, source!!, target!!)
    }
    return speed / route.path.size
}

// return is in minutes
fun calculateWalkingtime(graph: Graph<Node, DefaultWeightedEdge>, route: Route) : Double {
    var time = 0.0
    for (i in 0 until route.path.size - 1) {
        val source = route.path[i]
        val target = route.path[i + 1]
        time += graph.getEdgeWeight(graph.getEdge(source, target)) * 1000 / calculateWalkingSpeedForSegment(graph, source!!, target!!)
    }
    return time
}

fun calculateMETValue(speed : Double) : Double {
    return when {
        speed < 70 -> 3.1 // 0.894 m/s is approximately 2 mph
        speed < 80 -> 3.3  // Casual walking
        speed < 90 -> 3.6 // Brisk walking
        speed < 100 -> 4.0 // Fast-paced walking
        else -> 4.0 // Assuming 4.0 METs for speeds above 5 m/s
    }
}

fun calculateCaloriesBurned(graph: Graph<Node, DefaultWeightedEdge>, route: Route) : Double {
    val weightInKg = 57.0
    return (calculateMETValue(calculateWalkingSpeed(graph, route)) * weightInKg * calculateWalkingtime(graph, route)) / 200
}