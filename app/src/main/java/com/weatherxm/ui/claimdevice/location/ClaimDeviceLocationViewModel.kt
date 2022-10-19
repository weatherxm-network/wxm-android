package com.weatherxm.ui.claimdevice.location

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.mapbox.geojson.Point
import com.mapbox.search.result.SearchSuggestion
import com.weatherxm.R
import com.weatherxm.ui.common.UIError
import com.weatherxm.usecases.ClaimDeviceUseCase
import com.weatherxm.util.ResourcesHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.util.concurrent.TimeUnit

class ClaimDeviceLocationViewModel : ViewModel(), KoinComponent {
    // Current arbitrary values for the viewpager and the map
    companion object {
        const val ZOOM_LEVEL: Double = 15.0
        val REVERSE_GEOCODING_DELAY = TimeUnit.SECONDS.toMillis(1)
    }

    private val usecase: ClaimDeviceUseCase by inject()
    private val resHelper: ResourcesHelper by inject()
    private lateinit var locationClient: FusedLocationProviderClient
    private var reverseGeocodingJob: Job? = null

    private val onDeviceLocation = MutableLiveData<Location>()
    private val onSelectedSearchLocation = MutableLiveData<Location>()
    private val onLocationConfirmed = MutableLiveData(false)
    private val onSearchResults = MutableLiveData<List<SearchSuggestion>>(mutableListOf())
    private val onError = MutableLiveData<UIError>()
    private val onClearSearchBox = MutableLiveData(false)
    private val onReverseGeocodedAddress = MutableLiveData<String?>(null)

    fun onDeviceLocation() = onDeviceLocation
    fun onSearchResults() = onSearchResults
    fun onSelectedSearchLocation() = onSelectedSearchLocation
    fun onLocationConfirmed() = onLocationConfirmed
    fun onError() = onError
    fun onClearSearchBox() = onClearSearchBox
    fun onReverseGeocodedAddress() = onReverseGeocodedAddress

    fun confirmLocation() {
        onLocationConfirmed.postValue(true)
    }

    @Suppress("MagicNumber")
    @RequiresPermission(anyOf = [ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION])
    fun getLocationAndThen(context: Context, onLocation: (location: Location?) -> Unit) {
        locationClient = LocationServices.getFusedLocationProviderClient(context)
        val priority = when (PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.checkSelfPermission(context, ACCESS_FINE_LOCATION) -> {
                Priority.PRIORITY_HIGH_ACCURACY
            }
            ActivityCompat.checkSelfPermission(context, ACCESS_COARSE_LOCATION) -> {
                Priority.PRIORITY_BALANCED_POWER_ACCURACY
            }
            else -> {
                null
            }
        }
        priority?.let {
            locationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location == null) {
                        Timber.d("Current location is null. Requesting fresh location.")
                        locationClient.requestLocationUpdates(
                            LocationRequest.create()
                                .setNumUpdates(1)
                                .setInterval(TimeUnit.SECONDS.toMillis(2))
                                .setFastestInterval(0)
                                .setMaxWaitTime(TimeUnit.SECONDS.toMillis(3))
                                .setPriority(it),
                            object : LocationCallback() {
                                override fun onLocationResult(result: LocationResult) {
                                    result.lastLocation?.let {
                                        onDeviceLocation.postValue(it)
                                    } ?: onLocation.invoke(null)
                                }
                            },
                            Looper.getMainLooper()
                        )
                    } else {
                        Timber.d("Got current location: $location")
                        onDeviceLocation.postValue(location)
                    }
                }
                .addOnFailureListener {
                    Timber.d(it, "Could not get current location.")
                    onLocation.invoke(null)
                }
        }
    }

    fun geocoding(query: String) {
        viewModelScope.launch {
            usecase.getSearchSuggestions(query)
                .tap { suggestions ->
                    onSearchResults.postValue(suggestions)
                }.tapLeft {
                    onError.postValue(
                        UIError(resHelper.getString(R.string.error_search_suggestions))
                    )
                }
        }
    }

    fun getLocationFromSearchSuggestion(suggestion: SearchSuggestion) {
        viewModelScope.launch {
            usecase.getSuggestionLocation(suggestion)
                .tap { location ->
                    onSelectedSearchLocation.postValue(location)
                    onClearSearchBox.postValue(true)
                }
        }
    }

    fun getAddressFromPoint(point: Point) {
        reverseGeocodingJob?.let {
            if (it.isActive) {
                it.cancel("Cancelling running reverse geocoding job.")
            }
        }

        reverseGeocodingJob = viewModelScope.launch {
            delay(REVERSE_GEOCODING_DELAY)
            usecase.getAddressFromPoint(point)
                .tap {
                    onReverseGeocodedAddress.postValue(it)
                }
                .tapLeft {
                    onReverseGeocodedAddress.postValue(null)
                }
        }

        reverseGeocodingJob?.invokeOnCompletion {
            if (it is CancellationException) {
                Timber.d("Cancelled running reverse geocoding job.")
            }
        }
    }
}
