package com.example.projektmunka.logic

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class StepCounter(private val context: Context) {

    private var stepCount = 0

    private val stepCounterListener = object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

        override fun onSensorChanged(event: SensorEvent?) {
            event?.let {
                if (it.sensor.type == Sensor.TYPE_STEP_COUNTER)
                    stepCount = it.values[0].toInt()
            }
        }
    }

    fun registerStepCounterListener() {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        stepCounterSensor?.let {
            sensorManager.registerListener(
                stepCounterListener,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
    }

    fun unregisterStepCounterListener() {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorManager.unregisterListener(stepCounterListener)
    }

    fun getStepCount(): Int {
        return stepCount
    }
}