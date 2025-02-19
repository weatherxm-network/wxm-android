package com.weatherxm.ui.photoverification.intro

import android.annotation.SuppressLint
import android.os.Bundle
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.databinding.ActivityPhotoVerificationIntroBinding
import com.weatherxm.ui.common.Contracts
import com.weatherxm.ui.common.Contracts.ARG_DEVICE
import com.weatherxm.ui.common.Contracts.ARG_INSTRUCTIONS_ONLY
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.classSimpleName
import com.weatherxm.ui.common.parcelable
import com.weatherxm.ui.components.ActionDialogFragment
import com.weatherxm.ui.components.BaseActivity
import com.weatherxm.ui.photoverification.PhotoVerificationInstructionsFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

class PhotoVerificationIntroActivity : BaseActivity() {
    private lateinit var binding: ActivityPhotoVerificationIntroBinding
    private val viewModel: PhotoVerificationIntroViewModel by viewModel()

    private var instructionsOnly = false
    private var device = UIDevice.empty()
    private var stationPhotoUrls: ArrayList<String> = arrayListOf()

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoVerificationIntroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        instructionsOnly = intent.getBooleanExtra(ARG_INSTRUCTIONS_ONLY, false)
        device = intent.parcelable<UIDevice>(ARG_DEVICE) ?: UIDevice.empty()
        stationPhotoUrls = intent.getStringArrayListExtra(Contracts.ARG_PHOTOS) ?: arrayListOf()

        /**
         * User opened this screen from an empty state (either no photos in settings or in claiming
         * but the user has already accepted the terms before, and it's not a "Show Instructions"
         * case so we just redirect to Photo Gallery.
         */
        if (viewModel.getAcceptedTerms() && !instructionsOnly) {
            navigator.showPhotoGallery(null, this, device, stationPhotoUrls, true)
            finish()
        }
        if (instructionsOnly) {
            analytics.trackScreen(
                AnalyticsService.Screen.STATION_PHOTOS_INSTRUCTIONS, classSimpleName()
            )
        } else {
            analytics.trackScreen(
                AnalyticsService.Screen.STATION_PHOTOS_INTRO, classSimpleName()
            )
        }
    }

    override fun onResume() {
        super.onResume()

        with(binding.fragmentView.getFragment<PhotoVerificationInstructionsFragment>()) {
            if (instructionsOnly) {
                onInstructionScreen {
                    finish()
                }
            } else {
                onIntroScreen(
                    onClose = {
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
                    },
                    onTakePhoto = {
                        viewModel.setAcceptedTerms()
                        navigator.showPhotoGallery(
                            null,
                            this@PhotoVerificationIntroActivity,
                            device,
                            stationPhotoUrls,
                            true
                        )
                        finish()
                    }
                )
            }
        }
    }
}
