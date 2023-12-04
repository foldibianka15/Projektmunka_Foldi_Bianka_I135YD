package com.example.projektmunka.RouteUtils

import com.example.projektmunka.data.Node
import com.example.projektmunka.data.Route
import org.jgrapht.Graph
import org.jgrapht.graph.DefaultWeightedEdge

public fun addMilestones(route: Route, milestoneDistance : Double, graph: Graph<Node, DefaultWeightedEdge>)
        : MutableList<Node> {
    val milestoneNodes = mutableListOf<Node>()
    var distance = 0.0
    var milestoneCount = 1

    for(i in 0 .. route.path.size - 2) {
        val source = route.path[i]
        val target = route.path[i + 1]
        
        if (source != target) {
            distance += graph.getEdgeWeight(graph.getEdge(source, target))
        }

        if (distance > milestoneDistance * milestoneCount) {
            milestoneCount++
            milestoneNodes.add(target!!)
        }
    }

    return milestoneNodes
}

public fun addMilestones(route: Route, milestoneDistances : MutableList<Double>, graph: Graph<Node, DefaultWeightedEdge>)
        : MutableList<Node> {
    val milestoneNodes = mutableListOf<Node>()
    var distance = 0.0
    var milestoneIndex = 0
    var accumulatedDistance = 0.0

    for(i in 0 .. route.path.size - 2) {
        val source = route.path[i]
        val target = route.path[i + 1]
        distance += graph.getEdgeWeight(graph.getEdge(source, target))

        if (milestoneIndex < milestoneDistances.size && distance > milestoneDistances[milestoneIndex] + accumulatedDistance) {
            milestoneNodes.add(target!!)
            println(distance)
            accumulatedDistance += distance // waypointDistances[waypointIndex]
            milestoneIndex++
        }
    }

    return milestoneNodes
}