package com.smartfarm.mobile

import com.smartfarm.mobile.features.insights.chartFraction
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class FarmInsightsChartTest {
    @Test fun zeroAndInvalidMaximumStayZero() {
        assertEquals(0f, chartFraction(BigDecimal("20"), BigDecimal.ZERO), 0.00001f)
        assertEquals(0f, chartFraction(BigDecimal.ZERO, BigDecimal("20")), 0.00001f)
        assertEquals(0f, chartFraction(BigDecimal("-2"), BigDecimal("20")), 0.00001f)
    }
    @Test fun barsStayWithinZeroToOne() {
        assertEquals(0.5f, chartFraction(BigDecimal("25"), BigDecimal("50")), 0.00001f)
        assertEquals(1f, chartFraction(BigDecimal("75"), BigDecimal("50")), 0.00001f)
        assertEquals(1f, chartFraction(BigDecimal("20"), BigDecimal("20")), 0.00001f)
    }
}
