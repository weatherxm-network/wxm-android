package com.weatherxm.ui.components

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PolygonAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPolygonAnnotationManager
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.scalebar.scalebar
import com.mapbox.maps.viewannotation.ViewAnnotationManager
import com.weatherxm.databinding.FragmentMapBinding
import com.weatherxm.ui.components.BaseMapFragment.OnMapDebugInfoListener
import com.weatherxm.util.DisplayModeHelper
import dev.chrisbanes.insetter.applyInsetter
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.util.concurrent.TimeUnit

open class BaseMapFragment : BaseFragment() {
    companion object {
        const val CURRENT_LAT = "current_lat"
        const val CURRENT_LON = "current_lon"
        const val CURRENT_ZOOM = "current_zoom"
        const val DEFAULT_ZOOM_LEVEL: Double = 2.0
        const val ZOOMED_IN_ZOOM_LEVEL: Double = 11.0
        const val USER_SET_LOCATION_ZOOM_LEVEL: Double = 15.0
        val REVERSE_GEOCODING_DELAY = TimeUnit.SECONDS.toMillis(1)
    }

    fun interface OnMapDebugInfoListener {
        fun onMapDebugInfoUpdated(zoom: Double, center: Point)
    }

    private val displayModeHelper: DisplayModeHelper by inject()

    protected lateinit var binding: FragmentMapBinding
    protected lateinit var polygonManager: PolygonAnnotationManager
    protected lateinit var pointManager: PointAnnotationManager
    protected lateinit var viewManager: ViewAnnotationManager

    private lateinit var debugInfoListener: OnMapDebugInfoListener
    private lateinit var map: MapboxMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        debugInfoListener = try {
            activity as OnMapDebugInfoListener
        } catch (e: ClassCastException) {
            Timber.d(e, "Parent activity does not implement OnMapDebugInfoListener.")
            OnMapDebugInfoListener { _, _ ->  /* NOOP */ }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMapBinding.inflate(inflater, container, false)

        binding.progress.applyInsetter {
            type(statusBars = true) {
                margin(left = false, top = true, right = false, bottom = false)
            }
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        map = getMap()

        viewManager = binding.mapView.viewAnnotationManager

        map.subscribeCameraChanged {
            debugInfoListener.onMapDebugInfoUpdated(
                zoom = map.cameraState.zoom,
                center = map.cameraState.center
            )
        }

        map.loadStyle(getMapStyle()) {
            Timber.d("MapBox is ready and style loaded")

            binding.mapView.gestures.rotateEnabled = false
            binding.mapView.gestures.pitchEnabled = false
            binding.mapView.scalebar.enabled = false

            with(binding.mapView.annotations) {
                polygonManager = this.createPolygonAnnotationManager()
                pointManager = this.createPointAnnotationManager()
                pointManager.textSize = 0.0
            }

            // Update camera
            if (savedInstanceState != null) {
                map.setCamera(
                    CameraOptions.Builder()
                        .zoom(savedInstanceState.getDouble(CURRENT_ZOOM, DEFAULT_ZOOM_LEVEL))
                        .center(
                            Point.fromLngLat(
                                savedInstanceState.getDouble(CURRENT_LON, 0.0),
                                savedInstanceState.getDouble(CURRENT_LAT, 0.0)
                            )
                        )
                        .build()
                )
            } else {
                map.setCamera(
                    CameraOptions.Builder()
                        .zoom(DEFAULT_ZOOM_LEVEL)
                        .build()
                )
            }

            onMapReady(map)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val center = map.cameraState.center
        outState.putDouble(CURRENT_LAT, center.latitude())
        outState.putDouble(CURRENT_LON, center.longitude())
        outState.putDouble(CURRENT_ZOOM, map.cameraState.zoom)
        super.onSaveInstanceState(outState)
    }

    fun getMap(): MapboxMap = binding.mapView.mapboxMap

    fun getMapView(): MapView = binding.mapView

    open fun onMapReady(map: MapboxMap) {
        // Override in subclasses
    }

    open fun getMapStyle(): String {
        return if (displayModeHelper.isDarkModeEnabled()) {
            Style.DARK
        } else {
            Style.MAPBOX_STREETS
        }
    }
}
