package com.example.projektmunka.data

import kotlinx.serialization.Serializable

@Serializable
data class Node(
    val id: Long,
    val lat: Double,
    val lon: Double,
    var elevation: Double = 0.0, // TODO: change this ot val!
    val tags: Map<String, String>? = null,
    var importance: Int = 0  // Add an importance property with a default value of 0
) {
    override fun equals(other: Any?): Boolean {
        val otherNode: Node? = other as? Node

        if (otherNode != null && otherNode.id == this.id) {
            return true
        }
        return false
    }

    override fun hashCode(): Int {
        return this.id.hashCode()
    }
}
