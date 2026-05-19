package com.linkcart.infrastructure.config.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.linkcart.application.error.ErrorCode
import com.linkcart.application.error.ErrorResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint

class JsonAuthenticationEntryPoint(
    private val objectMapper: ObjectMapper,
) : AuthenticationEntryPoint {

    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException,
    ) {
        response.status = HttpStatus.UNAUTHORIZED.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        val body = ErrorResponse(code = ErrorCode.UNAUTHENTICATED, message = "인증이 필요합니다")
        response.writer.write(objectMapper.writeValueAsString(body))
    }
}
