package com.example.projektmunka.RouteUtils

import com.example.projektmunka.data.Node
import com.example.projektmunka.data.Route
import org.jgrapht.GraphPath
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
    for (j in 0 until route.path.size - 1) {
        val pathSource = route.path[j]
        val pathTarget = route.path[j + 1]

        drawLine(mMap, pathSource.lat, pathSource.lon, pathTarget.lat, pathTarget.lon)
    }
}

fun drawRoute2(mMap : MapView, route: Route) {
    for (j in 0 until route.path.size - 1) {
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

fun addMarkers(mMap : MapView, locations : MutableList<Node>) {
    for (location in locations) {
        val marker = Marker(mMap)
        marker.position = GeoPoint(location.lat, location.lon)
        mMap.overlays.add(marker)
    }
}

fun displayCircularRoute(mMap: MapView, pois : Route, connectedRoute : Route, userLocation : Node) {
    // Add markers to pois
    for (poi in pois.path) {
        addMarker(mMap, poi.lat, poi.lon)
    }

    // Add marker to start location
    addMarker(mMap, userLocation.lat, userLocation.lon)

    // Draw the route
    drawRoute2(mMap, connectedRoute)
}
