import androidx.appcompat.app.AppCompatActivity
import com.example.firstapp.utils.ShenandoahsHikingDifficulty
import com.example.firstapp.utils.calculateWalkingtime
import com.example.firstapp.utils.displayRoute
import com.example.projektmunka.data.Node
import com.example.projektmunka.data.Route
import com.example.projektmunka.utils.callOverpass
import com.example.projektmunka.utils.findNearestNode
import com.example.projektmunka.utils.getElevationData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.jgrapht.Graph
import org.jgrapht.alg.shortestpath.DijkstraShortestPath
import org.jgrapht.graph.DefaultWeightedEdge
import kotlin.random.Random

typealias RouteMetric = (Graph<Node, DefaultWeightedEdge>, Route) -> Double

private fun Test(start: Pair<Double, Double>, end: Pair<Double, Double>) {

    return runBlocking {

        val startNode = async(Dispatchers.IO) {
            findNearestNode(start.first, start.second)
        }.await() ?: return@runBlocking

        val endNode = async(Dispatchers.IO) {
            findNearestNode(end.first, end.second)
        }.await() ?: return@runBlocking

        val minLat = (if (start.first < end.first) start.first else end.first) - 0.01
        val minLon = (if (start.second < end.second) start.second else end.second) - 0.01
        val maxLat = (if (start.first > end.first) start.first else end.first) + 0.01
        val maxLon = (if (start.second > end.second) start.second else end.second) + 0.01
        val bbox = "$minLat,$minLon,$maxLat,$maxLon"

        val graph = async(Dispatchers.IO) {
            callOverpass(bbox)
        }.await()

        println(graph != null)

        val elevations = async(Dispatchers.IO) {
            getElevationData(graph!!)
        }.await()

        GenerateTimeDiffPaths(graph!!, startNode, endNode, 500, ::calculateWalkingtime, ::ShenandoahsHikingDifficulty, 70.0, 25.0, 3.0, 1.5)
    }
}

private fun TestMetric(graph: Graph<Node, DefaultWeightedEdge>,
                       maxIterations : Int,
                       metric: RouteMetric,
                       duplicatePunishment : Double,
                       testPairs : Int, rounds : Int) {

    //val fos =  android.content.ContextWrapper.openFileOutput("log.txt", AppCompatActivity.MODE_APPEND)
    val vertices = graph.vertexSet()

    for (t in 0 .. testPairs) {
        val startNode = vertices.random()
        val endNode = vertices.random()

        var minMetric = -2.0
        var maxMetric = -1.0

        for (round in 0 .. rounds) {
            val targetMetric = Random.nextDouble(minMetric, maxMetric)
            val metric : Double
            var bestMetric = 0.0
            var bestFitness = Double.MAX_VALUE

            for (i in 0 .. maxIterations) {
                val mid = vertices.random()
                val path = ConnectPoints(graph, startNode, mid, endNode)

                if (path != null) {
                    val metric = metric(graph, path)
                    val fitness = Math.abs(targetMetric - metric) + Punishment(path, duplicatePunishment)
                    //println("metric: " + metric(graph, path))
                    if (fitness < bestFitness) {
                        bestFitness = fitness
                        bestMetric = metric
                    }

                    if (minMetric < 0 || metric < minMetric) {
                        minMetric = metric
                    }

                    if (maxMetric < 0 || metric > maxMetric) {
                        maxMetric = metric
                    }
                }
            }


           // val FILENAME = "log.txt"
            //val string = "$targetMetric,$bestMetric\n"

            //fos.write(string.toByteArray())
            println("Round: $round, target metric: $targetMetric, best metric: $bestMetric, minRange: $minMetric, maxRange: $maxMetric")
        }

        //fos.write(t.toString().toByteArray())
    }
    //fos.write("\n\n".toString().toByteArray())
    //fos.close()
}

private fun GetRoute(start: Pair<Double, Double>, end: Pair<Double, Double>, maxIterations : Int, metric: RouteMetric, targetMetric : Double, duplicatePunishment : Double) : Route? {
    return runBlocking {
        val startNode = async(Dispatchers.IO) {
            findNearestNode(start.first, start.second)
        }.await() ?: return@runBlocking null

        val endNode = async(Dispatchers.IO) {
            findNearestNode(end.first, end.second)
        }.await() ?: return@runBlocking null

        val minLat = (if (startNode.lat < endNode.lat) startNode.lat else endNode.lat) - 0.01
        val minLon = (if (startNode.lon < endNode.lon) startNode.lon else endNode.lon) - 0.01
        val maxLat = (if (startNode.lat > endNode.lat) startNode.lat else endNode.lat) + 0.01
        val maxLon = (if (startNode.lon > endNode.lon) startNode.lon else endNode.lon) + 0.01
        val bbox = "$minLat,$minLon,$maxLat,$maxLon"

        val graph = async(Dispatchers.IO) {
            callOverpass(bbox)
        }.await() ?: return@runBlocking null

        val elevations = async(Dispatchers.IO) {
            getElevationData(graph)
        }.await()

        val path = GeneratePaths(graph, startNode, endNode, maxIterations, metric, targetMetric, duplicatePunishment)
        return@runBlocking path
    }
}

private fun GeneratePaths(graph: Graph<Node, DefaultWeightedEdge>, start: Node, end: Node, maxIterations : Int, metric: RouteMetric, targetMetric : Double, duplicatePunishment : Double) : Route {
    val vertices = graph.vertexSet()
    var bestFitness = Double.MAX_VALUE
    var bestPath = Route(mutableListOf())
    for (i in 0 .. maxIterations) {
        val mid = vertices.random()
        val path = ConnectPoints(graph, start, mid, end)

        if (path != null) {
            val fitness = Math.abs(targetMetric - metric(graph, path)) + Punishment(path, duplicatePunishment)

            if (fitness < bestFitness) {
                bestFitness = fitness
                bestPath = path
            }
        }
    }

    println("metric: " + metric(graph, bestPath))
    println("fitness: " + bestFitness)
    return bestPath
}

private fun GenerateTimeDiffPaths(graph: Graph<Node, DefaultWeightedEdge>, start: Node, end: Node, maxIterations : Int, timeMetric: RouteMetric, difficultyMetric : RouteMetric, targetTimeMetric : Double, targetDifficultyMetric : Double, timeThreshold : Double, duplicatePunishment : Double) : Route {
    val vertices = graph.vertexSet()
    var bestFitness = Double.MAX_VALUE
    var bestPath = Route(mutableListOf())

    for (i in 0 .. maxIterations) {
        val mid = vertices.random()
        val path = ConnectPoints(graph, start, mid, end)

        if (path != null) {
            val time = timeMetric(graph, path)
            val difficulty = difficultyMetric(graph, path)
            var fitness : Double

            if (Math.abs(targetTimeMetric - time) <= timeThreshold) {
                fitness = Math.abs(targetDifficultyMetric - difficulty + Punishment(path, duplicatePunishment))
            }
            else {
                fitness = Math.abs(targetTimeMetric - time) + Punishment(path, duplicatePunishment)

            }

            if (fitness < bestFitness) {
                bestFitness = fitness
                bestPath = path
                println("New best: fitess: $fitness, time: $time, difficulty: $difficulty")
            }
        }
    }
    val time = timeMetric(graph, bestPath)
    val difficulty = difficultyMetric(graph, bestPath)
    println("Best: time: $time, difficulty: $difficulty")
    return bestPath
}

private fun ConnectPoints(graph: Graph<Node, DefaultWeightedEdge>, start : Node, mid : Node, end: Node) : Route? {
    val dijkstra = DijkstraShortestPath(graph)
    val startToMid = dijkstra.getPath(start, mid)?.vertexList ?: return null
    val midToEnd = dijkstra.getPath(mid, end)?.vertexList?.toMutableList() ?: return null

    val startToEnd = mutableListOf<Node>()
    startToEnd.addAll(startToMid)
    if (midToEnd.size > 0) {
        midToEnd.removeAt(0)
    }
    startToEnd.addAll(midToEnd)

    return Route(startToEnd)
}

private fun Punishment(route: Route, penalty: Double) : Double {
    val duplicates = route.path.filter { route.path.indexOf(it) != route.path.lastIndexOf(it)}
    return  duplicates.size * penalty
}