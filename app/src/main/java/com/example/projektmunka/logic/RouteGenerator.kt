package com.example.projektmunka.logic

import GeneratePaths
import org.osmdroid.views.MapView
import android.location.Location
import com.example.projektmunka.RouteOptimizers.CircularDifficultRouteOptimizer
import com.example.projektmunka.RouteUtils.ShenandoahsHikingDifficulty
import com.example.projektmunka.RouteUtils.addMarker
import com.example.projektmunka.RouteUtils.calculateROpt
import com.example.projektmunka.RouteUtils.calculateSearchArea
import com.example.projektmunka.RouteUtils.displayCircularRoute
import com.example.projektmunka.RouteUtils.drawRoute
import com.example.projektmunka.RouteUtils.evaluateNodes
import com.example.projektmunka.RouteUtils.fetchCityGraph
import com.example.projektmunka.RouteUtils.fetchNodes
import com.example.projektmunka.RouteUtils.findClosestNonIsolatedNode
import com.example.projektmunka.RouteUtils.findNearestNode
import com.example.projektmunka.RouteUtils.findNearestOSMNode
import com.example.projektmunka.RouteUtils.getElevationData
import com.example.projektmunka.RouteUtils.getGraph
import com.example.projektmunka.RouteUtils.selectImportantPOIs
import com.example.projektmunka.data.Node
import com.example.projektmunka.data.Route
import com.example.projektmunka.utils.Constants.EXIT_DiSTANCE_FIND_NEAREST_NON_ISOLATED_NODE
import com.example.projektmunka.utils.Constants.MAX_DISTANCE_SEARCH_IMPORTANT_POIS
import com.example.projektmunka.utils.Constants.NUM_KEY_POIS
import com.example.projektmunka.utils.Constants.PEDESTRIAN_SPEED
import com.example.projektmunka.utils.Constants.SEARCH_RADIUS_NEAREST_NODE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class RouteGenerator {

    suspend fun generateCircularDifficultRoute(
        maxWalkingTimeInHours: Double,
        currentLocation: Location
    ): Route {
        val desiredRouteLength = maxWalkingTimeInHours * 4
        val rOpt = calculateROpt(PEDESTRIAN_SPEED, maxWalkingTimeInHours)
        val searchArea = calculateSearchArea(rOpt)

        val nearestNode = withContext(Dispatchers.IO) {
            findNearestOSMNode(currentLocation, SEARCH_RADIUS_NEAREST_NODE)
        } ?: throw Exception("Failed to find nearest node")

        val nodes = withContext(Dispatchers.IO) {
            fetchNodes(nearestNode.lat, nearestNode.lon, 600.0)
        } ?: throw Exception("Failed to fetch nodes")

        val evaluatedNodes = evaluateNodes(nodes)
        val importantPOIs = selectImportantPOIs(evaluatedNodes, MAX_DISTANCE_SEARCH_IMPORTANT_POIS)
        val cityGraph = fetchCityGraph(nearestNode.lat, nearestNode.lon, rOpt)
            ?: throw Exception("Failed to fetch city graph")

        withContext(Dispatchers.IO) { getElevationData(cityGraph) }

        val nearestNonIsolatedNode =
            findClosestNonIsolatedNode(cityGraph, nearestNode, EXIT_DiSTANCE_FIND_NEAREST_NON_ISOLATED_NODE)
                ?: throw Exception("Failed to find nearest non-isolated node")

        val poiToClosestNonIsolatedNode: MutableMap<Node, Node> = mutableMapOf()

        for (poi in importantPOIs) {
            val closestNonIsolatedNode = findClosestNonIsolatedNode(cityGraph, poi, EXIT_DiSTANCE_FIND_NEAREST_NON_ISOLATED_NODE)
                ?: throw Exception("Failed to find closest non-isolated node for POI")
            poiToClosestNonIsolatedNode[poi] = closestNonIsolatedNode
        }

        val generator = CircularDifficultRouteOptimizer(
            cityGraph,
            poiToClosestNonIsolatedNode,
            importantPOIs,
            NUM_KEY_POIS,
            desiredRouteLength,
            10.0,
            searchArea,
            20,
            5,
            50
        )

        val bestRoute = generator.runGeneticAlgorithm(nearestNonIsolatedNode)
        val connectedRoute = generator.connectPois(nearestNonIsolatedNode, bestRoute, cityGraph)

        return connectedRoute
    }

    private suspend fun generateRoute(
        source: Pair<Double, Double>,
        destination: Pair<Double, Double>,
        targetDifficulty: Double
    ): Route {
        val startNode = withContext(Dispatchers.IO) {
            findNearestNode(source.first, source.second)
        } ?: throw Exception("Failed to find nearest node for source")

        val endNode = withContext(Dispatchers.IO) {
            findNearestNode(destination.first, destination.second)
        } ?: throw Exception("Failed to find nearest node for destination")

        val graph = getGraph(startNode, endNode) ?: throw Exception("Failed to get graph")

        val bestRoute = GeneratePaths(
            graph, startNode, endNode, 300, ::ShenandoahsHikingDifficulty, targetDifficulty, 0.0
        )

       /* withContext(Dispatchers.Main) { // Assuming UI updates need to happen on the main thread
            drawRoute(osmMap, bestRoute)
            addMarker(osmMap, source.first, source.second)
            addMarker(osmMap, destination.first, destination.second)
        }*/

        return bestRoute
    }

}