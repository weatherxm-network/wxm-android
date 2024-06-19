package com.weatherxm.ui.devicesettings.wifi

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.DeviceInfo
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.empty
import com.weatherxm.ui.common.unmask
import com.weatherxm.ui.devicesettings.BaseDeviceSettingsViewModel
import com.weatherxm.ui.devicesettings.UIDeviceInfo
import com.weatherxm.ui.devicesettings.UIDeviceInfoItem
import com.weatherxm.usecases.StationSettingsUseCase
import com.weatherxm.util.DateTimeHelper.getFormattedDateAndTime
import com.weatherxm.util.Resources
import kotlinx.coroutines.launch
import timber.log.Timber

class DeviceSettingsWifiViewModel(
    device: UIDevice,
    private val usecase: StationSettingsUseCase,
    private val resources: Resources,
    private val analytics: AnalyticsWrapper
) : BaseDeviceSettingsViewModel(device, usecase, resources, analytics) {
    private val onDeviceInfo = MutableLiveData<UIDeviceInfo>()

    private val data = UIDeviceInfo(mutableListOf(), mutableListOf(), mutableListOf())

    fun onDeviceInfo(): LiveData<UIDeviceInfo> = onDeviceInfo

    override fun getDeviceInformation(context: Context) {
        data.default.add(
            UIDeviceInfoItem(resources.getString(R.string.station_default_name), device.name)
        )

        device.bundleTitle?.let {
            data.default.add(UIDeviceInfoItem(resources.getString(R.string.bundle_identifier), it))
        }
        device.claimedAt?.let {
            data.default.add(
                UIDeviceInfoItem(
                    resources.getString(R.string.claimed_at),
                    it.getFormattedDateAndTime(context)
                )
            )
        }
        onLoading.postValue(true)
        viewModelScope.launch {
            usecase.getDeviceInfo(device.id).onLeft {
                analytics.trackEventFailure(it.code)
                Timber.d("$it: Fetching remote device info failed for device: $device")
                onDeviceInfo.postValue(data)
            }.onRight { info ->
                Timber.d("Got device info: $info")
                handleInfo(context, info)
                onDeviceInfo.postValue(data)
            }
            onLoading.postValue(false)
        }
    }

    override fun handleInfo(context: Context, info: DeviceInfo) {
        // Get weather station info
        info.weatherStation?.apply {
            model?.let {
                data.station.add(UIDeviceInfoItem(resources.getString(R.string.model), it))
            }

            batteryState?.let {
                handleLowBatteryInfo(data.station, it)
            }

            hwVersion?.let {
                data.station.add(
                    UIDeviceInfoItem(resources.getString(R.string.hardware_version), it)
                )
            }

            lastActivity?.let {
                data.station.add(
                    UIDeviceInfoItem(
                        resources.getString(R.string.last_weather_station_activity),
                        it.getFormattedDateAndTime(context)
                    )
                )
            }
        }

        // Get gateway info
        info.gateway?.apply {
            model?.let {
                data.gateway.add(UIDeviceInfoItem(resources.getString(R.string.model), it))
            }

            serialNumber?.let {
                data.gateway.add(
                    UIDeviceInfoItem(resources.getString(R.string.serial_number), it.unmask())
                )
            }

            handleFirmwareInfo()

            val gpsTimestamp =
                gpsSatsLastActivity?.getFormattedDateAndTime(context) ?: String.empty()
            gpsSats?.let {
                data.gateway.add(
                    UIDeviceInfoItem(
                        resources.getString(R.string.gps_number_sats),
                        resources.getString(R.string.satellites, it, gpsTimestamp)
                    )
                )
            }

            val wifiTimestamp =
                wifiRssiLastActivity?.getFormattedDateAndTime(context) ?: String.empty()
            wifiRssi?.let {
                data.gateway.add(
                    UIDeviceInfoItem(
                        resources.getString(R.string.wifi_rssi),
                        resources.getString(R.string.rssi, it, wifiTimestamp)
                    )
                )
            }

            lastActivity?.let {
                data.gateway.add(
                    UIDeviceInfoItem(
                        resources.getString(R.string.last_gateway_activity),
                        it.getFormattedDateAndTime(context)
                    )
                )
            }
        }
    }

    private fun handleFirmwareInfo() {
        device.currentFirmware?.let { current ->
            val currentFirmware = if (device.currentFirmware.equals(device.assignedFirmware)) {
                current
            } else {
                "$current ${resources.getString(R.string.latest_hint)}"
            }
            data.gateway.add(
                UIDeviceInfoItem(resources.getString(R.string.firmware_version), currentFirmware)
            )
        }
    }
}
