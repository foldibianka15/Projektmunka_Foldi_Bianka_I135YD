package com.example.projektmunka.data

import kotlinx.serialization.Serializable

@Serializable
data class OverpassResponse(val elements: List<Element>?,
                            val version: Double) // Add this property if 'version' is expected

