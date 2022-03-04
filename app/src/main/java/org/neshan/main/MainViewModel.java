package org.neshan.main;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.neshan.R;
import org.neshan.common.model.LatLng;
import org.neshan.common.utils.PolylineEncoding;
import org.neshan.component.util.FunctionExtensionKt;
import org.neshan.data.network.Result;
import org.neshan.data.model.enums.RoutingType;
import org.neshan.data.model.error.GeneralError;
import org.neshan.data.model.error.SimpleError;
import org.neshan.data.model.response.AddressDetailResponse;
import org.neshan.data.model.response.Route;
import org.neshan.data.model.response.RoutingResponse;
import org.neshan.data.model.response.Step;
import org.neshan.data.util.Event;

import java.util.ArrayList;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;

@HiltViewModel
public class MainViewModel extends AndroidViewModel {

    private final MainModel mModel;

    private final CompositeDisposable mCompositeDisposable;

    // used for posting possible errors to view
    private final MutableLiveData<Event<GeneralError>> mGeneralError;

    // address detail for selected location
    private final MutableLiveData<Result<AddressDetailResponse>> mLocationAddressDetail;

    // calculated path for selected start and end points
    private final MutableLiveData<RoutingResponse> mRoutingDetail;

    // points for showing direction path on map
    private final MutableLiveData<ArrayList<LatLng>> mRoutePoints;

    // navigation start point
    private LatLng mStartPoint = null;
    // navigation end point
    private LatLng mEndPoint = null;

    @Inject
    public MainViewModel(@NonNull Application application, MainModel model) {
        super(application);

        mModel = model;
        mCompositeDisposable = new CompositeDisposable();
        mGeneralError = new MutableLiveData<>();
        mLocationAddressDetail = new MutableLiveData<>();
        mRoutingDetail = new MutableLiveData<>();
        mRoutePoints = new MutableLiveData<>();

    }

    public LiveData<Event<GeneralError>> getGeneralErrorLiveData() {
        return mGeneralError;
    }

    public LiveData<Result<AddressDetailResponse>> getLocationAddressDetailLiveData() {
        return mLocationAddressDetail;
    }

    public LiveData<RoutingResponse> getRoutingDetailLiveData() {
        return mRoutingDetail;
    }

    public LiveData<ArrayList<LatLng>> getRoutePoints() {
        return mRoutePoints;
    }

    public LatLng getStartPoint() {
        return mStartPoint;
    }

    public void setStartPoint(LatLng latLng) {
        mStartPoint = latLng;
    }

    public LatLng getEndPoint() {
        return mEndPoint;
    }

    public void setEndPoint(LatLng latLng) {
        mEndPoint = latLng;
    }

    /**
     * try to load address detail from server
     */
    public void loadAddressForLocation(LatLng latLng) {
        mLocationAddressDetail.postValue(Result.Companion.loading());
        mModel.getAddress(latLng.getLatitude(), latLng.getLongitude())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<>() {
                    @Override
                    public void onSubscribe(Disposable disposable) {
                        mCompositeDisposable.add(disposable);
                    }

                    @Override
                    public void onSuccess(AddressDetailResponse response) {

                        if (response.isSuccessFull()) {
                            mLocationAddressDetail.postValue(Result.Companion.success(response));
                        }

                    }

                    @Override
                    public void onError(Throwable e) {
                        mLocationAddressDetail.postValue(Result.Companion.error(e));
                        mGeneralError.postValue(new Event<>(FunctionExtensionKt.getError(e)));
                    }
                });
    }

    /**
     * try to load direction detail from server
     */
    public void loadDirection(RoutingType routingType) {
        if (mStartPoint == null) {
            SimpleError error = new SimpleError(getApplication().getString(R.string.start_point_not_selected));
            mGeneralError.postValue(new Event<>(error));
        } else if (mEndPoint == null) {
            SimpleError error = new SimpleError(getApplication().getString(R.string.end_point_not_selected));
            mGeneralError.postValue(new Event<>(error));
        } else {
            mModel.getDirection(routingType, mStartPoint, mEndPoint, 0)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new SingleObserver<>() {
                        @Override
                        public void onSubscribe(Disposable disposable) {
                            mCompositeDisposable.add(disposable);
                        }

                        @Override
                        public void onSuccess(RoutingResponse response) {
                            if (response.getRoutes() != null) {

                                mRoutingDetail.postValue(response);

                                try {
                                    Route route = response.getRoutes().get(0);

//                                    List<LatLng> routeOverviewPolylinePoints = PolylineEncoding.decode(
//                                            route.getOverviewPolyline().getEncodedPolyline()
//                                    );
//                                    mRoutePoints.postValue(new ArrayList<>(routeOverviewPolylinePoints));

                                    ArrayList<LatLng> decodedStepByStepPath = new ArrayList<>();
                                    for (Step step : route.getLegs().get(0).getSteps()) {
                                        decodedStepByStepPath.addAll(PolylineEncoding.decode(step.getEncodedPolyline()));
                                    }

                                    mRoutePoints.postValue(decodedStepByStepPath);
                                } catch (NullPointerException exception) {
                                    SimpleError error = new SimpleError(getApplication().getString(R.string.routing_failure));
                                    mGeneralError.postValue(new Event<>(error));
                                    exception.printStackTrace();
                                }
                            }
                        }

                        @Override
                        public void onError(Throwable e) {
                            mGeneralError.postValue(new Event<>(FunctionExtensionKt.getError(e)));
                        }
                    });
        }
    }

    @Override
    protected void onCleared() {

        // disposes any incomplete request to avoid possible error also unnecessary network usage
        if (!mCompositeDisposable.isDisposed()) {
            mCompositeDisposable.dispose();
        }

        super.onCleared();

    }

}
