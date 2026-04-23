package com.linkcart.infrastructure.ratelimit

interface RateLimiter {
    fun tryConsume(key: String): Boolean
}
