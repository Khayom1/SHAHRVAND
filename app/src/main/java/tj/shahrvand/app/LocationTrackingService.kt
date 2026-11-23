package tj.shahrvand.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import java.time.LocalDate
import java.util.ArrayDeque

class LocationTrackingService : Service(), LocationListener {
    companion object {
        const val ACTION_START = "tj.shahrvand.app.tracker.START"
        const val ACTION_STOP = "tj.shahrvand.app.tracker.STOP"
        const val EXTRA_MODE = "tracker_mode"
        const val MODE_RUN = "run"
        const val MODE_WALK = "walk"
        const val PREFS = "shahrvand_local"
        private const val CHANNEL_ID = "shahrvand_tracking"
        private const val NOTIFICATION_ID = 4201
        private const val RUN_THRESHOLD_MPS = 1.72f
    }

    private lateinit var locationManager: LocationManager
    private val prefs by lazy { getSharedPreferences(PREFS, MODE_PRIVATE) }
    private var previous: Location? = null
    private val speedWindow = ArrayDeque<Float>()
    private var mode = MODE_WALK

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopTracking()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_WALK
                beginTracking()
            }
            else -> {
                if (prefs.getBoolean("tracker_active", false)) {
                    mode = prefs.getString("tracker_mode", MODE_WALK) ?: MODE_WALK
                    beginTracking()
                } else {
                    stopSelf()
                }
            }
        }
        return START_STICKY
    }

    private fun beginTracking() {
        if (prefs.getBoolean("tracker_active", false) &&
            prefs.getString("tracker_mode", MODE_WALK) == mode &&
            previous != null
        ) return

        val date = LocalDate.now().toString()
        val oldDate = prefs.getString("tracker_day", "")
        val editor = prefs.edit()
        if (oldDate != date) {
            editor.putString("tracker_day", date)
                .putFloat("walk_today_m", 0f)
                .putFloat("run_today_m", 0f)
        }
        editor.putBoolean("tracker_active", true)
            .putString("tracker_mode", mode)
            .putFloat("segment_walk_m", 0f)
            .putFloat("segment_run_m", 0f)
            .putFloat("tracker_accuracy_m", -1f)
            .apply()

        previous = null
        speedWindow.clear()

        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        try {
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                prefs.edit().putString("tracker_error", "GPS_OFF").apply()
                stopTracking()
                return
            }
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                2500L,
                0f,
                this
            )
            prefs.edit().remove("tracker_error").apply()
        } catch (security: SecurityException) {
            prefs.edit().putString("tracker_error", "NO_PERMISSION").apply()
            stopTracking()
        } catch (error: Exception) {
            prefs.edit().putString("tracker_error", "GPS_ERROR").apply()
            stopTracking()
        }
    }

    override fun onLocationChanged(location: Location) {
        if (!prefs.getBoolean("tracker_active", false)) return

        val today = LocalDate.now().toString()
        if (prefs.getString("tracker_day", "") != today) {
            prefs.edit()
                .putString("tracker_day", today)
                .putFloat("walk_today_m", 0f)
                .putFloat("run_today_m", 0f)
                .apply()
        }

        val accuracy = if (location.hasAccuracy()) location.accuracy else 999f
        prefs.edit().putFloat("tracker_accuracy_m", accuracy).apply()
        if (accuracy > 28f) return

        val previousLocation = previous
        previous = location
        if (previousLocation == null) return

        val elapsedSeconds = (location.time - previousLocation.time) / 1000f
        if (elapsedSeconds <= 0.4f || elapsedSeconds > 20f) return

        val distance = previousLocation.distanceTo(location)
        if (distance < 0.8f || distance > 35f) return

        val rawSpeed = distance / elapsedSeconds
        if (rawSpeed < 0.45f || rawSpeed > 6.2f) return

        speedWindow.addLast(rawSpeed)
        while (speedWindow.size > 5) speedWindow.removeFirst()
        val smoothedSpeed = speedWindow.average().toFloat()
        val isRunning = smoothedSpeed >= RUN_THRESHOLD_MPS

        val currentWalk = prefs.getFloat("walk_today_m", 0f)
        val currentRun = prefs.getFloat("run_today_m", 0f)
        val segmentWalk = prefs.getFloat("segment_walk_m", 0f)
        val segmentRun = prefs.getFloat("segment_run_m", 0f)

        val edit = prefs.edit()
        if (isRunning) {
            edit.putFloat("run_today_m", currentRun + distance)
                .putFloat("segment_run_m", segmentRun + distance)
        } else {
            edit.putFloat("walk_today_m", currentWalk + distance)
                .putFloat("segment_walk_m", segmentWalk + distance)
        }
        edit.putFloat("tracker_speed_mps", smoothedSpeed)
            .putLong("tracker_last_fix", System.currentTimeMillis())
            .apply()

        updateNotification(isRunning, smoothedSpeed)
        val updatedWalk = prefs.getFloat("walk_today_m", 0f)
        val updatedRun = prefs.getFloat("run_today_m", 0f)
        val testMode = prefs.getBoolean("test_mode", true)
        val target = if (testMode) {
            if (mode == MODE_RUN) 120f else 200f
        } else {
            if (mode == MODE_RUN) 2000f else 5000f
        }
        val achieved = if (mode == MODE_RUN) updatedRun >= target else updatedWalk >= target
        if (achieved) finishMissionAutomatically()
    }

    private fun finishMissionAutomatically() {
        val missionId = if (mode == MODE_WALK) "walk_" + LocalDate.now().toString() else mode
        val completed = prefs.getStringSet("completed_v2", emptySet())?.toMutableSet() ?: mutableSetOf()
        val alreadyCompleted = missionId in completed
        completed.add(missionId)
        val testMode = prefs.getBoolean("test_mode", true)
        val reward = when (missionId) {
            MODE_RUN -> if (testMode) 8 else 40
            else -> if (testMode) 8 else 35
        }
        val editor = prefs.edit()
            .putStringSet("completed_v2", completed)
            .putBoolean("tracker_goal_reached_$mode", true)
            .putBoolean("tracker_active", false)
            .putString("tracker_last_completed", missionId)
            .putLong("tracker_completed_at", System.currentTimeMillis())
        if (!alreadyCompleted) {
            editor.putInt("total_xp", prefs.getInt("total_xp", 0) + reward)
        }
        editor.apply()
        try { locationManager.removeUpdates(this) } catch (_: Exception) { }
        previous = null
        speedWindow.clear()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun stopTracking() {
        try { locationManager.removeUpdates(this) } catch (_: Exception) { }
        val walk = prefs.getFloat("segment_walk_m", 0f)
        val run = prefs.getFloat("segment_run_m", 0f)
        prefs.edit()
            .putBoolean("tracker_active", false)
            .putFloat("last_segment_walk_m", walk)
            .putFloat("last_segment_run_m", run)
            .apply()
        previous = null
        speedWindow.clear()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SHAHRVAND — GPS-трекинг",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Огоҳинома ҳангоми ченкунии масофа"
                setShowBadge(false)
            }
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String? = null): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("ШАҲРВАНД")
            .setContentText(text ?: if (mode == MODE_RUN) "GPS фаъол • давидан" else "GPS фаъол • роҳгардӣ")
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotification(isRunning: Boolean, speed: Float) {
        val label = if (isRunning) "давидан" else "роҳгардӣ"
        val kmh = speed * 3.6f
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(
            NOTIFICATION_ID,
            buildNotification("GPS фаъол • $label • %.1f км/соат".format(kmh))
        )
    }

    override fun onProviderEnabled(provider: String) = Unit
    override fun onProviderDisabled(provider: String) {
        if (provider == LocationManager.GPS_PROVIDER) {
            prefs.edit().putString("tracker_error", "GPS_OFF").apply()
        }
    }
    @Deprecated("Deprecated by Android")
    override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) = Unit
}
