package com.example.wearosmodule

import android.hardware.Sensor
import android.hardware.SensorManager

class HeartRateSensorManager(private val sensorManager: SensorManager) {

    init{
        val heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
    }
}