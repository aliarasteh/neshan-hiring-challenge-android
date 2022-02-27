package org.neshan.navigation

import android.animation.ValueAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.carto.graphics.Color
import com.carto.styles.LineStyle
import com.carto.styles.LineStyleBuilder
import com.carto.styles.MarkerStyleBuilder
import com.carto.utils.BitmapUtils
import com.google.android.gms.location.LocationRequest
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import org.neshan.common.model.LatLng
import org.neshan.component.location.BoundLocationManager
import org.neshan.component.location.BoundLocationManager.Companion.REQUEST_CODE_FOREGROUND_PERMISSIONS
import org.neshan.component.location.LocationListener
import org.neshan.component.util.angleWithNorthAxis
import org.neshan.component.util.distanceFrom
import org.neshan.component.util.showError
import org.neshan.component.util.toBitmap
import org.neshan.data.model.error.SimpleError
import org.neshan.mapsdk.model.Marker
import org.neshan.mapsdk.model.Polyline
import org.neshan.navigation.databinding.ActivityNavigationBinding


@AndroidEntryPoint
class NavigationActivity : AppCompatActivity(), LocationListener {

    companion object {
        private const val TAG = "NavigationActivity"
        private const val LOCATION_UPDATE_INTERVAL = 3000L // 3 seconds
        private const val LOCATION_UPDATE_FASTEST_INTERVAL = 1000L // 1 second

        const val EXTRA_START_POINT = "start_point"
        const val EXTRA_END_POINT = "end_point"
    }

    private lateinit var mBinding: ActivityNavigationBinding

    private lateinit var mViewModel: NavigationViewModel

    // handle location updates
    private var mLocationManager: BoundLocationManager? = null

    // a marker for user location to be shown on map
    private var mUserLocationMarker: Marker? = null

    // poly line for showing progress path on map
    private var mProgressPathPolyLine: Polyline? = null

    // animate marker from start point to end point
    private var mMarkerAnimator: ValueAnimator? = null

    // keeps first point of routing points to avoid repetitive path updates
    private var mLastStartingPoint: LatLng? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBinding = ActivityNavigationBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mViewModel = ViewModelProvider(this)[NavigationViewModel::class.java]
        mBinding.vm = mViewModel

        initMap()

        setViewListeners()

        // observe ViewModel live data objects changes
        observeViewModelChange(mViewModel)

        setUpLocationManager()

        loadNavigationData()

    }

    override fun onLastLocation(location: Location) {
        onLocationChange(location)
    }

    /**
     * handle location change
     * */
    override fun onLocationChange(location: Location) {
        mViewModel.updateUserLocation(location)
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
                    mLocationManager?.startLocationUpdates()
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

    private fun initMap() {
        // change camera angle
        mBinding.mapview.setTilt(40f, 0.25f)
    }

    private fun setViewListeners() {

        mBinding.stop.setOnClickListener {
            onBackPressed()
        }

    }

    private fun loadNavigationData() {

        val startPoint = intent.getParcelableExtra<org.neshan.data.model.LatLng>(EXTRA_START_POINT)
        val endPoint = intent.getParcelableExtra<org.neshan.data.model.LatLng>(EXTRA_END_POINT)

        if (startPoint != null && endPoint != null) {
            mViewModel.startNavigation(
                LatLng(startPoint.latitude, startPoint.longitude),
                LatLng(endPoint.latitude, endPoint.longitude)
            )
        } else {
            showError(mBinding.root, SimpleError(getString(R.string.navigation_failure)))
            finish()
        }

    }

    private fun observeViewModelChange(viewModel: NavigationViewModel) {

        viewModel.progressPoints.observe(this) { progressPoints ->
            updatePathOnMap(progressPoints)
        }

    }

    /**
     * creates a PolyLine by routing points for showing path on map
     * */
    private fun updatePathOnMap(routePoints: ArrayList<LatLng>) {

        if (routePoints.size >= 2) {

            val markerStartPoint = routePoints[0]
            val markerEndPoint = routePoints[1]

            // check if route is updated
            if (mLastStartingPoint?.latitude != markerStartPoint.latitude
                && mLastStartingPoint?.longitude != markerStartPoint.longitude
            ) {
                mLastStartingPoint = markerStartPoint

                // create new poly line by routing points and update path on map
                if (mProgressPathPolyLine != null) {
                    mBinding.mapview.removePolyline(mProgressPathPolyLine)
                }
                mProgressPathPolyLine =
                    Polyline(routePoints, getLineStyle(R.color.colorPrimaryDim75))
                mBinding.mapview.addPolyline(mProgressPathPolyLine)

                // calculate first route angle with north axis
                // and set camera rotation to always show upward
                val bearingEndPoint = routePoints.getOrNull(2) ?: markerEndPoint
                val angle = angleWithNorthAxis(markerStartPoint, bearingEndPoint)
                mBinding.mapview.setBearing((angle).toFloat(), 0.7f)

                // start animating marker from first route point to second route point
                animateMarker(markerStartPoint, markerEndPoint)

                focusOnLocation(markerStartPoint)
            }

        }

    }

    /**
     * creates a marker and animates it from start point to end point
     * */
    private fun animateMarker(start: LatLng, end: LatLng) {

        if (mMarkerAnimator != null) {
            mMarkerAnimator!!.cancel()
        }

        // animate marker from start point to end point in calculated duration (animationDuration)
        val distance = start.distanceFrom(end).getOrNull(0) ?: 1f
        val animationDuration = distance * mViewModel.getAverageSpeedRatio()
        mMarkerAnimator = ValueAnimator.ofInt(0, 100)
        mMarkerAnimator!!.duration = animationDuration.toLong()
        mMarkerAnimator!!.addUpdateListener(object : ValueAnimator.AnimatorUpdateListener {
            var lastValue = 0
            override fun onAnimationUpdate(animation: ValueAnimator) {
                val percentageValue = (animation.animatedValue as Int)
                if (percentageValue != lastValue) {
                    lastValue = percentageValue
                    val latitude =
                        start.latitude + ((end.latitude - start.latitude) * percentageValue / 100)
                    val longitude =
                        start.longitude + ((end.longitude - start.longitude) * percentageValue / 100)

                    if (mUserLocationMarker != null) {
                        mBinding.mapview.removeMarker(mUserLocationMarker)
                    }

                    mUserLocationMarker = createMarker(LatLng(latitude, longitude))

                    mBinding.mapview.addMarker(mUserLocationMarker)
                }
            }

        })

        mMarkerAnimator!!.start()
    }

    private fun getLineStyle(colorResource: Int): LineStyle? {

        val lineStCr = LineStyleBuilder().apply {
            color = Color(ContextCompat.getColor(this@NavigationActivity, colorResource))
            width = 10f
            stretchFactor = 0f
        }

        return lineStCr.buildStyle()

    }

    /**
     * config and start location manager to track user location
     * */
    private fun setUpLocationManager() {

        val locationRequest = LocationRequest.create().apply {
            interval = LOCATION_UPDATE_INTERVAL
            fastestInterval = LOCATION_UPDATE_FASTEST_INTERVAL
            maxWaitTime = 1
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        mLocationManager = BoundLocationManager(this, locationRequest, this)
        mLocationManager?.startLocationUpdates()

    }

    private fun createMarker(latLng: LatLng): Marker {

        val markStCr = MarkerStyleBuilder()

        markStCr.size = 30f

        val drawable = ContextCompat.getDrawable(this, R.drawable.ic_marker)
        if (drawable != null) {
            val markerBitmap = BitmapUtils.createBitmapFromAndroidBitmap(drawable.toBitmap())
            markStCr.bitmap = markerBitmap
        }

        return Marker(latLng, markStCr.buildStyle())

    }

    private fun focusOnLocation(loc: LatLng) {

        mBinding.mapview.moveCamera(loc, 0.25f)
        mBinding.mapview.setZoom(17f, 0.25f)

    }

}

