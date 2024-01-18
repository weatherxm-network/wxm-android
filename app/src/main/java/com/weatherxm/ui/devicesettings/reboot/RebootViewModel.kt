package com.weatherxm.ui.devicesettings.reboot

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.data.BluetoothError
import com.weatherxm.data.Failure
import com.weatherxm.data.Resource
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.components.BluetoothHeliumViewModel
import com.weatherxm.ui.devicesettings.RebootState
import com.weatherxm.ui.devicesettings.RebootStatus
import com.weatherxm.usecases.BluetoothConnectionUseCase
import com.weatherxm.usecases.BluetoothScannerUseCase
import com.weatherxm.util.Analytics
import com.weatherxm.util.Failure.getCode
import com.weatherxm.util.Resources
import kotlinx.coroutines.launch

@Suppress("TooManyFunctions")
class RebootViewModel(
    val device: UIDevice,
    private val resources: Resources,
    connectionUseCase: BluetoothConnectionUseCase,
    scanUseCase: BluetoothScannerUseCase,
    analytics: Analytics
) : BluetoothHeliumViewModel(
    device.getLastCharsOfLabel(),
    scanUseCase,
    connectionUseCase,
    analytics
) {

    private val onStatus = MutableLiveData<Resource<RebootState>>()
    fun onStatus() = onStatus

    override fun onScanFailure(failure: Failure) {
        onStatus.postValue(
            if (failure == BluetoothError.DeviceNotFound) {
                Resource.error(
                    resources.getString(R.string.station_not_in_range_subtitle),
                    RebootState(RebootStatus.SCAN_FOR_STATION, BluetoothError.DeviceNotFound)
                )
            } else {
                Resource.error("", RebootState(RebootStatus.SCAN_FOR_STATION))
            }
        )
    }

    override fun onPaired() {
        reboot()
    }

    override fun onNotPaired() {
        onStatus.postValue(Resource.error("", RebootState(RebootStatus.PAIR_STATION)))
    }

    override fun onConnected() {
        reboot()
    }

    override fun onConnectionFailure(failure: Failure) {
        onStatus.postValue(
            Resource.error(failure.getCode(), RebootState(RebootStatus.CONNECT_TO_STATION))
        )
    }

    fun startConnectionProcess() {
        onStatus.postValue(Resource.loading(RebootState(RebootStatus.CONNECT_TO_STATION)))
        super.scanAndConnect()
    }

    private fun reboot() {
        viewModelScope.launch {
            onStatus.postValue(Resource.loading(RebootState(RebootStatus.REBOOTING)))
            connectionUseCase.reboot().onRight {
                onStatus.postValue(Resource.success(RebootState(RebootStatus.REBOOTING)))
            }.onLeft {
                analytics.trackEventFailure(it.code)
                Resource.error(it.getCode(), RebootState(RebootStatus.REBOOTING))
            }
        }
    }
}
