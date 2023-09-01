package com.weatherxm.ui.widgets.selectstation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.data.Resource
import com.weatherxm.ui.common.DeviceRelation
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.UserDevices
import com.weatherxm.usecases.WidgetSelectStationUseCase
import com.weatherxm.util.UIErrors.getDefaultMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class SelectStationViewModel : ViewModel(), KoinComponent {

    private val usecase: WidgetSelectStationUseCase by inject()

    private val devices = MutableLiveData<Resource<UserDevices>>()
    private val isNotLoggedIn = MutableLiveData<Unit>()

    fun devices(): LiveData<Resource<UserDevices>> = devices
    fun isNotLoggedIn(): LiveData<Unit> = isNotLoggedIn

    private var currentStationSelected = UIDevice.empty()

    private var userDevices: UserDevices? = null

    fun setStationSelected(device: UIDevice) {
        currentStationSelected = device
    }

    fun getStationSelected() = currentStationSelected

    fun getUserDevices() = userDevices

    fun checkIfLoggedInAndProceed() {
        Timber.d("Checking if user is logged in in the background")
        viewModelScope.launch(Dispatchers.IO) {
            usecase.isLoggedIn().onRight {
                if (it) {
                    fetch()
                } else {
                    isNotLoggedIn.postValue(Unit)
                }
            }.onLeft {
                isNotLoggedIn.postValue(Unit)
            }
        }
    }

    fun fetch() {
        this@SelectStationViewModel.devices.postValue(Resource.loading())
        viewModelScope.launch(Dispatchers.IO) {
            usecase.getUserDevices()
                .map { devices ->
                    Timber.d("Got ${devices.size} devices")
                    val ownedDevices = devices.count { it.relation == DeviceRelation.OWNED }
                    userDevices = UserDevices(
                        devices,
                        devices.size,
                        ownedDevices,
                        devices.size - ownedDevices
                    )
                    this@SelectStationViewModel.devices.postValue(Resource.success(userDevices))
                }
                .mapLeft {
                    this@SelectStationViewModel.devices.postValue(
                        Resource.error(it.getDefaultMessage())
                    )
                }
        }
    }

    fun saveWidgetData(widgetId: Int) {
        usecase.saveWidgetData(widgetId, currentStationSelected.id)
    }
}
