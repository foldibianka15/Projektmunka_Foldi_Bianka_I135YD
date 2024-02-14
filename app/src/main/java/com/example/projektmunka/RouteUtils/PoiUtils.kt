package com.example.projektmunka.RouteUtils

import com.example.projektmunka.data.ImportanceEvaluator
import com.example.projektmunka.data.Node

fun calculateROpt(pedestrianSpeed: Double, maxWalkingTimeInHours: Double): Double {
    val maxWalkingTimeInSeconds = maxWalkingTimeInHours * 3600
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