package com.linkcart.presentation.api

import com.linkcart.application.error.ErrorCode
import com.linkcart.application.error.ErrorDetail
import com.linkcart.application.error.ErrorResponse
import com.linkcart.application.error.FieldViolation
import jakarta.validation.ConstraintViolationException
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
        val fieldViolations = ex.bindingResult.allErrors.mapNotNull { error ->
            (error as? FieldError)?.let {
                FieldViolation(
                    field = it.field,
                    description = it.defaultMessage ?: "유효하지 않은 값입니다",
                )
            }
        }
        val message = fieldViolations.firstOrNull()?.description
            ?: ex.bindingResult.allErrors.firstOrNull()?.defaultMessage
            ?: "요청이 올바르지 않습니다"
        return ResponseEntity
            .badRequest()
            .body(
                ErrorResponse(
                    code = ErrorCode.INVALID_ARGUMENT,
                    message = message,
                    details = fieldViolations.takeIf { it.isNotEmpty() }
                        ?.let { listOf(ErrorDetail(type = "BadRequest", fieldViolations = it)) },
                ),
            )
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(ex: ConstraintViolationException): ResponseEntity<ErrorResponse> {
        val fieldViolations = ex.constraintViolations.map {
            FieldViolation(
                field = it.propertyPath.toString(),
                description = it.message,
            )
        }
        val message = fieldViolations.firstOrNull()?.description ?: "요청이 올바르지 않습니다"
        return ResponseEntity
            .badRequest()
            .body(
                ErrorResponse(
                    code = ErrorCode.INVALID_ARGUMENT,
                    message = message,
                    details = fieldViolations.takeIf { it.isNotEmpty() }
                        ?.let { listOf(ErrorDetail(type = "BadRequest", fieldViolations = it)) },
                ),
            )
    }

    @ExceptionHandler(MissingServletRequestParameterException::class, HttpMessageNotReadableException::class)
    fun handleBadRequest(ex: Exception): ResponseEntity<ErrorResponse> {
        val message = when (ex) {
            is MissingServletRequestParameterException -> "${ex.parameterName} 파라미터가 필요합니다"
            else -> "요청 본문이 올바르지 않습니다"
        }
        return ResponseEntity
            .badRequest()
            .body(ErrorResponse(code = ErrorCode.INVALID_ARGUMENT, message = message))
    }

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatusException(ex: ResponseStatusException): ResponseEntity<ErrorResponse> {
        val code = ErrorCode.fromHttpStatus(ex.statusCode)
        return ResponseEntity
            .status(ex.statusCode)
            .body(ErrorResponse(code = code, message = ex.reason ?: "요청 처리에 실패했습니다"))
    }
}
