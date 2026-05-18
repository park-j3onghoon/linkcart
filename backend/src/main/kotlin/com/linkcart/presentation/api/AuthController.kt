package com.linkcart.presentation.api

import com.linkcart.application.auth.port.GoogleOAuthException
import com.linkcart.application.auth.usecase.InvalidRefreshTokenException
import com.linkcart.application.auth.usecase.IssueTokensUsecase
import com.linkcart.application.auth.usecase.LoginWithGoogleUsecase
import com.linkcart.application.auth.usecase.LogoutUsecase
import com.linkcart.application.auth.usecase.RefreshTokensUsecase
import com.linkcart.domain.port.UserRepository
import com.linkcart.presentation.dto.LogoutRequest
import com.linkcart.presentation.dto.MeResponse
import com.linkcart.presentation.dto.OAuthLoginRequest
import com.linkcart.presentation.dto.OAuthLoginResponse
import com.linkcart.presentation.dto.RefreshRequest
import com.linkcart.presentation.dto.RefreshResponse
import com.linkcart.presentation.dto.UserResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.time.Clock
import java.time.Duration
import java.time.Instant

@RestController
class AuthController(
    private val loginWithGoogleUsecase: LoginWithGoogleUsecase,
    private val refreshTokensUsecase: RefreshTokensUsecase,
    private val logoutUsecase: LogoutUsecase,
    private val userRepository: UserRepository,
    private val clock: Clock = Clock.systemUTC(),
) {

    @PostMapping("/api/v1/auth/oauth/google")
    fun loginWithGoogle(@Valid @RequestBody request: OAuthLoginRequest): ResponseEntity<OAuthLoginResponse> {
        val result = try {
            loginWithGoogleUsecase.execute(code = request.code, redirectUri = request.redirectUri)
        } catch (e: GoogleOAuthException) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google 인증에 실패했습니다", e)
        }

        return ResponseEntity.ok(
            OAuthLoginResponse(
                accessToken = result.tokens.accessToken.token,
                refreshToken = result.tokens.rawRefreshToken,
                tokenType = "Bearer",
                expiresIn = expiresInSeconds(result.tokens.accessToken.expiresAt),
                user = UserResponse.from(result.user),
            ),
        )
    }

    @PostMapping("/api/v1/auth/tokens:refresh")
    fun refresh(@Valid @RequestBody request: RefreshRequest): ResponseEntity<RefreshResponse> {
        val tokens = try {
            refreshTokensUsecase.execute(request.refreshToken)
        } catch (e: InvalidRefreshTokenException) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token이 유효하지 않습니다", e)
        }

        return ResponseEntity.ok(
            RefreshResponse(
                accessToken = tokens.accessToken.token,
                refreshToken = tokens.rawRefreshToken,
                tokenType = "Bearer",
                expiresIn = expiresInSeconds(tokens.accessToken.expiresAt),
            ),
        )
    }

    @PostMapping("/api/v1/auth/tokens:revoke")
    fun logout(@Valid @RequestBody request: LogoutRequest): ResponseEntity<Void> {
        logoutUsecase.execute(request.refreshToken)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/api/v1/auth/me")
    fun me(@AuthenticationPrincipal userId: Long): ResponseEntity<MeResponse> {
        val user = userRepository.findById(userId)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다")
        return ResponseEntity.ok(MeResponse(user = UserResponse.from(user)))
    }

    private fun expiresInSeconds(expiresAt: Instant): Long =
        Duration.between(Instant.now(clock), expiresAt).seconds
}
