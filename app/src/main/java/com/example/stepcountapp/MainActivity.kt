package com.example.stepcountapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlin.math.sqrt

class MainActivity : AppCompatActivity(), SensorEventListener {

    val ACTIVITY_RECONGNITION_REQUEST_CODE = 100
    private var sensorManager: SensorManager? = null
    private var running: Boolean = false
    private var previousTotalSteps = 0
    private var totalSteps = 0
    var magnitudePreviousStep = 0.0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (isPermissionGranted()) {
            requestPermission()
        }

        loadData()
        resetSteps()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    private fun saveData() {
        val sharedPreferences = getSharedPreferences("step", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
       editor.putFloat("currentStep", previousTotalSteps.toFloat())
        editor.apply()
    }

    private fun loadData() {
        val sharedPreferences = getSharedPreferences("step", Context.MODE_PRIVATE)
        val savedNo: Float = sharedPreferences.getFloat("currentStep", 0f)
        previousTotalSteps = savedNo.toInt()
    }

    private fun resetSteps() {
        val stepTaken = findViewById<TextView>(R.id.tvStepCount)
        stepTaken.setOnClickListener {
            Toast.makeText(this, "Удерживайте, чтобы сбросить шаги", Toast.LENGTH_SHORT).show()
        }
        stepTaken.setOnLongClickListener {
            previousTotalSteps = totalSteps
            stepTaken.text = "0"
            saveData()
            true
        }
    }

    override fun onResume() {
        super.onResume()
        running = true
        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val countSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        val detectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        when {
            countSensor != null -> {
                sensorManager.registerListener(this, countSensor, SensorManager.SENSOR_DELAY_UI)
            }

            detectorSensor != null -> {
                sensorManager.registerListener(this, detectorSensor, SensorManager.SENSOR_DELAY_UI)
            }

            accelerometer != null -> {
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
            }

            else -> {
                Toast.makeText(this, "Your device not is compatible", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        running = false
        sensorManager?.unregisterListener(this)
    }

    private fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACTIVITY_RECOGNITION),
                ACTIVITY_RECONGNITION_REQUEST_CODE
            )
        }
    }

    private fun isPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACTIVITY_RECOGNITION
        ) != PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            ACTIVITY_RECONGNITION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                }
            }
        }
    }

    override fun onSensorChanged(sensorEvent: SensorEvent?) {
        var stepTaken = findViewById<TextView>(R.id.tvStepCount)
        if (sensorEvent!!.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val xaccel: Float = sensorEvent.values[0]
            val yaccel: Float = sensorEvent.values[1]
            val zaccel: Float = sensorEvent.values[2]
            val magnitude: Double =
                sqrt(xaccel * xaccel + yaccel * yaccel + zaccel * zaccel).toDouble()
            val magnitudeDelta: Double = magnitude - magnitudePreviousStep
            magnitudePreviousStep = magnitude
            if (magnitudeDelta > 6) {
                totalSteps++
            }
            val step: Int = totalSteps
            stepTaken.text = step.toString()
            Log.d("TAG", "onSensorChanged: $step")
        } else {
            if (running) {
                totalSteps = sensorEvent!!.values[0].toInt()
                val currentSteps = totalSteps - previousTotalSteps
                stepTaken.text = currentSteps.toString()
                Log.d("TAG", "onSensorChanged: $currentSteps")
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }
}