package com.weatherxm.data.datasource

import arrow.core.Either
import arrow.core.handleErrorWith
import com.weatherxm.data.ApiError
import com.weatherxm.data.Device
import com.weatherxm.data.Failure
import com.weatherxm.data.Location
import com.weatherxm.data.map
import com.weatherxm.data.network.ApiService
import com.weatherxm.data.network.ClaimDeviceBody
import com.weatherxm.data.network.DeleteDeviceBody
import com.weatherxm.data.network.FriendlyNameBody
import kotlinx.coroutines.delay
import timber.log.Timber

interface DeviceDataSource {
    suspend fun getUserDevices(): Either<Failure, List<Device>>
    suspend fun getUserDevice(deviceId: String): Either<Failure, Device>
    suspend fun claimDevice(
        serialNumber: String,
        location: Location,
        secret: String? = null,
        numOfRetries: Int = 0
    ): Either<Failure, Device>

    suspend fun setFriendlyName(deviceId: String, friendlyName: String): Either<Failure, Unit>
    suspend fun clearFriendlyName(deviceId: String): Either<Failure, Unit>
    suspend fun deleteDevice(serialNumber: String): Either<Failure, Unit>
}

class DeviceDataSourceImpl(private val apiService: ApiService) : DeviceDataSource {

    override suspend fun getUserDevices(): Either<Failure, List<Device>> {
        return apiService.getUserDevices().map()
    }

    override suspend fun getUserDevice(deviceId: String): Either<Failure, Device> {
        return apiService.getUserDevice(deviceId).map()
    }

    override suspend fun claimDevice(
        serialNumber: String,
        location: Location,
        secret: String?,
        numOfRetries: Int
    ): Either<Failure, Device> {
        return apiService.claimDevice(ClaimDeviceBody(serialNumber, location, secret))
            .map()
            .handleErrorWith {
                if (it is ApiError.UserError.ClaimError.DeviceClaiming && numOfRetries < 10) {
                    Timber.d("Claiming Failed with ${it.code}. Retrying after 5 seconds...")
                    delay(5000L)
                    claimDevice(serialNumber, location, secret, numOfRetries + 1)
                } else {
                    Either.Left(it)
                }
            }
    }

    override suspend fun setFriendlyName(
        deviceId: String,
        friendlyName: String
    ): Either<Failure, Unit> {
        return apiService.setFriendlyName(deviceId, FriendlyNameBody(friendlyName)).map()
    }

    override suspend fun clearFriendlyName(deviceId: String): Either<Failure, Unit> {
        return apiService.clearFriendlyName(deviceId).map()
    }

    override suspend fun deleteDevice(serialNumber: String): Either<Failure, Unit> {
        return apiService.deleteDevice(DeleteDeviceBody(serialNumber)).map()
    }
}
