package kr.hhplus.be.server.support.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class CommonFormatterTest {
    @Test
    @DisplayName("unixTime toLongYYYYMMDD 테스트")
    fun toLongYYYYMMDDTest() {
        val localDate = CommonFormatter.toLocalDate(1700000000000)
        val longYYYYMMDD = CommonFormatter.toLongYYYYMMDD(localDate)
        assertEquals(20231114, longYYYYMMDD)
    }

    @Test
    @DisplayName("unixTime toLocalDate 테스트")
    fun toLocalDateTest() {
        val localDate = CommonFormatter.toLocalDate(1700000000000)
        assertEquals(2023, localDate.year)
        assertEquals(11, localDate.monthValue)
        assertEquals(14, localDate.dayOfMonth)
    }

    @Test
    @DisplayName("unixTime toLocalDateTime 테스트")
    fun toLocalDateTimeTest() {
        val localDateTime = CommonFormatter.toLocalDateTime(1700000000000)
        assertEquals(2023, localDateTime.year)
        assertEquals(11, localDateTime.monthValue)
        assertEquals(14, localDateTime.dayOfMonth)
        assertEquals(22, localDateTime.hour)
        assertEquals(13, localDateTime.minute)
        assertEquals(20, localDateTime.second)
    }
}
