package com.example.projektmunka.utils

import com.example.projektmunka.data.Node
import com.example.projektmunka.data.Route
import org.jgrapht.Graph
import org.jgrapht.graph.DefaultWeightedEdge

public fun addWaypoints(route: Route, waypointDistance : Double, graph: Graph<Node, DefaultWeightedEdge>)
        : MutableList<Node> {
    val waypointNodes = mutableListOf<Node>()
    var distance = 0.0
    var waypointCount = 1

    for(i in 0 .. route.path.size - 2) {
        val source = route.path[i]
        val target = route.path[i + 1]
        
        if (source != target) {
            distance += graph.getEdgeWeight(graph.getEdge(source, target))
        }

        if (distance > waypointDistance * waypointCount) {
            waypointCount++
            waypointNodes.add(target!!)
        }
    }

    return waypointNodes
}

public fun addWaypoints(route: Route, waypointDistances : MutableList<Double>, graph: Graph<Node, DefaultWeightedEdge>)
        : MutableList<Node> {
    val waypointNodes = mutableListOf<Node>()
    var distance = 0.0
    var waypointIndex = 0
    var accumulatedDistance = 0.0

    for(i in 0 .. route.path.size - 2) {
        val source = route.path[i]
        val target = route.path[i + 1]
        distance += graph.getEdgeWeight(graph.getEdge(source, target))

        if (waypointIndex < waypointDistances.size && distance > waypointDistances[waypointIndex] + accumulatedDistance) {
            waypointNodes.add(target!!)
            println(distance)
            accumulatedDistance += distance // waypointDistances[waypointIndex]
            waypointIndex++
        }
    }

    return waypointNodes
}