package com.weatherxm.ui.explorer

import android.os.Parcelable
import androidx.annotation.Keep
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.PolygonAnnotationOptions
import com.squareup.moshi.JsonClass
import com.weatherxm.data.models.Bundle
import com.weatherxm.data.models.Location
import com.weatherxm.data.models.PublicHex
import com.weatherxm.ui.common.BundleName
import com.weatherxm.ui.common.DeviceRelation
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.empty
import kotlinx.parcelize.Parcelize
import timber.log.Timber

@Keep
@JsonClass(generateAdapter = true)
data class ExplorerData(
    val geoJsonSource: GeoJsonSource,
    val publicHexes: List<PublicHex>,
    var polygonsToDraw: List<PolygonAnnotationOptions> = listOf(),
    var pointsToDraw: List<PointAnnotationOptions> = listOf()
)

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class UICell(
    var index: String,
    var center: Location
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class NavigationLocation(
    var zoomLevel: Double,
    var location: Location
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
data class ExplorerCamera(
    var zoom: Double,
    var center: Point
)

@Keep
@JsonClass(generateAdapter = true)
data class SearchResult(
    var name: String?,
    var center: Location?,
    var addressPlace: String? = null,
    var stationBundle: Bundle? = null,
    var stationCellIndex: String? = null,
    var stationId: String? = null,
    var relation: DeviceRelation? = null,
) {
    fun toUIDevice(): UIDevice {
        return UIDevice(
            id = stationId ?: String.empty(),
            name = name ?: String.empty(),
            cellIndex = stationCellIndex ?: String.empty(),
            cellCenter = center,
            relation = relation,
            bundleName = try {
                BundleName.valueOf(stationBundle?.name ?: String.empty())
            } catch (e: IllegalArgumentException) {
                Timber.e("Wrong Bundle Name: ${stationBundle?.name} for Device $name")
                null
            },
            bundleTitle = stationBundle?.title,
            connectivity = stationBundle?.connectivity,
            wsModel = stationBundle?.wsModel,
            gwModel = stationBundle?.gwModel,
            hwClass = stationBundle?.hwClass,
            isActive = null,
            lastWeatherStationActivity = null,
            timezone = null,
            address = addressPlace,
            isDeviceFromSearchResult = true,
            currentWeather = null,
            assignedFirmware = null,
            currentFirmware = null,
            claimedAt = null,
            friendlyName = null,
            label = null,
            location = null,
            hex7 = null,
            totalRewards = null,
            actualReward = null,
            qodScore = null,
            polReason = null,
            metricsTimestamp = null,
            hasLowBattery = null
        )
    }
}
