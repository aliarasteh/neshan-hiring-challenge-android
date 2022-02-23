package org.neshan.choose_location

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.carto.styles.MarkerStyleBuilder
import com.carto.utils.BitmapUtils
import com.google.android.gms.location.LocationRequest
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import org.neshan.choose_location.databinding.ActivityChooseLocationBinding
import org.neshan.common.model.LatLng
import org.neshan.component.location.ForegroundLocationService
import org.neshan.mapsdk.model.Marker
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class ChooseLocationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChooseLocationBinding

    private var marker: Marker? = null

    private var userLocation: Location? = null

    private var foregroundLocationServiceBound = false

    // Provides location updates for while-in-use feature.
    private var foregroundLocationService: ForegroundLocationService? = null

    // Listens for location broadcasts from ForegroundLocationService.
    private val foregroundBroadcastReceiver: ForegroundBroadcastReceiver by lazy {
        ForegroundBroadcastReceiver()
    }

    // Monitors connection to the while-in-use service.
    private val foregroundServiceConnection: ServiceConnection by lazy {
        getServiceConnection()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChooseLocationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setUpViewListeners()
    }

    override fun onStart() {
        super.onStart()
        binding.mapview.setZoom(14f, 0f)

        val serviceIntent = Intent(this, ForegroundLocationService::class.java).apply {
            putExtra("test", 15010)
        }
        bindService(serviceIntent, foregroundServiceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(this).registerReceiver(
            foregroundBroadcastReceiver,
            IntentFilter(ForegroundLocationService.ACTION_FOREGROUND_LOCATION_BROADCAST)
        )
    }

    override fun onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(foregroundBroadcastReceiver)
        super.onPause()
    }

    override fun onStop() {
        if (foregroundLocationServiceBound) {
            unbindService(foregroundServiceConnection)
//            foregroundLocationServiceBound = false
        }

        foregroundLocationService?.unsubscribeToLocationUpdates()

        super.onStop()
    }

    private fun setUpViewListeners() {
        binding.back.setOnClickListener {
            onBackPressed()
        }

        binding.location.setOnClickListener {
            if (userLocation != null) {
                focusOnLocation(LatLng(userLocation!!.latitude, userLocation!!.longitude))
            } else {
                subscribeToLocationUpdates()
            }
        }

        binding.confirm.setOnClickListener {
            chooseSelectedPosition()
        }
    }

    private fun getServiceConnection(): ServiceConnection {
        return object : ServiceConnection {

            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                val binder = service as ForegroundLocationService.LocalBinder
                foregroundLocationService = binder.service
                foregroundLocationServiceBound = true

                subscribeToLocationUpdates()
            }

            override fun onServiceDisconnected(name: ComponentName) {
                foregroundLocationService = null
                foregroundLocationServiceBound = false
            }
        }
    }

    private fun subscribeToLocationUpdates() {
        if (foregroundPermissionApproved()) {
            foregroundLocationService?.subscribeToLocationUpdates(LocationRequest.create().apply {
                interval = TimeUnit.SECONDS.toMillis(3)
                fastestInterval = TimeUnit.SECONDS.toMillis(3)
                maxWaitTime = TimeUnit.SECONDS.toMillis(1)
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            })
        } else {
            requestForegroundPermissions()
        }
    }

    private fun foregroundPermissionApproved(): Boolean {
        return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    private fun requestForegroundPermissions() {
        val provideRationale = foregroundPermissionApproved()

        // If the user denied a previous request, but didn't check "Don't ask again", provide
        // additional rationale.
        if (provideRationale) {
            Snackbar.make(
                binding.root,
                R.string.permission_rationale,
                Snackbar.LENGTH_LONG
            ).setAction(R.string.ok) {
                // Request permission
                ActivityCompat.requestPermissions(
                    this@ChooseLocationActivity,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_FOREGROUND_PERMISSIONS_REQUEST_CODE
                )
            }.show()
        } else {
            Log.d(TAG, "Request foreground permission")
            ActivityCompat.requestPermissions(
                this@ChooseLocationActivity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_FOREGROUND_PERMISSIONS_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d(TAG, "onRequestPermissionResult")

        when (requestCode) {
            REQUEST_FOREGROUND_PERMISSIONS_REQUEST_CODE -> when {
                grantResults.isEmpty() ->
                    // If user interaction was interrupted, the permission request
                    // is cancelled and you receive empty arrays.
                    Log.d(TAG, "User interaction was cancelled.")
                grantResults[0] == PackageManager.PERMISSION_GRANTED ->
                    // Permission was granted.
                    subscribeToLocationUpdates()
                else -> {
                    // Permission denied.

                    Snackbar.make(
                        binding.root,
                        R.string.permission_denied_explanation,
                        Snackbar.LENGTH_LONG
                    )
                        .setAction(R.string.settings) {
                            // Build intent that displays the App settings screen.
                            val intent = Intent()
                            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            val uri = Uri.fromParts(
                                "package",
                                application.packageName,
                                null
                            )
                            intent.data = uri
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                        }.show()
                }
            }
        }
    }

    // handle location change
    private fun onLocationChange(location: Location) {
        val latLng = LatLng(location.latitude, location.longitude)
        addUserMarker(latLng)
        if (userLocation == null) focusOnLocation(latLng)
        userLocation = location
    }

    private fun addUserMarker(loc: LatLng) {
        //remove existing marker from map
        if (marker != null) {
            binding.mapview.removeMarker(marker)
        }
        // Creating marker style. We should use an object of type MarkerStyleCreator, set all features on it
        // and then call buildStyle method on it. This method returns an object of type MarkerStyle
        val markStCr = MarkerStyleBuilder()
        markStCr.size = 30f
        markStCr.bitmap = BitmapUtils.createBitmapFromAndroidBitmap(
            BitmapFactory.decodeResource(
                resources, R.drawable.ic_marker
            )
        )
        val markSt = markStCr.buildStyle()

        // Creating user marker
        marker = Marker(loc, markSt)

        // Adding user marker to map!
        binding.mapview.addMarker(marker)
    }

    private fun focusOnLocation(loc: LatLng) {
        binding.mapview.moveCamera(loc, 0.25f)
        binding.mapview.setZoom(15f, 0.25f)
    }

    private fun chooseSelectedPosition() {
        val latLng = binding.mapview.cameraTargetPosition
        setResult(RESULT_OK, Intent().apply {
            putExtra(KEY_LATITUDE, latLng.latitude)
            putExtra(KEY_LONGITUDE, latLng.longitude)
        })
        onBackPressed()
    }

    companion object {
        private const val TAG = "ChooseLocationActivity"
        private const val REQUEST_FOREGROUND_PERMISSIONS_REQUEST_CODE = 1001

        const val KEY_LATITUDE = "latitude"
        const val KEY_LONGITUDE = "longitude"
    }

    /**
     * Receiver for location broadcasts from [ForegroundLocationService].
     */
    private inner class ForegroundBroadcastReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val location = intent.getParcelableExtra<Location>(
                ForegroundLocationService.EXTRA_LOCATION
            )
            location?.let {
                onLocationChange(location)
            }
        }
    }
}