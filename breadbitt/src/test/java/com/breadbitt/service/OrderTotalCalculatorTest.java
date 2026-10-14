
package com.breadbitt.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class OrderTotalCalculatorTest {

    private final OrderTotalCalculator calculator = new OrderTotalCalculator();

    @Test
    void shouldCalculateTotalForMultipleItems() {
        BigDecimal result = calculator.calculateItemTotal(
                new BigDecimal("4.50"),
                3);

        assertEquals(new BigDecimal("13.50"), result);
    }

    @Test
    void shouldCalculateTotalForSingleItem() {
        BigDecimal result = calculator.calculateItemTotal(
                new BigDecimal("2.99"),
                1);

        assertEquals(new BigDecimal("2.99"), result);
    }

    @Test
    void shouldRoundTotalToTwoDecimalPlaces() {
        BigDecimal result = calculator.calculateItemTotal(
                new BigDecimal("1.235"),
                2);

        assertEquals(new BigDecimal("2.47"), result);
    }

    @Test
    void shouldRejectNegativePrice() {
        assertThrows(
                IllegalArgumentException.class,
                () -> calculator.calculateItemTotal(
                        new BigDecimal("-2.00"),
                        1));
    }

    @Test
    void shouldRejectZeroQuantity() {
        assertThrows(
                IllegalArgumentException.class,
                () -> calculator.calculateItemTotal(
                        new BigDecimal("3.00"),
                        0));
    }

    @Test
    void shouldRejectNegativeQuantity() {
        assertThrows(
                IllegalArgumentException.class,
                () -> calculator.calculateItemTotal(
                        new BigDecimal("3.00"),
                        -1));
    }
}
