
package com.breadbitt.service

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Calculates bakery order totals using Kotlin.
 * Designed to work alongside the existing Java services.
 */
class OrderTotalCalculator {

    fun calculateItemTotal(
        unitPrice: BigDecimal,
        quantity: Int
    ): BigDecimal {
        require(unitPrice >= BigDecimal.ZERO) {
            "Unit price cannot be negative"
        }

        require(quantity > 0) {
            "Quantity must be greater than zero"
        }

        return unitPrice
            .multiply(BigDecimal.valueOf(quantity.toLong()))
            .setScale(2, RoundingMode.HALF_UP)
    }
}
