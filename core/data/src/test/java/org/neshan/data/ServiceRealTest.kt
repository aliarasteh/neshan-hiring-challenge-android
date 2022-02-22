/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.neshan.data

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.neshan.data.network.ApiClient
import org.neshan.data.network.RetrofitConfig
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.collection.IsCollectionWithSize.hasSize
import org.hamcrest.core.Is.`is`
import org.hamcrest.core.IsNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * this class tests api services by sending real requests to server
 * Note: it's not really common to implement such tests, testing api services
 * must be implemented in server side or using other tools
 * */
@RunWith(JUnit4::class)
class ServiceRealTest {

    private lateinit var apiClient: ApiClient

    @Before
    fun setUpApiClient() {
        val retrofitConfig = RetrofitConfig()
        retrofitConfig.initialize()
        apiClient = retrofitConfig.createService(ApiClient::class.java)
    }

    @Test
    fun testAllFunctionsTested() {
        val apiMethodsName: List<String> = ApiClient::class.java.declaredMethods.map {
            it.name
        }

        val serviceTestMethodsName: List<String> = ServiceRealTest::class.java.declaredMethods.map {
            it.name
        }

        val diff = apiMethodsName.filterNot { serviceTestMethodsName.contains(it) }

        println("Functions that not Tested (count : ${diff.size}) >>> \n" + diff.toString())

        assertThat(diff.isEmpty(), `is`(false))
    }

    @ExperimentalCoroutinesApi
    @Test
    fun getCommunityList_checkResponses() {
        runBlocking {
            for (page in 1..3) {
                val result = apiClient.getCommunityList(page)
                val communityResponse = result.body()

                assertThat(result.isSuccessful, `is`(true))
                assertThat(communityResponse, IsNull.notNullValue())
                assertThat(communityResponse?.communityList, hasSize(20))
            }
        }
    }

    @ExperimentalCoroutinesApi
    @Test
    fun getCommunityList_checkLastPage() {
        runBlocking {
            val result = apiClient.getCommunityList(4)
            val communityResponse = result.body()

            assertThat(result.isSuccessful, `is`(true))
            assertThat(communityResponse, IsNull.notNullValue())
            assertThat(communityResponse?.communityList, hasSize(18))
        }
    }

}
