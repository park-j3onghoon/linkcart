package com.linkcart.infrastructure.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.linkcart.infrastructure.ratelimit.InMemoryTokenBucketRateLimiter
import com.linkcart.infrastructure.ratelimit.RateLimitFilter
import com.linkcart.infrastructure.ratelimit.RateLimiter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RateLimitConfig {

    @Bean
    fun parseRateLimiter(
        @Value("\${linkcart.rate-limit.parse.capacity:30}") capacity: Int,
        @Value("\${linkcart.rate-limit.parse.refill-per-second:0.5}") refillPerSecond: Double,
    ): RateLimiter = InMemoryTokenBucketRateLimiter(
        capacity = capacity,
        refillPerSecond = refillPerSecond,
    )

    @Bean
    fun rateLimitFilter(
        parseRateLimiter: RateLimiter,
        objectMapper: ObjectMapper,
    ): RateLimitFilter = RateLimitFilter(
        parseRateLimiter = parseRateLimiter,
        objectMapper = objectMapper,
    )
}
