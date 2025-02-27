package com.weatherxm.service

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.services.CacheService
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.UploadPhotosState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import net.gotev.uploadservice.data.UploadInfo
import net.gotev.uploadservice.exceptions.UploadError
import net.gotev.uploadservice.exceptions.UserCancelledUploadException
import net.gotev.uploadservice.network.ServerResponse
import net.gotev.uploadservice.observer.request.RequestObserverDelegate
import timber.log.Timber

class GlobalUploadObserverService(
    private val analytics: AnalyticsWrapper,
    private val cacheService: CacheService
) : RequestObserverDelegate {
    private var device = UIDevice.empty()
    private var numberOfPhotosToUpload = 0
    private var currentUploadedPhotos = 0
    private var photosProgress = mutableMapOf<String, Int>()
    private val onUploadPhotosState = MutableSharedFlow<UploadPhotosState>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private fun getAverageProgress() = photosProgress.values.average().toInt()

    override fun onProgress(context: Context, uploadInfo: UploadInfo) {
        Timber.d(
            "[UPLOAD SERVICE] Average Progress: ${getAverageProgress()} " +
                "- Single Progress: ${uploadInfo.progressPercent}"
        )
        photosProgress[uploadInfo.uploadId] = uploadInfo.progressPercent
        onUploadPhotosState.tryEmit(
            UploadPhotosState(
                device = device,
                progress = getAverageProgress(),
                isSuccess = false,
                isError = false
            )
        )
    }

    @Suppress("MagicNumber")
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun onSuccess(
        context: Context,
        uploadInfo: UploadInfo,
        serverResponse: ServerResponse
    ) {
        Timber.d("[UPLOAD SERVICE] Success: $serverResponse | $uploadInfo")
        photosProgress[uploadInfo.uploadId] = uploadInfo.progressPercent
        currentUploadedPhotos += 1

        if (getAverageProgress() == 100 && currentUploadedPhotos == numberOfPhotosToUpload) {
            Timber.d("[UPLOAD SERVICE] All photos uploaded successfully.")
            analytics.trackEventViewContent(
                contentName = AnalyticsService.ParamValue.UPLOADING_PHOTOS_SUCCESS.paramValue,
                null,
                Pair(FirebaseAnalytics.Param.ITEM_ID, device.name)
            )
            onUploadPhotosState.tryEmit(
                UploadPhotosState(
                    device = device,
                    progress = getAverageProgress(),
                    isSuccess = true,
                    isError = false
                )
            )
            onUploadPhotosState.resetReplayCache()
        }

        cacheService.removeUploadIdRequest(uploadInfo.uploadId)
        cacheService.removeDevicePhotoUploadId(device.id, uploadInfo.uploadId)
    }

    override fun onError(context: Context, uploadInfo: UploadInfo, exception: Throwable) {
        when (exception) {
            is UserCancelledUploadException -> {
                Timber.e(exception, "[UPLOAD SERVICE] User Cancelled: $uploadInfo")
                onUploadPhotosState.tryEmit(
                    UploadPhotosState(
                        device = device,
                        progress = uploadInfo.progressPercent,
                        isSuccess = false,
                        isError = false,
                        isCancelled = true
                    )
                )
            }
            is UploadError -> {
                Timber.e(exception, "[UPLOAD SERVICE] Error: ${exception.serverResponse}")
                onUploadPhotosState.tryEmit(
                    UploadPhotosState(
                        device = device,
                        progress = uploadInfo.progressPercent,
                        isSuccess = false,
                        isError = true
                    )
                )
            }
            else -> {
                Timber.e(exception, "[UPLOAD SERVICE] Error: $uploadInfo")
                onUploadPhotosState.tryEmit(
                    UploadPhotosState(
                        device = device,
                        progress = uploadInfo.progressPercent,
                        isSuccess = false,
                        isError = true
                    )
                )
            }
        }
    }

    override fun onCompleted(context: Context, uploadInfo: UploadInfo) {
        Timber.d("[UPLOAD SERVICE] Completed: $uploadInfo")
    }

    override fun onCompletedWhileNotObserving() {
        Timber.d("[UPLOAD SERVICE] Completed while not observing")
    }

    fun setData(device: UIDevice, numberOfPhotosToUpload: Int) {
        this.device = device
        this.numberOfPhotosToUpload = numberOfPhotosToUpload
        this.currentUploadedPhotos = 0
        photosProgress = mutableMapOf()
    }

    fun getUploadPhotosState(): Flow<UploadPhotosState> {
        return onUploadPhotosState
    }
}
