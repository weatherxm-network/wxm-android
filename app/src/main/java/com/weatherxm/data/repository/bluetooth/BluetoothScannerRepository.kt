package com.weatherxm.data.repository.bluetooth

import arrow.core.Either
import com.weatherxm.data.BluetoothDeviceWithEUI
import com.weatherxm.data.Failure
import kotlinx.coroutines.flow.Flow

interface BluetoothScannerRepository {
    suspend fun registerOnScanning(): Flow<BluetoothDeviceWithEUI>
    suspend fun startScanning(): Either<Failure, Unit>
}
