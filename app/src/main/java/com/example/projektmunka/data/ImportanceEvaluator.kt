package com.example.projektmunka.data

class ImportanceEvaluator {
    companion object {
        fun evaluate(node: Node): Int {
            var importance = 0

            // Add your criteria for evaluating importance here
            if (node.tags?.containsKey("wikipedia") == true || node.tags?.containsKey("wikidata") == true) {
                importance += 3
            }

            // Check for 'historic' tag
            if (node.tags?.containsKey("historic") == true) {
                importance += 3
            }

            // Check for a link or URL (website tag)
            if (node.tags?.containsKey("website") == true || node.tags?.containsKey("url") == true) {
                importance += 1
            }

            // Check for each 'name' tag (including 'old_name')
            val nameTags = node.tags?.keys?.filter { it.startsWith("name") }
            importance += nameTags?.size ?: 0

            // Check for tags defining architectural style
            val architecturalStyleTags =
                listOf("architectural_style", "building:architecture", "heritage")
            for (tag in architecturalStyleTags) {
                if (node.tags?.containsKey(tag) == true) {
                    importance += 1
                }
            }

            // Check for additional tags
            val additionalTags = listOf(
                "tourism",
                "historic",
                "landuse",
                "water",
                "waterway",
                "highway",
                "Amenity",
                "graffiti",
                "place_of_worship"
            )

            for (tag in additionalTags) {
                if (node.tags?.containsKey(tag) == true) {
                    importance += 1
                }
            }

            return importance
        }
    }
}