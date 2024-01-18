package com.weatherxm.ui.claimdevice.helium.result

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.data.BluetoothError
import com.weatherxm.data.Frequency
import com.weatherxm.ui.common.UIError
import com.weatherxm.ui.common.unmask
import com.weatherxm.usecases.BluetoothConnectionUseCase
import com.weatherxm.util.Analytics
import com.weatherxm.util.Resources
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class ClaimHeliumResultViewModel(
    private val resources: Resources,
    private val connectionUseCase: BluetoothConnectionUseCase,
    private val analytics: Analytics
) : ViewModel() {
    companion object {
        val CONNECT_DELAY_ON_REBOOT = TimeUnit.SECONDS.toMillis(10L)
    }

    private val onBLEError = MutableLiveData<UIError>()
    private val onBLEConnection = MutableLiveData<Boolean>()
    private val onRebooting = MutableLiveData<Boolean>()
    private val onBLEDevEUI = MutableLiveData<String>()
    private val onBLEClaimingKey = MutableLiveData<String>()

    fun onBLEError() = onBLEError
    fun onBLEConnection() = onBLEConnection
    fun onRebooting() = onRebooting
    fun onBLEDevEUI() = onBLEDevEUI
    fun onBLEClaimingKey() = onBLEClaimingKey

    fun setFrequency(frequency: Frequency) {
        viewModelScope.launch {
            connectionUseCase.setFrequency(frequency).onRight {
                reboot()
            }.onLeft {
                analytics.trackEventFailure(it.code)
                onBLEError.postValue(
                    UIError(resources.getString(R.string.set_frequency_failed_desc), it.code) {
                        setFrequency(frequency)
                    }
                )
            }
        }
    }

    private fun reboot() {
        viewModelScope.launch {
            onRebooting.postValue(true)
            connectionUseCase.reboot().onRight {
                connectToPeripheral()
            }.onLeft {
                analytics.trackEventFailure(it.code)
                onBLEError.postValue(
                    UIError(resources.getString(R.string.helium_reboot_failed), it.code) {
                        reboot()
                    }
                )
            }
        }
    }

    fun connectToPeripheral() {
        viewModelScope.launch {
            onBLEConnection.postValue(true)
            delay(CONNECT_DELAY_ON_REBOOT)
            connectionUseCase.connectToPeripheral().onLeft {
                analytics.trackEventFailure(it.code)
                onBLEError.postValue(when (it) {
                    is BluetoothError.BluetoothDisabledException -> {
                        UIError(
                            resources.getString(R.string.helium_bluetooth_disabled), it.code
                        ) {
                            connectToPeripheral()
                        }
                    }
                    is BluetoothError.ConnectionLostException -> {
                        UIError(
                            resources.getString(R.string.ble_connection_lost_description),
                            it.code
                        ) {
                            connectToPeripheral()
                        }
                    }
                    else -> {
                        UIError(
                            resources.getString(R.string.helium_connection_rejected), it.code
                        ) {
                            connectToPeripheral()
                        }
                    }
                })
            }.onRight {
                fetchDeviceEUI()
            }
        }
    }

    private fun fetchDeviceEUI() {
        viewModelScope.launch {
            connectionUseCase.fetchDeviceEUI().onRight {
                /**
                 * BLE returns Dev EUI with `:` in between so we need to unmask it
                 */
                onBLEDevEUI.postValue(it.unmask())
                fetchClaimingKey()
            }.onLeft {
                analytics.trackEventFailure(it.code)
                onBLEError.postValue(
                    UIError(resources.getString(R.string.helium_fetching_info_failed), it.code) {
                        fetchDeviceEUI()
                    }
                )
            }
        }
    }

    private fun fetchClaimingKey() {
        viewModelScope.launch {
            connectionUseCase.fetchClaimingKey().onRight {
                onBLEClaimingKey.postValue(it)
            }.onLeft {
                analytics.trackEventFailure(it.code)
                onBLEError.postValue(
                    UIError(resources.getString(R.string.helium_fetching_info_failed), it.code) {
                        fetchClaimingKey()
                    }
                )
            }
        }
    }
}
