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
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import org.neshan.choose_location.databinding.ActivityChooseLocationBinding
import org.neshan.common.model.LatLng
import org.neshan.component.location.ForegroundLocationService
import org.neshan.mapsdk.model.Marker
import java.util.concurrent.TimeUnit

class ChooseLocationActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "ChooseLocationActivity"
        private const val REQUEST_CODE_LOCATION_SETTING = 1001
        private const val REQUEST_CODE_FOREGROUND_PERMISSIONS = 1002

        const val EXTRA_LATITUDE = "latitude"
        const val EXTRA_LONGITUDE = "longitude"
    }

    private lateinit var mBinding: ActivityChooseLocationBinding

    private var mLocationMarker: Marker? = null

    private var mUserLocation: Location? = null

    private var mForegroundLocationServiceBound = false

    // Provides location updates for while-in-use feature.
    private var mForegroundLocationService: ForegroundLocationService? = null

    // Listens for location broadcasts from ForegroundLocationService.
    private val mForegroundBroadcastReceiver: ForegroundBroadcastReceiver by lazy {
        ForegroundBroadcastReceiver()
    }

    // Monitors connection to the while-in-use service.
    private val mForegroundServiceConnection: ServiceConnection by lazy {
        getServiceConnection()
    }

    private val mLocationSettingTask: Task<LocationSettingsResponse> by lazy {
        getLocationSetting()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBinding = ActivityChooseLocationBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        setViewListeners()

    }

    override fun onStart() {
        super.onStart()

        mBinding.mapview.setZoom(14f, 0f)

        val serviceIntent = Intent(this, ForegroundLocationService::class.java)
        bindService(serviceIntent, mForegroundServiceConnection, Context.BIND_AUTO_CREATE)

    }

    override fun onResume() {
        super.onResume()

        LocalBroadcastManager.getInstance(this).registerReceiver(
            mForegroundBroadcastReceiver,
            IntentFilter(ForegroundLocationService.ACTION_FOREGROUND_LOCATION_BROADCAST)
        )

    }

    override fun onPause() {

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mForegroundBroadcastReceiver)

        super.onPause()
    }

    override fun onStop() {

        if (mForegroundLocationServiceBound) {
            unbindService(mForegroundServiceConnection)
//            foregroundLocationServiceBound = false
        }

        mForegroundLocationService?.unsubscribeToLocationUpdates()

        super.onStop()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        Log.d(TAG, "onRequestPermissionResult")

        when (requestCode) {
            REQUEST_CODE_FOREGROUND_PERMISSIONS -> when {
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
                        mBinding.root,
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

    private fun setViewListeners() {

        mBinding.back.setOnClickListener {
            onBackPressed()
        }

        mBinding.location.setOnClickListener {
            if (mUserLocation != null) {
                focusOnLocation(LatLng(mUserLocation!!.latitude, mUserLocation!!.longitude))
            } else {
                subscribeToLocationUpdates()
            }
        }

        mBinding.confirm.setOnClickListener {
            chooseSelectedPosition()
        }

    }

    private fun getServiceConnection(): ServiceConnection {

        return object : ServiceConnection {

            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                val binder = service as ForegroundLocationService.LocalBinder
                mForegroundLocationService = binder.service
                val locationRequest = LocationRequest.create().apply {
                    interval = TimeUnit.SECONDS.toMillis(3)
                    fastestInterval = TimeUnit.SECONDS.toMillis(1)
                    maxWaitTime = TimeUnit.SECONDS.toMillis(1)
                    priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                }
                mForegroundLocationService?.setLocationRequest(locationRequest)
                mForegroundLocationServiceBound = true

                subscribeToLocationUpdates()
            }

            override fun onServiceDisconnected(name: ComponentName) {
                mForegroundLocationService = null
                mForegroundLocationServiceBound = false
            }
        }

    }

    private fun getLocationSetting(): Task<LocationSettingsResponse> {

        val builder = LocationSettingsRequest.Builder()
        mForegroundLocationService?.getLocationRequest()?.let {
            builder.addLocationRequest(it)
        }

        return LocationServices.getSettingsClient(this).checkLocationSettings(builder.build())

    }

    private fun subscribeToLocationUpdates() {

        if (foregroundPermissionApproved()) {
            mForegroundLocationService?.subscribeToLocationUpdates()

            checkLocationAvailability()
        } else {
            requestForegroundPermissions()
        }

    }

    private fun checkLocationAvailability() {

        mLocationSettingTask.addOnSuccessListener {
            Log.d(TAG, "All location settings are satisfied")
        }.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                Log.e(TAG, "Location settings are not satisfied")
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    exception.startResolutionForResult(
                        this@ChooseLocationActivity,
                        REQUEST_CODE_LOCATION_SETTING
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
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
                mBinding.root,
                R.string.permission_rationale,
                Snackbar.LENGTH_LONG
            ).setAction(R.string.ok) {
                // Request permission
                ActivityCompat.requestPermissions(
                    this@ChooseLocationActivity,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_CODE_FOREGROUND_PERMISSIONS
                )
            }.show()
        } else {
            Log.d(TAG, "Request foreground permission")
            ActivityCompat.requestPermissions(
                this@ChooseLocationActivity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_CODE_FOREGROUND_PERMISSIONS
            )
        }

    }

    // handle location change
    private fun onLocationChange(location: Location) {

        val latLng = LatLng(location.latitude, location.longitude)

        addUserMarker(latLng)

        if (mUserLocation == null) {
            focusOnLocation(latLng)
        }

        mUserLocation = location

    }

    private fun addUserMarker(loc: LatLng) {
        //remove existing marker from map
        if (mLocationMarker != null) {
            mBinding.mapview.removeMarker(mLocationMarker)
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
        mLocationMarker = Marker(loc, markSt)

        // Adding user marker to map!
        mBinding.mapview.addMarker(mLocationMarker)

    }

    private fun focusOnLocation(loc: LatLng) {

        mBinding.mapview.moveCamera(loc, 0.25f)
        mBinding.mapview.setZoom(15f, 0.25f)

    }

    private fun chooseSelectedPosition() {

        val latLng = mBinding.mapview.cameraTargetPosition
        setResult(RESULT_OK, Intent().apply {
            putExtra(EXTRA_LATITUDE, latLng.latitude)
            putExtra(EXTRA_LONGITUDE, latLng.longitude)
        })

        onBackPressed()

    }

    /**
     * Receiver for location broadcasts from [ForegroundLocationService].
     */
    private inner class ForegroundBroadcastReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {

            val location =
                intent.getParcelableExtra<Location>(ForegroundLocationService.EXTRA_LOCATION)
            location?.let { onLocationChange(location) }

        }
    }
}