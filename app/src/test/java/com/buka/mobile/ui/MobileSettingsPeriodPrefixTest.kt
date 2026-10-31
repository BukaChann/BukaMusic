package com.buka.mobile.ui

import org.junit.Assert.assertTrue
import org.junit.Test

class MobileSettingsPeriodPrefixTest {
    @Test
    fun allPeriodsProduceDatePrefixesWithoutFormatExceptions() {
        for (index in 0..3) {
            val prefix = periodPrefix(index)
            assertTrue(
                "Unexpected prefix for period $index: $prefix",
                Regex("""^\d{4}(-\d{2})?(-\d{2})?$""").matches(prefix),
            )
        }
    }
}
