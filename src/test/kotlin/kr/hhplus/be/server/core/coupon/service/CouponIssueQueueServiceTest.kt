package kr.hhplus.be.server.core.coupon.service

import com.fasterxml.jackson.databind.ObjectMapper
import kr.hhplus.be.server.core.coupon.domain.CouponIssueRequest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import org.mockito.kotlin.*
import org.springframework.data.redis.core.ListOperations
import org.springframework.data.redis.core.RedisTemplate

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CouponIssueQueueService 단위 테스트")
class CouponIssueQueueServiceTest {

    @Mock
    private lateinit var redisTemplate: RedisTemplate<String, String>

    @Mock
    private lateinit var listOperations: ListOperations<String, String>

    @Mock
    private lateinit var objectMapper: ObjectMapper

    @InjectMocks
    private lateinit var couponIssueQueueService: CouponIssueQueueService

    @BeforeEach
    fun setup() {
        clearInvocations(redisTemplate, listOperations, objectMapper)
        whenever(redisTemplate.opsForList()).thenReturn(listOperations)
    }

    @Test
    @DisplayName("쿠폰 발급 요청을 대기열에 추가 성공")
    fun `쿠폰 발급 요청을 대기열에 추가 성공`() {
        // given
        val userId = 1L
        val couponId = 100L
        val request = CouponIssueRequest.create(userId, couponId)
        val jsonString = """{"userId":1,"couponId":100,"requestId":"${request.requestId}","timestamp":${request.timestamp}}"""
        val queueKey = "coupon:issue:queue:$couponId"

        whenever(objectMapper.writeValueAsString(any<CouponIssueRequest>())).thenReturn(jsonString)

        // when
        val result = couponIssueQueueService.addCouponIssueRequest(userId, couponId)

        // then
        assertNotNull(result)
        assertTrue(result.startsWith("coupon-issue-$userId-$couponId-"))

        verify(objectMapper).writeValueAsString(any<CouponIssueRequest>())
        verify(listOperations).leftPush(queueKey, jsonString)
    }

    @Test
    @DisplayName("대기열에서 쿠폰 발급 요청 조회 및 제거 성공")
    fun `대기열에서 쿠폰 발급 요청 조회 및 제거 성공`() {
        // given
        val userId = 1L
        val couponId = 100L
        val request = CouponIssueRequest.create(userId, couponId)
        val jsonString = """{"userId":1,"couponId":100,"requestId":"${request.requestId}","timestamp":${request.timestamp}}"""
        val queueKey = "coupon:issue:queue:$couponId"

        whenever(redisTemplate.keys("coupon:issue:queue:*")).thenReturn(setOf(queueKey))
        whenever(listOperations.rightPop(queueKey)).thenReturn(jsonString)
        whenever(objectMapper.readValue(jsonString, CouponIssueRequest::class.java)).thenReturn(request)

        // when
        val result = couponIssueQueueService.getNextCouponIssueRequest()

        // then
        assertNotNull(result)
        assertEquals(userId, result!!.userId)
        assertEquals(couponId, result.couponId)

        verify(redisTemplate).keys("coupon:issue:queue:*")
        verify(listOperations).rightPop(queueKey)
        verify(objectMapper).readValue(jsonString, CouponIssueRequest::class.java)
    }

    @Test
    @DisplayName("대기열이 비어있을 때 null 반환")
    fun `대기열이 비어있을 때 null 반환`() {
        // given
        val queueKey = "coupon:issue:queue:100"
        whenever(redisTemplate.keys("coupon:issue:queue:*")).thenReturn(setOf(queueKey))
        whenever(listOperations.rightPop(queueKey)).thenReturn(null)

        // when
        val result = couponIssueQueueService.getNextCouponIssueRequest()

        // then
        assertNull(result)

        verify(redisTemplate).keys("coupon:issue:queue:*")
        verify(listOperations).rightPop(queueKey)
        verify(objectMapper, never()).readValue(any<String>(), eq(CouponIssueRequest::class.java))
    }

    @Test
    @DisplayName("JSON 파싱 실패 시 다음 요청 처리")
    fun `JSON 파싱 실패 시 다음 요청 처리`() {
        // given
        val queueKey1 = "coupon:issue:queue:100"
        val queueKey2 = "coupon:issue:queue:200"
        val invalidJsonString = "invalid-json"
        val userId = 1L
        val couponId = 200L
        val validRequest = CouponIssueRequest.create(userId, couponId)
        val validJsonString = """{"userId":1,"couponId":200,"requestId":"${validRequest.requestId}","timestamp":${validRequest.timestamp}}"""

        whenever(redisTemplate.keys("coupon:issue:queue:*")).thenReturn(setOf(queueKey1, queueKey2))
        whenever(listOperations.rightPop(queueKey1)).thenReturn(invalidJsonString)
        whenever(listOperations.rightPop(queueKey2)).thenReturn(validJsonString)
        whenever(objectMapper.readValue(invalidJsonString, CouponIssueRequest::class.java))
            .thenThrow(RuntimeException("JSON 파싱 실패"))
        whenever(objectMapper.readValue(validJsonString, CouponIssueRequest::class.java))
            .thenReturn(validRequest)

        // when
        val result = couponIssueQueueService.getNextCouponIssueRequest()

        // then
        assertNotNull(result)
        assertEquals(userId, result!!.userId)
        assertEquals(couponId, result.couponId)

        verify(redisTemplate).keys("coupon:issue:queue:*")
        verify(listOperations).rightPop(queueKey1)
        verify(listOperations).rightPop(queueKey2)
        verify(objectMapper).readValue(invalidJsonString, CouponIssueRequest::class.java)
        verify(objectMapper).readValue(validJsonString, CouponIssueRequest::class.java)
    }

    @Test
    @DisplayName("특정 쿠폰의 대기열 크기 조회")
    fun `특정 쿠폰의 대기열 크기 조회`() {
        // given
        val couponId = 100L
        val queueKey = "coupon:issue:queue:$couponId"
        val expectedSize = 5L

        whenever(listOperations.size(queueKey)).thenReturn(expectedSize)

        // when
        val result = couponIssueQueueService.getQueueSize(couponId)

        // then
        assertEquals(expectedSize, result)

        verify(listOperations).size(queueKey)
    }

    @Test
    @DisplayName("대기열 크기 조회 시 null 반환되면 0 반환")
    fun `대기열 크기 조회 시 null 반환되면 0 반환`() {
        // given
        val couponId = 100L
        val queueKey = "coupon:issue:queue:$couponId"

        whenever(listOperations.size(queueKey)).thenReturn(null)

        // when
        val result = couponIssueQueueService.getQueueSize(couponId)

        // then
        assertEquals(0L, result)

        verify(listOperations).size(queueKey)
    }

    @Test
    @DisplayName("대기열 키가 없을 때 빈 결과 반환")
    fun `대기열 키가 없을 때 빈 결과 반환`() {
        // given
        whenever(redisTemplate.keys("coupon:issue:queue:*")).thenReturn(emptySet())

        // when
        val result = couponIssueQueueService.getNextCouponIssueRequest()

        // then
        assertNull(result)

        verify(redisTemplate).keys("coupon:issue:queue:*")
    }
}
