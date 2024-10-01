package com.weatherxm.util

import android.icu.text.CompactDecimalFormat
import android.icu.text.NumberFormat
import com.weatherxm.ui.common.Contracts.EMPTY_VALUE
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat

object NumberUtils : KoinComponent {
    private val compactDecimalFormat: CompactDecimalFormat by inject()
    private val numberFormat: NumberFormat by inject()

    private const val DIVISOR_WEI_TO_ETH = "1000000000000000000"
    private const val ETH_DECIMALS = 18
    private const val GROUPING_SIZE = 3

    fun compactNumber(number: Number?): String {
        return number?.let {
            compactDecimalFormat.format(number)
        } ?: EMPTY_VALUE
    }

    fun formatNumber(number: Number?, decimals: Int = 0): String {
        return number?.let {
            numberFormat.minimumFractionDigits = decimals
            numberFormat.maximumFractionDigits = decimals
            numberFormat.format(it)
        } ?: EMPTY_VALUE
    }

    fun formatTokens(amount: Float?): String {
        return amount?.let {
            formatTokens(it.toBigDecimal())
        } ?: EMPTY_VALUE
    }

    fun formatTokens(amount: BigDecimal): String {
        val decimalFormat = DecimalFormat("0.00")
        decimalFormat.roundingMode = RoundingMode.HALF_UP
        decimalFormat.groupingSize = GROUPING_SIZE
        decimalFormat.isGroupingUsed = true
        return decimalFormat.format(amount)
    }

    @Suppress("MagicNumber")
    fun weiToETH(amount: BigDecimal): BigDecimal {
        /**
         * Mandatory otherwise we get a result of 0E-18 instead of 0
         */
        if (amount == BigDecimal.ZERO) {
            return BigDecimal.ZERO
        }
        return amount.divide(BigDecimal(DIVISOR_WEI_TO_ETH), ETH_DECIMALS, RoundingMode.HALF_UP)
    }

    fun roundToDecimals(value: Number, decimals: Int = 1): Float {
        return value.toFloat().toBigDecimal().setScale(decimals, RoundingMode.HALF_UP).toFloat()
    }
}


