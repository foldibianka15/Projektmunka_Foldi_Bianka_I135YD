package com.example.projektmunka.RouteUtils


import com.example.projektmunka.data.Node
import com.example.projektmunka.data.Route
import org.jgrapht.Graph
import org.jgrapht.graph.DefaultWeightedEdge
import android.location.Location
import com.google.firebase.firestore.GeoPoint
import java.util.HashSet
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

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

fun calculateDifficultyLevelChange(heartrate : Double, age : Int) : Int {
    val percantage = (heartrate / (220.0 - age)) * 100
    return when {
        percantage < 50 -> 1 // növelni kell a terhelést
        percantage < 85 -> 0 // pont jó terhelés, nem kell változtatni
        else -> -1 // túl megterhelő, csökkenteni kell a terhelést
    }
}