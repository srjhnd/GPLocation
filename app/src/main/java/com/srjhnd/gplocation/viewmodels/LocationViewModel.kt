package com.srjhnd.gplocation.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.srjhnd.gplocation.data.Location
import com.srjhnd.gplocation.data.LocationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LocationViewModel(application: Application, private val locationRepository: LocationRepository) : AndroidViewModel(application) {
    val locationLiveData:LiveData<Location>  = locationRepository.getLocation(application.applicationContext)
    val connectionState: LiveData<Boolean>  = locationRepository.connectionState

    fun persistLocation(location: Location) = CoroutineScope(Dispatchers.IO).launch {
        locationRepository.persistLocation(location)
    }

    fun postLocation(location: Location) = CoroutineScope(Dispatchers.IO).launch {
        locationRepository.postLocation(location)
    }
}