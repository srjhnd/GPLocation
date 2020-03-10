package com.srjhnd.gplocation

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.*
import com.srjhnd.gplocation.data.Location
import com.srjhnd.gplocation.data.LocationRepository
import com.srjhnd.gplocation.utils.InjectionUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.*


class GPLocationService : Service() {
    companion object {
        private const val LOG_TAG = "GPLocaitonService"
        private const val EXTRA_STARTED_FROM_NOTIFICATION = "EXTRA_STARTED_FROM_NOTIFICATION"
        const val UPDATE_FASTEST_INTERVAL: Long = 6000
        const val UPDATE_INTERVAL: Long = 10000
        private const val NOTIFICATION_ID = 1

        const val CHANNEL_ID = "GPNotificationChannel"
        const val INTENT_LOCATION_EXTRA = "INTENT_LOCATION_EXTRA"
        const val PACKAGE_NAME = "com.srjhnd.gplocation.GPLocationService"
    }

    inner class LocalBinder : Binder() {
        val service: GPLocationService
            get() = this@GPLocationService
    }


    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var repository: LocationRepository
    private var notificationManager: NotificationManager? = null
    private var bConfigChanged = false


    override fun onCreate() {
        repository = InjectionUtils.getLocationRepository(applicationContext)
        setupLocationAPI()
        createNotificationChannel()
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(LOG_TAG, "service started")
        val startedFromNotification =
            intent?.getBooleanExtra(EXTRA_STARTED_FROM_NOTIFICATION, false) ?: false
        if (startedFromNotification) {
            stopLocationUpdates()
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "GPLocation Service",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun getNotification(): Notification? {
        val activityPendingIntent =
            PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), 0)

        val intent = Intent(this, GPLocationService::class.java)
        intent.putExtra(EXTRA_STARTED_FROM_NOTIFICATION, true)
        val servicePendingIntent =
            PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop tracking", servicePendingIntent)
            .setContentText("Location is being posted in the background.")
            .setContentTitle(getString(R.string.app_name))
            .setContentIntent(activityPendingIntent)
            .setOngoing(true)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setTicker("Location is being posted in the background.")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(CHANNEL_ID) // Channel ID
        }
        return builder.build()
    }

    private fun setupLocationAPI() {
        createLocationRequest()
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult?) {
                Log.d(LOG_TAG, "New location arrived.")
                val tmpLocation = result?.lastLocation
                if (tmpLocation != null) {
                    val location = Location(
                        tmpLocation.longitude,
                        tmpLocation.latitude,
                        DateFormat.getTimeInstance().format(Date())
                    )
                    val intent = Intent(PACKAGE_NAME)
                    intent.putExtra(
                        INTENT_LOCATION_EXTRA,
                        location
                    )
                    LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
                    CoroutineScope(Dispatchers.IO).launch {
                        repository.postLocation(location)
                    }
                }
            }
        }
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

    }

    fun startLocationUpdates() {
        Log.d(LOG_TAG, "Starting location updates")
        startService(Intent(applicationContext, GPLocationService::class.java))
        try {
            fusedLocationProviderClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.myLooper()
            )
        } catch (se: SecurityException) {
            Log.d(LOG_TAG, "Failed to request updates. $se")
        }
    }

    private fun stopLocationUpdates() {
        Log.d(LOG_TAG, "Stoping location updates")
        try {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        } catch (se: SecurityException) {
            Log.d(LOG_TAG, "Failed to stop location updates. $se")
        }
    }


    override fun onBind(intent: Intent?): IBinder? {
        Log.d(LOG_TAG, "onBind() called")
        stopForeground(true)
        bConfigChanged = false
        return LocalBinder()
    }

    override fun onRebind(intent: Intent?) {
        Log.d(LOG_TAG, "onRebind() called")
        stopForeground(true)
        bConfigChanged = false
        super.onRebind(intent)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        if (!bConfigChanged) {
            Log.d(LOG_TAG, "Starting GPLocation service in foreground")
            startForeground(NOTIFICATION_ID, getNotification())

            if (serviceIsRunningInForeground(this)) {
                notificationManager?.notify(
                    NOTIFICATION_ID,
                    getNotification()
                )
            }
        }
        return true
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        bConfigChanged = true
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest()
        locationRequest.interval = UPDATE_INTERVAL
        locationRequest.fastestInterval = UPDATE_FASTEST_INTERVAL
        locationRequest.priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
    }

    fun serviceIsRunningInForeground(context: Context): Boolean {
        val manager = context.getSystemService(
            Context.ACTIVITY_SERVICE
        ) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (this.javaClass.name == service.service.className) {
                if (service.foreground) {
                    return true
                }
            }
        }
        return false
    }

}