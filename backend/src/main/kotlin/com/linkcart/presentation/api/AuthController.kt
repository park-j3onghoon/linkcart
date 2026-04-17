package com.linkcart.presentation.api

import com.linkcart.application.auth.port.GoogleOAuthException
import com.linkcart.application.auth.usecase.LoginWithGoogleUsecase
import com.linkcart.domain.port.UserRepository
import com.linkcart.presentation.dto.MeResponse
import com.linkcart.presentation.dto.OAuthLoginRequest
import com.linkcart.presentation.dto.OAuthLoginResponse
import com.linkcart.presentation.dto.UserResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.time.Clock
import java.time.Instant

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val loginWithGoogleUsecase: LoginWithGoogleUsecase,
    private val userRepository: UserRepository,
    private val clock: Clock = Clock.systemUTC(),
) {

    @PostMapping("/oauth/google")
    fun loginWithGoogle(@Valid @RequestBody request: OAuthLoginRequest): ResponseEntity<OAuthLoginResponse> {
        val result = try {
            loginWithGoogleUsecase.execute(code = request.code, redirectUri = request.redirectUri)
        } catch (e: GoogleOAuthException) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google 인증에 실패했습니다", e)
        }

        val expiresIn = java.time.Duration.between(Instant.now(clock), result.accessToken.expiresAt).seconds

        return ResponseEntity.ok(
            OAuthLoginResponse(
                accessToken = result.accessToken.token,
                tokenType = "Bearer",
                expiresIn = expiresIn,
                user = UserResponse.from(result.user),
            ),
        )
    }

    @GetMapping("/me")
    fun me(@AuthenticationPrincipal userId: Long): ResponseEntity<MeResponse> {
        val user = userRepository.findById(userId)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "사용자를 찾을 수 없습니다")
        return ResponseEntity.ok(MeResponse(user = UserResponse.from(user)))
    }
}
