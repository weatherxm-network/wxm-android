package com.weatherxm.ui.claimdevice.helium.verify

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.weatherxm.util.Validator
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ClaimHeliumVerifyViewModel : ViewModel(), KoinComponent {
    private val validator: Validator by inject()
    private var devEUI: String = ""
    private var deviceKey: String = ""

    private val onDevKeyError = MutableLiveData(false)
    fun onDevKeyError() = onDevKeyError

    private val onDevEUIError = MutableLiveData(false)
    fun onDevEUIError() = onDevEUIError

    private val onVerifyError = MutableLiveData(false)
    fun onVerifyError() = onVerifyError

    fun setDeviceEUI(devEUI: String) {
        this.devEUI = devEUI
    }

    fun setDeviceKey(key: String) {
        deviceKey = key
    }

    fun getDevEUI(): String {
        return devEUI
    }

    fun getDeviceKey(): String {
        return deviceKey
    }

    @Suppress("MagicNumber")
    fun getEUIFromScanner(result: String?): String {
        return result?.take(16) ?: ""
    }

    @Suppress("MagicNumber")
    fun getKeyFromScanner(result: String?): String {
        return result?.substring(16..31) ?: ""
    }

    fun checkAndVerify(devEUI: String, devKey: String) {
        val validDevEUI = validator.validateDevEUI(devEUI)
        val validDevKey = validator.validateDevKey(devKey)

        onDevEUIError.postValue(!validDevEUI)
        onDevKeyError.postValue(!validDevKey)

        if (validDevEUI && validDevKey) {
            setDeviceEUI(devEUI)
            setDeviceKey(devKey)
            // TODO: Verified. Continue with API call and open helium pairing status fragment.
        }
    }
}
