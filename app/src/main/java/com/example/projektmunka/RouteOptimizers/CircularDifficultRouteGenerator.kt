package com.example.projektmunka.RouteOptimizers

import com.example.projektmunka.RouteUtils.calculateRouteArea
import com.example.projektmunka.RouteUtils.calculateRouteAscent
import com.example.projektmunka.RouteUtils.calculateRouteLength
import com.example.projektmunka.RouteUtils.countSelfIntersections
import com.example.projektmunka.data.Node
import com.example.projektmunka.data.Route
import org.jgrapht.Graph
import org.jgrapht.alg.shortestpath.DijkstraShortestPath
import org.jgrapht.graph.DefaultWeightedEdge
import java.util.Random
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow

class CircularDifficultRouteGenerator(
    private val graph: Graph<Node, DefaultWeightedEdge>,
    private val poiToClosestNonIsolatedNode: MutableMap<Node, Node>,
    private val keyPois: List<Node>,
    private val numKeyPois: Int,
    private val desiredRouteLength: Double,
    private val targetRouteAscent: Double,
    private val searchArea: Double,
    private val populationSize: Int,
    private val survivorRate: Int,
    private val maxGenerations: Int
) {

    private var population = mutableListOf<Route>()

    private fun initializePopulation(userLocation: Node) {
        population = generateInitialPopulation(keyPois, numKeyPois, populationSize, userLocation)
    }

    fun runGeneticAlgorithm(userLocation: Node): Route {

        initializePopulation(userLocation)

        val nChildren = (populationSize - survivorRate) / survivorRate

        repeat(maxGenerations) {
            val fitnessScores = population.map { route ->
                evaluateFitness(
                    userLocation,
                    route,
                    desiredRouteLength,
                    targetRouteAscent,
                    searchArea,
                    graph
                )
            }

            val selectedRoutes = selectNodes(population, fitnessScores, survivorRate).distinct()
            val newPopulation = mutableListOf<Route>()

            // Crossover: Create offspring routes by PMX crossover
            if (selectedRoutes.size >= 2) {
                val randomIndices = selectedRoutes.indices.shuffled().take(2)
                val parent1 = selectedRoutes[randomIndices[0]]
                val parent2 = selectedRoutes[randomIndices[1]]
                val cutPoints = Pair(1, 3)
                val offspring = PMXCrossover(parent1, parent2, cutPoints)

                val matchedPairs = mutableSetOf<Pair<Route, Route>>()

                for (parent in selectedRoutes) {
                    newPopulation.add(parent)
                    for (i in 0 until nChildren) {
                        var partner: Route

                        // Select random partner for crossover (ensuring they haven't been matched before)
                        do {
                            partner = selectedRoutes.random()
                        } while (partner == parent && selectedRoutes.size > 1)

                        // Add the current pair to the set of matched pairs
                        matchedPairs.add(Pair(parent, partner))

                        // Mutate offspring
                        val mutatedOffspring1 = exchangeMutation(offspring.first)
                        val mutatedOffspring2 = exchangeMutation(offspring.second)

                        // Randomly choose whether to add offspring1 or offspring2 to the new population
                        val addFirstOffSpring = (0..1).random()
                        if (addFirstOffSpring == 0) {
                            newPopulation.add(mutatedOffspring1)
                        } else {
                            newPopulation.add(mutatedOffspring2)
                        }
                    }
                }
            } else {
                for (parent in selectedRoutes) {
                    newPopulation.add(parent)

                    for (i in 0 until nChildren) {
                        newPopulation.add(exchangeMutation(parent))
                    }
                }
            }
            // Replace the old population with the new one
            population = newPopulation
        }

        return population.maxBy {
            evaluateFitness(
                userLocation,
                it,
                desiredRouteLength,
                targetRouteAscent,
                searchArea,
                graph
            )
        }!!
    }

    private fun generateInitialPopulation(
        keyPois: List<Node>,
        numKeyPois: Int,
        populationSize: Int,
        userLocation: Node
    ): MutableList<Route> {
        val initialPopulation = mutableListOf<Route>()
        val random = Random()

        repeat(populationSize) {
            val shuffledKeyPois = keyPois.shuffled(random)
            val route: MutableList<Node> =
                shuffledKeyPois.filter { it != userLocation }.take(numKeyPois).toMutableList()

            initialPopulation.add(Route(route))
        }
        return initialPopulation
    }

    private fun selectNodes(
        population: List<Route>,
        fitnessScore: List<Double>,
        survivorRate: Int
    ): List<Route> {

        val rankedNodes = population.zip(fitnessScore).sortedByDescending { it.second }
        return rankedNodes.take(survivorRate).map { it.first }
    }

    private fun evaluateFitness(
        userLocation: Node,
        route: Route,
        desiredRouteLength: Double,
        targetRouteAscent: Double,
        searchArea: Double,
        graph: Graph<Node, DefaultWeightedEdge>
    ): Double {
        val connectedRoute = connectPois(userLocation, route, graph)

        // Calculate total interestingness
        val beauty = route.path.sumOf { it.importance }

        // Calculate routh length
        val routeLength = calculateRouteLength(connectedRoute)

        // Calculate the number of self-intersections
        val selfIntersections = countSelfIntersections(connectedRoute.path)

        val routeAscent = calculateRouteAscent(connectedRoute)
        val ascentDelta = abs(routeAscent - targetRouteAscent)
        val ascentMultiplier = (1 - min(0.9, ascentDelta - routeAscent))

        // Calculate the area of the polygon outlined by the route
        val routeArea = calculateRouteArea(route)

        val areaMultiplier = routeArea / searchArea

        val distanceDelta = abs(routeLength - desiredRouteLength)
        val lengthMultiplier = (1 - min(0.9, distanceDelta - desiredRouteLength)).pow(2)

        val selfIntersectionMultiplier = 1.0 / (1 + selfIntersections)

        //return lengthMultiplier
        return beauty * lengthMultiplier * areaMultiplier * selfIntersectionMultiplier * ascentMultiplier
    }

    private fun PMXCrossover(
        parent1: Route,
        parent2: Route,
        cutPoints: Pair<Int, Int>
    ): Pair<Route, Route> {
        val size = parent1.path.size
        val offspring1 = MutableList<Node?>(size) { null }
        val offspring2 = MutableList<Node?>(size) { null }

        val (startIdx, endIdx) = cutPoints

        // Copy the segment between startIdx and endIdx from parent1 to offspring1 and from parent2 to offspring2
        for (i in startIdx..endIdx) {
            offspring1[i] = parent2.path[i]
            offspring2[i] = parent1.path[i]
        }

        for (i in 0 until size) {
            if (i < startIdx || i > endIdx) {
                var value1 = parent1.path[i]
                var value2 = parent2.path[i]

                while (offspring1.contains(value1)) {
                    val index = parent2.path.indexOf(value1)
                    value1 = parent1.path[index]
                }

                while (offspring2.contains(value2)) {
                    val index = parent1.path.indexOf(value2)
                    value2 = parent2.path[index]
                }

                // After the while loop, make sure value1 is not already in offspring1
                if (!offspring1.contains(value1)) {
                    offspring1[i] = value1
                }

                if (!offspring2.contains(value2)) {
                    offspring2[i] = value2
                }
            }
        }
        return Pair(
            Route(offspring1.toList() as MutableList<Node>),
            Route(offspring2.toList() as MutableList<Node>)
        )
    }

    private fun exchangeMutation(route: Route): Route {
        val random = Random()

        // Randomly select two distinct indices in the route
        var index1 = random.nextInt(route.path.size)
        var index2: Int

        do {
            index2 = random.nextInt(route.path.size)
        } while (index2 == index1)

        // Perform the exchange mutatuon by swapping the nodes at index1 and index2
        val mutatedRoute = route.path.toMutableList()
        val temp = mutatedRoute[index1]
        mutatedRoute[index1] = mutatedRoute[index2]
        mutatedRoute[index2] = temp

        return Route(mutatedRoute)
    }

    fun connectPois(
        userLocation: Node,
        pois: Route,
        graph: Graph<Node, DefaultWeightedEdge>
    ): Route {
        val dijkstra = DijkstraShortestPath(graph)
        val connectedRoute = Route(mutableListOf())

        connectedRoute.path.addAll(
            dijkstra.getPath(
                userLocation,
                poiToClosestNonIsolatedNode[pois.path.first()]
            ).vertexList
        )
        for (i in 0..pois.path.size - 2) {
            val current = pois.path[i]
            val next = pois.path[i + 1]

            val currentNonIsolated = poiToClosestNonIsolatedNode[current]
            val nextNonIsolated = poiToClosestNonIsolatedNode[next]

            connectedRoute.path.addAll(
                dijkstra.getPath(
                    currentNonIsolated,
                    nextNonIsolated
                ).vertexList
            )
        }
        connectedRoute.path.addAll(
            dijkstra.getPath(
                poiToClosestNonIsolatedNode[pois.path.last()],
                userLocation
            ).vertexList
        )

        return connectedRoute
    }
}