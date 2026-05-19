package com.linkcart.infrastructure.ratelimit

import com.fasterxml.jackson.databind.ObjectMapper
import com.linkcart.application.error.ErrorCode
import com.linkcart.application.error.ErrorResponse
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.filter.OncePerRequestFilter

class RateLimitFilter(
    private val parseRateLimiter: RateLimiter,
    private val objectMapper: ObjectMapper,
    private val pathPrefixes: Set<String> = setOf(PARSE_PATH),
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val path = request.requestURI
        if (pathPrefixes.none { path == it || path.startsWith("$it/") }) {
            filterChain.doFilter(request, response)
            return
        }

        val key = clientKey(request)
        if (parseRateLimiter.tryConsume(key)) {
            filterChain.doFilter(request, response)
            return
        }

        response.status = HttpStatus.TOO_MANY_REQUESTS.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()
        objectMapper.writeValue(
            response.writer,
            ErrorResponse(
                code = ErrorCode.RESOURCE_EXHAUSTED,
                message = "요청이 너무 많습니다. 잠시 후 다시 시도해주세요.",
            ),
        )
    }

    private fun clientKey(request: HttpServletRequest): String {
        val forwarded = request.getHeader("X-Forwarded-For")?.split(",")?.firstOrNull()?.trim()
        return if (!forwarded.isNullOrBlank()) forwarded else request.remoteAddr ?: "unknown"
    }

    companion object {
        const val PARSE_PATH = "/api/v1/products:parse"
    }
}
