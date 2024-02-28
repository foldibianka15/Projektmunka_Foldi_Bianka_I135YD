package com.example.projektmunka.logic

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.example.projektmunka.data.Node
import com.example.projektmunka.data.Route
import com.example.projektmunka.repository.UserRouteRepository
import com.example.projektmunka.services.LocationService
import com.example.projektmunka.utils.Constants.TIMER_INTERVAL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Timer
import java.util.TimerTask

@SuppressLint("MissingPermission")
class UserRouteTracker(
    private val locationService: LocationService,
    private val routeGenerator: RouteGenerator,
    private val fitnessCalculator: FitnessCalculator,
    private val stepCounter: StepCounter,
) {

    private var isTrackingStarted = false
    private var startTime: Long? = null
    private var endTime: Long? = null
    private var timer: Timer? = null
    private var elapsedTime: Long = 0

    private val _locationList = MutableStateFlow(mutableListOf<Pair<Location, Long>>())
    val locationList = _locationList.asStateFlow()

    private  val _lastLocation = MutableStateFlow<Location?>(null)
    val lastLocation = _lastLocation.asStateFlow()

    private val _distanceTravelled = MutableStateFlow(0.0)
    val distanceTravelled = _distanceTravelled.asStateFlow()

    private val _averageSpeed = MutableStateFlow(0.0)
    val averageSpeed = _averageSpeed.asStateFlow()

    private val _stepsTaken = MutableStateFlow(0)
    val stepsTaken = _stepsTaken.asStateFlow()

    private val _calorieBurned = MutableStateFlow(0.0)
    val calorieBurned = _calorieBurned.asStateFlow()

    private val _heartRate = MutableStateFlow(0)
    val heartRate = _heartRate.asStateFlow()

    private val _averageHeartRate = MutableStateFlow(0.0)
    val averageHeartRate = _averageHeartRate.asStateFlow()

    private val _isSessionFinished = MutableStateFlow(false)
    val isSessionFinished = _isSessionFinished.asStateFlow()

    private val _generatedRoute = MutableStateFlow<Route?>(null)
    val generatedRoute = _generatedRoute.asStateFlow()

    private val _currentRoute = MutableStateFlow<Route?>(null)
    val currentRoute = _currentRoute.asStateFlow()

    fun startTimer() {
        timer = Timer()
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                elapsedTime += TIMER_INTERVAL
            }
        }, 0, TIMER_INTERVAL.toLong())
    }

    fun stopTimer(){
        timer?.cancel()
        timer = null
    }

    fun startTracking(currentLocaion: Location) {
        if (!isTrackingStarted) {
            isTrackingStarted = true

            stepCounter.registerStepCounterListener()
            startTime = System.currentTimeMillis()
            startTimer()

            CoroutineScope(Dispatchers.IO).launch {
                val route = routeGenerator.generateCircularDifficultRoute(1.5, currentLocaion)
                _generatedRoute.emit(route)

                val routeStartingPoint = route.path.firstOrNull()

                routeStartingPoint?.let {
                    // Convert Node to Location
                    val startingLocation = it.toLocation()
                    // Start tracking using the Location object
                    observeLocationData()
                    updateSessionData(startingLocation)
                }
            }
        }
    }

    fun Node.toLocation(): Location {
        val location = Location("")
        location.latitude = this.lat
        location.longitude = this.lon
        return location
    }

    private fun observeLocationData() {
        CoroutineScope(locationService.scope.coroutineContext).launch {
            locationService.locationDataFlow.collect{ locationData ->
                locationData?.let {
                    _locationList.value = _locationList.value.toMutableList().apply {
                        add(locationData)
                    }
                    _lastLocation.emit(locationData.first)

                }
            }
        }
    }

    fun stopTracking() {
        if(isTrackingStarted) {
            isTrackingStarted = false
            locationService.stopLocationUpdates()
            endTime = System.currentTimeMillis()
            stopTimer()
            stepCounter.unregisterStepCounterListener()
        }
    }

    private fun updateSessionData(startPoint: Location) {
        CoroutineScope(Dispatchers.Default).launch {
            val distance = fitnessCalculator.calculateDistance(startPoint, lastLocation.value!!)
            _distanceTravelled.emit(_distanceTravelled.value + distance)
            _averageSpeed.emit(fitnessCalculator.calculateAverageSpeed(distance, elapsedTime))
            _stepsTaken.emit(stepCounter.getStepCount())
            _calorieBurned.emit(fitnessCalculator.calculateCaloriesBurned(locationList.value))
            //_heartRate = fitnessCalculator.calculateHeartRate()
            //_averageHeartRate = fitnessCalculator.calculateAverageHeartRate()
        }
    }


    /*private fun saveUserRouteToDatabase(route: Route){
        userRouteRepository.saveUserRouteToDatabase(route)
    }*/


}