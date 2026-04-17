package com.linkcart.presentation.api

import com.linkcart.presentation.dto.ErrorResponse
import jakarta.validation.ConstraintViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ResponseStatusException

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValid(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val firstMessage = ex.bindingResult.allErrors.firstOrNull()?.let { error ->
            if (error is FieldError) {
                error.defaultMessage
            } else {
                error.defaultMessage
            }
        } ?: "요청이 올바르지 않습니다"

        return ResponseEntity
            .badRequest()
            .body(ErrorResponse(code = "validation_error", message = firstMessage))
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(ex: ConstraintViolationException): ResponseEntity<ErrorResponse> {
        val message = ex.constraintViolations.firstOrNull()?.message ?: "요청이 올바르지 않습니다"
        return ResponseEntity
            .badRequest()
            .body(ErrorResponse(code = "validation_error", message = message))
    }

    @ExceptionHandler(MissingServletRequestParameterException::class, HttpMessageNotReadableException::class)
    fun handleBadRequest(ex: Exception): ResponseEntity<ErrorResponse> {
        val message = when (ex) {
            is MissingServletRequestParameterException -> "${ex.parameterName} 파라미터가 필요합니다"
            else -> "요청 본문이 올바르지 않습니다"
        }

        return ResponseEntity
            .badRequest()
            .body(ErrorResponse(code = "invalid_request", message = message))
    }

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatusException(ex: ResponseStatusException): ResponseEntity<ErrorResponse> {
        val code = when (ex.statusCode) {
            HttpStatus.BAD_REQUEST -> "invalid_request"
            HttpStatus.BAD_GATEWAY -> "upstream_error"
            else -> "request_failed"
        }

        return ResponseEntity
            .status(ex.statusCode)
            .body(ErrorResponse(code = code, message = ex.reason ?: "요청 처리에 실패했습니다"))
    }
}
