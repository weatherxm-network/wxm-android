package com.weatherxm.ui.explorer

import android.os.Bundle
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.mapbox.geojson.Point
import com.weatherxm.BuildConfig
import com.weatherxm.R
import com.weatherxm.data.Status
import com.weatherxm.databinding.ActivityExplorerBinding
import com.weatherxm.ui.BaseMapFragment
import com.weatherxm.ui.Navigator
import com.weatherxm.ui.common.Animation.HideAnimation.SlideOutToBottom
import com.weatherxm.ui.common.Animation.ShowAnimation.SlideInFromBottom
import com.weatherxm.ui.common.hide
import com.weatherxm.ui.common.show
import dev.chrisbanes.insetter.applyInsetter
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.util.*

class ExplorerActivity : AppCompatActivity(), KoinComponent,
    BaseMapFragment.OnMapDebugInfoListener {

    private val navigator: Navigator by inject()
    private val model: ExplorerViewModel by viewModels()
    private lateinit var binding: ActivityExplorerBinding

    private var snackbar: Snackbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityExplorerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.overlayContainer.applyInsetter {
            type(navigationBars = true) {
                margin(left = false, top = false, right = false, bottom = true)
            }
        }

        model.explorerState().observe(this) { resource ->
            Timber.d("Status updated: ${resource.status}")
            when (resource.status) {
                Status.SUCCESS -> {
                    binding.devicesCount.text =
                        getString(R.string.devices_count, resource.data?.totalDevices)
                    binding.devicesCountCard.visibility = VISIBLE
                    snackbar?.dismiss()
                }
                Status.ERROR -> {
                    Timber.d("Got error: $resource.message")
                    resource.message?.let { showErrorOnMapLoading(it) }
                    binding.devicesCountCard.visibility = GONE
                }
                Status.LOADING -> {
                    snackbar?.dismiss()
                    binding.devicesCountCard.visibility = GONE
                }
            }
        }

        model.onHexSelected().observe(this) {
            navigator.showPublicDevicesList(supportFragmentManager)
        }

        model.onPublicDeviceSelected().observe(this) {
            navigator.showDeviceDetails(supportFragmentManager, it)
        }

        binding.login.setOnClickListener {
            navigator.showLogin(this)
        }

        binding.signupPrompt.setOnClickListener {
            navigator.showSignup(this)
        }

        model.showMapOverlayViews().observe(this) { shouldShow ->
            if (shouldShow) {
                binding.overlayContainer.show(SlideInFromBottom)
            } else {
                binding.overlayContainer.hide(SlideOutToBottom)
            }
        }

        binding.mapDebugInfoContainer.visibility = if (BuildConfig.DEBUG) VISIBLE else GONE
    }

    @Suppress("MagicNumber")
    override fun onMapDebugInfoUpdated(zoom: Double, center: Point) {
        fun format(number: Number, decimals: Int = 2): String {
            return String.format(Locale.getDefault(), "%.${decimals}f", number)
        }

        binding.mapDebugInfo.text = "ZOOM = ${format(zoom)}\nCENTER = " +
            "${format(center.latitude(), 6)}, ${format(center.longitude(), 6)}"
    }

    private fun showErrorOnMapLoading(message: String) {
        if (snackbar?.isShown == true) {
            snackbar?.dismiss()
        }
        snackbar = Snackbar
            .make(binding.root, message, Snackbar.LENGTH_INDEFINITE)
            .setAction(R.string.action_retry) {
                model.fetch()
            }
        snackbar?.show()
    }
}
