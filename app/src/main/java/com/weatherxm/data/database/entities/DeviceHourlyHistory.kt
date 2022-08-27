package com.weatherxm.data.database.entities

import androidx.annotation.NonNull
import androidx.room.ColumnInfo
import androidx.room.Entity
import com.weatherxm.data.HourlyWeather
import java.time.ZonedDateTime

@Entity(primaryKeys = ["device_id", "timestamp"])
data class DeviceHourlyHistory(
    @NonNull
    @ColumnInfo(name = "device_id")
    val deviceId: String,
    @NonNull
    @ColumnInfo(name = "timestamp")
    val timestamp: ZonedDateTime,
    @ColumnInfo(name = "temperature")
    val temperature: Float?,
    @ColumnInfo(name = "precipitation_intensity")
    val precipitationIntensity: Float?,
    @ColumnInfo(name = "precipitation_probability")
    val precipProbability: Int?,
    @ColumnInfo(name = "feels_like")
    val feelsLike: Float?,
    @ColumnInfo(name = "wind_direction")
    val windDirection: Int?,
    @ColumnInfo(name = "humidity")
    val humidity: Int?,
    @ColumnInfo(name = "wind_speed")
    val windSpeed: Float?,
    @ColumnInfo(name = "wind_gust")
    val windGust: Float?,
    @ColumnInfo(name = "uv_index")
    val uvIndex: Int?,
    @ColumnInfo(name = "pressure")
    val pressure: Float?
) : BaseModel() {

    companion object {
        fun fromHourlyWeather(deviceId: String, hourlyWeather: HourlyWeather): DeviceHourlyHistory {
            return DeviceHourlyHistory(
                deviceId,
                ZonedDateTime.parse(hourlyWeather.timestamp),
                hourlyWeather.temperature,
                hourlyWeather.precipitation,
                hourlyWeather.precipProbability,
                hourlyWeather.feelsLike,
                hourlyWeather.windDirection,
                hourlyWeather.humidity,
                hourlyWeather.windSpeed,
                hourlyWeather.windGust,
                hourlyWeather.uvIndex,
                hourlyWeather.pressure
            )
        }
    }
}
