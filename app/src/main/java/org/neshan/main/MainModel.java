package org.neshan.main;

import org.neshan.common.model.LatLng;
import org.neshan.data.model.enums.RoutingType;
import org.neshan.data.model.response.AddressDetailResponse;
import org.neshan.data.model.response.RoutingResponse;
import org.neshan.data.network.ApiClient;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MainModel {

    private final ApiClient mApiClient;

    @Inject
    public MainModel(ApiClient apiClient) {
        this.mApiClient = apiClient;
    }

    /**
     * loads address detail for specific location from api service
     */
    public Single<AddressDetailResponse> getAddress(double latitude, double longitude) {

        return mApiClient.getAddress(latitude, longitude)
                .subscribeOn(Schedulers.io());

    }

    /**
     * loads routes from start point to end point from api service
     */
    public Single<RoutingResponse> getDirection(RoutingType routType, LatLng start, LatLng end, int bearing) {

        String startPoint = start.getLatitude() + "," + start.getLongitude();
        String endPoint = end.getLatitude() + "," + end.getLongitude();

        return mApiClient.getDirection(routType.getValue(), startPoint, endPoint, bearing)
                .subscribeOn(Schedulers.io());

    }

}
