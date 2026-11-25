package tj.shahrvand.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.Manifest
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.security.MessageDigest
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

data class GpsTrackingMetrics(
    val distanceM: Int = 0,
    val samples: Int = 0,
    val averageAccuracyM: Double = 999.0,
    val maxSpeedKmh: Double = 0.0,
    val stepCount: Int = 0,
    val routeHash: String = ""
)

object GpsTrackingStore {
    private val lock = Any()
    private val _metrics = MutableStateFlow(GpsTrackingMetrics())
    val metrics: StateFlow<GpsTrackingMetrics> = _metrics
    private var previous: Location? = null
    private var distance = 0f
    private var accuracySum = 0.0
    private var sampleCount = 0
    private var maxSpeed = 0.0
    private var firstStepCount = -1f
    private var currentStepCount = -1f
    private val routePoints = StringBuilder()

    fun reset() = synchronized(lock) {
        previous = null
        distance = 0f
        accuracySum = 0.0
        sampleCount = 0
        maxSpeed = 0.0
        firstStepCount = -1f
        currentStepCount = -1f
        routePoints.clear()
        _metrics.value = GpsTrackingMetrics()
    }

    fun addLocation(location: Location) = synchronized(lock) {
        if (!location.hasAccuracy() || location.accuracy > 35f) return@synchronized
        val old = previous
        if (old != null) {
            val delta = old.distanceTo(location)
            val seconds = (location.time - old.time).coerceAtLeast(1L) / 1000f
            val speed = delta / seconds * 3.6
            if (delta in 1f..80f && speed <= 55.0) {
                distance += delta
                maxSpeed = max(maxSpeed, speed)
            }
        }
        previous = location
        sampleCount++
        accuracySum += location.accuracy.toDouble()
        routePoints.append(String.format(Locale.US, "%.6f,%.6f,%d|", location.latitude, location.longitude, location.time))
        if (routePoints.length > 180_000) routePoints.delete(0, routePoints.length / 2)
        val hash = MessageDigest.getInstance("SHA-256").digest(routePoints.toString().toByteArray())
            .joinToString("") { "%02x".format(it) }
        _metrics.value = GpsTrackingMetrics(
            distanceM = distance.roundToInt(),
            samples = sampleCount,
            averageAccuracyM = accuracySum / sampleCount,
            maxSpeedKmh = maxSpeed,
            stepCount = if (firstStepCount < 0f || currentStepCount < 0f) 0 else (currentStepCount - firstStepCount).roundToInt().coerceAtLeast(0),
            routeHash = hash
        )
    }

    fun addStepCount(value: Float) = synchronized(lock) {
        if (firstStepCount < 0f) firstStepCount = value
        currentStepCount = value
        _metrics.value = _metrics.value.copy(stepCount = (currentStepCount - firstStepCount).roundToInt().coerceAtLeast(0))
    }
}

class ActivityLocationService : Service() {
    private var locationManager: LocationManager? = null
    private var sensorManager: SensorManager? = null
    private var stepListener: SensorEventListener? = null
    private var locationListener: LocationListener? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        startTracking()
        return START_STICKY
    }

    private fun startTracking() {
        if (locationListener != null) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            stopSelf()
            return
        }
        val manager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationManager = manager
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                GpsTrackingStore.addLocation(location)
            }
            @Deprecated("Deprecated in Android")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }
        locationListener = listener
        try {
            manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 2f, listener)
        } catch (_: SecurityException) {
            stopSelf()
            return
        }

        val sensors = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorManager = sensors
        val stepSensor = sensors.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (stepSensor != null && ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED) {
            val steps = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    if (event.values.isNotEmpty()) GpsTrackingStore.addStepCount(event.values[0])
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }
            stepListener = steps
            sensors.registerListener(steps, stepSensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "SHAHRVAND activity tracking", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("SHAHRVAND")
            .setContentText("GPS activity session is being recorded on this device")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

    override fun onDestroy() {
        locationListener?.let { listener -> runCatching { locationManager?.removeUpdates(listener) } }
        stepListener?.let { sensorManager?.unregisterListener(it) }
        locationListener = null
        stepListener = null
        locationManager = null
        sensorManager = null
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "shahrvand_activity_tracking"
        private const val NOTIFICATION_ID = 7104
    }
}
