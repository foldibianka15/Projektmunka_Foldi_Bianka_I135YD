package com.example.firstapp.utils

import com.example.projektmunka.data.Route
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

fun addMarker(mMap : MapView, lat: Double, lon: Double) {
    val marker = Marker(mMap)
    marker.position = GeoPoint(lat, lon)
    mMap.overlays.add(marker)
}

fun drawLine(
    mMap : MapView,
    sourceLat: Double,
    sourceLon: Double,
    targetLat: Double,
    targetLon: Double
) {
    val line = Polyline()
    line.addPoint(GeoPoint(sourceLat, sourceLon))
    line.addPoint(GeoPoint(targetLat, targetLon))
    mMap.overlays.add(line)
}

fun displayRoute(mMap : MapView, route : Route) {
    for (i in 0..route.path.size - 2) {
        val source = route.path[i]
        val target = route.path[i + 1]
        drawLine(mMap, source!!.lat, source!!.lon, target!!.lat, target!!.lon)
    }
}