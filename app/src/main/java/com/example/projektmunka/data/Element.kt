package com.example.projektmunka.data

import kotlinx.serialization.Serializable

@Serializable
data class Element(
    val type: String,
    val id: Long,
    val lat: Double = 0.0, // Provide default values
    val lon: Double = 0.0,
    val nodes: List<Long> = emptyList()
)
