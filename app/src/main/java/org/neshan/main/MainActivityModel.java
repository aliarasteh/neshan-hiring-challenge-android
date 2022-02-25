package org.neshan.main;

import org.neshan.data.model.response.AddressDetailResponse;
import org.neshan.data.network.ApiClient;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MainActivityModel {

    private final ApiClient mApiClient;

    @Inject
    public MainActivityModel(ApiClient apiClient) {
        this.mApiClient = apiClient;
    }

    // loads address detail for specific location from neshan service
    public Single<AddressDetailResponse> getAddress(double latitude, double longitude) {

        return mApiClient.getAddress(latitude, longitude)
                .subscribeOn(Schedulers.io());

    }

}
