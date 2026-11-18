package com.example.kalkulator;

import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CalculatorEngineTest {

    @Test
    public void basicOperations_areAccurate() {
        assertEquals("15", CalculatorEngine.evaluate("10 + 5").formattedResult);
        assertEquals("5", CalculatorEngine.evaluate("10 − 5").formattedResult);
        assertEquals("50", CalculatorEngine.evaluate("10 × 5").formattedResult);
        assertEquals("2", CalculatorEngine.evaluate("10 ÷ 5").formattedResult);
    }

    @Test
    public void operatorPrecedence_isRespected() {
        assertEquals("14", CalculatorEngine.evaluate("2 + 3 × 4").formattedResult);
        assertEquals("20", CalculatorEngine.evaluate("(2 + 3) × 4").formattedResult);
    }

    @Test
    public void decimalPrecision_avoidsFloatingPointErrors() {
        assertEquals("0.3", CalculatorEngine.evaluate("0.1 + 0.2").formattedResult);
        assertEquals("2.5", CalculatorEngine.evaluate("5 ÷ 2").formattedResult);
    }

    @Test
    public void percentageAndNegativeNumbers_workCorrectly() {
        assertEquals("20", CalculatorEngine.evaluate("200 × 10%").formattedResult);
        assertEquals("0.5", CalculatorEngine.evaluate("50%").formattedResult);
        assertEquals("3", CalculatorEngine.evaluate("-5 + 8").formattedResult);
        assertEquals("-20", CalculatorEngine.evaluate("10 × -2").formattedResult);
    }

    @Test
    public void divisionByZero_returnsError() {
        CalculatorEngine.EvaluationResult res = CalculatorEngine.evaluate("10 ÷ 0");
        assertFalse(res.success);
        assertEquals("Tidak bisa dibagi 0", res.errorMessage);
    }

    @Test
    public void quickToolsCalculations_areAccurate() {
        BigDecimal pct = CalculatorEngine.calculatePercentage(
                new BigDecimal("15"), new BigDecimal("200"));
        assertEquals("30", CalculatorEngine.formatNumber(pct));

        CalculatorEngine.DiscountResult disc = CalculatorEngine.calculateDiscount(
                new BigDecimal("100000"), new BigDecimal("20"));
        assertEquals("20000", CalculatorEngine.formatNumber(disc.savings));
        assertEquals("80000", CalculatorEngine.formatNumber(disc.finalPrice));

        BigDecimal f = CalculatorEngine.celsiusToFahrenheit(new BigDecimal("100"));
        assertEquals("212", CalculatorEngine.formatNumber(f));

        BigDecimal c = CalculatorEngine.fahrenheitToCelsius(new BigDecimal("32"));
        assertEquals("0", CalculatorEngine.formatNumber(c));
    }
}
