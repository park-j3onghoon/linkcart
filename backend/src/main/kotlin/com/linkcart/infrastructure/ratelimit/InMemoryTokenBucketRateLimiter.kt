package com.linkcart.infrastructure.ratelimit

import java.time.Clock
import java.util.concurrent.ConcurrentHashMap

class InMemoryTokenBucketRateLimiter(
    private val capacity: Int,
    private val refillPerSecond: Double,
    private val clock: Clock = Clock.systemUTC(),
) : RateLimiter {

    init {
        require(capacity > 0) { "capacity must be > 0, got $capacity" }
        require(refillPerSecond > 0) { "refillPerSecond must be > 0, got $refillPerSecond" }
    }

    private val buckets = ConcurrentHashMap<String, TokenBucket>()

    override fun tryConsume(key: String): Boolean {
        val bucket = buckets.computeIfAbsent(key) { TokenBucket() }
        return bucket.tryConsume()
    }

    private inner class TokenBucket {
        private var tokens: Double = capacity.toDouble()
        private var lastRefillMillis: Long = clock.millis()

        @Synchronized
        fun tryConsume(): Boolean {
            refill()
            if (tokens < 1.0) return false
            tokens -= 1.0
            return true
        }

        private fun refill() {
            val now = clock.millis()
            val elapsedSeconds = (now - lastRefillMillis).coerceAtLeast(0) / 1000.0
            if (elapsedSeconds <= 0) return
            tokens = (tokens + elapsedSeconds * refillPerSecond).coerceAtMost(capacity.toDouble())
            lastRefillMillis = now
        }
    }
}
