package com.weatherxm.ui.home.devices

import com.weatherxm.R
import com.weatherxm.TestConfig.DEVICE_NOT_FOUND_MSG
import com.weatherxm.TestConfig.REACH_OUT_MSG
import com.weatherxm.TestConfig.dispatcher
import com.weatherxm.TestConfig.failure
import com.weatherxm.TestConfig.resources
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.TestUtils.testHandleFailureViewModel
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.models.ApiError
import com.weatherxm.ui.InstantExecutorListener
import com.weatherxm.ui.common.DeviceRelation
import com.weatherxm.ui.common.DeviceTotalRewards
import com.weatherxm.ui.common.DeviceTotalRewardsDetails
import com.weatherxm.ui.common.DevicesFilterType
import com.weatherxm.ui.common.DevicesGroupBy
import com.weatherxm.ui.common.DevicesRewards
import com.weatherxm.ui.common.DevicesSortFilterOptions
import com.weatherxm.ui.common.DevicesSortOrder
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.empty
import com.weatherxm.usecases.DeviceListUseCase
import com.weatherxm.usecases.FollowUseCase
import com.weatherxm.util.Resources
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class DevicesViewModelTest : BehaviorSpec({
    val deviceListUseCase = mockk<DeviceListUseCase>()
    val followUseCase = mockk<FollowUseCase>()
    val analytics = mockk<AnalyticsWrapper>()
    lateinit var viewModel: DevicesViewModel

    val deviceId = "deviceId"
    val devices = listOf(
        UIDevice.empty(),
        UIDevice(
            deviceId,
            String.empty(),
            String.empty(),
            DeviceRelation.OWNED,
            null,
            "friendlyName",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            100F,
            100F,
            null,
            null,
            null
        )
    )
    val devicesWithRewards = listOf(
        DeviceTotalRewards(deviceId, "friendlyName", 100F, DeviceTotalRewardsDetails.empty())
    )
    val devicesRewards = DevicesRewards(100F, 100F, devicesWithRewards)
    val deviceNotFoundFailure = ApiError.DeviceNotFound("")
    val maxFollowedFailure = ApiError.MaxFollowed("")
    val unauthorizedFailure = ApiError.GenericError.JWTError.UnauthorizedError("", "unauthorized")
    val maxFollowedMsg = "Max Followed Failure"
    val deviceSortFilterOptions = DevicesSortFilterOptions()

    listener(InstantExecutorListener())

    beforeSpec {
        startKoin {
            modules(
                module {
                    single<Resources> {
                        resources
                    }
                }
            )
        }
        justRun { analytics.trackEventFailure(any()) }
        every { resources.getString(R.string.error_max_followed) } returns maxFollowedMsg
        justRun { deviceListUseCase.setDevicesSortFilterOptions(any()) }

        viewModel =
            DevicesViewModel(deviceListUseCase, followUseCase, analytics, resources, dispatcher)
    }

    context("Fetch devices") {
        given("a usecase returning the list of the devices") {
            When("it's failure") {
                coMockEitherLeft(
                    { deviceListUseCase.getUserDevices() },
                    failure
                )
                testHandleFailureViewModel(
                    { viewModel.fetch() },
                    analytics,
                    viewModel.devices(),
                    1,
                    REACH_OUT_MSG
                )
                then("The function `hasNoDevices` should return false") {
                    viewModel.hasNoDevices() shouldBe false
                }
            }
            When("it's a success") {
                coMockEitherRight({ deviceListUseCase.getUserDevices() }, devices)
                runTest { viewModel.fetch() }
                then("LiveData should post the updated devices value") {
                    viewModel.devices().isSuccess(devices)
                }
                then("The function `hasNoDevices` should return false") {
                    viewModel.hasNoDevices() shouldBe false
                }
                and("calculate the rewards from the devices") {
                    then("LiveData onDevicesRewards should post the respective DevicesRewards") {
                        viewModel.onDevicesRewards().value shouldBe devicesRewards
                    }
                }
                When("the user has no devices") {
                    coMockEitherRight(
                        { deviceListUseCase.getUserDevices() },
                        mutableListOf<UIDevice>()
                    )
                    runTest { viewModel.fetch() }
                    then("The function `hasNoDevices` should return true") {
                        viewModel.hasNoDevices() shouldBe true
                    }
                }
            }
        }
    }

    context("Unfollow a station") {
        given("a usecase returning the response of the unfollow request") {
            When("it's failure") {
                and("it's a DeviceNotFound failure") {
                    coMockEitherLeft(
                        { followUseCase.unfollowStation(deviceId) },
                        deviceNotFoundFailure
                    )
                    testHandleFailureViewModel(
                        { viewModel.unFollowStation(deviceId) },
                        analytics,
                        viewModel.onUnFollowStatus(),
                        2,
                        DEVICE_NOT_FOUND_MSG
                    )
                }
                and("it's a MaxFollowed failure") {
                    coMockEitherLeft(
                        { followUseCase.unfollowStation(deviceId) },
                        maxFollowedFailure
                    )
                    testHandleFailureViewModel(
                        { viewModel.unFollowStation(deviceId) },
                        analytics,
                        viewModel.onUnFollowStatus(),
                        3,
                        maxFollowedMsg
                    )
                }
                and("it's a UnauthorizedError failure") {
                    coMockEitherLeft(
                        { followUseCase.unfollowStation(deviceId) },
                        unauthorizedFailure
                    )
                    testHandleFailureViewModel(
                        { viewModel.unFollowStation(deviceId) },
                        analytics,
                        viewModel.onUnFollowStatus(),
                        4,
                        unauthorizedFailure.message ?: String.empty()
                    )
                }
                and("it's any other failure") {
                    coMockEitherLeft(
                        { followUseCase.unfollowStation(deviceId) },
                        failure
                    )
                    testHandleFailureViewModel(
                        { viewModel.unFollowStation(deviceId) },
                        analytics,
                        viewModel.onUnFollowStatus(),
                        5,
                        REACH_OUT_MSG
                    )
                }
            }
            When("it's a success") {
                coMockEitherRight({ followUseCase.unfollowStation(deviceId) }, Unit)
                coMockEitherRight({ deviceListUseCase.getUserDevices() }, emptyList<UIDevice>())

                runTest { viewModel.unFollowStation(deviceId) }

                then("LiveData onUnFollowStatus should post the value Unit as success") {
                    viewModel.onUnFollowStatus().isSuccess(Unit)
                }
                then("fetching devices should take place again and the list should be empty") {
                    viewModel.devices().isSuccess(emptyList())
                }
            }
        }
    }

    context("Set the device sort filter and group options") {
        given("a sortOrderId, a filterId and a groupById") {
            When("Sort Order = R.id.dateAdded, Filter = R.id.showAll, Group = R.id.noGrouping") {
                viewModel.setDevicesSortFilterOptions(R.id.dateAdded, R.id.showAll, R.id.noGrouping)
                then("The usecase should call the set function with the respective options") {
                    verify(exactly = 1) {
                        deviceListUseCase.setDevicesSortFilterOptions(DevicesSortFilterOptions())
                    }
                }
            }
            When("Sort Order = R.id.name, Filter = R.id.ownedOnly, Group = R.id.relationship") {
                viewModel.setDevicesSortFilterOptions(R.id.name, R.id.ownedOnly, R.id.relationship)
                then("The usecase should call the set function with the respective options") {
                    verify(exactly = 1) {
                        deviceListUseCase.setDevicesSortFilterOptions(
                            DevicesSortFilterOptions(
                                DevicesSortOrder.NAME,
                                DevicesFilterType.OWNED,
                                DevicesGroupBy.RELATIONSHIP
                            )
                        )
                    }
                }
            }
            When("Sort Order = R.id.lastActive, Filter = R.id.favoritesOnly, Group = R.id.status") {
                viewModel.setDevicesSortFilterOptions(
                    R.id.lastActive,
                    R.id.favoritesOnly,
                    R.id.status
                )
                then("The usecase should call the set function with the respective options") {
                    verify(exactly = 1) {
                        deviceListUseCase.setDevicesSortFilterOptions(
                            DevicesSortFilterOptions(
                                DevicesSortOrder.LAST_ACTIVE,
                                DevicesFilterType.FAVORITES,
                                DevicesGroupBy.STATUS
                            )
                        )
                    }
                }
            }
        }
    }

    context("Get the device sort filter and group options") {
        given("a usecase returning the device sort filter and group options") {
            every {
                deviceListUseCase.getDevicesSortFilterOptions()
            } returns deviceSortFilterOptions
            then("return them") {
                viewModel.getDevicesSortFilterOptions() shouldBe deviceSortFilterOptions
            }
        }
    }

    afterSpec {
        stopKoin()
    }
})
