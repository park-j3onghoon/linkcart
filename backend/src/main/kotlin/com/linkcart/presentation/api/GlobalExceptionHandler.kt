package com.linkcart.presentation.api

import com.linkcart.application.auth.port.GoogleOAuthException
import com.linkcart.application.auth.usecase.InvalidRefreshTokenException
import com.linkcart.application.error.ErrorCode
import com.linkcart.application.error.ErrorDetail
import com.linkcart.application.error.ErrorResponse
import com.linkcart.application.error.FieldViolation
import com.linkcart.application.image.usecase.ImageFetchFailedException
import com.linkcart.application.image.usecase.UnsafeImageUrlException
import com.linkcart.application.image.usecase.UnsupportedImageFormatException
import com.linkcart.domain.model.EmptyShareListException
import com.linkcart.application.share.usecase.ShareListNotFoundException
import com.linkcart.application.user.usecase.DuplicateUserProductException
import com.linkcart.application.user.usecase.InvalidPageSizeException
import com.linkcart.application.user.usecase.InvalidPageTokenException
import com.linkcart.application.user.usecase.UnauthenticatedException
import com.linkcart.application.user.usecase.UserProductNotFoundException
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
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

    private val log = LoggerFactory.getLogger(javaClass)

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
        val fallbackMessage = ex.bindingResult.allErrors.firstOrNull()?.defaultMessage
        return badRequestWithFieldViolations(fieldViolations, fallbackMessage)
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(ex: ConstraintViolationException): ResponseEntity<ErrorResponse> {
        val fieldViolations = ex.constraintViolations.map {
            FieldViolation(field = it.propertyPath.toString(), description = it.message)
        }
        return badRequestWithFieldViolations(fieldViolations, fallbackMessage = null)
    }

    private fun badRequestWithFieldViolations(
        fieldViolations: List<FieldViolation>,
        fallbackMessage: String?,
    ): ResponseEntity<ErrorResponse> {
        val message = fieldViolations.firstOrNull()?.description
            ?: fallbackMessage
            ?: "요청이 올바르지 않습니다"
        val details = fieldViolations.takeIf { it.isNotEmpty() }
            ?.let { listOf(ErrorDetail(type = "BadRequest", fieldViolations = it)) }
        return ResponseEntity
            .badRequest()
            .body(ErrorResponse(code = ErrorCode.INVALID_ARGUMENT, message = message, details = details))
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

    @ExceptionHandler(ShareListNotFoundException::class, UserProductNotFoundException::class)
    fun handleNotFound(ex: RuntimeException): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(ErrorCode.NOT_FOUND.httpStatus)
            .body(ErrorResponse(code = ErrorCode.NOT_FOUND, message = ex.message ?: "리소스를 찾을 수 없습니다"))

    @ExceptionHandler(
        EmptyShareListException::class,
        InvalidPageTokenException::class,
        InvalidPageSizeException::class,
        UnsafeImageUrlException::class,
    )
    fun handleInvalidArgumentDomain(ex: RuntimeException): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .badRequest()
            .body(ErrorResponse(code = ErrorCode.INVALID_ARGUMENT, message = ex.message ?: "요청이 올바르지 않습니다"))

    @ExceptionHandler(DuplicateUserProductException::class)
    fun handleAlreadyExists(ex: DuplicateUserProductException): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(ErrorCode.ALREADY_EXISTS.httpStatus)
            .body(ErrorResponse(code = ErrorCode.ALREADY_EXISTS, message = ex.message ?: "이미 존재하는 리소스입니다"))

    @ExceptionHandler(ImageFetchFailedException::class, UnsupportedImageFormatException::class)
    fun handleUpstreamUnavailable(ex: RuntimeException): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(ErrorCode.UNAVAILABLE.httpStatus)
            .body(ErrorResponse(code = ErrorCode.UNAVAILABLE, message = ex.message ?: "외부 서비스 오류"))

    @ExceptionHandler(GoogleOAuthException::class)
    fun handleGoogleOAuth(ex: GoogleOAuthException): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(ErrorCode.UNAUTHENTICATED.httpStatus)
            .body(ErrorResponse(code = ErrorCode.UNAUTHENTICATED, message = "Google 인증에 실패했습니다"))

    @ExceptionHandler(InvalidRefreshTokenException::class)
    fun handleInvalidRefreshToken(ex: InvalidRefreshTokenException): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(ErrorCode.UNAUTHENTICATED.httpStatus)
            .body(ErrorResponse(code = ErrorCode.UNAUTHENTICATED, message = "Refresh token이 유효하지 않습니다"))

    @ExceptionHandler(UnauthenticatedException::class)
    fun handleUnauthenticated(ex: UnauthenticatedException): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(ErrorCode.UNAUTHENTICATED.httpStatus)
            .body(ErrorResponse(code = ErrorCode.UNAUTHENTICATED, message = ex.message ?: "인증이 필요합니다"))

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatusException(ex: ResponseStatusException): ResponseEntity<ErrorResponse> {
        val code = ErrorCode.fromHttpStatus(ex.statusCode)
        return ResponseEntity
            .status(ex.statusCode)
            .body(ErrorResponse(code = code, message = ex.reason ?: "요청 처리에 실패했습니다"))
    }

    /**
     * 마지막 안전망. unchecked 예외가 컨트롤러 밖으로 새지 않도록 INTERNAL로 변환.
     * 원본 메시지는 절대 클라이언트에 노출하지 않고 로깅으로만 보존한다.
     */
    @ExceptionHandler(Exception::class)
    fun handleUncaught(ex: Exception): ResponseEntity<ErrorResponse> {
        log.error("Unhandled exception leaked to GlobalExceptionHandler", ex)
        return ResponseEntity
            .status(ErrorCode.INTERNAL.httpStatus)
            .body(ErrorResponse(code = ErrorCode.INTERNAL, message = "서버 오류가 발생했습니다"))
    }
}
