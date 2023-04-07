package com.weatherxm.ui.devicehistory

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.weatherxm.R
import com.weatherxm.data.Status
import com.weatherxm.databinding.FragmentHistoryChartsBinding
import com.weatherxm.util.Weather
import com.weatherxm.util.clearHighlightValue
import com.weatherxm.util.getDatasetsNumber
import com.weatherxm.util.initializeHumidity24hChart
import com.weatherxm.util.initializePrecipitation24hChart
import com.weatherxm.util.initializePressure24hChart
import com.weatherxm.util.initializeTemperature24hChart
import com.weatherxm.util.initializeUV24hChart
import com.weatherxm.util.initializeWind24hChart
import com.weatherxm.util.onHighlightValue
import timber.log.Timber
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class HistoryChartsFragment : Fragment() {

    fun interface SwipeRefreshCallback {
        fun onSwipeRefresh()
    }

    private val model: HistoryChartsViewModel by activityViewModels()
    private var callback: SwipeRefreshCallback? = null
    private lateinit var binding: FragmentHistoryChartsBinding

    private var onAutoHighlighting = false

    @Suppress("SwallowedException")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            callback = activity as SwipeRefreshCallback
        } catch (e: ClassCastException) {
            Timber.w("${activity?.localClassName} does not implement SwipeRefreshCallback")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentHistoryChartsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding.displayTimeNotice) {
            visibility = model.device.timezone?.let {
                text = getString(R.string.displayed_times, it)
                View.VISIBLE
            } ?: View.GONE
        }

        binding.swiperefresh.setOnRefreshListener {
            callback?.onSwipeRefresh()
        }

        model.charts().observe(viewLifecycleOwner) { resource ->
            Timber.d("Charts updated: ${resource.status}")
            when (resource.status) {
                Status.SUCCESS -> {
                    binding.swiperefresh.isRefreshing = false
                    val isEmpty = resource.data == null || resource.data.isEmpty()
                    if (isEmpty) {
                        binding.chartsView.visibility = View.GONE
                        binding.empty.clear()
                        binding.empty.title(getString(R.string.empty_history_day_title))
                        binding.empty.subtitle(
                            resource.data?.date?.let {
                                getString(
                                    R.string.empty_history_day_subtitle_with_day,
                                    it.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
                                )
                            } ?: getString(R.string.empty_history_day_subtitle)
                        )
                        binding.empty.animation(R.raw.anim_empty_generic)
                        binding.empty.visibility = View.VISIBLE
                    } else {
                        resource.data?.let {
                            Timber.d("Updating charts for ${it.date}")
                            clearCharts()
                            initTemperatureChart(it.temperature, it.feelsLike)
                            initWindChart(it.windSpeed, it.windGust, it.windDirection)
                            initPrecipitationChart(it.precipitation, it.precipitationAccumulated)
                            initHumidityChart(it.humidity)
                            initPressureChart(it.pressure)
                            initUvChart(it.uv)
                            if (model.isTodayShown()) {
                                // Auto highlight latest entry
                                autoHighlightCharts(model.getLatestChartEntry(it.temperature))
                            } else {
                                // Auto highlight past dates on 00:00
                                autoHighlightCharts(0F)
                            }
                        }
                        binding.empty.visibility = View.GONE
                        binding.chartsView.visibility = View.VISIBLE
                    }
                }
                Status.ERROR -> {
                    Timber.d("Got error: $resource.message")
                    binding.swiperefresh.isRefreshing = false
                    binding.chartsView.visibility = View.GONE
                    binding.empty.clear()
                    binding.empty.animation(R.raw.anim_error)
                    binding.empty.title(getString(R.string.error_history_no_data_on_day))
                    binding.empty.subtitle(resource.message)
                    binding.empty.action(getString(R.string.action_retry))
                    binding.empty.listener { callback?.onSwipeRefresh() }
                    binding.empty.visibility = View.VISIBLE
                }
                Status.LOADING -> {
                    if (binding.swiperefresh.isRefreshing) {
                        binding.empty.clear()
                        binding.empty.visibility = View.GONE
                    } else {
                        binding.chartsView.visibility = View.GONE
                        binding.empty.clear()
                        binding.empty.animation(R.raw.anim_loading)
                        binding.empty.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun clearCharts() {
        binding.chartTemperature.onClearHighlight()
        binding.chartTemperature.getChart().clearHighlightValue()
        binding.chartTemperature.getChart().clear()
        binding.chartPressure.onClearHighlight()
        binding.chartPressure.getChart().clearHighlightValue()
        binding.chartPressure.getChart().clear()
        binding.chartHumidity.onClearHighlight()
        binding.chartHumidity.getChart().clearHighlightValue()
        binding.chartHumidity.getChart().clear()
        binding.chartPrecipitation.onClearHighlight()
        binding.chartPrecipitation.getChart().clearHighlightValue()
        binding.chartPrecipitation.getChart().clear()
        binding.chartWind.onClearHighlight()
        binding.chartWind.getChart().clearHighlightValue()
        binding.chartWind.getChart().clear()
        binding.chartUv.onClearHighlight()
        binding.chartUv.getChart().clearHighlightValue()
        binding.chartUv.getChart().clear()
    }

    private fun initTemperatureChart(temperatureData: LineChartData, feelsLikeData: LineChartData) {
        if (temperatureData.isDataValid() && feelsLikeData.isDataValid()) {
            binding.chartTemperature
                .getChart()
                .initializeTemperature24hChart(temperatureData, feelsLikeData)
            binding.chartTemperature.getChart().setOnChartValueSelectedListener(
                object : OnChartValueSelectedListener {
                    override fun onValueSelected(e: Entry?, h: Highlight?) {
                        if (e != null) {
                            val time = temperatureData.timestamps[e.x.toInt()]

                            /**
                             * Ignore conversion of the temperature because the data on the Y axis
                             * is already converted to the user's preference so we need to handle
                             * only the decimals
                             */
                            val temperature = Weather.getFormattedTemperature(
                                e.y, decimals = 1, ignoreConversion = true
                            )
                            val feelsLike = Weather.getFormattedTemperature(
                                feelsLikeData.entries[e.x.toInt()].y,
                                decimals = 1,
                                ignoreConversion = true
                            )
                            binding.chartTemperature.onHighlightedData(time, temperature, feelsLike)

                            autoHighlightCharts(e.x)
                        } else {
                            binding.chartTemperature.onClearHighlight()
                        }
                    }

                    override fun onNothingSelected() {
                        // Do Nothing
                    }
                })
        } else {
            showNoDataText(binding.chartTemperature.getChart())
        }
    }

    private fun initHumidityChart(data: LineChartData) {
        if (data.isDataValid()) {
            binding.chartHumidity.getChart().initializeHumidity24hChart(data)
            binding.chartHumidity.getChart().setOnChartValueSelectedListener(
                object : OnChartValueSelectedListener {
                    override fun onValueSelected(e: Entry?, h: Highlight?) {
                        if (e != null) {
                            val time = data.timestamps[e.x.toInt()]
                            val humidity = Weather.getFormattedHumidity(e.y.toInt())
                            binding.chartHumidity.onHighlightedData(time, humidity)

                            autoHighlightCharts(e.x)
                        } else {
                            binding.chartHumidity.onClearHighlight()
                        }
                    }

                    override fun onNothingSelected() {
                        // Do Nothing
                    }
                })
        } else {
            showNoDataText(binding.chartHumidity.getChart())
        }
    }

    private fun initPressureChart(data: LineChartData) {
        if (data.isDataValid()) {
            binding.chartPressure.getChart().initializePressure24hChart(data)
            binding.chartPressure.getChart().setOnChartValueSelectedListener(
                object : OnChartValueSelectedListener {
                    override fun onValueSelected(e: Entry?, h: Highlight?) {
                        if (e != null) {
                            val time = data.timestamps[e.x.toInt()]
                            val pressure =
                                Weather.getFormattedPressure(e.y, ignoreConversion = true)
                            binding.chartPressure.onHighlightedData(time, pressure)

                            autoHighlightCharts(e.x)
                        } else {
                            binding.chartPressure.onClearHighlight()
                        }
                    }

                    override fun onNothingSelected() {
                        // Do Nothing
                    }
                })
        } else {
            showNoDataText(binding.chartPressure.getChart())
        }
    }

    private fun initUvChart(data: LineChartData) {
        if (data.isDataValid()) {
            binding.chartUv.getChart().initializeUV24hChart(data)
            binding.chartUv.getChart().setOnChartValueSelectedListener(
                object : OnChartValueSelectedListener {
                    override fun onValueSelected(e: Entry?, h: Highlight?) {
                        if (e != null) {
                            val time = data.timestamps[e.x.toInt()]
                            val uv = Weather.getFormattedUV(e.y.toInt())
                            binding.chartUv.onHighlightedData(time, uv)

                            autoHighlightCharts(e.x)
                        } else {
                            binding.chartUv.onClearHighlight()
                        }
                    }

                    override fun onNothingSelected() {
                        // Do Nothing
                    }
                })
        } else {
            showNoDataText(binding.chartUv.getChart())
        }
    }

    private fun initPrecipitationChart(rateData: LineChartData, accumulatedData: LineChartData) {
        if (rateData.isDataValid() && accumulatedData.isDataValid()) {
            binding.chartPrecipitation
                .getChart()
                .initializePrecipitation24hChart(rateData, accumulatedData)
            binding.chartPrecipitation.getChart().setOnChartValueSelectedListener(
                object : OnChartValueSelectedListener {
                    override fun onValueSelected(e: Entry?, h: Highlight?) {
                        if (e != null) {
                            val time = accumulatedData.timestamps[e.x.toInt()]
                            val accumulated = Weather.getFormattedPrecipitation(
                                accumulatedData.entries[e.x.toInt()].y,
                                isRainRate = false,
                                ignoreConversion = true
                            )
                            val rate = Weather.getFormattedPrecipitation(
                                rateData.entries[e.x.toInt()].y,
                                isRainRate = true,
                                ignoreConversion = true
                            )
                            binding.chartPrecipitation.onHighlightedData(time, rate, accumulated)

                            autoHighlightCharts(e.x)
                        } else {
                            binding.chartPrecipitation.onClearHighlight()
                        }
                    }

                    override fun onNothingSelected() {
                        // Do Nothing
                    }
                })
        } else {
            showNoDataText(binding.chartPrecipitation.getChart())
        }
    }

    private fun initWindChart(
        windSpeedData: LineChartData, windGustData: LineChartData, windDirectionData: LineChartData
    ) {
        if (windSpeedData.isDataValid()
            && windGustData.isDataValid()
            && windDirectionData.isDataValid()
        ) {
            binding.chartWind
                .getChart()
                .initializeWind24hChart(windSpeedData, windGustData)
            binding.chartWind.getChart().setOnChartValueSelectedListener(
                object : OnChartValueSelectedListener {
                    override fun onValueSelected(e: Entry?, h: Highlight?) {
                        if (e != null) {
                            val time = windSpeedData.timestamps[e.x.toInt()]
                            val windSpeed = Weather.getFormattedWind(
                                e.y,
                                windDirectionData.entries[e.x.toInt()].y.toInt(),
                                ignoreConversion = true
                            )
                            val windGust = Weather.getFormattedWind(
                                windGustData.entries[e.x.toInt()].y,
                                windDirectionData.entries[e.x.toInt()].y.toInt(),
                                ignoreConversion = true
                            )
                            binding.chartWind.onHighlightedData(time, windSpeed, windGust)

                            autoHighlightCharts(e.x)
                        } else {
                            binding.chartWind.onClearHighlight()
                        }
                    }

                    override fun onNothingSelected() {
                        // Do Nothing
                    }
                })
        } else {
            showNoDataText(binding.chartWind.getChart())
        }
    }

    private fun autoHighlightCharts(x: Float) {
        if (onAutoHighlighting) return
        onAutoHighlighting = true
        with(binding) {
            chartTemperature.getChart()
                .onHighlightValue(x, chartTemperature.getChart().getDatasetsNumber() / 2)
            chartPrecipitation.getChart()
                .onHighlightValue(x, chartPrecipitation.getChart().getDatasetsNumber() / 2)
            chartWind.getChart().onHighlightValue(x, chartWind.getChart().getDatasetsNumber() / 2)
            chartHumidity.getChart().onHighlightValue(x, 0)
            chartPressure.getChart().onHighlightValue(x, 0)
            chartUv.getChart().onHighlightValue(x, 0)
        }
        onAutoHighlighting = false
    }

    private fun showNoDataText(lineChart: LineChart) {
        lineChart.setNoDataText(getString(R.string.error_history_no_data_chart_found))
        context?.getColor(R.color.colorOnSurface)?.let {
            lineChart.setNoDataTextColor(it)
        }
        lineChart.setNoDataTextTypeface(Typeface.DEFAULT_BOLD)
    }
}
