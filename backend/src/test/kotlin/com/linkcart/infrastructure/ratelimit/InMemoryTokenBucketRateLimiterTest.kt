package com.linkcart.infrastructure.ratelimit

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InMemoryTokenBucketRateLimiterTest {

    @Test
    fun `allows requests up to capacity, then denies`() {
        val limiter = InMemoryTokenBucketRateLimiter(
            capacity = 3,
            refillPerSecond = 1.0,
            clock = fixedClock("2026-04-24T00:00:00Z"),
        )

        assertTrue(limiter.tryConsume("ip-1"))
        assertTrue(limiter.tryConsume("ip-1"))
        assertTrue(limiter.tryConsume("ip-1"))
        assertFalse(limiter.tryConsume("ip-1"))
    }

    @Test
    fun `keys are isolated`() {
        val limiter = InMemoryTokenBucketRateLimiter(
            capacity = 1,
            refillPerSecond = 1.0,
            clock = fixedClock("2026-04-24T00:00:00Z"),
        )

        assertTrue(limiter.tryConsume("ip-1"))
        assertFalse(limiter.tryConsume("ip-1"))
        assertTrue(limiter.tryConsume("ip-2"))
    }

    @Test
    fun `refills tokens based on elapsed time`() {
        val mutableClock = MutableClock(Instant.parse("2026-04-24T00:00:00Z"))
        val limiter = InMemoryTokenBucketRateLimiter(
            capacity = 2,
            refillPerSecond = 1.0,  // 1 token per second
            clock = mutableClock,
        )

        assertTrue(limiter.tryConsume("ip-1"))
        assertTrue(limiter.tryConsume("ip-1"))
        assertFalse(limiter.tryConsume("ip-1"))

        mutableClock.advanceMillis(1100L)  // +1.1s → +1 token
        assertTrue(limiter.tryConsume("ip-1"))
        assertFalse(limiter.tryConsume("ip-1"))
    }

    @Test
    fun `refill is capped at capacity`() {
        val mutableClock = MutableClock(Instant.parse("2026-04-24T00:00:00Z"))
        val limiter = InMemoryTokenBucketRateLimiter(
            capacity = 2,
            refillPerSecond = 1.0,
            clock = mutableClock,
        )

        assertTrue(limiter.tryConsume("ip-1"))
        mutableClock.advanceMillis(10_000L)  // long wait → capped at 2, not 10

        assertTrue(limiter.tryConsume("ip-1"))
        assertTrue(limiter.tryConsume("ip-1"))
        assertFalse(limiter.tryConsume("ip-1"))
    }

    @Test
    fun `rejects non-positive capacity or refill`() {
        assertThrows<IllegalArgumentException> {
            InMemoryTokenBucketRateLimiter(capacity = 0, refillPerSecond = 1.0)
        }
        assertThrows<IllegalArgumentException> {
            InMemoryTokenBucketRateLimiter(capacity = 1, refillPerSecond = 0.0)
        }
    }

    private fun fixedClock(iso: String): Clock =
        Clock.fixed(Instant.parse(iso), ZoneOffset.UTC)

    private class MutableClock(private var current: Instant) : Clock() {
        fun advanceMillis(delta: Long) {
            current = current.plusMillis(delta)
        }

        override fun getZone() = ZoneOffset.UTC
        override fun withZone(zone: java.time.ZoneId?): Clock = this
        override fun instant(): Instant = current
        override fun millis(): Long = current.toEpochMilli()
    }
}
