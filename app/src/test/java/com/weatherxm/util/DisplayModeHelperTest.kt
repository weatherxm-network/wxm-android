package com.weatherxm.util

import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.content.res.Resources
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.analytics.AnalyticsWrapper
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.core.spec.style.scopes.BehaviorSpecWhenContainerScope
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify

class DisplayModeHelperTest : BehaviorSpec({
    val resources = mockk<Resources>()
    val configuration = mockk<Configuration>()
    val sharedPreferences = mockk<SharedPreferences>()
    val analyticsWrapper = mockk<AnalyticsWrapper>()
    val displayModeHelper = DisplayModeHelper(resources, sharedPreferences, analyticsWrapper)

    val theme = "theme"
    val system = "system"
    val dark = "dark"
    val light = "light"

    beforeSpec {
        every { resources.configuration } returns configuration
        every { resources.getString(R.string.key_theme) } returns theme
        every { resources.getString(R.string.dark_value) } returns dark
        every { resources.getString(R.string.light_value) } returns light
        every { resources.getString(R.string.system_value) } returns system
        every { analyticsWrapper.setDisplayMode(any()) } just Runs
        mockkStatic(AppCompatDelegate::class)
        every { AppCompatDelegate.setDefaultNightMode(any()) } just Runs
    }

    suspend fun BehaviorSpecWhenContainerScope.testDisplayMode(
        mode: String,
        setDefaultNightMode: Int
    ) {
        // Mock response from shared preferences which acts as "current theme" value
        every { sharedPreferences.getString(theme, system) } returns mode

        then("The setter should work properly") {
            displayModeHelper.setDisplayMode(mode)
            verify(exactly = 1) {
                AppCompatDelegate.setDefaultNightMode(setDefaultNightMode)
            }
        }
        and("The getter should work properly") {
            displayModeHelper.getDisplayMode() shouldBe mode
        }
        and("The isSystem() should work properly") {
            displayModeHelper.isSystem() shouldBe (mode == system)
        }
        and("The isDarkModeEnabled() should work properly") {
            if (mode == system) {
                When("The system is dark") {
                    configuration.uiMode = UI_MODE_NIGHT_YES
                    displayModeHelper.isDarkModeEnabled() shouldBe true
                }
                When("The system is light") {
                    configuration.uiMode = UI_MODE_NIGHT_NO
                    displayModeHelper.isDarkModeEnabled() shouldBe false
                }
            } else {
                displayModeHelper.isDarkModeEnabled() shouldBe (mode == dark)
            }
        }
        and("The updateDisplayModeInAnalytics() should work properly") {
            val analyticsProperty = when (mode) {
                dark -> AnalyticsService.UserProperty.DARK.propertyName
                light -> AnalyticsService.UserProperty.LIGHT.propertyName
                else -> AnalyticsService.UserProperty.SYSTEM.propertyName
            }
            verify(exactly = 1) { analyticsWrapper.setDisplayMode(analyticsProperty) }
        }
    }

    context("Perform display mode related tests (set/get etc)") {
        given("a display mode") {
            var displayMode: String
            When("SYSTEM") {
                displayMode = system
                testDisplayMode(displayMode, MODE_NIGHT_FOLLOW_SYSTEM)
            }
            When("DARK") {
                displayMode = dark
                testDisplayMode(displayMode, MODE_NIGHT_YES)
            }
            When("LIGHT") {
                displayMode = light
                testDisplayMode(displayMode, MODE_NIGHT_NO)
            }
        }
        given("no selected display mode") {
            // Re-mock AppCompatDelegate to reset calls and use the `exactly` arg below
            mockkStatic(AppCompatDelegate::class)
            every { AppCompatDelegate.setDefaultNightMode(any()) } just Runs
            every { sharedPreferences.getString(theme, system) } returns system
            displayModeHelper.setDisplayMode()
            then("System should be set") {
                verify(exactly = 1) {
                    AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_FOLLOW_SYSTEM)
                }
            }

        }
    }
})
