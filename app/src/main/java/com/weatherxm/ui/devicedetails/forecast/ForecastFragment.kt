package com.weatherxm.ui.devicedetails.forecast

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.FirebaseAnalytics
import com.weatherxm.R
import com.weatherxm.data.Status
import com.weatherxm.databinding.FragmentDeviceDetailsForecastBinding
import com.weatherxm.ui.Navigator
import com.weatherxm.ui.common.DeviceRelation.UNFOLLOWED
import com.weatherxm.ui.common.setVisible
import com.weatherxm.ui.devicedetails.DeviceDetailsViewModel
import com.weatherxm.util.Analytics
import com.weatherxm.util.setHtml
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

class ForecastFragment : Fragment(), KoinComponent {
    private lateinit var binding: FragmentDeviceDetailsForecastBinding
    private val analytics: Analytics by inject()
    private val navigator: Navigator by inject()
    private val parentModel: DeviceDetailsViewModel by activityViewModels()
    private val model: ForecastViewModel by viewModel {
        parametersOf(parentModel.device)
    }
    private var snackbar: Snackbar? = null

    private lateinit var forecastAdapter: ForecastAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDeviceDetailsForecastBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.swiperefresh.setOnRefreshListener {
            if (model.device.relation != UNFOLLOWED) {
                model.fetchForecast(true)
            }
        }

        initHiddenContent()

        // Initialize the adapter with empty data
        forecastAdapter = ForecastAdapter { index, state ->
            val stateParam = if (state) {
                Pair(Analytics.CustomParam.STATE.paramName, Analytics.ParamValue.OPEN.paramValue)
            } else {
                Pair(Analytics.CustomParam.STATE.paramName, Analytics.ParamValue.CLOSE.paramValue)
            }
            analytics.trackEventSelectContent(
                Analytics.ParamValue.FORECAST_DAY.paramValue,
                Pair(FirebaseAnalytics.Param.ITEM_ID, model.device.id),
                stateParam,
                index = index.toLong()
            )
        }
        binding.forecastRecycler.adapter = forecastAdapter

        parentModel.onFollowStatus().observe(viewLifecycleOwner) {
            if (it.status == Status.SUCCESS) {
                model.device = parentModel.device
                fetchOrHideContent()
            }
        }

        parentModel.onDeviceFirstFetch().observe(viewLifecycleOwner) {
            model.device = it
            model.fetchForecast(true)
        }

        model.onForecast().observe(viewLifecycleOwner) {
            forecastAdapter.submitList(it)
            binding.forecastRecycler.setVisible(true)
        }

        model.onLoading().observe(viewLifecycleOwner) {
            if (it && binding.swiperefresh.isRefreshing) {
                binding.progress.visibility = View.INVISIBLE
            } else if (it) {
                binding.progress.visibility = View.VISIBLE
            } else {
                binding.swiperefresh.isRefreshing = false
                binding.progress.visibility = View.INVISIBLE
            }
        }

        model.onError().observe(viewLifecycleOwner) {
            showSnackbarMessage(it.errorMessage, it.retryFunction)
        }

        fetchOrHideContent()
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(
            Analytics.Screen.FORECAST,
            ForecastFragment::class.simpleName
        )
    }

    private fun fetchOrHideContent() {
        if (model.device.relation != UNFOLLOWED) {
            binding.hiddenContentContainer.setVisible(false)
            model.fetchForecast()
        } else if (model.device.relation == UNFOLLOWED) {
            binding.forecastRecycler.setVisible(false)
            binding.hiddenContentContainer.setVisible(true)
        }
    }

    private fun initHiddenContent() {
        binding.hiddenContentText.setHtml(R.string.hidden_content_prompt, model.device.name)
        binding.hiddenContentBtn.setOnClickListener {
            if (parentModel.isLoggedIn() == true) {
                if (model.device.relation == UNFOLLOWED && !model.device.isOnline()) {
                    navigator.showHandleFollowDialog(activity, true, model.device.name) {
                        parentModel.followStation()
                    }
                } else {
                    parentModel.followStation()
                }
            } else {
                navigator.showLoginDialog(
                    fragmentActivity = activity,
                    title = getString(R.string.add_favorites),
                    htmlMessage = getString(R.string.hidden_content_login_prompt, model.device.name)
                )
            }
        }
    }

    private fun showSnackbarMessage(message: String, callback: (() -> Unit)? = null) {
        if (snackbar?.isShown == true) {
            snackbar?.dismiss()
        }

        if (callback != null) {
            snackbar = Snackbar.make(binding.root, message, Snackbar.LENGTH_INDEFINITE)
            snackbar?.setAction(R.string.action_retry) {
                callback()
            }
        } else {
            snackbar = Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
        }
        snackbar?.show()
    }
}
