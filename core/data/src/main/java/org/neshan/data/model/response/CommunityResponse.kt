package org.neshan.data.model.response

import com.google.gson.annotations.SerializedName

/**
 * this class is used for parsing community list response
 * */
data class CommunityResponse(
    @SerializedName("response")
    val communityList: List<Community>,
    val errorCode: Int?,
    val type: String?
)

data class Community(
    val topic: String,
    val firstName: String,
    val pictureUrl: String,
    val natives: List<String>?,
    val learns: List<String>?,
    val referenceCnt: Int,
)