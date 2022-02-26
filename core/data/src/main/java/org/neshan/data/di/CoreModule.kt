package org.neshan.data.di

import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.neshan.data.network.ApiClient
import org.neshan.data.network.RetrofitConfig
import javax.inject.Singleton

/**
 * provides class instances to be used in other modules
 * */
@Module
@InstallIn(SingletonComponent::class)
object CoreModule {

    @Provides
    @Singleton
    fun provideGson(): Gson {

        return Gson()

    }

    @Provides
    @Singleton
    fun provideApiClient(retrofitConfig: RetrofitConfig): ApiClient {
        retrofitConfig.initialize()

        return retrofitConfig.createService(ApiClient::class.java)

    }

}