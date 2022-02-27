package org.neshan.navigation

import android.animation.ValueAnimator
import android.app.Application
import android.location.Location
import androidx.databinding.ObservableField
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.SingleObserver
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import org.neshan.common.model.LatLng
import org.neshan.common.utils.PolylineEncoding
import org.neshan.component.util.distanceFrom
import org.neshan.data.model.enums.RoutingType
import org.neshan.data.model.response.RoutingResponse
import javax.inject.Inject

@HiltViewModel
class NavigationViewModel @Inject constructor(
    application: Application,
    private val mModel: NavigationModel
) : AndroidViewModel(application) {

    val duration = ObservableField<String>()

    val distance = ObservableField<String>()

    private val mCompositeDisposable by lazy { CompositeDisposable() }

    private var mStartPoint: LatLng? = null

    private var mEndPoint: LatLng? = null

    // remained points for routing
    private val _progressPoints = MutableLiveData<ArrayList<LatLng>>()
    val progressPoints: LiveData<ArrayList<LatLng>> by lazy { _progressPoints }

    private val _markerPosition = MutableLiveData<LatLng>()
    val markerPosition: LiveData<LatLng> by lazy { _markerPosition }

    private var mRoutingPoints: ArrayList<LatLng>? = null

    private var mUserLocation: Location? = null

    private val mSpeedCalculator = SpeedCalculator(1000f)

    private var mLoadingDirection = false

    private var mLastReachedPointIndex = 0

    // keeps first point of routing points to avoid repetitive path updates
    private var mLastStartingPoint: LatLng? = null

    // animate marker position from start point to end point
    private var mMarkerAnimator: ValueAnimator? = null

    fun startNavigation(startPoint: LatLng, endPoint: LatLng) {

        mStartPoint = startPoint
        mEndPoint = endPoint

        loadDirection(mStartPoint!!, mEndPoint!!, RoutingType.CAR, 0)

    }

    override fun onCleared() {

        // disposes any incomplete request to avoid possible error also unnecessary network usage
        if (!mCompositeDisposable.isDisposed) {
            mCompositeDisposable.dispose()
        }

        if (mMarkerAnimator != null && mMarkerAnimator!!.isRunning) {
            mMarkerAnimator!!.cancel()
        }

        super.onCleared()

    }

    /**
     * set user location and start updating movement speed and calculate
     * passed points
     * */
    fun updateUserLocation(location: Location) {

        mUserLocation = location

        mSpeedCalculator.update(LatLng(location.latitude, location.longitude))

        // if loading direction -> avoid updating progress
        if (!mRoutingPoints.isNullOrEmpty() && !mLoadingDirection) {
            calculateUserProgress(mRoutingPoints!!)
        }

    }

    fun getAverageSpeedRatio(): Float {
        return mSpeedCalculator.getAverageSpeedRatio()
    }

    /**
     * try to load direction detail from server
     */
    private fun loadDirection(
        startPoint: LatLng,
        endPoint: LatLng,
        routingType: RoutingType,
        bearing: Int
    ) {
        if (!mLoadingDirection) {
            mLoadingDirection = true
            mModel.getDirection(routingType, startPoint, endPoint, bearing)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : SingleObserver<RoutingResponse> {

                    override fun onSubscribe(disposable: Disposable) {
                        mCompositeDisposable.add(disposable)
                    }

                    override fun onSuccess(response: RoutingResponse) {
                        mLoadingDirection = false

                        if (response.routes != null) {

                            mRoutingPoints = ArrayList()

                            response.routes?.firstOrNull()?.legs?.firstOrNull()?.let { leg ->

                                leg.steps.map { step ->
                                    mRoutingPoints!!.addAll(PolylineEncoding.decode(step.encodedPolyline))

                                    if (mRoutingPoints!!.size >= 2) {
                                        _progressPoints.postValue(mRoutingPoints!!)

                                        _markerPosition.postValue(mRoutingPoints!!.first())
                                    }

                                }

                                distance.set(leg.distance.text)
                                duration.set(leg.duration.text)

                            }
                        }
                    }

                    override fun onError(e: Throwable) {
                        mLoadingDirection = false
                        // TODO: show error to user
                        e.printStackTrace()
                    }

                })
        }
    }

    /**
     * calculate remained routing points
     * */
    private fun calculateUserProgress(points: ArrayList<LatLng>) {

        var index = mLastReachedPointIndex

        // region try finding closest point to user location

        var point = points[index]
        var distanceResult =
            point.distanceFrom(LatLng(mUserLocation!!.latitude, mUserLocation!!.longitude))
        var minDistance = distanceResult[0]

        // user is far from last point
        if (minDistance > 70) {
            index = 0
        }

        while (index < points.size - 1) {
            index++
            point = points[index]
            distanceResult =
                point.distanceFrom(LatLng(mUserLocation!!.latitude, mUserLocation!!.longitude))
            if (distanceResult[0] < minDistance) {
                mLastReachedPointIndex = index
                minDistance = distanceResult[0]
            }
        }

        // endregion

        // if user has gone far from route -> request direction again
        if (minDistance > 70) {

            // try to recalculate path
            val startPoint = LatLng(mUserLocation!!.latitude, mUserLocation!!.longitude)
            loadDirection(startPoint, mEndPoint!!, RoutingType.CAR, mUserLocation!!.bearing.toInt())

        } else {

            // consider all points after closest point as remained routing points
            val remainedPoints = mRoutingPoints!!.subList(mLastReachedPointIndex, points.size)

            if (remainedPoints.size >= 2) {

                val startingPoint = remainedPoints[0]
                // check if start point is new
                if (mLastStartingPoint?.latitude != startingPoint.latitude && mLastStartingPoint?.longitude != startingPoint.longitude
                ) {
                    mLastStartingPoint = startingPoint

                    _progressPoints.postValue(ArrayList(remainedPoints))

                    startMarkerAnimation(startingPoint, remainedPoints[1])
                }
            }

        }

    }

    /**
     * animates marker position from start point to end point
     * */
    private fun startMarkerAnimation(start: LatLng, end: LatLng) {

        if (mMarkerAnimator != null && mMarkerAnimator!!.isRunning) {
            mMarkerAnimator!!.cancel()
        }

        // animate marker from start point to end point in calculated duration (animationDuration)
        val distance = start.distanceFrom(end).getOrNull(0) ?: 1f
        val animationDuration = distance * mSpeedCalculator.getAverageSpeedRatio()
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

                    _markerPosition.postValue(LatLng(latitude, longitude))
                }
            }

        })

        mMarkerAnimator!!.start()

    }

    /**
     * helper class for calculating average speed according to past 5 visited locations
     * */
    inner class SpeedCalculator(defaultSpeed: Float) {
        private var mIndex = 0
        private val mRecords =
            floatArrayOf(defaultSpeed, defaultSpeed, defaultSpeed, defaultSpeed, defaultSpeed)
        private var mLastTime: Long = 0
        private var mLastLocation: LatLng? = null

        fun getAverageSpeedRatio(): Float {
            return mRecords.average().toFloat()
        }

        fun update(latLng: LatLng) {

            if (mLastLocation != null) {
                // calculate time difference with previous update
                val newTime = System.currentTimeMillis()
                val duration = newTime - mLastTime

                // calculate traveled distance from previous update
                mLastLocation?.distanceFrom(latLng)?.getOrNull(0)?.let { distance ->
                    val speed = duration / distance
                    mRecords[mIndex % mRecords.size] = speed
                    mIndex++
                }

            } else {
                mLastLocation = latLng
                mLastTime = System.currentTimeMillis()
            }

        }
    }
}