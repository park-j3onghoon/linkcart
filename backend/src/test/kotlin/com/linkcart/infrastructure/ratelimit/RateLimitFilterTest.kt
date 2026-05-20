package com.linkcart.infrastructure.ratelimit

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RateLimitFilterTest {

    private val objectMapper = ObjectMapper()

    private fun newFilter(
        rateLimiter: RateLimiter,
        paths: Set<String> = setOf(RateLimitFilter.PARSE_PATH, RateLimitFilter.SHARELISTS_LOOKUP_PATH),
    ): RateLimitFilter = RateLimitFilter(rateLimiter, objectMapper, paths)

    @Test
    fun `non-protected path skips the bucket`() {
        val limiter = mock(RateLimiter::class.java)
        val request = MockHttpServletRequest("GET", "/health")
        val response = MockHttpServletResponse()
        val chain = mock(FilterChain::class.java)

        newFilter(limiter).doFilter(request, response, chain)

        verify(chain).doFilter(request, response)
        verify(limiter, never()).tryConsume(org.mockito.ArgumentMatchers.anyString())
    }

    @Test
    fun `consumes token when path matches and passes through on success`() {
        val limiter = mock(RateLimiter::class.java)
        given(limiter.tryConsume("1.2.3.4")).willReturn(true)
        val request = MockHttpServletRequest("POST", "/api/v1/products:parse").apply {
            remoteAddr = "1.2.3.4"
        }
        val response = MockHttpServletResponse()
        val chain = mock(FilterChain::class.java)

        newFilter(limiter).doFilter(request, response, chain)

        verify(limiter).tryConsume("1.2.3.4")
        verify(chain).doFilter(request, response)
    }

    @Test
    fun `returns 429 with AIP-193 body when bucket is empty`() {
        val limiter = mock(RateLimiter::class.java)
        given(limiter.tryConsume("9.9.9.9")).willReturn(false)
        val request = MockHttpServletRequest("POST", "/api/v1/products:parse").apply {
            remoteAddr = "9.9.9.9"
        }
        val response = MockHttpServletResponse()
        val chain = mock(FilterChain::class.java)

        newFilter(limiter).doFilter(request, response, chain)

        assertEquals(429, response.status)
        assertEquals("application/json", response.contentType?.substringBefore(";"))
        val body = response.contentAsString
        assertTrue(body.contains("\"code\":\"RESOURCE_EXHAUSTED\""))
        verify(chain, never()).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())
    }

    @Test
    fun `uses first X-Forwarded-For entry as client key`() {
        val limiter = mock(RateLimiter::class.java)
        given(limiter.tryConsume("203.0.113.1")).willReturn(true)
        val request = MockHttpServletRequest("POST", "/api/v1/products:parse").apply {
            addHeader("X-Forwarded-For", "203.0.113.1, 10.0.0.1")
            remoteAddr = "10.0.0.2"
        }

        newFilter(limiter).doFilter(request, MockHttpServletResponse(), mock(FilterChain::class.java))

        verify(limiter).tryConsume("203.0.113.1")
    }

    @Test
    fun `blank X-Forwarded-For falls through to remoteAddr`() {
        val limiter = mock(RateLimiter::class.java)
        given(limiter.tryConsume("10.0.0.1")).willReturn(true)
        val request = MockHttpServletRequest("POST", "/api/v1/products:parse").apply {
            addHeader("X-Forwarded-For", "  ")
            remoteAddr = "10.0.0.1"
        }

        newFilter(limiter).doFilter(request, MockHttpServletResponse(), mock(FilterChain::class.java))

        verify(limiter).tryConsume("10.0.0.1")
    }

    @Test
    fun `protects shareLists lookup path too`() {
        val limiter = mock(RateLimiter::class.java)
        given(limiter.tryConsume("8.8.8.8")).willReturn(true)
        val request = MockHttpServletRequest("POST", "/api/v1/shareLists:lookup").apply {
            remoteAddr = "8.8.8.8"
        }

        newFilter(limiter).doFilter(request, MockHttpServletResponse(), mock(FilterChain::class.java))

        verify(limiter).tryConsume("8.8.8.8")
    }
}
