package com.srjhnd.gplocation.utils

import android.app.Application
import android.content.Context
import com.srjhnd.gplocation.api.ConnectivityInterceptor
import com.srjhnd.gplocation.api.GPLocationAPIService
import com.srjhnd.gplocation.data.LocationRepository
import com.srjhnd.gplocation.viewmodels.LocationViewModelFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


object InjectionUtils {
    private const val BASE_URL = "https://surajhande.pythonanywhere.com"
    fun getLocationRepository(context: Context): LocationRepository {
        val httpLoggingInterceptor = HttpLoggingInterceptor()
        httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(httpLoggingInterceptor)
            .addInterceptor(ConnectivityInterceptor(context))
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create()).build()

        val gpLocationAPIService = retrofit.create(GPLocationAPIService::class.java)
        return LocationRepository.getInstance(context, gpLocationAPIService)
    }

    fun provideLocationViewModel(application: Application): LocationViewModelFactory {
        return LocationViewModelFactory(
            application,
            getLocationRepository(application.applicationContext)
        )
    }
}