package org.neshan.data.network

import org.neshan.data.model.response.CommunityResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * The API interface for all required server apis
 */
interface ApiClient {

    /**
     * loads community items by page.
     * There are 20 community members per page.The last page has less than 20 members
     * @param page: requested page index
     */
    @GET("community_{page}.json")
    suspend fun getCommunityList(@Path("page") page: Int): Response<CommunityResponse>

}