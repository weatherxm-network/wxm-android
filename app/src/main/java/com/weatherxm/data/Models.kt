package com.weatherxm.data

import android.os.Parcelable
import androidx.annotation.Keep
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.weatherxm.ui.TokenSummary
import kotlinx.parcelize.Parcelize
import java.time.ZonedDateTime

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class User(
    val id: String,
    val email: String,
    val name: String?,
    val firstName: String?,
    val lastName: String?,
    val wallet: Wallet?,
) : Parcelable {
    fun hasWallet() = wallet?.address?.isNotEmpty() == true
}

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class Wallet(
    val address: String?,
    val updatedAt: Long?,
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class Location(
    val lat: Double,
    val lon: Double
) : Parcelable {
    companion object {
        fun empty() = Location(0.0, 0.0)
    }

    fun isEmpty() = this.lat == 0.0 && this.lon == 0.0
}

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class Device(
    val id: String,
    val name: String,
    val label: String?,
    val location: Location?,
    val timezone: String?,
    val attributes: Attributes?,
    @Json(name = "current_weather")
    val currentWeather: HourlyWeather?,
    val address: String?
) : Parcelable {
    // TODO: When we have the new field for the "label" of the device use it here
    fun getNameOrLabel(): String {
        return name
    }
}

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class Attributes(
    val isActive: Boolean?,
    val lastActiveAt: ZonedDateTime?,
    val hex3: Hex,
    val hex7: Hex,
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class Hex(
    val index: String,
    val polygon: Array<Location>,
    val center: Location
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class Tokens(
    val daily: TokensSummaryResponse,
    val weekly: TokensSummaryResponse,
    val monthly: TokensSummaryResponse
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class TokensSummaryResponse(
    val total: Float?,
    val tokens: List<TokenEntry>?
) : Parcelable {
    fun toTokenSummary(): TokenSummary {
        val summary = TokenSummary(0F, mutableListOf())

        total?.let {
            summary.total = it
        }

        tokens?.let {
            it.forEach { tokenEntry ->
                if (tokenEntry.timestamp != null && tokenEntry.actualReward != null) {
                    summary.values.add(Pair(tokenEntry.timestamp, tokenEntry.actualReward))
                }
            }
        }
        return summary
    }
}

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class TokenEntry(
    val timestamp: String?,
    @Json(name = "actual_reward")
    val actualReward: Float?
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class TransactionsResponse(
    val data: List<Transaction>,
    @Json(name = "total_pages")
    val totalPages: Int,
    @Json(name = "has_next_page")
    val hasNextPage: Boolean
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class Transaction(
    val timestamp: String?,
    @Json(name = "tx_hash")
    val txHash: String?,
    @Json(name = "validation_score")
    val validationScore: Float?,
    @Json(name = "daily_reward")
    val dailyReward: Float?,
    @Json(name = "actual_reward")
    val actualReward: Float?,
    @Json(name = "total_rewards")
    val totalRewards: Float?,
    @Json(name = "lost_rewards")
    val lostRewards: Float?,
    @Json(name = "wxm_balance")
    val wxmBalance: Float?
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class WeatherData(
    var date: String?,
    val tz: String?,
    val hourly: List<HourlyWeather>?,
    val daily: DailyData?
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class HourlyWeather(
    var timestamp: String,
    val precipitation: Float?,
    val temperature: Float?,
    @Json(name = "wind_direction")
    val windDirection: Int?,
    val humidity: Int?,
    @Json(name = "wind_speed")
    val windSpeed: Float?,
    @Json(name = "wind_gust")
    val windGust: Float?,
    val icon: String?,
    @Json(name = "precipitation_probability")
    val precipProbability: Int?,
    @Json(name = "uv_index")
    val uvIndex: Int?,
    @Json(name = "cloud_cover")
    val cloudCover: Int?,
    val pressure: Float?
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class DailyData(
    var timestamp: String?,
    @Json(name = "precip_intensity")
    val precipIntensity: Float?,
    @Json(name = "precipitation_type")
    val precipType: String?,
    @Json(name = "temperature_min")
    val temperatureMin: Float?,
    @Json(name = "temperature_max")
    val temperatureMax: Float?,
    @Json(name = "wind_direction")
    val windDirection: Int?,
    val humidity: Int?,
    @Json(name = "wind_speed")
    val windSpeed: Float?,
    @Json(name = "wind_gust")
    val windGust: Float?,
    @Json(name = "icon")
    val icon: String?,
    @Json(name = "precipitation_probability")
    val precipProbability: Int?,
    @Json(name = "uv_index")
    val uvIndex: Int?,
    @Json(name = "cloud_cover")
    val cloudCover: Int?,
    val pressure: Float?
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class HistoryDaily(
    val timestamp: Long?,
    @Json(name = "precip_intensity")
    val precipitationIntensity: Float?,
    @Json(name = "min_temperature")
    val minTemperature: Float?,
    @Json(name = "max_temperature")
    val maxTemperature: Float?,
    @Json(name = "wind_direction")
    val windDirection: Int?,
    val humidity: Int?,
    @Json(name = "wind_speed")
    val windSpeed: Float?,
    val icon: String?,
    @Json(name = "dew_point")
    val dewPoint: Float?,
    @Json(name = "uv_index")
    val uvIndex: Int?,
    @Json(name = "pressure")
    val pressure: Float?,
    @Json(name = "cloud_cover")
    val cloudCover: Int?,
    @Json(name = "wind_gust")
    val windGust: Float?
) : Parcelable
