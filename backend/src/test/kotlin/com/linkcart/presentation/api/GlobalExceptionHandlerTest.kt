package com.linkcart.presentation.api

import com.linkcart.application.auth.port.GoogleOAuthException
import com.linkcart.application.auth.usecase.InvalidRefreshTokenException
import com.linkcart.application.error.ErrorCode
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
import jakarta.validation.ConstraintViolation
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Path
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.server.ResponseStatusException
import kotlin.test.assertEquals

class GlobalExceptionHandlerTest {

    private val sut = GlobalExceptionHandler()

    @Test
    fun `handleConstraintViolation maps to INVALID_ARGUMENT with field violations`() {
        val path = mock(Path::class.java).also { given(it.toString()).willReturn("execute.pageSize") }
        val violation = mock(ConstraintViolation::class.java).also {
            given(it.propertyPath).willReturn(path)
            given(it.message).willReturn("must be positive")
        }
        val ex = ConstraintViolationException("invalid", setOf(violation))

        val response = sut.handleConstraintViolation(ex)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        val body = response.body!!
        assertEquals(ErrorCode.INVALID_ARGUMENT, body.code)
        assertEquals("must be positive", body.message)
        assertEquals("BadRequest", body.details!!.single().type)
        assertEquals("execute.pageSize", body.details!!.single().fieldViolations!!.single().field)
    }

    @Test
    fun `handleConstraintViolation falls back when violations are empty`() {
        val response = sut.handleConstraintViolation(ConstraintViolationException("x", emptySet()))

        assertEquals("요청이 올바르지 않습니다", response.body!!.message)
    }

    @Test
    fun `handleBadRequest names missing param`() {
        val ex = MissingServletRequestParameterException("token", "String")

        val response = sut.handleBadRequest(ex)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals(ErrorCode.INVALID_ARGUMENT, response.body!!.code)
        assertEquals("token 파라미터가 필요합니다", response.body!!.message)
    }

    @Test
    fun `handleBadRequest reports body parse failure with generic message`() {
        val response = sut.handleBadRequest(HttpMessageNotReadableException("malformed"))

        assertEquals("요청 본문이 올바르지 않습니다", response.body!!.message)
    }

    @Test
    fun `handleNotFound for ShareListNotFoundException uses exception message`() {
        val response = sut.handleNotFound(ShareListNotFoundException("리스트 없음"))

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        assertEquals("리스트 없음", response.body!!.message)
    }

    @Test
    fun `handleNotFound for UserProductNotFoundException maps to NOT_FOUND`() {
        val response = sut.handleNotFound(UserProductNotFoundException("상품 없음"))

        assertEquals(ErrorCode.NOT_FOUND, response.body!!.code)
    }

    @Test
    fun `handleInvalidArgumentDomain covers EmptyShareListException`() {
        val response = sut.handleInvalidArgumentDomain(EmptyShareListException("비었음"))

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals(ErrorCode.INVALID_ARGUMENT, response.body!!.code)
        assertEquals("비었음", response.body!!.message)
    }

    @Test
    fun `handleInvalidArgumentDomain covers InvalidPageTokenException`() {
        val response = sut.handleInvalidArgumentDomain(InvalidPageTokenException("토큰 깨짐"))

        assertEquals("토큰 깨짐", response.body!!.message)
    }

    @Test
    fun `handleInvalidArgumentDomain covers InvalidPageSizeException`() {
        val response = sut.handleInvalidArgumentDomain(InvalidPageSizeException("사이즈 초과"))

        assertEquals("사이즈 초과", response.body!!.message)
    }

    @Test
    fun `handleInvalidArgumentDomain covers UnsafeImageUrlException`() {
        val response = sut.handleInvalidArgumentDomain(UnsafeImageUrlException("internal IP"))

        assertEquals("internal IP", response.body!!.message)
    }

    @Test
    fun `handleAlreadyExists maps DuplicateUserProductException to 409`() {
        val response = sut.handleAlreadyExists(DuplicateUserProductException("중복"))

        assertEquals(HttpStatus.CONFLICT, response.statusCode)
        assertEquals(ErrorCode.ALREADY_EXISTS, response.body!!.code)
    }

    @Test
    fun `handleUpstreamUnavailable maps ImageFetchFailedException to 503`() {
        val response = sut.handleUpstreamUnavailable(ImageFetchFailedException("타임아웃"))

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.statusCode)
        assertEquals(ErrorCode.UNAVAILABLE, response.body!!.code)
    }

    @Test
    fun `handleUpstreamUnavailable maps UnsupportedImageFormatException to 503`() {
        val response = sut.handleUpstreamUnavailable(UnsupportedImageFormatException("svg"))

        assertEquals(ErrorCode.UNAVAILABLE, response.body!!.code)
    }

    @Test
    fun `handleGoogleOAuth maps to UNAUTHENTICATED with fixed message`() {
        val response = sut.handleGoogleOAuth(GoogleOAuthException("internal detail"))

        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        assertEquals("Google 인증에 실패했습니다", response.body!!.message)
    }

    @Test
    fun `handleInvalidRefreshToken maps to UNAUTHENTICATED with fixed message`() {
        val response = sut.handleInvalidRefreshToken(InvalidRefreshTokenException("reuse"))

        assertEquals("Refresh token이 유효하지 않습니다", response.body!!.message)
    }

    @Test
    fun `handleUnauthenticated uses exception message when present`() {
        val response = sut.handleUnauthenticated(UnauthenticatedException("미인증"))

        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        assertEquals("미인증", response.body!!.message)
    }

    @Test
    fun `handleResponseStatusException maps status via ErrorCode fromHttpStatus`() {
        val response = sut.handleResponseStatusException(
            ResponseStatusException(HttpStatus.FORBIDDEN, "권한 없음"),
        )

        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
        assertEquals(ErrorCode.PERMISSION_DENIED, response.body!!.code)
        assertEquals("권한 없음", response.body!!.message)
    }

    @Test
    fun `handleUncaught hides internal message and returns INTERNAL`() {
        val response = sut.handleUncaught(IllegalStateException("내부 상세 — 노출 금지"))

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertEquals(ErrorCode.INTERNAL, response.body!!.code)
        assertEquals("서버 오류가 발생했습니다", response.body!!.message)
    }
}
