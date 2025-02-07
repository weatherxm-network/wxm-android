package com.weatherxm.util

import com.google.gson.Gson
import com.mapbox.api.staticmap.v1.MapboxStaticMap
import com.mapbox.api.staticmap.v1.StaticMapCriteria
import com.mapbox.api.staticmap.v1.models.StaticMarkerAnnotation
import com.mapbox.api.staticmap.v1.models.StaticPolylineAnnotation
import com.mapbox.geojson.Point
import com.mapbox.geojson.utils.PolylineUtils
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.PolygonAnnotation
import com.mapbox.maps.plugin.annotation.generated.PolygonAnnotationOptions
import com.mapbox.search.result.SearchSuggestion
import com.weatherxm.R
import com.weatherxm.data.models.Hex
import com.weatherxm.data.models.Location
import com.weatherxm.data.models.PublicHex
import com.weatherxm.ui.explorer.ExplorerViewModel.Companion.FILL_OPACITY_HEXAGONS
import com.weatherxm.ui.explorer.UICell
import com.weatherxm.ui.explorer.UICellJsonAdapter
import okhttp3.HttpUrl
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object MapboxUtils : KoinComponent {
    private val adapter: UICellJsonAdapter by inject()
    private val resources: Resources by inject()
    private val gson: Gson by inject()

    fun getCustomData(polygonAnnotation: PolygonAnnotation): UICell? {
        val data = polygonAnnotation.getJsonObjectCopy().getAsJsonObject("custom_data")
        return adapter.fromJson(data.toString())
    }

    fun parseSearchSuggestion(searchSuggestion: SearchSuggestion): String {
        var parsedAddress = searchSuggestion.name
        val searchAddress = searchSuggestion.address
        searchAddress?.street?.let {
            parsedAddress += ", $it"
        }
        searchAddress?.place?.let {
            parsedAddress += ", $it"
        }
        searchAddress?.region?.let {
            parsedAddress += ", $it"
        }
        searchAddress?.country?.let {
            parsedAddress += ", $it"
        }
        searchAddress?.postcode?.let {
            parsedAddress += ", $it"
        }
        return parsedAddress
    }

    @Suppress("MagicNumber")
    fun getMinimap(width: Int, userLocation: Location?, hex: Hex?): HttpUrl? {
        return hex?.let {
            val hexPoints = hex.polygon.map {
                Point.fromLngLat(it.lon, it.lat)
            }
            val staticHex = StaticPolylineAnnotation.builder()
                .polyline(PolylineUtils.encode(hexPoints, 5))
                .fillColor("3388ff")
                .fillOpacity(FILL_OPACITY_HEXAGONS.toFloat())
                .strokeColor("FFFFFF")
                .strokeWidth(0.0)
                .build()

            with(MapboxStaticMap.builder()) {
                accessToken(resources.getString(R.string.mapbox_access_token))
                styleId(StaticMapCriteria.DARK_STYLE)
                cameraPoint(Point.fromLngLat(hex.center.lon, hex.center.lat))
                cameraZoom(11.0)
                width(width)
                height(200)
                staticPolylineAnnotations(listOf(staticHex))
                retina(true)

                userLocation?.let {
                    val marker = StaticMarkerAnnotation.builder()
                        .color("0A3FAD")
                        .lnglat(Point.fromLngLat(userLocation.lon, userLocation.lat))
                        .build()
                    staticMarkerAnnotations(listOf(marker))
                }
                build()
            }.url()
        }
    }

    fun List<PublicHex>.toPolygonAnnotationOptions(): List<PolygonAnnotationOptions> {
        return map {
            PolygonAnnotationOptions()
                .withFillColor(resources.getColor(R.color.hex_fill_color))
                .withFillOpacity(FILL_OPACITY_HEXAGONS)
                .withFillOutlineColor(resources.getColor(R.color.white))
                .withData(gson.toJsonTree(UICell(it.index, it.center)))
                .withPoints(polygonPointsToLatLng(it.polygon))
        }
    }

    fun List<PublicHex>.toPointAnnotationOptions(): List<PointAnnotationOptions> {
        return map {
            PointAnnotationOptions()
                .withPoint(Point.fromLngLat(it.center.lon, it.center.lat))
                .withTextField(it.deviceCount?.toString() ?: "")
                .withTextColor(resources.getColor(R.color.dark_text))
        }
    }

    fun polygonPointsToLatLng(pointsOfPolygon: List<Location>): List<MutableList<Point>> {
        val latLongs = listOf(pointsOfPolygon.map { coordinates ->
            Point.fromLngLat(coordinates.lon, coordinates.lat)
        }.toMutableList())

        // Custom/Temporary fix for: https://github.com/mapbox/mapbox-maps-android/issues/733
        latLongs.map { coordinates ->
            coordinates.add(coordinates[0])
        }
        return latLongs
    }
}
