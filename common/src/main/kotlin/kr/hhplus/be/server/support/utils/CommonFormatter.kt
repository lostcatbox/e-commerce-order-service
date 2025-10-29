package kr.hhplus.be.server.support.utils

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class CommonFormatter {
    companion object {
        /**
         * LocalDate를 YYYYMMDD 형태의 Long으로 변환
         * @param date 변환할 LocalDate 객체
         * @return YYYYMMDD 형태의 Long 값
         */
        fun toLongYYYYMMDD(date: LocalDate): Long {
            val ofPattern = DateTimeFormatter.ofPattern("yyyyMMdd")
            return date.format(ofPattern).toLong()
        }

        /**
         * UNIX 타임스탬프(밀리초)를 LocalDate로 변환
         * @param unixTime 변환할 UNIX 타임스탬프 (밀리초)
         * @return 변환된 LocalDate 객체
         */
        fun toLocalDate(unixTime: Long): LocalDate = Instant.ofEpochMilli(unixTime).atOffset(ZoneOffset.UTC).toLocalDate()

        /**
         * UNIX 타임스탬프(밀리초)를 LocalDateTime으로 변환
         * @param unixTime 변환할 UNIX 타임스탬프 (밀리초)
         * @return 변환된 LocalDateTime 객체
         */
        fun toLocalDateTime(unixTime: Long): LocalDateTime = Instant.ofEpochMilli(unixTime).atOffset(ZoneOffset.UTC).toLocalDateTime()
    }
}
