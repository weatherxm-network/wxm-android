package com.weatherxm.util

import java.text.DecimalFormat
import kotlin.math.floor

@Suppress("MagicNumber")
object UnitConverter {

    private val CARDINAL_VALUES = listOf(
        "N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
        "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"
    )

    fun celsiusToFahrenheit(celsius: Float): Float {
        return celsius * 9 / 5 + 32
    }

    /*
     * Unexpected numbers show up on History Charts
     * because of this math expression leading to results like
     * {0.001in} or {0.0005in}, therefore we format and round the numbers here as we want
     * maximum 2 decimals on inches
     */
    fun millimetersToInches(mm: Float): Float {
        val resultAsString = DecimalFormat("0.00").format((mm / 25.4F))
        return if (resultAsString.contains(",")) {
            // 0,00 in `toFloat()` throws NumberFormatException whereas 0.00 does not.
            resultAsString.replace(",", ".").toFloat()
        } else {
            resultAsString.toFloat()
        }
    }

    fun hpaToInHg(hpa: Float): Float {
        return hpa * 0.0295F
    }

    fun msToKmh(ms: Float): Float {
        return ms * 3.6F
    }

    fun msToMph(ms: Float): Float {
        return ms * 2.237F
    }

    fun msToKnots(ms: Float): Float {
        return ms * 1.944F
    }

    fun msToBeaufort(ms: Float): Int {
        return when {
            ms < 0.2 -> 0
            ms < 1.5 -> 1
            ms < 3.3 -> 2
            ms < 5.4 -> 3
            ms < 7.9 -> 4
            ms < 10.7 -> 5
            ms < 13.8 -> 6
            ms < 17.1 -> 7
            ms < 20.7 -> 8
            ms < 24.4 -> 9
            ms < 28.4 -> 10
            ms < 32.6 -> 11
            else -> 12
        }
    }

    fun degreesToCardinal(value: Int): String {
        return CARDINAL_VALUES[getIndexOfCardinal(value)]
    }

    // Get the index of the cardinal
    fun getIndexOfCardinal(value: Int): Int {
        val normalized = floor((value / 22.5) + 0.5).toInt()
        return normalized.mod(16)
    }
}


