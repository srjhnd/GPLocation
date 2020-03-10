package com.srjhnd.gplocation.data

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.srjhnd.gplocation.api.GPLocationAPIService
import com.srjhnd.gplocation.api.NoConnectivityException
import com.srjhnd.gplocation.utils.SharedPrefsUtils
import okhttp3.ResponseBody
import java.lang.Exception

/* Singleton repository */
class LocationRepository private constructor(
    private val applicationContext: Context,
    private val gpLocationAPIService: GPLocationAPIService
) {

    private val locationLiveData: MutableLiveData<Location> = MutableLiveData()
    val connectionState: MutableLiveData<Boolean> = MutableLiveData(true)

    fun getLocation(applicationContext: Context): LiveData<Location> {
        val location = SharedPrefsUtils.getSavedLocation(applicationContext)
        if (location != null)
            locationLiveData.postValue(location)
        return locationLiveData
    }


    companion object {
        private const val LOG_TAG = "LocationRepository"
        private var instance: LocationRepository? = null
        fun getInstance(
            applicationContext: Context,
            gpLocationAPIService: GPLocationAPIService
        ): LocationRepository {
            return instance ?: synchronized(this) {
                instance ?: LocationRepository(applicationContext, gpLocationAPIService).also { instance = it }
            }
        }
    }

    /* persistence layer using SharedPreference since an in house database layer would be an
       overkill for storing a single value.
     */
    fun persistLocation(location: Location) {
        locationLiveData.postValue(location)
        SharedPrefsUtils.putSignifiedLocation(applicationContext, location)
    }

    /* posting location using Location API service */
    suspend fun postLocation(location: Location) {
        var responseBody = null
        try {
            val responseBody = gpLocationAPIService.putLocation(location)
            connectionState.postValue(true)
            Log.d(LOG_TAG, "Location posted successfully. ${responseBody.string()}")
        } catch (e: NoConnectivityException) {
            connectionState.postValue(false)
            Log.d(LOG_TAG, "Network not available.")
        } catch (e: Exception) {
            Log.d(LOG_TAG, "Something went wrong updating location to server.")
        }

    }

}