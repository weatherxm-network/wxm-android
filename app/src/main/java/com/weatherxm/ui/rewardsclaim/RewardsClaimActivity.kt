package com.weatherxm.ui.rewardsclaim

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.weatherxm.BuildConfig
import com.weatherxm.R
import com.weatherxm.databinding.ActivityWebviewBinding
import com.weatherxm.ui.common.Contracts.ARG_TOKEN_CLAIMED_AMOUNT
import com.weatherxm.ui.common.Contracts.ARG_WALLET_REWARDS
import com.weatherxm.ui.common.UIWalletRewards
import com.weatherxm.ui.common.toast
import com.weatherxm.util.applyInsets
import org.koin.core.component.KoinComponent
import timber.log.Timber

class RewardsClaimActivity : AppCompatActivity(), KoinComponent {
    private lateinit var binding: ActivityWebviewBinding
    private val model: RewardsClaimViewModel by viewModels()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWebviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.applyInsets()
        binding.toolbar.title = getString(R.string.claim_rewards)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        val rewardsData = intent?.extras?.getParcelable<UIWalletRewards>(ARG_WALLET_REWARDS)
        if (rewardsData == null) {
            Timber.d("Could not start RewardsClaimActivity. Rewards Data is null.")
            toast(R.string.error_generic_message)
            finish()
            return
        }

        binding.webview.settings.javaScriptEnabled = true
        binding.webview.settings.domStorageEnabled = true

        binding.webview.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val isHttpsUrl = request?.url.toString().startsWith("https", false)
                val isRedirectUrl = model.isRedirectUrl(request?.url)
                return if (!isHttpsUrl && !isRedirectUrl) {
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, request?.url))
                    } catch (e: ActivityNotFoundException) {
                        Timber.d(e, "[CLAIM DApp] Could not open the respective wallet.")
                        toast(R.string.error_cannot_open_wallet)
                    }
                    true
                } else if (isRedirectUrl) {
                    Timber.d("[CLAIM DApp] Got redirect URL: ${request?.url?.toString()}")
                    setResult(
                        Activity.RESULT_OK,
                        Intent().putExtra(
                            ARG_TOKEN_CLAIMED_AMOUNT, model.getAmountFromRedirectUrl(request?.url)
                        )
                    )
                    finish()
                    true
                } else {
                    Timber.d("[CLAIM DApp] Got URL: ${request?.url?.toString()}")
                    false
                }
            }
        }

        val queryParams = model.getQueryParams(rewardsData)
        binding.webview.loadUrl("${BuildConfig.CLAIM_APP_URL}$queryParams")
    }
}
