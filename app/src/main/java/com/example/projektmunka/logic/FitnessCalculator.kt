package com.example.projektmunka.logic

import android.location.Location
import com.example.projektmunka.RouteUtils.calculateWalkingSpeed
import com.example.projektmunka.RouteUtils.calculateWalkingTime
import com.example.projektmunka.RouteUtils.calculateWalkingtime
import com.example.projektmunka.data.Node
import com.example.projektmunka.data.Route
import com.google.firebase.Timestamp
import org.jgrapht.Graph
import org.jgrapht.graph.DefaultWeightedEdge

class FitnessCalculator {

    fun calculateDistance(startLocation: Location, endLocation: Location): Double {
        val results = FloatArray(1)
        Location.distanceBetween(startLocation.latitude, startLocation.longitude, endLocation.latitude, endLocation.longitude, results)
        return results[0].toDouble()
    }

    fun calculateAverageSpeed(distanceTravelled: Double, elapsedTimeInSeconds: Long): Double {
        if (elapsedTimeInSeconds == 0L) return 0.0
        return distanceTravelled / elapsedTimeInSeconds
    }

    fun calculateCaloriesBurned(locationData: MutableList<Pair<Location, Long>>) : Double {
        val weightInKg = 57.0
        return (calculateMETValue(calculateWalkingSpeed(locationData) * weightInKg * calculateWalkingTime(locationData) / 200))
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
    fun calculateHeartRate(): Int {
        return 0
    }

    fun calculateAverageHeartRate(): Double {
        return 0.0
    }
}