package com.weatherxm.ui.widgets.currentweather

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
import android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID
import android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.preference.PreferenceManager
import com.weatherxm.R
import com.weatherxm.data.services.CacheService.Companion.KEY_CURRENT_WEATHER_WIDGET_IDS
import com.weatherxm.ui.common.Contracts
import com.weatherxm.ui.common.Contracts.ARG_DEVICE
import com.weatherxm.ui.common.Contracts.ARG_IS_CUSTOM_APPWIDGET_UPDATE
import com.weatherxm.ui.common.Contracts.ARG_WIDGET_TYPE
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.empty
import com.weatherxm.ui.common.parcelable
import com.weatherxm.ui.widgets.WidgetType
import com.weatherxm.ui.widgets.onDevice
import com.weatherxm.ui.widgets.onError
import com.weatherxm.ui.widgets.onShouldLogin
import com.weatherxm.ui.widgets.onShouldSelectStation
import com.weatherxm.usecases.WidgetCurrentWeatherUseCase
import com.weatherxm.util.WidgetHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Implementation of Current Weather Widget Tile functionality.
 */
class CurrentWeatherWidgetTile : AppWidgetProvider(), KoinComponent {
    private val usecase: WidgetCurrentWeatherUseCase by inject()
    private val widgetHelper: WidgetHelper by inject()

    @Suppress("MagicNumber")
    override fun onReceive(context: Context, intent: Intent?) {
        // These variables are useful for identifying what type of update to do
        val extras = intent?.extras
        val appWidgetId =
            extras?.getInt(EXTRA_APPWIDGET_ID, INVALID_APPWIDGET_ID) ?: INVALID_APPWIDGET_ID
        val device = extras?.parcelable<UIDevice>(ARG_DEVICE)
        val shouldLogin = extras?.getBoolean(Contracts.ARG_WIDGET_SHOULD_LOGIN, false)
        val shouldSelectStation =
            extras?.getBoolean(Contracts.ARG_WIDGET_SHOULD_SELECT_STATION, false)
        val onJustLoggedIn = extras?.getBoolean(Contracts.ARG_WIDGET_ON_LOGGED_IN, false)
        val widgetIdsFromIntent = extras?.getIntArray(AppWidgetManager.EXTRA_APPWIDGET_IDS)

        val validWidgetTypeForUpdate =
            extras?.parcelable<WidgetType>(ARG_WIDGET_TYPE) == WidgetType.CURRENT_WEATHER_TILE

        /**
         * Only update widget on actions we have triggered:
         * a. Creation of widget
         * b. Login/Logout
         * c. Work Manager Update
         */
        val shouldUpdate = intent?.action == ACTION_APPWIDGET_UPDATE
            && extras?.getBoolean(ARG_IS_CUSTOM_APPWIDGET_UPDATE) ?: false

        if (!shouldUpdate && (widgetIdsFromIntent == null || widgetIdsFromIntent.isEmpty())) {
            return
        } else if (!shouldUpdate) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val ids = prefs.getStringSet(KEY_CURRENT_WEATHER_WIDGET_IDS, setOf())

            /**
             * The first line covers the case of a user clearing app storage & cache so we reset
             * the widget to the "Select Station" state
             */
            if (ids.isNullOrEmpty()) {
                CurrentWeatherWidgetWorkerUpdate.clearAllWidgets(context, widgetHelper)
            } else {
                CurrentWeatherWidgetWorkerUpdate.restartAllWorkers(context)
            }
            return
        }

        if (appWidgetId != INVALID_APPWIDGET_ID && validWidgetTypeForUpdate) {
            /**
             * Fix when adding a widget for the first time, without this delay that widget won't be
             * able to render correctly. So the delay here is an ugly, but working fix.
             */
            widgetHelper.getWidgetIds().onRight {
                if (it.size == 1) {
                    runBlocking {
                        delay(500L)
                    }
                }
            }

            updateWidget(context, shouldLogin, shouldSelectStation, device, appWidgetId)
        } else if (widgetIdsFromIntent != null && widgetIdsFromIntent.isNotEmpty()) {
            updateAllWidgets(context, shouldLogin, onJustLoggedIn, widgetIdsFromIntent)
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        CurrentWeatherWidgetWorkerUpdate.restartAllWorkers(context)
    }

    override fun onDeleted(context: Context?, appWidgetIds: IntArray?) {
        super.onDeleted(context, appWidgetIds)
        if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
            usecase.removeWidgetId(appWidgetIds[0])
        }
    }

    private fun updateWidget(
        context: Context,
        shouldLogin: Boolean?,
        shouldSelectStation: Boolean?,
        device: UIDevice?,
        appWidgetId: Int
    ) {
        val widgetManager = AppWidgetManager.getInstance(context)
        val remoteViews = RemoteViews(context.packageName, R.layout.widget_current_weather_tile)

        if (shouldLogin == true) {
            remoteViews.onShouldLogin(context, widgetManager, appWidgetId)
        } else if (shouldSelectStation == true) {
            remoteViews.onShouldSelectStation(context, widgetManager, appWidgetId)
        } else if (device == null) {
            remoteViews.onError(widgetManager, appWidgetId)
        } else {
            remoteViews.onDevice(
                context, widgetManager, appWidgetId, device, WidgetType.CURRENT_WEATHER_TILE
            )
        }
    }

    private fun updateAllWidgets(
        context: Context,
        shouldLogin: Boolean?,
        onJustLoggedIn: Boolean?,
        widgetIdsFromIntent: IntArray
    ) {
        val widgetManager = AppWidgetManager.getInstance(context)
        val widgetsToUpdate = widgetIdsFromIntent.filter {
            widgetHelper.getWidgetTypeById(widgetManager, it).apply {
                if (this == null) usecase.removeWidgetId(it)
            } == WidgetType.CURRENT_WEATHER_TILE
        }

        if (shouldLogin == true) {
            val remoteViews = RemoteViews(context.packageName, R.layout.widget_current_weather_tile)
            widgetsToUpdate.forEach {
                remoteViews.onShouldLogin(context, widgetManager, it)
            }
        } else if (onJustLoggedIn == true) {
            widgetsToUpdate.forEach {
                CurrentWeatherWidgetWorkerUpdate.initAndStart(
                    context,
                    it,
                    usecase.getWidgetDevice(it) ?: String.empty()
                )
            }
        }
    }
}
