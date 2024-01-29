package com.weatherxm.ui.claimdevice.helium.pair

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.BLUETOOTH_CONNECT
import android.Manifest.permission.BLUETOOTH_SCAN
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.core.view.forEach
import com.weatherxm.R
import com.weatherxm.data.Resource
import com.weatherxm.data.Status
import com.weatherxm.databinding.FragmentClaimHeliumPairBinding
import com.weatherxm.ui.claimdevice.helium.ClaimHeliumViewModel
import com.weatherxm.ui.common.UIError
import com.weatherxm.ui.common.hide
import com.weatherxm.ui.common.setBluetoothDrawable
import com.weatherxm.ui.common.setHtml
import com.weatherxm.ui.common.setNoDevicesFoundDrawable
import com.weatherxm.ui.common.setWarningDrawable
import com.weatherxm.ui.common.show
import com.weatherxm.ui.components.ActionDialogFragment
import com.weatherxm.ui.components.BaseFragment
import com.weatherxm.util.Analytics
import com.weatherxm.util.checkPermissionsAndThen
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class ClaimHeliumPairFragment : BaseFragment() {
    private val model: ClaimHeliumPairViewModel by viewModel()
    private val parentModel: ClaimHeliumViewModel by activityViewModel()
    private val bluetoothAdapter: BluetoothAdapter? by inject()
    private lateinit var binding: FragmentClaimHeliumPairBinding
    private lateinit var adapter: ScannedDevicesListAdapter

    private val enableBluetoothLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                checkAndScanBleDevices()
            } else {
                binding.infoIcon.setWarningDrawable(requireContext())
                showInfoMessage(R.string.bluetooth_not_enabled, R.string.bluetooth_not_enabled_desc)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentClaimHeliumPairBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ScannedDevicesListAdapter {
            model.setupBluetoothClaiming(it)
            setEnabledScannedDevices(false)
            binding.progressBar.visibility = GONE
        }

        binding.recycler.adapter = adapter

        with(binding.connectionLostSubtitle) {
            movementMethod =
                me.saket.bettermovementmethod.BetterLinkMovementMethod.newInstance().apply {
                    setOnLinkClickListener { _, url ->
                        navigator.openWebsite(context, url)
                        return@setOnLinkClickListener true
                    }
                }
            setHtml(
                R.string.ble_connection_lost_desc,
                getString(R.string.troubleshooting_helium_url)
            )
        }

        binding.quit.setOnClickListener {
            parentModel.cancel()
        }

        binding.contactSupport.setOnClickListener {
            navigator.openSupportCenter(context = context)
        }
        binding.retry.setOnClickListener {
            binding.connectionErrorContainer.hide(null)
            binding.mainContainer.show(null)
            model.setupBluetoothClaiming()
        }

        binding.scanAgain.setOnClickListener {
            analytics.trackEventSelectContent(Analytics.ParamValue.BLE_SCAN_AGAIN.paramValue)
            adapter.submitList(mutableListOf())
            enableBluetoothAndScan()
        }

        binding.accessBluetoothPrompt.setOnClickListener {
            navigator.openAppSettings(context)
        }

        model.onNewScannedDevice().observe(viewLifecycleOwner) {
            adapter.submitList(it)
            adapter.notifyDataSetChanged()
            binding.infoContainer.visibility = GONE
            binding.recycler.visibility = VISIBLE
        }

        model.onScanStatus().observe(viewLifecycleOwner) {
            updateUI(it)
        }

        model.onScanProgress().observe(viewLifecycleOwner) {
            binding.progressBar.progress = it
        }

        model.onBLEError().observe(viewLifecycleOwner) {
            setEnabledScannedDevices(true)
            showErrorDialog(it)
        }

        model.onBLEConnectionLost().observe(viewLifecycleOwner) {
            if (it) {
                setEnabledScannedDevices(true)
                binding.mainContainer.visibility = GONE
                binding.connectionErrorContainer.visibility = VISIBLE
            }
        }

        model.onBLEConnection().observe(viewLifecycleOwner) {
            if (it) {
                parentModel.next()
            }
        }

        enableBluetoothAndScan()
    }

    private fun setEnabledScannedDevices(isEnabled: Boolean) {
        binding.recycler.forEach { view ->
            view.isEnabled = isEnabled
        }
    }

    private fun showErrorDialog(uiError: UIError) {
        ActionDialogFragment
            .Builder(
                title = getString(R.string.pairing_failed),
                message = uiError.errorMessage
            )
            .onNegativeClick(getString(R.string.action_quit_claiming)) {
                analytics.trackEventUserAction(
                    actionName = Analytics.ParamValue.HELIUM_BLE_POPUP_ERROR.paramValue,
                    contentType = Analytics.ParamValue.HELIUM_BLE_POPUP.paramValue,
                    Pair(
                        Analytics.CustomParam.ACTION.paramName,
                        Analytics.ParamValue.CANCEL.paramValue
                    )
                )
                parentModel.cancel()
            }
            .onPositiveClick(getString(R.string.action_try_again)) {
                analytics.trackEventUserAction(
                    actionName = Analytics.ParamValue.HELIUM_BLE_POPUP_ERROR.paramValue,
                    contentType = Analytics.ParamValue.HELIUM_BLE_POPUP.paramValue,
                    Pair(
                        Analytics.CustomParam.ACTION.paramName,
                        Analytics.ParamValue.TRY_AGAIN.paramValue
                    )
                )
                uiError.retryFunction?.invoke()
            }
            .build()
            .show(this)

        analytics.trackScreen(
            Analytics.Screen.BLE_CONNECTION_POPUP_ERROR,
            ClaimHeliumPairFragment::class.simpleName
        )
    }

    private fun updateUI(result: Resource<Unit>) {
        when (result.status) {
            Status.SUCCESS -> {
                binding.progressBar.visibility = GONE
                binding.scanAgain.isEnabled = true
                if (adapter.currentList.isNotEmpty()) {
                    binding.infoContainer.visibility = GONE
                } else {
                    binding.recycler.visibility = GONE
                    binding.infoIcon.setNoDevicesFoundDrawable(requireContext())
                    showInfoMessage(R.string.no_devices_found, R.string.no_devices_found_desc)
                }
            }
            Status.ERROR -> {
                binding.progressBar.visibility = GONE
                binding.scanAgain.isEnabled = true
                binding.recycler.visibility = GONE
                binding.infoIcon.setWarningDrawable(requireContext())
                showInfoMessage(R.string.scan_failed_title, R.string.scan_failed_desc)
            }
            Status.LOADING -> {
                binding.progressBar.visibility = VISIBLE
                binding.scanAgain.isEnabled = false
                binding.recycler.visibility = GONE
                binding.infoIcon.setBluetoothDrawable(requireContext())
                showInfoMessage(R.string.scanning_in_progress, null)
            }
        }
    }

    private fun showInfoMessage(@StringRes title: Int, @StringRes subtitle: Int?) {
        binding.infoTitle.text = getString(title)
        with(binding.infoSubtitle) {
            subtitle?.let {
                setHtml(it)
                visibility = VISIBLE
            } ?: run {
                visibility = GONE
            }
        }
        binding.infoContainer.visibility = VISIBLE
    }

    private fun showNoBluetoothAccessText() {
        binding.infoIcon.setBluetoothDrawable(requireContext())
        showInfoMessage(R.string.no_bluetooth_access, R.string.no_bluetooth_access_desc)
        binding.accessBluetoothPrompt.visibility = VISIBLE
    }

    private fun enableBluetoothAndScan() {
        bluetoothAdapter?.let {
            if (it.isEnabled) {
                checkAndScanBleDevices()
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    activity?.checkPermissionsAndThen(
                        permissions = arrayOf(BLUETOOTH_CONNECT),
                        rationaleTitle = getString(R.string.permission_bluetooth_title),
                        rationaleMessage = getString(R.string.perm_bluetooth_scanning_desc),
                        onGranted = {
                            navigator.showBluetoothEnablePrompt(enableBluetoothLauncher)
                        },
                        onDenied = { showNoBluetoothAccessText() })
                } else {
                    navigator.showBluetoothEnablePrompt(enableBluetoothLauncher)
                }
            }
        } ?: run {
            binding.infoIcon.setBluetoothDrawable(requireContext())
            showInfoMessage(R.string.no_bluetooth_available, R.string.no_bluetooth_available_desc)
        }
    }

    private fun checkAndScanBleDevices() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            activity?.checkPermissionsAndThen(
                permissions = arrayOf(BLUETOOTH_SCAN, BLUETOOTH_CONNECT),
                rationaleTitle = getString(R.string.permission_bluetooth_title),
                rationaleMessage = getString(R.string.perm_bluetooth_scanning_desc),
                onGranted = { model.scanBleDevices() },
                onDenied = { showNoBluetoothAccessText() }
            )
        } else {
            activity?.checkPermissionsAndThen(
                permissions = arrayOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION),
                rationaleTitle = getString(R.string.permission_location_title),
                rationaleMessage = getString(R.string.perm_location_scanning_desc),
                onGranted = { model.scanBleDevices() },
                onDenied = {
                    binding.infoIcon.setBluetoothDrawable(requireContext())
                    showInfoMessage(R.string.no_location_access, R.string.no_location_access_desc)
                    binding.accessBluetoothPrompt.visibility = VISIBLE
                }
            )
        }
    }
}
