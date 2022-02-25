package org.neshan.data.network

import io.reactivex.rxjava3.core.Single
import org.neshan.data.model.response.AddressDetailResponse
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

/**
 * The API interface for all required server apis
 */
interface ApiClient {

    /**
     * loads address detail for specific location by latitude and longitude
     * @param lat: latitude for desired location
     * @param lng: longitude for desired location
     */
    @GET("v4/reverse")
    fun getAddress(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double
    ): Single<AddressDetailResponse>

}