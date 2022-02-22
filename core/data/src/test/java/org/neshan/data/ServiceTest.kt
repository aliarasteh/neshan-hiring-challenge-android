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

import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ServiceTest {

    private lateinit var mockWebServer: MockWebServer

    @Before
    fun setUpApiClient() {
        mockWebServer = MockWebServer()
    }

    @After
    fun stopService() {
        mockWebServer.shutdown()
    }

//    val dispatcher: Dispatcher = object : Dispatcher() {
//         @Throws(InterruptedException::class)
//         override fun dispatch(request: RecordedRequest): MockResponse {
//             when (request.path) {
//                 "common/mobile-version" -> return MockResponse().setResponseCode(200)
//                 "v1/check/version/" -> return MockResponse().setResponseCode(200)
//                     .setBody("version=9")
//                 "/v1/profile/info" -> return MockResponse().setResponseCode(200)
//                     .setBody("{\\\"info\\\":{\\\"name\":\"Lucas Albuquerque\",\"age\":\"21\",\"gender\":\"male\"}}")
//             }
//             return MockResponse().setResponseCode(404)
//         }
//     }
//
//     @Test
//     fun testSuccessfulResponse() {
//         karestoonMockWebServer.setDispatcher(object : Dispatcher() {
//             override fun dispatch(request: RecordedRequest): MockResponse {
//                 return MockResponse()
//                     .setResponseCode(200)
//                     .setBody(Util.readTestResourceFile("check_update.json"))
//             }
//         })
//     }


}
