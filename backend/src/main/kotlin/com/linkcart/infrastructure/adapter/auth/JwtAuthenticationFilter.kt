package com.linkcart.infrastructure.adapter.auth

import com.linkcart.application.auth.port.AccessTokenIssuer
import com.linkcart.application.auth.port.InvalidAccessTokenException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.web.filter.OncePerRequestFilter

class JwtAuthenticationFilter(
    private val accessTokenIssuer: AccessTokenIssuer,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val token = extractBearerToken(request)
        if (token != null && SecurityContextHolder.getContext().authentication == null) {
            try {
                val userId = accessTokenIssuer.verify(token)
                val authentication = UsernamePasswordAuthenticationToken(userId, null, emptyList()).apply {
                    details = WebAuthenticationDetailsSource().buildDetails(request)
                }
                SecurityContextHolder.getContext().authentication = authentication
            } catch (_: InvalidAccessTokenException) {
                // SecurityContext 비움 → authenticationEntryPoint가 401 반환
                SecurityContextHolder.clearContext()
            }
        }
        filterChain.doFilter(request, response)
    }

    private fun extractBearerToken(request: HttpServletRequest): String? {
        val header = request.getHeader(HttpHeaders.AUTHORIZATION) ?: return null
        if (!header.startsWith(BEARER_PREFIX)) return null
        return header.substring(BEARER_PREFIX.length).trim().ifBlank { null }
    }

    companion object {
        private const val BEARER_PREFIX = "Bearer "
    }
}
