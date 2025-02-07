package com.weatherxm.ui.explorer

import android.annotation.SuppressLint
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
import com.mapbox.maps.extension.style.layers.generated.HeatmapLayer
import com.mapbox.maps.extension.style.layers.generated.heatmapLayer
import com.mapbox.maps.plugin.annotation.generated.PolygonAnnotationOptions
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.models.Location
import com.weatherxm.data.models.PublicHex
import com.weatherxm.ui.common.Resource
import com.weatherxm.ui.common.SingleLiveEvent
import com.weatherxm.ui.components.BaseMapFragment.Companion.DEFAULT_ZOOM_LEVEL
import com.weatherxm.ui.components.BaseMapFragment.Companion.ZOOMED_IN_ZOOM_LEVEL
import com.weatherxm.usecases.ExplorerUseCase
import com.weatherxm.util.Failure.getDefaultMessage
import com.weatherxm.util.LocationHelper
import com.weatherxm.util.MapboxUtils.toPointAnnotationOptions
import com.weatherxm.util.MapboxUtils.toPolygonAnnotationOptions
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

@Suppress("TooManyFunctions")
class ExplorerViewModel(
    private val explorerUseCase: ExplorerUseCase,
    private val analytics: AnalyticsWrapper,
    private val locationHelper: LocationHelper,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {
    companion object {
        const val FILL_OPACITY_HEXAGONS: Double = 0.5
        const val HEATMAP_SOURCE_ID = "heatmap"
        const val HEATMAP_LAYER_ID = "heatmap-layer"
        const val HEATMAP_LAYER_SOURCE = "heatmap"
        const val HEATMAP_WEIGHT_KEY = ExplorerUseCase.DEVICE_COUNT_KEY
    }

    // Explorer Data
    private val onExplorerData = MutableLiveData<ExplorerData>()

    // Success/Loading/Error Status
    private val onStatus = MutableLiveData<Resource<Unit>>()

    // New Polygons to be drawn
    private val onNewPolygons = SingleLiveEvent<List<PolygonAnnotationOptions>>()

    /**
     * Needed for passing info to the fragment to handle when a prefilled location is used
     * so we want the camera to move directly to that point
     * or
     * when a starting location is used
     * so we want the camera to move directly to that point (usually country level but on low zoom)
     */
    private val onNavigateToLocation = SingleLiveEvent<NavigationLocation>()

    // Needed for passing info to the fragment to handle when my location button is clicked
    private val onMyLocationClicked = SingleLiveEvent<Boolean>()

    // Needed for passing info to the activity to show/hide elements when onMapClick
    private val showMapOverlayViews = MutableLiveData(true)

    // Needed for passing info to the activity to show/hide elements when search view is opened
    private val onSearchOpenStatus = MutableLiveData(false)

    // Save the current explorer camera zoom and center
    private var currentCamera: ExplorerCamera? = null

    // Save the current status of where the explorer is located (before/after login)
    private var explorerAfterLoggedIn = false

    @Suppress("MagicNumber")
    val heatmapLayer: HeatmapLayer by lazy {
        heatmapLayer(
            HEATMAP_LAYER_ID,
            HEATMAP_SOURCE_ID
        ) {
            maxZoom(10.0) // Hide layer at zoom level 10
            sourceLayer(HEATMAP_LAYER_SOURCE)
            // Begin color ramp at 0-stop with a 0-transparency color
            // to create a blur-like effect.
            heatmapColor(
                interpolate {
                    linear()
                    heatmapDensity()
                    stop {
                        literal(0)
                        rgba(33.0, 102.0, 172.0, 0.0)
                    }
                    stop {
                        literal(0.2)
                        rgb(103.0, 169.0, 207.0)
                    }
                    stop {
                        literal(0.4)
                        rgb(162.0, 187.0, 201.0)
                    }
                    stop {
                        literal(0.6)
                        rgb(149.0, 153.0, 189.0)
                    }
                    stop {
                        literal(0.8)
                        rgb(103.0, 118.0, 247.0)
                    }
                    stop {
                        literal(1)
                        rgb(0.0, 255.0, 206.0)
                    }
                }
            )
            // Increase the heatmap weight based on the device count
            heatmapWeight(
                interpolate {
                    linear()
                    get { literal(HEATMAP_WEIGHT_KEY) }
                    stop {
                        literal(0)
                        literal(0) // 0 weight at 0 cell device count
                    }
                    stop {
                        literal(100)
                        literal(100) // 100 weight at 100 cell device count
                    }
                }
            )
            // Adjust the heatmap radius by zoom level
            heatmapRadius(
                interpolate {
                    linear()
                    zoom()
                    stop {
                        literal(0.0)
                        literal(2) // 2 radius at zoom level 0.0
                    }
                    stop {
                        literal(9.0)
                        literal(20) // 20 radius at zoom level 9.0
                    }
                }
            )
            // Adjust heatmap opacity based on zoom level, until it totally disappears
            heatmapOpacity(
                interpolate {
                    exponential {
                        literal(0.5) // Exponential interpolation base
                    }
                    zoom()
                    stop {
                        literal(0.0)
                        literal(1.0) // 1.0 opacity at zoom level 0.0
                    }
                    stop {
                        literal(8.0)
                        literal(0.9) // 0.9 opacity at zoom level 8.0
                    }
                    stop {
                        literal(9.0)
                        literal(0.5) // 0.5 opacity at zoom level 9.0
                    }
                    stop {
                        literal(9.5)
                        literal(0.1) // 0.1 opacity at zoom level 9.5
                    }
                    stop {
                        literal(10.0)
                        literal(0.0)  // 0.0 opacity at zoom level 10.0
                    }
                }
            )
        }
    }

    private var useUserLocation: Boolean = true

    // Save currently shown hexes by their indexes in order to send to the UI only the added/removed
    private var hexesIndexes = listOf<String>()

    fun onMyLocationClicked() = onMyLocationClicked
    fun onNavigateToLocation() = onNavigateToLocation
    fun showMapOverlayViews() = showMapOverlayViews
    fun onSearchOpenStatus() = onSearchOpenStatus
    fun onStatus(): LiveData<Resource<Unit>> = onStatus
    fun onNewPolygons(): LiveData<List<PolygonAnnotationOptions>> = onNewPolygons
    fun onExplorerData(): LiveData<ExplorerData> = onExplorerData

    fun navigateToLocation(location: Location, isUserLocation: Boolean = false) {
        Timber.d("Got starting location [${location.lat}, ${location.lon}")
        if (isUserLocation && useUserLocation) {
            onNavigateToLocation.postValue(NavigationLocation(DEFAULT_ZOOM_LEVEL, location))
        } else if (!isUserLocation) {
            useUserLocation = false
            onNavigateToLocation.postValue(NavigationLocation(ZOOMED_IN_ZOOM_LEVEL, location))
        }
    }

    fun setExplorerAfterLoggedIn(isAfterLoggedIn: Boolean) {
        explorerAfterLoggedIn = isAfterLoggedIn
    }

    fun isExplorerAfterLoggedIn(): Boolean {
        return explorerAfterLoggedIn
    }

    fun onMyLocation() {
        onMyLocationClicked.postValue(true)
    }

    fun fetch() {
        onStatus.postValue(Resource.loading())

        viewModelScope.launch(dispatcher) {
            explorerUseCase.getCells().onRight { response ->
                if (hexesIndexes.isEmpty()) {
                    hexesIndexes = response.publicHexes.map {
                        it.index
                    }
                    /**
                     * The explorer map is empty so send all the hexes to be drawn
                     */
                    response.polygonsToDraw = response.publicHexes.toPolygonAnnotationOptions()
                    response.pointsToDraw = response.publicHexes.toPointAnnotationOptions()
                    onExplorerData.postValue(response)
                } else {
                    val newHexes = mutableListOf<PublicHex>()

                    val responseHexesIndexes = response.publicHexes.map {
                        /**
                         * Add only the hexes in the response that are new
                         */
                        if (!hexesIndexes.contains(it.index)) {
                            newHexes.add(it)
                        }
                        it.index
                    }

                    /**
                     * Save the updated hexes indexes to compare with the next response
                     */
                    hexesIndexes = responseHexesIndexes

                    /**
                     * Save all the polygons in ExplorerData as it's used to pre-draw the map
                     * (before loading of new data takes place) when a user visits another
                     * screen and comes back in the explorer
                     */
                    onExplorerData.value?.polygonsToDraw =
                        response.publicHexes.toPolygonAnnotationOptions()

                    /**
                     * Send only the new polygons in the response
                     * in order not to re-draw the whole explorer map but only the new hexes
                     */
                    onNewPolygons.postValue(newHexes.toPolygonAnnotationOptions())
                }
                onStatus.postValue(Resource.success(Unit))
            }.onLeft {
                analytics.trackEventFailure(it.code)
                onStatus.postValue(
                    Resource.error(it.getDefaultMessage(R.string.error_reach_out_short))
                )
            }
        }
    }

    fun onMapClick() {
        val current = showMapOverlayViews.value == true
        showMapOverlayViews.postValue(!current)
    }

    fun onSearchOpenStatus(isOpen: Boolean) {
        onSearchOpenStatus.postValue(isOpen)
    }

    fun setCurrentCamera(zoom: Double, center: Point) {
        currentCamera = ExplorerCamera(zoom, center)
    }

    fun getCurrentCamera(): ExplorerCamera? {
        return currentCamera
    }

    @SuppressLint("MissingPermission")
    fun getLocation(onLocation: (location: Location?) -> Unit) {
        locationHelper.getLocationAndThen(onLocation)
    }

    init {
        if (locationHelper.hasLocationPermissions()) {
            getLocation { location ->
                location?.let {
                    navigateToLocation(it, true)
                }
            }
        } else {
            viewModelScope.launch(dispatcher) {
                explorerUseCase.getUserCountryLocation()?.let {
                    navigateToLocation(it, true)
                }
            }
        }
    }
}
