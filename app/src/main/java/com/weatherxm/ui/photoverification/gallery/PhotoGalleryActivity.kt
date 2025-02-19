package com.weatherxm.ui.photoverification.gallery

import android.Manifest.permission.CAMERA
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.load
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.databinding.ActivityPhotoGalleryBinding
import com.weatherxm.ui.common.Contracts
import com.weatherxm.ui.common.Contracts.ARG_DEVICE
import com.weatherxm.ui.common.Contracts.ARG_NEW_PHOTO_VERIFICATION
import com.weatherxm.ui.common.StationPhoto
import com.weatherxm.ui.common.Status
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.classSimpleName
import com.weatherxm.ui.common.disable
import com.weatherxm.ui.common.empty
import com.weatherxm.ui.common.enable
import com.weatherxm.ui.common.parcelable
import com.weatherxm.ui.common.putParcelableList
import com.weatherxm.ui.common.setHtml
import com.weatherxm.ui.common.visible
import com.weatherxm.ui.components.ActionDialogFragment
import com.weatherxm.ui.components.BaseActivity
import com.weatherxm.util.checkPermissionsAndThen
import com.weatherxm.util.hasPermission
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import java.io.File

class PhotoGalleryActivity : BaseActivity() {
    companion object {
        const val MIN_PHOTOS = 2
        const val MAX_PHOTOS = 6
    }

    private lateinit var binding: ActivityPhotoGalleryBinding

    private val model: PhotoGalleryViewModel by viewModel {
        val stationPhotoUrls = intent.getStringArrayListExtra(Contracts.ARG_PHOTOS) ?: arrayListOf()
        parametersOf(
            intent.parcelable<UIDevice>(ARG_DEVICE) ?: UIDevice.empty(),
            stationPhotoUrls.map { StationPhoto(it, null) },
            intent.getBooleanExtra(ARG_NEW_PHOTO_VERIFICATION, false)
        )
    }

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                model.addPhoto(latestPhotoTakenPath)
            }
        }

    private var wentToSettingsForPermissions = false
    private var latestPhotoTakenPath: String = String.empty()
    private var selectedPhoto: MutableState<StationPhoto?> = mutableStateOf(null)

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.emptyPhotosText.setHtml(R.string.tap_plus_icon)

        model.onPhotosNumber().observe(this) {
            onPhotosNumber(it)
        }

        if (model.fromClaiming) {
            binding.toolbar.setNavigationIcon(R.drawable.ic_close)
        } else {
            binding.toolbar.setNavigationIcon(R.drawable.ic_back)
        }

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        onBackPressedDispatcher.addCallback {
            onExit()
        }

        binding.uploadBtn.setOnClickListener {
            ActionDialogFragment
                .Builder(
                    title = getString(R.string.upload_your_photos),
                    message = getString(R.string.upload_your_photos_dialog_message),
                    negative = getString(R.string.action_back)
                )
                .onPositiveClick(getString(R.string.action_upload)) {
                    analytics.trackEventUserAction(
                        AnalyticsService.ParamValue.START_UPLOADING_PHOTOS.paramValue
                    )
                    navigator.showPhotoUpload(this, model.device, model.getPhotosLocalPaths())
                    finish()
                }
                .build()
                .show(this)
        }

        binding.instructionsBtn.setOnClickListener {
            navigator.showPhotoVerificationIntro(this, model.device, instructionsOnly = true)
        }

        binding.openSettingsBtn.setOnClickListener {
            navigator.openAppSettings(this)
            wentToSettingsForPermissions = true
        }

        binding.deletePhotoBtn.setOnClickListener {
            onDeletePhoto()
        }

        binding.thumbnails.setContent {
            Thumbnails()
        }

        model.onDeletingPhotoStatus().observe(this) {
            binding.loading.visible(it.status == Status.LOADING)
            if (it.status == Status.ERROR && it.message != null) {
                showSnackbarMessage(binding.root, it.message)
            }
        }

        if (model.photos.isEmpty()) {
            binding.addPhotoBtn.performClick()
        }
        analytics.trackScreen(AnalyticsService.Screen.STATION_PHOTOS_GALLERY, classSimpleName())
    }

    override fun onResume() {
        if (wentToSettingsForPermissions && hasPermission(CAMERA)) {
            onPermissionsGivenFromSettings()
        }
        super.onResume()
    }

    private fun onExit() {
        if (model.fromClaiming) {
            ActionDialogFragment
                .Builder(
                    title = getString(R.string.exit_photo_verification),
                    message = getString(R.string.exit_photo_verification_message),
                    positive = getString(R.string.action_stay_and_verify)
                )
                .onNegativeClick(getString(R.string.action_exit_anyway)) {
                    analytics.trackEventUserAction(
                        AnalyticsService.ParamValue.EXIT_PHOTO_VERIFICATION.paramValue
                    )
                    finish()
                }
                .build()
                .show(this)
        } else {
            val remotePhotos = model.photos.filter { it.remotePath != null }.size
            if (remotePhotos < 2) {
                ActionDialogFragment
                    .Builder(
                        title = getString(R.string.exit_photo_verification),
                        message = getString(R.string.exit_photo_verification_start_over),
                        positive = getString(R.string.action_stay_and_verify)
                    )
                    .onNegativeClick(getString(R.string.action_exit_anyway)) {
                        analytics.trackEventUserAction(
                            AnalyticsService.ParamValue.EXIT_PHOTO_VERIFICATION.paramValue
                        )
                        setResult(true)
                        finish()
                    }
                    .build()
                    .show(this)
            } else {
                finish()
            }
        }
    }

    private fun setResult(shouldDeleteAll: Boolean) {
        val intent = Intent()
            .putExtra(Contracts.ARG_DELETE_ALL_PHOTOS, shouldDeleteAll)
            .putParcelableList(Contracts.ARG_PHOTOS, model.photos)
        setResult(RESULT_OK, intent)
    }

    private fun onPhotosNumber(photosNumber: Int) {
        binding.uploadBtn.isEnabled = photosNumber >= MIN_PHOTOS && model.getLocalPhotosNumber() > 0
        if (photosNumber == MAX_PHOTOS) {
            binding.addPhotoBtn.setOnClickListener {
                showSnackbarMessage(binding.root, getString(R.string.max_photos_reached_message))
            }
        } else {
            binding.addPhotoBtn.setOnClickListener {
                analytics.trackEventUserAction(
                    AnalyticsService.ParamValue.ADD_STATION_PHOTO.paramValue
                )
                getCameraPermissions()
            }
        }
        when (photosNumber) {
            0 -> {
                binding.toolbar.subtitle = getString(R.string.add_2_more_to_upload)
                binding.emptyPhotosText.visible(true)
                binding.deletePhotoBtn.disable()
            }
            1 -> {
                binding.toolbar.subtitle = getString(R.string.add_1_more_to_upload)
                binding.deletePhotoBtn.enable()
            }
            else -> {
                binding.toolbar.subtitle = null
                binding.deletePhotoBtn.enable()
            }
        }
    }

    private fun onDeletePhoto() {
        selectedPhoto.value?.let {
            if (it.remotePath != null) {
                ActionDialogFragment
                    .Builder(
                        title = getString(R.string.delete_this_photo),
                        message = getString(R.string.delete_this_photo_message),
                        negative = getString(R.string.action_back)
                    )
                    .onPositiveClick(getString(R.string.action_delete)) {
                        setResult(false)
                        model.deletePhoto(it)
                    }
                    .build()
                    .show(this)
            } else {
                model.deletePhoto(it)
            }
        }
    }

    @Suppress("MagicNumber")
    private fun onCameraDenied() {
        if (model.photos.isEmpty()) {
            binding.deletePhotoBtn.disable()
            binding.instructionsBtn.alpha = 0.4F
            binding.instructionsBtn.isEnabled = false
            binding.addPhotoBtn.disable()
            binding.emptyPhotosText.visible(false)
            binding.permissionsContainer.visible(true)
        }
    }

    private fun onPermissionsGivenFromSettings() {
        binding.instructionsBtn.alpha = 1.0F
        binding.instructionsBtn.isEnabled = true
        binding.addPhotoBtn.enable()
        binding.permissionsContainer.visible(false)
        if (model.photos.isEmpty()) {
            binding.emptyPhotosText.visible(true)
            binding.deletePhotoBtn.disable()
        }
        wentToSettingsForPermissions = false
    }

    /**
     * `fromOnResume` helps us identify along with the `wentToSettingsForPermissions` if the user
     * came back from the settings screen or not, in order not to open the camera on every
     * `onResume` invocation but only on the `onCreate` one.
     */
    @SuppressLint("InlinedApi")
    private fun getCameraPermissions() {
        /**
         * Location-related permissions are also needed to be granted beforehand in order
         * to have them in EXIF Metadata.
         */
        checkPermissionsAndThen(
            permissions = arrayOf(CAMERA),
            rationaleTitle = getString(R.string.camera_permission_required_title),
            rationaleMessage = getString(R.string.camera_permission_required),
            onGranted = { navigator.openCamera(cameraLauncher, this, createPhotoFile()) },
            onDenied = { onCameraDenied() },
            showOnPermanentlyDenied = false
        )
    }

    private fun createPhotoFile(): File {
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(model.device.id, ".jpg", storageDir).apply {
            latestPhotoTakenPath = absolutePath
        }
    }

    @Suppress("FunctionNaming")
    @Composable
    fun Thumbnails() {
        val photos = remember { model.onPhotos }
        binding.emptyPhotosText.visible(photos.isEmpty())
        binding.selectedPhoto.visible(photos.isNotEmpty())
        selectedPhoto.value = photos.lastOrNull()?.apply {
            binding.selectedPhoto.load(remotePath ?: localPath)
        }

        LazyRow(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_small))
        ) {
            items(photos) { photo ->
                Thumbnail(
                    item = photo,
                    isSelected = photo == selectedPhoto.value,
                    onClick = {
                        binding.selectedPhoto.load(photo.remotePath ?: photo.localPath)
                        selectedPhoto.value = photo
                    }
                )
            }
        }
    }

    @Suppress("FunctionNaming")
    @Composable
    fun Thumbnail(item: StationPhoto, isSelected: Boolean, onClick: () -> Unit) {
        var width = 48.dp
        var height = 70.dp
        var border: BorderStroke? = null

        if (isSelected) {
            width = 62.dp
            height = 88.dp
            border = BorderStroke(2.dp, colorResource(R.color.colorPrimary))
        }

        Card(
            Modifier
                .size(width, height)
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(dimensionResource(R.dimen.radius_small)),
            border = border
        ) {
            if (!item.remotePath.isNullOrEmpty()) {
                AsyncImage(
                    model = item.remotePath,
                    contentDescription = item.remotePath,
                    contentScale = ContentScale.Crop
                )
            } else if (!item.localPath.isNullOrEmpty()) {
                AsyncImage(
                    model = item.localPath,
                    contentDescription = item.localPath,
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}
