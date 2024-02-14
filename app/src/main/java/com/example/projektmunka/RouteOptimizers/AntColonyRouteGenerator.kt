package com.example.projektmunka.RouteOptimizers

import RouteMetric
import com.example.projektmunka.data.Node
import com.example.projektmunka.data.Route
import org.jgrapht.Graph
import org.jgrapht.alg.shortestpath.DijkstraShortestPath
import org.jgrapht.graph.DefaultWeightedEdge
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class AntColonyRouteGenerator(
    private val graph: Graph<Node, DefaultWeightedEdge>,
    private val populationSize: Int,    // 10
    private val evaporation: Double,    // 0.65
    private val targetRouteLength: Double,
    private val alpha: Double,  // 1.0
    private val beta: Double,   // 10.0
    private val Q: Double,  // 4.0
    private val maxIterations: Int,    // 200
    private val stepCount: Int,  // 1000
    private val metric: RouteMetric
) {

    private val pheromones: MutableMap<DefaultWeightedEdge, Double> = mutableMapOf()

    init {
        for (edge in graph.edgeSet()) {
            pheromones.put(edge, 0.1)
        }
    }

    fun biasPheromones(source: Node, target: Node) {
        val dijkstra = DijkstraShortestPath(graph)
        val path = dijkstra.getPath(source, target).vertexList

        for (i in 0..path.size - 2) {
            pheromones[graph.getEdge(path[i], path[i + 1])] = 0.5
        }
    }

    fun findRoute(source: Node, target: Node): Route {

        println("In find route!")
        for (round in 0..maxIterations) {
            val antPaths = mutableListOf<Route>()
            for (antIndex in 0..populationSize - 1) {
                antPaths.add(Route(mutableListOf()))
                antPaths.last().path.add(source)
            }

            for (antIndex in 0..populationSize - 1) {

                for (i in 0..stepCount) {
                    val nextNode = calculateNextNode(antPaths[antIndex], target)

                    print("Round: $round, ant: $antIndex, step: $i -->")
                    if (nextNode == null) {
                        break
                    }
                    antPaths[antIndex].path.add(nextNode)
                    if (nextNode == target) {
                        println("Target reached by ant " + antIndex + "at round: " + round)
                        break
                    }

                }
                println()
            }

            // recompute pheromones
            for (keyValuePair in pheromones) {
                keyValuePair.setValue(keyValuePair.value * evaporation)
            }

            for (route in antPaths) {
                if (route.path.last() != null && route.path.last() == target) {
                    val delta = Q / fitnessANT(route, target)
                    for (i in 0..route.path.size - 2) {
                        val edge = graph.getEdge(route.path[i], route.path[i + 1])
                        pheromones[edge] = pheromones[edge]!! + delta
                    }
                }
            }
        }

        val bestPath = Route(mutableListOf<Node>())
        bestPath.path.add(source)

        for (i in 0..1000) { // max iterations
            val nextNode = calculateNextNode(bestPath, target)

            if (nextNode == null) {
                break
            }
            bestPath.path.add(nextNode)
            if (nextNode == target) {
                break
            }
        }

        if (bestPath.path.last() == target) {
            println("Best path has reached the target!")
            println("Target metric is: " + targetRouteLength)
            println("Best path's metric is: " + metric(graph, bestPath))
        } else {
            println("Best path has not reached the target")
        }

        return bestPath
    }

    fun calculateNextNode(route: Route, goal: Node): Node? {
        println()
        val currentNode = route.path.last()
        val edges = graph.edgesOf(currentNode).toMutableList()

        val nodes = mutableListOf<Node>()
        val distances = mutableListOf<Double>()
        val edgePheromones = mutableListOf<Double>()
        val probabilities = mutableListOf<Double>()
        var sum = 0.0

        val maxValue = 10000.0

        for (edge in edges) {
            val source = graph.getEdgeSource(edge)
            val target = graph.getEdgeTarget(edge)
            var adjecentNode: Node

            if (source != currentNode) {
                adjecentNode = source
            } else {
                adjecentNode = target
            }

            if (route.path.contains(adjecentNode)) {
                continue
            }

            nodes.add(adjecentNode)
            var tmp = calculateDistance(adjecentNode, goal)

            if (tmp.isInfinite()) {
                tmp = maxValue
            }

            distances.add(tmp)

            val p = pheromones[edge]
            if (p != null) {
                edgePheromones.add(p)
            } else {
                edgePheromones.add(0.0)
            }

            var probability =
                Math.pow(edgePheromones.last(), alpha) * Math.pow(distances.last(), beta)
            probability = Math.pow(probability, Math.E)

            if (probability.isInfinite()) {
                probability = maxValue
            }

            probabilities.add(probability)
            sum += probability
        }

        for (i in 0..probabilities.size - 1) {
            probabilities[i] /= sum
        }

        // Generate a random number between 0 and 1
        val randomValue = Random.nextDouble()

        // Initialize the cumulative probability
        var cumulativeProbability = 0.0

        // Iterate through the nodes and their corresponding probabilities
        for (i in nodes.indices) {
            cumulativeProbability += probabilities[i]

            // If the random value falls within the cumulative probability, select this node
            if (randomValue <= cumulativeProbability) {
                return nodes[i]
            }
        }

        return null
    }

    fun calculateRouteLength(route: Route): Double {
        var length = 0.0
        for (i in 0 until route.path.size - 1) {
            val source = route.path[i]
            val target = route.path[i + 1]
            length += graph.getEdgeWeight(graph.getEdge(source, target))
        }
        return length
    }

    fun fitnessANT(route2: Route, target: Node): Double {
        return kotlin.math.abs(metric(this.graph, route2) - targetRouteLength)
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
}