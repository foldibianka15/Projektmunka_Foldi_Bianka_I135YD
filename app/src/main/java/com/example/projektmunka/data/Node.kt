package com.example.projektmunka.data

import kotlinx.serialization.Serializable

@Serializable
data class Node(
    val id: Long,
    val lat: Double,
    val lon: Double,
    val tags: Map<String, String>? = null,
    var importance: Int = 0  // Add an importance property with a default value of 0
)
