package com.example.projektmunka.utils

import com.example.projektmunka.data.Node
import com.example.projektmunka.data.Route
import org.jgrapht.Graph
import org.jgrapht.GraphPath
import org.jgrapht.alg.shortestpath.DijkstraShortestPath
import org.jgrapht.graph.DefaultWeightedEdge
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

fun drawLine(mMap : MapView, sourceLat: Double, sourceLon: Double, targetLat: Double, targetLon: Double) {
    val line = Polyline()
    line.addPoint(GeoPoint(sourceLat, sourceLon))
    line.addPoint(GeoPoint(targetLat, targetLon))
    mMap.overlays.add(line)
}

fun drawPathOnMap(mMap : MapView, path: GraphPath<Node, DefaultWeightedEdge>?) {
    if (path != null) {
        val pathNodes = path.vertexList
        for (j in 0 until pathNodes.size - 1) {
            val pathSource = pathNodes[j]
            val pathTarget = pathNodes[j + 1]

            drawLine(mMap, pathSource.lat, pathSource.lon, pathTarget.lat, pathTarget.lon)
        }
    }
}

fun drawRoute(mMap : MapView, route: Route) {
    for (j in 0 until route.path.size - 2) {
        val pathSource = route.path[j]
        val pathTarget = route.path[j + 1]

        drawLine(mMap, pathSource.lat, pathSource.lon, pathTarget.lat, pathTarget.lon)
    }
}

fun addMarker(mMap : MapView, lat: Double, lon: Double) {
    val marker = Marker(mMap)
    marker.position = GeoPoint(lat, lon)
    mMap.overlays.add(marker)
}

fun displayCircularRoute(mMap: MapView, pois : Route, connectedRoute : Route, userLocation : Node) {
    // Add markers to pois
    for (poi in pois.path) {
        addMarker(mMap, poi.lat, poi.lon)
    }

    // Add marker to start location
    addMarker(mMap, userLocation.lat, userLocation.lon)

    // Draw the route
    drawRoute(mMap, connectedRoute)
}

/*
fun drawShortestPathOnMap(mMap : MapView, route: Route, cityGraph: Graph<Node, DefaultWeightedEdge>?) {
    val nodes = route.path

    for (i in 0 until nodes.size - 1) {
        val sourceNode = nodes[i]
        val targetNode = nodes[i + 1]

        if (cityGraph != null) {
            println("citygraph not null")
        }
        else{
            println("citygraph is null")
        }
        val dijkstra = DijkstraShortestPath(cityGraph)

        addMarker(mMap, nodes[0].lat, nodes[0].lon)
        // Attempt to find a direct path
        val directPath = dijkstra.getPath(sourceNode, targetNode)
        if (directPath != null) {
            drawPathOnMap(mMap, directPath)
        } else {
            // Find the nearest node to the source with outgoing edges
            val nearestSourceNode = cityGraph?.let { findClosestNonIsolatedNode(it, sourceNode, 0.1) }

            // Find the nearest node to the target with outgoing edges
            val nearestTargetNode = cityGraph?.let { findClosestNonIsolatedNode(it, targetNode , 0.1) }

            if (nearestSourceNode != null) {
                addMarker(mMap, nearestSourceNode.lat, nearestSourceNode.lon)
            }

            if (nearestSourceNode != null && nearestTargetNode != null) {
                val dijkstra = DijkstraShortestPath(cityGraph)
                val shortestPath = dijkstra.getPath(nearestSourceNode, nearestTargetNode)

                if (shortestPath != null) {
                    // Draw the path on the map
                    drawPathOnMap(mMap, shortestPath)

                    // There is a path between the nearest nodes, connect them or perform any other actions
                    println("Connecting ${nearestSourceNode} and ${nearestTargetNode}")
                } else {
                    // There is no path between the nearest nodes
                    println("No path between nearest nodes")
                }
            }
        }
    }
}
*/