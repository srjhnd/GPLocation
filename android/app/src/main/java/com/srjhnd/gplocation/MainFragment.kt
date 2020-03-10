package com.srjhnd.gplocation

import android.Manifest
import android.app.Activity
import android.content.*
import android.content.IntentSender.SendIntentException
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.SettingsClient
import com.google.android.material.snackbar.Snackbar
import com.srjhnd.gplocation.data.Location
import com.srjhnd.gplocation.databinding.MainFragmentBinding
import com.srjhnd.gplocation.utils.InjectionUtils
import com.srjhnd.gplocation.viewmodels.LocationViewModel

class MainFragment : Fragment() {

    companion object {
        private const val LOG_TAG = "MAIN_FRAGMENT"
        private const val REQUSET_CODE = 55
        private const val REQUEST_CHECK_SETTINGS = 56
    }

    private lateinit var binding: MainFragmentBinding

    private lateinit var connectionSnackbar: Snackbar
    private lateinit var locationSettingsRequest: LocationSettingsRequest
    private lateinit var settingsClient: SettingsClient

    private var bound = false
    private var gpLocationService: GPLocationService? = null
    private val locationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val tmpLocation =
                intent?.getParcelableExtra<Location>(GPLocationService.INTENT_LOCATION_EXTRA)
            if (tmpLocation != null) {
                viewModel.persistLocation(tmpLocation)
            }
        }
    }
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            gpLocationService = null
            bound = false
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            gpLocationService = (service as GPLocationService.LocalBinder).service
            bound = true
        }
    }

    private val viewModel: LocationViewModel by viewModels {
        InjectionUtils.provideLocationViewModel(requireActivity().application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.main_fragment, container, false)
        connectionSnackbar =
            Snackbar.make(binding.parentLayout, "Network Unavailable.", Snackbar.LENGTH_INDEFINITE)
        connectionSnackbar.setBackgroundTint(resources.getColor(R.color.colorRed))
        subscribeUI()
        checkLocationOn()
        if (!checkPermission()) {
            requestPermission()
        } else {
            /* start location tracking if all permissions satisfied */
            gpLocationService?.startLocationUpdates()
        }

        return binding.root
    }

    private fun subscribeUI() {
        /* observing on location data to update ui accordingly */
        viewModel.locationLiveData.observe(viewLifecycleOwner) { location ->
            binding.location = location
            viewModel.postLocation(location)
            Log.d(LOG_TAG, "Location updated to UI")
        }
        /* observing on connection state to show netweork error while posting */
        viewModel.connectionState.observe(viewLifecycleOwner) { state ->
            if (!state && !connectionSnackbar.isShown) {
                connectionSnackbar.show()
            } else if (state)
                connectionSnackbar.dismiss()
        }
    }

    /* turns on the google play location service */
    private fun checkLocationOn() {
        settingsClient = LocationServices.getSettingsClient(activity as Activity)
        val locationRequest = LocationRequest()
        locationRequest.interval = GPLocationService.UPDATE_INTERVAL
        locationRequest.fastestInterval = GPLocationService.UPDATE_FASTEST_INTERVAL
        locationRequest.priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY

        locationSettingsRequest =
            LocationSettingsRequest.Builder().addLocationRequest(locationRequest).build()
        settingsClient.checkLocationSettings(locationSettingsRequest)
            .addOnSuccessListener { gpLocationService?.startLocationUpdates() }
            .addOnFailureListener { exception ->
                if (exception is ResolvableApiException) {
                    try {
                        exception.startResolutionForResult(
                            activity,
                            REQUEST_CHECK_SETTINGS
                        )
                    } catch (sendEx: SendIntentException) {
                    }
                }
            }
    }

    private fun checkPermission(): Boolean {
        return (ContextCompat.checkSelfPermission(
            (activity as Context), Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED)
    }

    private fun requestPermission() {
        requestPermissions(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            REQUSET_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        Log.d(LOG_TAG, "onRequestPermissionsResult")
        if (requestCode == REQUSET_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(LOG_TAG, "Permission granted.")
                gpLocationService?.startLocationUpdates()
            } else {
                Log.d(LOG_TAG, "Permission denied.")
                Snackbar.make(
                    binding.parentLayout,
                    "Please grant location permission to use the app.",
                    Snackbar.LENGTH_INDEFINITE
                ).setAction("settings") {
                    val intent = Intent()
                    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    val uri = Uri.fromParts(
                        "package",
                        BuildConfig.APPLICATION_ID,
                        null
                    )
                    intent.data = uri
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                }.show()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        Log.d(LOG_TAG, "onResume() called")
        activity?.bindService(
            Intent(
                (activity as Activity),
                GPLocationService::class.java
            ), serviceConnection, Context.BIND_AUTO_CREATE
        )

        LocalBroadcastManager.getInstance(activity as Activity)
            .registerReceiver(locationReceiver, IntentFilter(GPLocationService.PACKAGE_NAME))
    }

    override fun onPause() {
        super.onPause()
        Log.d(LOG_TAG, "onPause() called")
        LocalBroadcastManager.getInstance(activity as Activity).unregisterReceiver(locationReceiver)
    }

    override fun onStop() {
        Log.d(LOG_TAG, "onStop() called")
        if (bound) {
            activity?.unbindService(serviceConnection)
            bound = false
        }
        super.onStop()
    }

}