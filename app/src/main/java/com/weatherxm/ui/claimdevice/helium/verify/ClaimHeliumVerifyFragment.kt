package com.weatherxm.ui.claimdevice.helium.verify

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import com.weatherxm.R
import com.weatherxm.databinding.FragmentClaimHeliumVerifyBinding
import com.weatherxm.ui.Navigator
import com.weatherxm.ui.claimdevice.helium.ClaimHeliumViewModel
import com.weatherxm.ui.common.show
import com.weatherxm.util.onTextChanged
import org.koin.android.ext.android.inject

class ClaimHeliumVerifyFragment : Fragment() {
    private val parentModel: ClaimHeliumViewModel by activityViewModels()
    private val model: ClaimHeliumVerifyViewModel by activityViewModels()
    private val navigator: Navigator by inject()

    private lateinit var binding: FragmentClaimHeliumVerifyBinding

    // Register the launcher and result handler for QR code scanner
    private val barcodeLauncher =
        registerForActivityResult(ScanContract()) { result: ScanIntentResult ->
            result.contents.let {
                val scannedEUI = model.getEUIFromScanner(it)
                val scannedKey = model.getKeyFromScanner(it)
                binding.devKey.setText(scannedKey)
                binding.devEUI.setText(scannedEUI)
                model.setDeviceKey(scannedKey)
                model.setDeviceEUI(scannedEUI)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentClaimHeliumVerifyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.devEUI.onTextChanged {
            binding.devEUIContainer.error = null
        }

        binding.devKey.onTextChanged {
            binding.devKeyContainer.error = null
        }

        binding.scan.setOnClickListener {
            barcodeLauncher.launch(ScanOptions().setBeepEnabled(false))
        }

        binding.cancel.setOnClickListener {
            parentModel.cancel()
        }

        binding.verify.setOnClickListener {
            model.checkAndVerify(
                binding.devEUI.text.toString().trim(),
                binding.devKey.text.toString().trim()
            )
            // TODO: Remove this
            navigator.showHeliumPairingStatus(requireActivity().supportFragmentManager)
        }

        model.onDevEUIError().observe(viewLifecycleOwner) {
            if (it) {
                binding.devEUIContainer.error = getString(R.string.invalid_dev_eui)
            }
        }

        model.onDevKeyError().observe(viewLifecycleOwner) {
            if (it) {
                binding.devKeyContainer.error = getString(R.string.invalid_dev_key)
            }
        }

        model.onVerifyError().observe(viewLifecycleOwner) {
            if (it) {
                binding.errorCard.htmlMessage(R.string.wrong_combination_message) {
                    navigator.sendSupportEmail(
                        context = context,
                        subject = getString(R.string.support_email_subject_helium_verify_failed),
                        body = getString(
                            R.string.support_email_body_claiming_helium,
                            model.getDevEUI(),
                            model.getDeviceKey()
                        )
                    )
                }
                binding.errorCard.show()
            }
        }
    }
}
