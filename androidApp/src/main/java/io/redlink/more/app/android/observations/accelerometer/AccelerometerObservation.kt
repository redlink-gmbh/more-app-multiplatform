/*
 * Copyright LBI-DHP and/or licensed to LBI-DHP under one or more
 * contributor license agreements (LBI-DHP: Ludwig Boltzmann Institute
 * for Digital Health and Prevention -- A research institute of the
 * Ludwig Boltzmann Gesellschaft, Österreichische Vereinigung zur
 * Förderung der wissenschaftlichen Forschung).
 * Licensed under the Apache 2.0 license with Commons Clause
 * (see https://www.apache.org/licenses/LICENSE-2.0 and
 * https://commonsclause.com/).
 */
package io.redlink.more.app.android.observations.accelerometer

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import io.redlink.more.more_app_mutliplatform.observations.Observation
import io.redlink.more.more_app_mutliplatform.observations.observationTypes.AccelerometerType
import io.redlink.more.more_app_mutliplatform.util.Scope

private const val TAG = "AccelerometerObservation"

class AccelerometerObservation(
    context: Context
) : Observation(observationType = AccelerometerType(emptySet())), SensorEventListener {
    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val sensor = this.sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var sampleFrequency: Int = SensorManager.SENSOR_DELAY_NORMAL

    override fun onSensorChanged(event: SensorEvent?) {
        event?.values?.let {
            if (it.isNotEmpty()) {
                Scope.launch {
                    storeData(mapOf("x" to it[0], "y" to it[1], "z" to it[2]))
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.i(TAG, "Sensor accuracy changed to $accuracy")
    }

    override fun start(): Boolean {
        return sensor?.let {
            sensorManager.registerListener(this, it, sampleFrequency)
        } ?: false
    }

    override fun stop(onCompletion: () -> Unit) {
        sensor?.let {
            sensorManager.unregisterListener(this)
        }
        onCompletion()
    }

    override fun observerAccessible(): Boolean {
        return this.sensor != null
    }

    override fun applyObservationConfig(settings: Map<String, Any>) {
    }
}