package org.neshan.navigation;

import org.neshan.common.model.LatLng;
import org.neshan.data.model.enums.RoutingType;
import org.neshan.data.model.response.AddressDetailResponse;
import org.neshan.data.model.response.RoutingResponse;
import org.neshan.data.network.ApiClient;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class NavigationModel {

    private final ApiClient mApiClient;

    @Inject
    public NavigationModel(ApiClient apiClient) {
        this.mApiClient = apiClient;
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
