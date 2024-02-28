import com.example.projektmunka.data.Node
import com.example.projektmunka.data.Route
import org.jgrapht.Graph
import org.jgrapht.alg.shortestpath.DijkstraShortestPath
import org.jgrapht.alg.shortestpath.YenKShortestPath
import org.jgrapht.graph.DefaultWeightedEdge
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

typealias RouteMetric = (Graph<Node, DefaultWeightedEdge>, Route) -> Double

class GeneticAlgorithmRouteOptimizer(
    private val graph: Graph<Node, DefaultWeightedEdge>,
    private val startNode: Node,
    private val endNode: Node,
    private val targetRouteLength: Double,
    private val populationSize: Int,
    private val survivorRate: Int,
    private val returnFitness: Double,
    private val maxGenerations: Int,
    private val routeMetric: RouteMetric
) {

    private val dijkstra: DijkstraShortestPath<Node, DefaultWeightedEdge> = DijkstraShortestPath(graph)

    init {

        // You can perform any setup or initialization logic here if needed
    }

    fun findRoute(): Route {
        // Initial population
        val path = dijkstra.getPath(startNode, endNode).vertexList
        var population = mutableListOf<Route>()

        for (i in 1..populationSize - 1) {
            population.add(Route(path.toMutableList()))
        }

        val nChildren = (populationSize - survivorRate) / survivorRate

        // Genetic algorithm loop
        for (generation in 0 until maxGenerations) {
            // Evaluate fitness and select the best paths
            val evaluatedPaths = population.map { Pair(it, fitness(it)) }
            val bestPaths = evaluatedPaths.sortedBy { it.second }.take(survivorRate)

            // Check if the target length is achieved by the best path
            val bestPath = bestPaths.first()
            if (bestPath.second <= returnFitness) {
                return bestPath.first
            }

            // Create a new generation
            var newPopulation = mutableListOf<Route>()

            for (survivor in bestPaths) {
                newPopulation.add(survivor.first)
                for (i in 0 until nChildren) {
                    newPopulation.add(mutatePath(survivor.first))
                }
            }

            // filter out degenerate paths
            newPopulation = newPopulation.filter { !hasDuplicates(it.path) }.toMutableList()

            // Replace the old population with the new one
            population = newPopulation.toMutableList()
        }

        return population.minBy { fitness(it) }!!
    }

    fun findRouteWithCrossover(): Route {
        // Initial population
        val path = dijkstra.getPath(startNode, endNode).vertexList
        var population = mutableListOf<Route>()

        for (i in 1..populationSize) {
            population.add(Route(path.toMutableList()))
        }

        val nChildren = (populationSize - survivorRate) / survivorRate

        // Genetic algorithm loop
        for (generation in 0 until maxGenerations) {
            // Evaluate fitness and select the best paths
            val evaluatedPaths = population.map { Pair(it, fitness(it)) }
            val bestPaths = evaluatedPaths.sortedBy { it.second }.take(survivorRate)

            // Check if the target length is achieved by the best path
            val bestPath = bestPaths.first()
            if (bestPath.second <= returnFitness) {
                return bestPath.first
            }

            // Create a new generation
            var newPopulation = mutableListOf<Route>()

            for (survivor in bestPaths) {
                newPopulation.add(survivor.first)

                for (i in 0 until nChildren) {
                    val otherParent = bestPaths.random()
                    newPopulation.add(mutatePath(crossover(survivor.first, otherParent.first)))
                }
            }

            // filter out degenerate paths
            newPopulation = newPopulation.filter { !hasDuplicates(it.path) }.toMutableList()

            // Replace the old population with the new one
            population = newPopulation.toMutableList()
        }

        // Return the best path found after the specified number of generations
        return population.minBy { fitness(it) }!!
    }

    fun nearestNode(lat : Double, lon: Double, threshold: Double) : Node? {
        for (node in graph.vertexSet()) {
            if (calculateDistance(Node(-1, lat, lon), node) < threshold) {
                return node
            }
        }
        return null
    }

    fun pickRandomParent(pool: MutableList<Route>, parent: Route) : Route {
        var choice = pool.random()
        while (choice == parent) {
            choice = pool.random()
        }
        return choice
    }

    fun calculateDistance(node1: Node, node2: Node): Double {
        val radius = 6371.0 // Earth's radius in kilometers

        val lat1Rad = Math.toRadians(node1.lat)
        val lon1Rad = Math.toRadians(node1.lon)
        val lat2Rad = Math.toRadians(node2.lat)
        val lon2Rad = Math.toRadians(node2.lon)

        val dLat = lat2Rad - lat1Rad
        val dLon = lon2Rad - lon1Rad

        val a = sin(dLat / 2).pow(2) + cos(lat1Rad) * cos(lat2Rad) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return radius * c
    }

    fun fitness(route2: Route) : Double {
        return kotlin.math.abs(routeMetric(graph, route2) - targetRouteLength)
    }

    fun calculateRouteLength(route: Route): Double {
        var length = 0.0
        for (i in 0 until route.path.size - 1) {
            val source = route.path[i]
            val target = route.path[i + 1]
            length += graph.getEdgeWeight(graph.getEdge(source, target))
        }
        return length
    }

    fun mutatePath(route: Route): Route {
        val path = route.path.toMutableList()

        val intersections = mutableListOf<Node?>()
        for (node in path) {
            if (graph.outgoingEdgesOf(node).size > 2 ){
                intersections.add(node)
            }
        }

        val L = 2

        var insertPosition = 0

        if (intersections.size <= L) {
            insertPosition = Random.nextInt(0, intersections.size)
        }
        else {
            insertPosition = Random.nextInt(0, intersections.size - L)
        }

        val head = path.slice(0..path.indexOf(intersections[insertPosition]))
        val subPath = path.slice(path.indexOf(intersections[insertPosition]) ..path.indexOf(intersections[insertPosition + L]))
        val tail = path.slice(path.indexOf(intersections[insertPosition + L]) + 1..path.size - 1)


        // Find K shortest paths using a modified Dijkstra's algorithm
        val yenKShortestPath = YenKShortestPath(graph)

        // Find 20 shortest paths from vertex 1 to vertex 4
        val shortestPaths = yenKShortestPath.getPaths(intersections[insertPosition], intersections[insertPosition + L], 2)

        var newSubPath = shortestPaths[Random.nextInt(shortestPaths.size)].vertexList.toMutableList()

        if (shortestPaths.size > 1) {
            while (newSubPath == subPath) {
                newSubPath = shortestPaths[Random.nextInt(shortestPaths.size)].vertexList.toMutableList()
            }
        }

        if (newSubPath.isNotEmpty()) {
            newSubPath.removeAt(0)
        }


        val newPath = mutableListOf<Node>()
        newPath.addAll(head)
        newPath.addAll(newSubPath)
        newPath.addAll(tail)

        return Route(newPath)
    }

    fun crossover(parent1: Route, parent2: Route) : Route {
        val childRoute = mutableListOf<Node>()
        val commonPoints = parent1.path.intersect(parent2.path).toMutableList()
        commonPoints.removeFirst()
        commonPoints.removeLast()

        if (commonPoints.isNotEmpty()){
            val swapPoint = commonPoints.random()
            val parent1SwapIndex = parent1.path.indexOf(swapPoint)
            val parent2SwapIndex = parent2.path.indexOf(swapPoint)


            childRoute.addAll(parent1.path.subList(0, parent1SwapIndex))
            childRoute.addAll(parent2.path.subList(parent2SwapIndex, parent2.path.size))

            return Route(childRoute)
        }

        val artificialSwapPoint = parent1.path.subList(1, parent1.path.size - 1).random() // TODO: check indexing
        val artificialSwapIndex = parent1.path.indexOf(artificialSwapPoint)
        val parent2ForkPoint =  parent2.path.minBy { calculateDistance(artificialSwapPoint!!, it!!) }
        val forkIndex = parent2.path.indexOf(parent2ForkPoint)

        val dijkstra = DijkstraShortestPath(graph)
        val connectingPath = dijkstra.getPath(parent2ForkPoint, artificialSwapPoint).vertexList

        childRoute.addAll(parent2.path.subList(0, forkIndex))
        childRoute.addAll(connectingPath)
        childRoute.addAll(parent1.path.subList(artificialSwapIndex, parent1.path.size))

        return Route(childRoute)
    }

    fun hasDuplicates(list: MutableList<Node>): Boolean {
        val set = HashSet<Node?>()
        for (element in list) {
            if (!set.add(element)) {
                return true // Element is already in the set, so it's a duplicate
            }
        }
        return false // No duplicates found
    }
}
