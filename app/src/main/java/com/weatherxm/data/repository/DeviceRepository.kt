package com.weatherxm.data.repository

import arrow.core.Either
import com.weatherxm.data.Device
import com.weatherxm.data.Failure
import com.weatherxm.data.datasource.DeviceDataSource
import org.koin.core.component.KoinComponent

class DeviceRepository(
    private val deviceDataSource: DeviceDataSource
) : KoinComponent {

    suspend fun getUserDevices(): Either<Failure, List<Device>> {
        return deviceDataSource.getUserDevices()
    }

    suspend fun getUserDevice(deviceId: String): Either<Failure, Device> {
        return deviceDataSource.getUserDevice(deviceId)
    }

    suspend fun getPublicDevices(): Either<Failure, List<Device>> {
        return deviceDataSource.getPublicDevices()
    }

    fun getNameOrLabel(name: String, label: String?): Either<Failure, String> {
        return if (!label.isNullOrEmpty()) {
            Either.Right("$label ($name)")
        } else {
            Either.Right(name)
        }
    }
}
