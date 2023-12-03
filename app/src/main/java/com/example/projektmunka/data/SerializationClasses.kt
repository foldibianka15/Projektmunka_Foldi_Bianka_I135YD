package com.example.projektmunka.data

import kotlinx.serialization.Serializable

@Serializable
data class ElevationRequest(
    val locations: List<Coordinate>
)

@Serializable
data class Coordinate(
    val latitude: Double,
    val longitude: Double
)

@Serializable
data class ElevationResponse(
    val results: List<ResponseCoordinate>
)

@Serializable
data class ResponseCoordinate(
    val elevation: Double,
    val latitude: Double,
    val longitude: Double
)