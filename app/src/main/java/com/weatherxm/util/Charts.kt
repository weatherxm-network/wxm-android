package com.weatherxm.util

import android.content.Context
import android.content.res.Resources
import android.view.MotionEvent
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.weatherxm.R
import com.weatherxm.ui.BarChartData
import com.weatherxm.ui.LineChartData
import com.weatherxm.ui.common.show

private const val CHART_BOTTOM_OFFSET = 20F
private const val LINE_WIDTH = 2F
private const val POINT_SIZE = 2F
private const val MAXIMUMS_GRID_LINES_Y_AXIS = 4
private const val PRECIP_INCHES_GRANULARITY_Y_AXIS = 0.01F
private const val DEFAULT_GRANULARITY_Y_AXIS = 0.1F
private const val INHG_GRANULARITY_Y_AXIS = 0.01F
private const val TIME_GRANULARITY_X_AXIS = 3F

private fun LineChart.setDefaultSettings() {
    // General Chart Settings
    description.isEnabled = false
    extraBottomOffset = CHART_BOTTOM_OFFSET
    legend.isEnabled = false

    // Line and highlight Settings
    lineData.setDrawValues(false)
    // General Chart Settings

    // Y Axis settings
    axisLeft.isGranularityEnabled = true
    isScaleYEnabled = false
    axisRight.isEnabled = false
    axisLeft.gridColor = resources.getColor(R.color.chart_grid_color, context.theme)
    axisLeft.setLabelCount(MAXIMUMS_GRID_LINES_Y_AXIS, false)

    // X axis settings
    xAxis.position = XAxis.XAxisPosition.BOTTOM
    xAxis.setDrawAxisLine(false)
    xAxis.granularity = TIME_GRANULARITY_X_AXIS
    xAxis.gridColor = resources.getColor(R.color.chart_grid_color, context.theme)

    setOnTouchListener { _, event ->
        when (event.action) {
            MotionEvent.ACTION_UP -> {
                highlightValue(null)
            }
        }
        this.performClick()
    }
}

private fun LineDataSet.setDefaultSettings(context: Context, resources: Resources) {
    setDrawCircleHole(false)
    circleRadius = POINT_SIZE
    lineWidth = LINE_WIDTH
    mode = LineDataSet.Mode.CUBIC_BEZIER
    highLightColor = resources.getColor(R.color.highlighter, context.theme)
}

fun LineChart.initializeTemperature24hChart(chartData: LineChartData) {
    val dataSet = LineDataSet(chartData.entries, chartData.name)
    val lineData = LineData(dataSet)
    data = lineData

    // Set the default settings we want to all LineCharts
    setDefaultSettings()

    // General Chart Settings
    marker =
        CustomDefaultMarkerView(context, chartData.timestamps, chartData.name, chartData.unit, 1)

    // Line and highlight Settings
    dataSet.setDefaultSettings(context, resources)
    dataSet.color = resources.getColor(chartData.lineColor, context.theme)
    dataSet.setCircleColor(resources.getColor(chartData.lineColor, context.theme))

    // Y Axis settings

    // If max - min < 2 that means that the values are probably too close together.
    // Which causes a bug not showing labels on Y axis because granularity is set 1.
    // So this is a custom fix to change that granularity and show decimals at the Y labels
    if (dataSet.yMax - dataSet.yMin < 2) {
        axisLeft.granularity = DEFAULT_GRANULARITY_Y_AXIS
        axisLeft.valueFormatter = CustomYAxisFormatter(chartData.unit, 1)
    } else {
        axisLeft.valueFormatter = CustomYAxisFormatter(chartData.unit)
    }

    // X axis settings
    xAxis.valueFormatter = CustomXAxisFormatter(chartData.timestamps)
    show()
    notifyDataSetChanged()
}

fun LineChart.initializeHumidity24hChart(chartData: LineChartData) {
    val dataSet = LineDataSet(chartData.entries, chartData.name)
    val lineData = LineData(dataSet)
    data = lineData

    // Set the default settings we want to all LineCharts
    setDefaultSettings()

    // General Chart Settings
    marker =
        CustomDefaultMarkerView(context, chartData.timestamps, chartData.name, chartData.unit)

    // Line and highlight Settings
    dataSet.setDefaultSettings(context, resources)
    dataSet.color = resources.getColor(chartData.lineColor, context.theme)
    dataSet.setCircleColor(resources.getColor(chartData.lineColor, context.theme))

    // Y Axis settings
    axisLeft.valueFormatter = CustomYAxisFormatter(chartData.unit)

    // X axis settings
    xAxis.valueFormatter = CustomXAxisFormatter(chartData.timestamps)
    show()
    notifyDataSetChanged()
}

fun LineChart.initializePressure24hChart(chartData: LineChartData) {
    val dataSet = LineDataSet(chartData.entries, chartData.name)
    val lineData = LineData(dataSet)
    data = lineData

    // Set the default settings we want to all LineCharts
    setDefaultSettings()

    // In History, if hPa show 1 decimal, if inHg show 2, so we need this variable for such cases
    val inHgUsed = chartData.unit == resources.getString(R.string.pressure_inHg)

    // General Chart Settings
    val decimalsOnMarkerView = if (inHgUsed) 2 else 1
    marker =
        CustomDefaultMarkerView(
            context,
            chartData.timestamps,
            chartData.name,
            chartData.unit,
            decimalsOnMarkerView
        )

    // Line and highlight Settings
    dataSet.setDefaultSettings(context, resources)
    dataSet.color = resources.getColor(chartData.lineColor, context.theme)
    dataSet.setCircleColor(resources.getColor(chartData.lineColor, context.theme))

    // Y Axis settings

    /*
    * If max - min < 2 that means that the values are probably too close together.
    * Which causes a bug not showing labels on Y axis because granularity is set 1.
    * So this is a custom fix to change that granularity and show decimals at the Y labels.
    * Also custom fix if inHg is used to show 2 decimals instead of one, for the same reason
    */
    if (dataSet.yMax - dataSet.yMin < 2 && !inHgUsed) {
        axisLeft.granularity = DEFAULT_GRANULARITY_Y_AXIS
        axisLeft.valueFormatter = CustomYAxisFormatter(chartData.unit, 1)
    } else if (inHgUsed) {
        axisLeft.granularity = INHG_GRANULARITY_Y_AXIS
        axisLeft.valueFormatter = CustomYAxisFormatter(chartData.unit, 2)
    } else {
        axisLeft.valueFormatter = CustomYAxisFormatter(chartData.unit)
    }

    // X axis settings
    xAxis.valueFormatter = CustomXAxisFormatter(chartData.timestamps)
    show()
    notifyDataSetChanged()
}

/*
    precipProbabilityData is nullable because we have that data only on Forecast Charts.
    On history chart this is null
*/
fun LineChart.initializePrecipitation24hChart(precipIntensityData: LineChartData) {
    val dataSetPrecipIntensity = LineDataSet(precipIntensityData.entries, precipIntensityData.name)
    dataSetPrecipIntensity.axisDependency = YAxis.AxisDependency.LEFT

    // use ILineDataSet to have multiple lines in a chart (in case we have probability)
    val dataSets = mutableListOf<ILineDataSet>()

    dataSets.add(dataSetPrecipIntensity)

    val lineData = LineData(dataSets)
    data = lineData

    // Set the default settings we want to all LineCharts
    setDefaultSettings()

    // Marker view initialization
    val decimals = Weather.getDecimalsPrecipitation()
    marker = CustomDefaultMarkerView(
        context,
        precipIntensityData.timestamps,
        precipIntensityData.name,
        precipIntensityData.unit,
        decimals
    )

    // Precipitation Intensity Settings
    dataSetPrecipIntensity.setDefaultSettings(context, resources)
    dataSetPrecipIntensity.mode = LineDataSet.Mode.STEPPED
    dataSetPrecipIntensity.setDrawFilled(true)
    dataSetPrecipIntensity.color = resources.getColor(precipIntensityData.lineColor, context.theme)
    dataSetPrecipIntensity.setCircleColor(
        resources.getColor(precipIntensityData.lineColor, context.theme)
    )

    // Y Axis settings
    axisLeft.granularity =
        if (precipIntensityData.unit == resources.getString(R.string.precipitation_in)) {
            PRECIP_INCHES_GRANULARITY_Y_AXIS
        } else {
            DEFAULT_GRANULARITY_Y_AXIS
        }
    axisLeft.axisMinimum = dataSetPrecipIntensity.yMin
    axisLeft.valueFormatter =
        CustomYAxisFormatter(precipIntensityData.unit, Weather.getDecimalsPrecipitation())

    // X axis settings
    xAxis.valueFormatter = CustomXAxisFormatter(precipIntensityData.timestamps)
    show()
    notifyDataSetChanged()
}

fun LineChart.initializeWind24hChart(
    windSpeedData: LineChartData, windGustData: LineChartData, windDirectionData: LineChartData
) {
    val dataSetWindSpeed = LineDataSet(windSpeedData.entries, windSpeedData.name)
    val dataSetWindGust = LineDataSet(windGustData.entries, windGustData.name)

    dataSetWindSpeed.axisDependency = YAxis.AxisDependency.LEFT

    // use the interface ILineDataSet to have multiple lines in a chart
    val dataSets = mutableListOf<ILineDataSet>()
    dataSets.add(dataSetWindSpeed)
    dataSets.add(dataSetWindGust)
    val lineData = LineData(dataSets)
    data = lineData

    // Set the default settings we want to all LineCharts
    setDefaultSettings()

    // General Chart Settings
    legend.isEnabled = true
    legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
    marker = CustomWindMarkerView(
        context,
        windSpeedData.timestamps,
        windGustData.entries,
        windDirectionData.entries,
        windSpeedData.name,
        windGustData.name,
        windSpeedData.unit
    )

    // Wind Speed
    dataSetWindSpeed.setDefaultSettings(context, resources)
    dataSetWindSpeed.color = resources.getColor(windSpeedData.lineColor, context.theme)
    dataSetWindSpeed.setCircleColor(resources.getColor(windSpeedData.lineColor, context.theme))

    // Wind Gust Settings
    dataSetWindGust.setDefaultSettings(context, resources)
    dataSetWindGust.setDrawIcons(false)
    dataSetWindGust.isHighlightEnabled = false
    dataSetWindGust.color = resources.getColor(windGustData.lineColor, context.theme)
    dataSetWindGust.setCircleColor(resources.getColor(windGustData.lineColor, context.theme))

    // Y Axis settings
    axisLeft.axisMinimum = dataSetWindSpeed.yMin
    axisLeft.valueFormatter = CustomYAxisFormatter(windSpeedData.unit)

    // X axis settings
    xAxis.valueFormatter = CustomXAxisFormatter(windGustData.timestamps)
    show()
    notifyDataSetChanged()
}

fun BarChart.initializeUV24hChart(data: BarChartData) {
    val dataSet = BarDataSet(data.entries, data.name)
    val barData = BarData(dataSet)
    setData(barData)

    // General Chart Settings
    description.isEnabled = false
    legend.isEnabled = false
    marker = CustomDefaultMarkerView(context, data.timestamps, data.name, data.unit)
    extraBottomOffset = CHART_BOTTOM_OFFSET

    // Bar and highlight Settings
    barData.setDrawValues(false)
    dataSet.color = resources.getColor(R.color.uvIndex, context.theme)
    dataSet.highLightColor = resources.getColor(R.color.highlighter, context.theme)

    setOnTouchListener { _, event ->
        when (event.action) {
            MotionEvent.ACTION_UP -> {
                highlightValue(null)
            }
        }
        this.performClick()
    }

    // Y Axis settings
    axisLeft.axisMinimum = 0F
    axisLeft.isGranularityEnabled = true
    axisLeft.granularity = 1F
    axisLeft.valueFormatter = CustomYAxisFormatter(data.unit)
    axisRight.isEnabled = false
    isScaleYEnabled = false

    // X axis settings
    xAxis.position = XAxis.XAxisPosition.BOTTOM
    xAxis.setDrawAxisLine(false)
    xAxis.setDrawGridLines(false)
    xAxis.valueFormatter = CustomXAxisFormatter(data.timestamps)
    xAxis.granularity = TIME_GRANULARITY_X_AXIS
    show()
    notifyDataSetChanged()
}
